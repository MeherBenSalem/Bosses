"""Deploy only the built static site. Credentials are read from an external SSH note."""
import argparse, datetime, re, tarfile, tempfile
from pathlib import Path
import paramiko

parser=argparse.ArgumentParser()
parser.add_argument('--connection-file',required=True)
parser.add_argument('--https',action='store_true',help='Issue certificate after DNS resolves and switch this vhost to HTTPS')
args=parser.parse_args()
site=Path(__file__).resolve().parents[1]
note=Path(args.connection_file).read_text();user,host=re.search(r'ssh\s+([\w.-]+)@([\d.]+)',note).groups()
client=paramiko.SSHClient();client.load_system_host_keys();client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect(host,username=user,password=note.strip().splitlines()[-1].strip(),timeout=20)
with client.open_sftp() as sftp:
    try:
        with sftp.file('/etc/nginx/sites-available/docs.nightbeam.dev') as f:existing_config=f.read().decode()
    except FileNotFoundError:existing_config=''
    if existing_config and '/var/www/remnant-codex/' not in existing_config:
        raise RuntimeError('The requested hostname already has an unrelated Nginx configuration; deployment stopped.')
def run(command):
    _,stdout,stderr=client.exec_command(command,timeout=120)
    output=stdout.read().decode();error=stderr.read().decode();code=stdout.channel.recv_exit_status()
    if code:raise RuntimeError(f'Command failed ({code}): {error[-1800:]} {output[-1800:]}')
    if output.strip():print(output.strip())
    return output

stamp=datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%d-%H%M%S')
release=f'/var/www/remnant-codex/releases/{stamp}'
with tempfile.TemporaryDirectory(prefix='remnant-site-') as d:
    archive=Path(d)/'site.tgz'
    with tarfile.open(archive,'w:gz') as tar:
        for p in (site/'dist').rglob('*'):
            if p.is_file():tar.add(p,arcname=p.relative_to(site/'dist'))
    with client.open_sftp() as sftp:sftp.put(str(archive),f'/tmp/remnant-site-{stamp}.tgz')
    run(f'sudo -n mkdir -p {release} && sudo -n tar -xzf /tmp/remnant-site-{stamp}.tgz -C {release} && sudo -n chmod -R a+rX {release}')
    run(f'sudo -n ln -s {release} /var/www/remnant-codex/current-{stamp} && sudo -n mv -Tf /var/www/remnant-codex/current-{stamp} /var/www/remnant-codex/current')

common='''
    server_name docs.nightbeam.dev;
    location = / { return 302 /remnants/; }
    location = /remnants { return 301 /remnants/; }
    location /remnants/ {
        alias /var/www/remnant-codex/current/;
        index index.html;
        add_header Cache-Control "no-cache";
        add_header X-Content-Type-Options "nosniff" always;
        add_header X-Frame-Options "DENY" always;
        add_header Referrer-Policy "strict-origin-when-cross-origin" always;
        add_header Content-Security-Policy "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob:; font-src 'self'; connect-src 'self' blob:; object-src 'none'; base-uri 'self'; frame-ancestors 'none'" always;
    }
    location ^~ /.well-known/acme-challenge/ { root /var/www/certbot; }
    location / { return 404; }
    gzip on;
    gzip_types text/css application/javascript application/json application/octet-stream model/gltf-binary;
    gzip_min_length 1024;
'''

def install_conf(config):
    remote=f'/tmp/remnant-nginx-{stamp}.conf'
    with client.open_sftp() as sftp:
        with sftp.file(remote,'w') as f:f.write(config)
    # Keep an exact backup of this dedicated vhost; never modify the other sites.
    run(f'if test -f /etc/nginx/sites-available/docs.nightbeam.dev; then sudo -n cp /etc/nginx/sites-available/docs.nightbeam.dev /etc/nginx/sites-available/docs.nightbeam.dev.backup-{stamp}; fi')
    run(f'sudo -n cp {remote} /etc/nginx/sites-available/docs.nightbeam.dev && sudo -n ln -sfn /etc/nginx/sites-available/docs.nightbeam.dev /etc/nginx/sites-enabled/docs.nightbeam.dev')
    try:run('sudo -n nginx -t')
    except Exception:
        run(f'if test -f /etc/nginx/sites-available/docs.nightbeam.dev.backup-{stamp}; then sudo -n cp /etc/nginx/sites-available/docs.nightbeam.dev.backup-{stamp} /etc/nginx/sites-available/docs.nightbeam.dev; else sudo -n unlink /etc/nginx/sites-enabled/docs.nightbeam.dev; fi')
        raise
    run('sudo -n systemctl reload nginx')

has_https='listen 443' in existing_config
if not has_https:install_conf('server {\n    listen 80;\n'+common+'}\n')
check_url='--resolve docs.nightbeam.dev:443:127.0.0.1 https://docs.nightbeam.dev/remnants/codex.json' if has_https else "-H 'Host: docs.nightbeam.dev' http://127.0.0.1/remnants/codex.json"
run("curl -fsS "+check_url+" -o /tmp/remnant-codex-check.json && python3 -c \"import json; d=json.load(open('/tmp/remnant-codex-check.json')); print('VPS serves',len(d['creatures']),'creatures and',len(d['configs']),'config files')\"")
if args.https or has_https:
    run('getent ahostsv4 docs.nightbeam.dev')
    # Uses the VPS's existing Certbot account and its prior terms acceptance.
    if args.https:run('sudo -n certbot certonly --webroot -w /var/www/certbot -d docs.nightbeam.dev --cert-name docs.nightbeam.dev --key-type ecdsa --non-interactive --keep-until-expiring')
    https='''server {
    listen 80;
    server_name docs.nightbeam.dev;
    location ^~ /.well-known/acme-challenge/ { root /var/www/certbot; }
    location / { return 301 https://$host$request_uri; }
}
server {
    listen 443 ssl http2;
    ssl_certificate /etc/letsencrypt/live/docs.nightbeam.dev/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/docs.nightbeam.dev/privkey.pem;
    include /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;
'''+common+'}\n'
    install_conf(https)
    with client.open_sftp() as sftp:
        sftp.put(str(site/'deploy/renew-docs-certificate.sh'),'/tmp/remnant-renew-docs-certificate.sh')
    run('sudo -n install -m 755 /tmp/remnant-renew-docs-certificate.sh /etc/letsencrypt/renewal-hooks/deploy/remnant-docs-nginx.sh && sudo -n sh -n /etc/letsencrypt/renewal-hooks/deploy/remnant-docs-nginx.sh')
    run('curl -fsSI https://docs.nightbeam.dev/remnants/ | head -5')
print('Release:',release)
client.close()
