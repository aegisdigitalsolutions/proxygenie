#!/usr/bin/env bash
# Fill __UPSTREAM_*__ placeholders from env or profiles/upstream.env → profiles/out/
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

mkdir -p out
render() {
  local src="$1" dest="out/$2"
  sed \
    -e "s|__UPSTREAM_HOST__|${UPSTREAM_HOST}|g" \
    -e "s|__UPSTREAM_PORT__|${UPSTREAM_PORT}|g" \
    -e "s|__UPSTREAM_USER__|${UPSTREAM_USER}|g" \
    -e "s|__UPSTREAM_PASS__|${UPSTREAM_PASS}|g" \
    "$src" > "$dest"
  echo "wrote $dest"
}

render metaclas.yaml metaclas.yaml
render shadowrocket.conf shadowrocket.conf
render shadowrocket-proxy.txt shadowrocket-proxy.txt

echo
echo "Import from profiles/out/ (secrets filled — do not commit)"
echo "  Android: metaclas.yaml"
echo "  iPhone:  shadowrocket.conf  or  shadowrocket-proxy.txt"
