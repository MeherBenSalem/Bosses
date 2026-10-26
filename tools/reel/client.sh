#!/usr/bin/env bash
# Starts or stops the reel director client (tools/reel) on a virtual display with software OpenGL.
#   tools/reel/client.sh start [log]   # 1080x1920 window; REEL_WIDTH / REEL_HEIGHT override
#   tools/reel/client.sh stop
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
DISPLAY_ID="${REEL_DISPLAY:-:99}"

case "${1:-}" in
  start)
    LOG="${2:-$ROOT/1.21.1/fabric/runs/reel/client.log}"
    mkdir -p "$(dirname "$LOG")"
    if ! pgrep -x Xvfb >/dev/null; then
      setsid nohup Xvfb "$DISPLAY_ID" -screen 0 2160x3840x24 -nolisten tcp >/dev/null 2>&1 &
      sleep 1
    fi
    cd "$ROOT/1.21.1"
    DISPLAY="$DISPLAY_ID" LIBGL_ALWAYS_SOFTWARE=1 GALLIUM_DRIVER=llvmpipe setsid nohup bash ./gradlew :fabric:runReelClient \
      -PreelDirector -PreelWidth="${REEL_WIDTH:-1080}" -PreelHeight="${REEL_HEIGHT:-1920}" --console=plain >"$LOG" 2>&1 &
    echo "started; log: $LOG"
    ;;
  stop)
    for pid in $(pgrep -f 'net[.]fabricmc[.]devlaunchinjector[.]Main' || true) $(pgrep -f 'org[.]gradle[.]wrapper[.]GradleWrapperMain' || true); do
      kill "$pid" 2>/dev/null || true
    done
    echo stopped
    ;;
  *)
    echo "usage: $0 start [log] | stop" >&2
    exit 2
    ;;
esac
