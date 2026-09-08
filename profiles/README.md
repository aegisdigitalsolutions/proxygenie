# Client profiles — Proxygenie loadout

## Fighting hotspot hell (read this)

10 devices on one carrier hotspot = bufferbloat + throttle.

**Fix:** one Android gateway aggregates everyone over SOCKS; pace the uplink;
only that phone faces the carrier / pro proxy.

→ **[`HOTSPOT.md`](HOTSPOT.md)**

## Fast path

| Role | Profile |
|------|---------|
| Android **gateway** (10 devices) | `metaclas-gateway.yaml` **or** LaneNode APK v1.1 |
| iPhone **via gateway** | `shadowrocket-via-gateway.conf` |
| Solo phone (no LAN flock) | `metaclas.yaml` / `shadowrocket.conf` |

1. Replace `__UPSTREAM_*__` (and `__GATEWAY_HOST__` / LAN pass for gateway mode).  
2. Or `./profiles/render.sh` after editing `upstream.env`.  
3. Import → connect.

## What’s inside the Meta loadout

TUN, sniffer, fake-ip/DoH, MetaCubeX geo, Loyalsoldier rule-sets, split groups  
(Streaming / Apple / Google / Telegram / AI / Games). `tcp-concurrent` off to
reduce uplink stampede.

## Honest limits

- Upstream is still **HTTP CONNECT** until pro support gives TLS/VLESS/Hysteria.  
- Aggregation + upload pacing fixes bufferbloat **now**.  
- Obfuscation on the phone→proxy hop is the next rung for DPI/throttle.

## Files

- `HOTSPOT.md` — architecture  
- `metaclas-gateway.yaml` — allow-lan aggregator  
- `metaclas.yaml` — full Meta solo/client  
- `shadowrocket-via-gateway.conf` — iPhone → gateway  
- `shadowrocket.conf` — iPhone → upstream  
- `shadowrocket-proxy.txt` — one-line HTTP URI  
- `render.sh` / `upstream.env.example`  
