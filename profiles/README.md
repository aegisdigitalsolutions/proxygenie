# Client profiles — Proxygenie loadout

This is the Meta-style bump without fake Reality: **your pro HTTP CONNECT
upstream** + real Clash Meta / Shadowrocket routing (rule-providers, split
groups, fake-ip/DoH, TUN). That combo is what kicks consumer VPN mush —
ExpressVPN is one dumb tunnel; this is smart routing over a path you control.

## Fast path

1. Replace `__UPSTREAM_HOST__` `__UPSTREAM_PORT__` `__UPSTREAM_USER__` `__UPSTREAM_PASS__`  
   (same values as LaneNode / GitHub Actions secrets).
2. **Android:** import `metaclas.yaml` into MetaClash / FlClash → start TUN.  
3. **iPhone:** import `shadowrocket.conf` → Connect (prefer cellular if Wi-Fi jitter sucks).

Or:

```bash
cp profiles/upstream.env.example profiles/upstream.env
# edit real values
./profiles/render.sh
# use profiles/out/*
```

## What’s inside

| Piece | Why it matters |
|-------|----------------|
| TUN + sniffer + fake-ip | Whole-device traffic, correct SNI, fast dial |
| MetaCubeX geo + Loyalsoldier rule-sets | The “MetaClash feels proprietary” part — real domain intelligence |
| Groups: PROXY / Streaming / Apple / Google / Telegram / AI / Games | Flip one category without nuking everything |
| Ads → REJECT | Less junk on the pipe |
| LAN / captive → DIRECT | Phone stays usable on home Wi-Fi |

## Reality (still)

- Upstream is **HTTP CONNECT** (LaneNode port `20027`). No invented VLESS.
- UDP (FaceTime media, some games) is weak over HTTP — Apple group can go DIRECT if calls flake; add a real VLESS/Hysteria/WG node later for UDP.
- First MetaClash start downloads geo + rule-sets (needs network once).

## Files

- `metaclas.yaml` — Android MetaClash / FlClash  
- `shadowrocket.conf` — iPhone full config  
- `shadowrocket-proxy.txt` — one-line HTTP server URI  
- `render.sh` / `upstream.env.example` — fill secrets without committing them  
