#!/usr/bin/env bash
# Contact sheet of a captured shot: one frame every STEP seconds, COLS across.
#   tools/reel/sheet.sh NAME [STEP] [COLS] [WIDTH] -> prints the sheet path
set -euo pipefail
OUT="$(cd "$(dirname "$0")/../.." && pwd)/1.21.1/fabric/runs/reel/reel/out"
NAME="$1"; STEP="${2:-0.5}"; COLS="${3:-8}"; W="${4:-240}"
DEST="${SHEET_DIR:-$OUT/sheets}"; mkdir -p "$DEST"
N=$(ffprobe -v error -count_frames -select_streams v:0 -show_entries stream=nb_read_frames -of csv=p=0 "$OUT/$NAME.mp4")
EVERY=$(python3 -c "print(max(1,round($STEP*30)))")
ROWS=$(python3 -c "import math;print(math.ceil(math.ceil($N/$EVERY)/$COLS))")
ffmpeg -loglevel error -y -i "$OUT/$NAME.mp4" -vf "select='not(mod(n\,$EVERY))',scale=$W:-1,drawtext=text='%{n}':x=4:y=4:fontsize=18:fontcolor=yellow:box=1:boxcolor=black@0.5,tile=${COLS}x${ROWS}" -frames:v 1 "$DEST/$NAME.png"
echo "$DEST/$NAME.png"
