# Lane Node

Android phone as a second uplink for the LAN: listen on Wi-Fi, bind egress to cellular, chain through your upstream HTTP proxy.

## What it does

1. Phone stays on home Wi-Fi so LAN clients (Surge, etc.) can reach it.
2. `CellularBinder` keeps the cellular radio up and binds every outbound socket to that network (`Network.bindSocket`), not process-wide.
3. `ProxyServer` accepts SOCKS5 and HTTP CONNECT on one port (default `8899`).
4. Each connection is CONNECT-chained to your upstream proxy over cellular so traffic exits a stable IP.

## Build (GitHub Actions)

Push to `main` or run **Build APK** manually. Download the `LaneNode-apk` artifact.

Set these repository secrets (Settings → Secrets and variables → Actions) so the APK ships with defaults:

| Secret | Purpose |
|--------|---------|
| `UPSTREAM_HOST` | Upstream HTTP proxy host |
| `UPSTREAM_PORT` | Port (default `20027` if unset) |
| `UPSTREAM_USER` | Proxy username |
| `UPSTREAM_PASS` | Proxy password |

Nothing sensitive is committed; secrets are injected at CI build time into `BuildConfig`.

## Use

1. Install the debug APK on a phone with Wi-Fi + mobile data.
2. Allow notifications / unrestricted battery if Android asks.
3. Confirm upstream fields (or rely on baked-in secrets), leave **Chain to upstream proxy** on, tap **Start**.
4. Point Surge (or similar) at `socks5://<phone-lan-ip>:8899`.

## Local build

```bash
# optional: put UPSTREAM_* in gradle.properties (do not commit)
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Client profiles

Drop-in **MetaClash** + **Shadowrocket** configs, plus hotspot aggregation guide:

See [`profiles/README.md`](profiles/README.md) and [`profiles/HOTSPOT.md`](profiles/HOTSPOT.md).

LaneNode **v1.1** adds max-flow caps and upload pacing so ~10 LAN clients
don’t destroy the cellular uplink.