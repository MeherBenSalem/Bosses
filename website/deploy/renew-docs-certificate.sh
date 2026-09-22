#!/bin/sh
set -eu
if [ "${RENEWED_LINEAGE:-}" = "/etc/letsencrypt/live/docs.nightbeam.dev" ]; then
    /usr/sbin/nginx -t
    /bin/systemctl reload nginx
fi
