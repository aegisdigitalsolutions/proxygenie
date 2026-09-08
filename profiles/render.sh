#!/usr/bin/env bash
# Fill placeholders from env or profiles/upstream.env → profiles/out/
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

if [[ -f upstream.env ]]; then
  set -a
  # shellcheck disable=SC1091
  source upstream.env
  set +a
fi

: "${UPSTREAM_HOST:?Set UPSTREAM_HOST (or put it in profiles/upstream.env)}"
: "${UPSTREAM_PORT:=20027}"
: "${UPSTREAM_USER:?Set UPSTREAM_USER}"
: "${UPSTREAM_PASS:?Set UPSTREAM_PASS}"
: "${GATEWAY_HOST:=192.168.43.1}"
: "${LAN_PASS:=CHANGE_ME_LAN_PASS}"

mkdir -p out
render() {
  local src="$1" dest="out/$2"
  sed \
    -e "s|__UPSTREAM_HOST__|${UPSTREAM_HOST}|g" \
    -e "s|__UPSTREAM_PORT__|${UPSTREAM_PORT}|g" \
    -e "s|__UPSTREAM_USER__|${UPSTREAM_USER}|g" \
    -e "s|__UPSTREAM_PASS__|${UPSTREAM_PASS}|g" \
    -e "s|__GATEWAY_HOST__|${GATEWAY_HOST}|g" \
    -e "s|__LAN_PASS__|${LAN_PASS}|g" \
    -e "s|CHANGE_ME_LAN_PASS|${LAN_PASS}|g" \
    "$src" > "$dest"
  echo "wrote $dest"
}

render metaclas.yaml metaclas.yaml
render metaclas-gateway.yaml metaclas-gateway.yaml
render shadowrocket.conf shadowrocket.conf
render shadowrocket-via-gateway.conf shadowrocket-via-gateway.conf
render shadowrocket-proxy.txt shadowrocket-proxy.txt

echo
echo "Import from profiles/out/ (secrets filled — do not commit)"
echo "  Gateway phone:  metaclas-gateway.yaml  OR  LaneNode APK"
echo "  iPhone clients: shadowrocket-via-gateway.conf"
echo "  Solo travel:    metaclas.yaml / shadowrocket.conf"
echo "  Read:           profiles/HOTSPOT.md"
