# Client profiles

**Canonical setup:** MetaClash on the S26, with **LaneNode as its tunnel**.

1. Start **LaneNode** (four `UPSTREAM_*` secrets + upload cap).  
2. Import **`metaclas-chained.yaml`** into MetaClash → start TUN.  
3. iPhones: **`shadowrocket-via-gateway.conf`** → S26 `:7891`.

Details: [`HOTSPOT.md`](HOTSPOT.md)

| File | Role |
|------|------|
| `metaclas-chained.yaml` | **Use this** — MetaClash → LaneNode |
| `shadowrocket-via-gateway.conf` | iPhone → S26 MetaClash |
| `metaclas.yaml` / `shadowrocket.conf` | Solo / travel (no LaneNode) |
| `metaclas-gateway.yaml` | MetaClash straight to upstream (no LaneNode) |
| `render.sh` / `upstream.env.example` | Fill placeholders locally |
