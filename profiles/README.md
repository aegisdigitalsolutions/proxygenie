# Client profiles (drop-in)

Your upstream today is an **HTTP CONNECT** proxy (LaneNode secrets / port `20027`).
These profiles route **MetaClash / FlClash** (Android) and **Shadowrocket** (iPhone) through that same proxy.

VLESS/Reality cannot be invented client-side — it needs a real Xray node. Optional stubs are commented in both configs for when you add one.

## Fast path (phone edit)

1. Open `metaclas.yaml` / `shadowrocket.conf`
2. Replace `__UPSTREAM_HOST__`, `__UPSTREAM_PORT__`, `__UPSTREAM_USER__`, `__UPSTREAM_PASS__`
3. Import → hit connect

## Or render once on a computer

```bash
cp profiles/upstream.env.example profiles/upstream.env
# edit real values
./profiles/render.sh
# use files under profiles/out/
```

`upstream.env` and `profiles/out/` are gitignored.

## Android — MetaClash / FlClash

Import `metaclas.yaml` → select profile → start TUN/VPN → group **PROXY** = `Upstream-HTTP`.

## iPhone — Shadowrocket

- **Full:** import `shadowrocket.conf` as Config → connect  
- **Server only:** paste `shadowrocket-proxy.txt` as an HTTP server, then Global or attach rules

Turn on cellular / prefer-cellular when Wi-Fi upload/jitter is bad.

## Reality check

| Helps | Does not fix alone |
|------|---------------------|
| Stable egress via your proxy | Raw Wi-Fi bufferbloat on DIRECT |
| DoH + fake-ip + concurrent dial | UDP (FaceTime media) over HTTP CONNECT |
| LAN stays DIRECT | Need VLESS/Hysteria/WG for real UDP tunnel |

FaceTime-ish domains sit on the **Apple** group (PROXY or DIRECT) because HTTP CONNECT is TCP-only. After you add a UDP-capable node, point PROXY/Apple at it.
