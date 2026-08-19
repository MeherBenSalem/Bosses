#!/usr/bin/env python3
"""Tiny Blockbench MCP HTTP client."""
from __future__ import annotations

import json
import sys
import urllib.request

BASE = "http://127.0.0.1:3000/bb-mcp"
SESSION_PATH = __file__.replace("bb_mcp.py", ".bb_mcp_session")
_id = 100


def _request(payload: dict, session: str | None = None, timeout: int = 120) -> tuple[dict, str | None]:
    headers = {
        "Content-Type": "application/json",
        "Accept": "application/json, text/event-stream",
    }
    if session:
        headers["mcp-session-id"] = session
    req = urllib.request.Request(BASE, data=json.dumps(payload).encode(), headers=headers, method="POST")
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        sid = resp.headers.get("mcp-session-id")
        body = resp.read().decode("utf-8", errors="replace")
        return json.loads(body) if body else {}, sid


def ensure_session() -> str:
    try:
        sid = open(SESSION_PATH, encoding="utf-8").read().strip()
        if sid:
            return sid
    except OSError:
        pass
    data, sid = _request(
        {
            "jsonrpc": "2.0",
            "id": 1,
            "method": "initialize",
            "params": {
                "protocolVersion": "2024-11-05",
                "capabilities": {},
                "clientInfo": {"name": "grok-bb-mcp", "version": "1.0"},
            },
        }
    )
    if not sid:
        raise RuntimeError(f"no session: {data}")
    _request({"jsonrpc": "2.0", "method": "notifications/initialized"}, session=sid)
    open(SESSION_PATH, "w", encoding="utf-8").write(sid)
    return sid


def call(name: str, arguments: dict | None = None, timeout: int = 180) -> object:
    global _id
    _id += 1
    sid = ensure_session()
    data, _ = _request(
        {
            "jsonrpc": "2.0",
            "id": _id,
            "method": "tools/call",
            "params": {"name": name, "arguments": arguments or {}},
        },
        session=sid,
        timeout=timeout,
    )
    if "error" in data:
        raise RuntimeError(json.dumps(data["error"], indent=2))
    content = data.get("result", {}).get("content", [])
    texts = []
    for item in content:
        if item.get("type") == "text":
            texts.append(item.get("text", ""))
        else:
            texts.append(json.dumps(item))
    joined = "\n".join(texts)
    try:
        return json.loads(joined)
    except json.JSONDecodeError:
        return joined


def ev(code: str) -> object:
    return call("risky_eval", {"code": code})


if __name__ == "__main__":
    action = sys.argv[1] if len(sys.argv) > 1 else "eval"
    if action == "eval":
        print(json.dumps(ev(sys.argv[2]), indent=2, default=str)[:20000])
    elif action == "call":
        args = json.loads(sys.argv[3]) if len(sys.argv) > 3 else {}
        print(json.dumps(call(sys.argv[2], args), indent=2, default=str)[:20000])
    else:
        raise SystemExit("usage: bb_mcp.py eval <js> | call <tool> [json-args]")
