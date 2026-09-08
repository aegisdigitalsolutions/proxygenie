# Hotspot survival — aggregate + don’t look like 10 phones

You’re fighting **two** different killers:

1. **Upstream bufferbloat** — 10 devices stampede a ~2 Mbps cell uplink → 1000+ ms buffer, jitter hell  
2. **Carrier hotspot throttle** — TTL / tether fingerprints / bulk parallel flows → scheduler puts you in the slow lane  

Fake Reality won’t save a saturated buffer. **Clean aggregation + paced uplink + one face to the carrier** will.

## Architecture (this is the fix)

```
 iPhone / tablets / kids’ devices
        │  SOCKS5 or HTTP CONNECT  (NOT raw hotspot browsing)
        ▼
 ┌──────────────────────────────┐
 │  Gateway Android phone       │
 │  LaneNode  OR  MetaClash     │
 │  • listens on LAN            │
 │  • caps parallel flows       │
 │  • paces UPLOAD ~75% of cell │
 │  • ONE egress on cellular    │
 └──────────────┬───────────────┘
                │  HTTP CONNECT → your pro proxy
                ▼
         Upstream (stable IP)
```

### Why carriers chill out

| Bad (what you had) | Good (this) |
|--------------------|-------------|
| 10 devices NAT through hotspot | 10 devices **proxy** into one phone |
| TTL decremented → tethered fingerprint | Sockets originate on the phone (TTL looks native) |
| Unshaped upload fills the buffer | Token-bucket upload cap keeps latency usable |
| Each device opens 100 connections | Gateway enforces max active flows |

Obfuscation still helps **on the phone→proxy hop** (HTTPS/TLS wrapper or later Reality).  
But obfuscation alone **cannot** fix bufferbloat — you must not fill the pipe.

## How to run it tonight

### Option A — LaneNode (best for “cellular bind + LAN”)

1. Install LaneNode on the gateway Android (Wi‑Fi + mobile data on).  
2. Set upstream host/user/pass.  
3. **Upload cap kbps** ≈ 75% of measured cell upload (e.g. 1400 if you see ~2 Mbps).  
4. **Max active flows** ≈ 32–64 for ~10 devices.  
5. Start. Note `lan address` + port (default `8899`).  
6. Every other device → SOCKS5 `phone-ip:8899` (Shadowrocket / MetaClash / system proxy).  
7. Do **not** leave devices on “just use hotspot with no proxy” — that’s the throttle bait.

### Option B — MetaClash as gateway

1. Import `metaclas-gateway.yaml` on the Android phone.  
2. Fill `__UPSTREAM_*__` and change LAN auth password.  
3. `allow-lan: true` → other devices use `socks5://phone-ip:7891`.  
4. Prefer **Rule** mode; keep TUN for apps on the phone itself.

### iPhone clients

Import `shadowrocket-via-gateway.conf` — points at `__GATEWAY_HOST__` (the Android LAN IP), not the upstream directly.  
Only the gateway phone talks to the pro proxy / carrier.

## Tuning the upload cap

1. Speed-test **upload only** on the gateway with nothing else running.  
2. Set cap to **0.7–0.8 × that**.  
3. Re-test latency while uploading (ping / FaceTime).  
4. If latency still spikes → lower the cap. If upload feels too slow and latency is fine → nudge up.

## Obfuscation (next rung, not tonight’s blocker)

Ask pro support for one of:

- **HTTPS proxy** (TLS on the CONNECT hop) — looks like normal TLS  
- **VLESS Reality / Hysteria2** — real obfuscation + UDP for FaceTime  

Until then: aggregation + pacing beats ExpressVPN-on-hotspot every day of the week.

## Files

| File | Role |
|------|------|
| LaneNode app (v1.1) | Gateway with flow cap + upload pacing |
| `metaclas-gateway.yaml` | MetaClash allow-lan aggregator |
| `metaclas.yaml` | Full Meta loadout (phone-as-client or solo) |
| `shadowrocket-via-gateway.conf` | iPhone → LAN gateway |
| `shadowrocket.conf` | iPhone → upstream direct (solo travel) |
