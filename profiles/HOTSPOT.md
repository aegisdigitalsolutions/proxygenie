# Hotspot survival — MetaClash → LaneNode tunnel

LaneNode is **not** the app you live in. It’s the **tunnel MetaClash dials**.

```
 devices / S26 apps
        │
        ▼
   MetaClash  (TUN + rules + DNS)
        │  socks5://127.0.0.1:8899
        ▼
   LaneNode   (cellular bind + upload pace + pro CONNECT)
        │
        ▼
   your upstream proxy
```

## S26 boot order

1. **LaneNode** — put the four secrets here (`UPSTREAM_HOST/PORT/USER/PASS`), set upload cap, **Start**, wait for cellular UP.  
2. **MetaClash** — import **`metaclas-chained.yaml`**, change `lane:CHANGE_ME_LAN_PASS`, start TUN.  
3. **iPhones** — Shadowrocket → `S26-ip:7891` (user `lane` / your LAN pass) via `shadowrocket-via-gateway.conf`.

## Who holds what

| App | Config |
|-----|--------|
| LaneNode | The four upstream secrets + upload cap / max flows |
| MetaClash | `metaclas-chained.yaml` — outbound is only `LaneNode` on localhost |
| iPhone Shadowrocket | Gateway = S26 hotspot IP + LAN password |

## Files

- `metaclas-chained.yaml` — MetaClash profile (LaneNode = tunnel)  
- LaneNode APK v1.1 — the tunnel  
- `shadowrocket-via-gateway.conf` — iPhones into MetaClash on the S26  
