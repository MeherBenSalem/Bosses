"""Minimal JSON-RPC client for the Blockbench MCP plugin (http://localhost:3000/bb-mcp)."""
import base64
import json
import urllib.request

URL = 'http://localhost:3000/bb-mcp'


class BlockbenchError(RuntimeError):
    pass


class Blockbench:
    def __init__(self, client_name='Remnants asset author', url=URL, timeout=120):
        self.url = url
        self.timeout = timeout
        self.session = None
        self._id = 0
        self.rpc('initialize', {'protocolVersion': '2024-11-05', 'capabilities': {},
                                'clientInfo': {'name': client_name, 'version': '3.0'}})

    def rpc(self, method, params):
        self._id += 1
        headers = {'Content-Type': 'application/json', 'Accept': 'application/json, text/event-stream'}
        if self.session:
            headers['mcp-session-id'] = self.session
        body = json.dumps({'jsonrpc': '2.0', 'id': self._id, 'method': method, 'params': params}).encode()
        req = urllib.request.Request(self.url, body, headers)
        with urllib.request.urlopen(req, timeout=self.timeout) as res:
            self.session = res.headers.get('mcp-session-id', self.session)
            raw = res.read().decode('utf-8')
        if raw.lstrip().startswith('event:') or raw.lstrip().startswith('data:'):
            raw = next(line[5:] for line in raw.splitlines() if line.startswith('data:'))
        value = json.loads(raw)
        if 'error' in value:
            raise BlockbenchError(value['error'])
        return value['result']

    def call(self, tool, **args):
        result = self.rpc('tools/call', {'name': tool, 'arguments': args})
        if result.get('isError'):
            raise BlockbenchError(f'{tool}: {self.text(result)}')
        return result

    @staticmethod
    def text(result):
        return next((c['text'] for c in result.get('content', []) if c.get('type') == 'text'), '')

    def json(self, tool, **args):
        return json.loads(self.text(self.call(tool, **args)))

    def eval(self, code):
        """Run JavaScript inside Blockbench and return its JSON-decoded result."""
        out = self.text(self.call('risky_eval', code=code))
        for _ in range(2):  # risky_eval JSON-encodes its result; JSON.stringify results are encoded twice
            if not isinstance(out, str):
                break
            try:
                out = json.loads(out)
            except json.JSONDecodeError:
                break
        return out

    def images(self, result):
        return [base64.b64decode(c['data']) for c in result.get('content', []) if c.get('type') == 'image']
