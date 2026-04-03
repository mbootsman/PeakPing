#!/usr/bin/env python3
"""
Generates app/src/main/assets/egm96_1deg.bin — a compact EGM96 geoid
undulation grid used by GeoidModel.kt to correct WGS84 altitude to MSL
on Android < 14.

Source: GeographicLib EGM96 5-arcmin PGM (public domain, WGS84).
Output: 181 rows × 361 cols of big-endian signed Int16 in centimetres.
        Row 0 = 90°N, row 180 = 90°S; col 0 = 0°E, col 360 = 360°E.

Run once, then commit the generated file:
    python3 scripts/generate_geoid.py
    git add app/src/main/assets/egm96_1deg.bin
"""

import os, io, struct, tarfile, urllib.request

SRC_URL  = ("https://downloads.sourceforge.net/project/geographiclib/"
            "geoids-distrib/egm96-5.tar.bz2")
OUT_PATH = os.path.join(os.path.dirname(__file__),
                        "..", "app", "src", "main", "assets", "egm96_1deg.bin")

def main():
    print("Downloading EGM96 5-arcmin grid from GeographicLib (~10 MB)…")
    req  = urllib.request.Request(SRC_URL, headers={"User-Agent": "curl/7.68.0"})
    data = urllib.request.urlopen(req, timeout=120).read()
    print(f"  Downloaded {len(data):,} bytes")

    with tarfile.open(fileobj=io.BytesIO(data), mode="r:bz2") as tar:
        pgm_entry  = next(m for m in tar.getmembers() if m.name.endswith(".pgm"))
        pgm_bytes  = tar.extractfile(pgm_entry).read()
    print(f"  Extracted {pgm_entry.name}: {len(pgm_bytes):,} bytes")

    # Parse PGM header
    offset = scale = width = height = None
    pos = 0
    while True:
        nl   = pgm_bytes.index(b"\n", pos)
        line = pgm_bytes[pos:nl].decode("ascii").strip()
        pos  = nl + 1
        if   line.upper().startswith("# OFFSET"): offset = float(line.split()[-1])
        elif line.upper().startswith("# SCALE"):  scale  = float(line.split()[-1])
        elif line.startswith("P5") or line.startswith("#"): continue
        else:
            w, h    = map(int, line.split())
            width, height = w, h
            pos = pgm_bytes.index(b"\n", pos) + 1  # skip MAXVAL line
            break

    print(f"  Grid: {width} × {height}, offset={offset}, scale={scale}")
    raw = pgm_bytes[pos:]
    assert len(raw) == width * height * 2, f"size mismatch: {len(raw)} vs {width*height*2}"

    # Sample to 1° grid (egm96-5 = 12 samples per degree)
    out_rows, out_cols, step = 181, 361, 12
    os.makedirs(os.path.dirname(OUT_PATH), exist_ok=True)
    with open(OUT_PATH, "wb") as f:
        for r in range(out_rows):
            src_r = min(r * step, height - 1)
            for c in range(out_cols):
                src_c  = (c * step) % width
                off    = (src_r * width + src_c) * 2
                uint16 = struct.unpack_from(">H", raw, off)[0]
                n_m    = uint16 * scale + offset
                cm     = max(-32768, min(32767, int(round(n_m * 100))))
                f.write(struct.pack(">h", cm))

    size = os.path.getsize(OUT_PATH)
    print(f"\nWrote {OUT_PATH}")
    print(f"  Size: {size:,} bytes ({size // 1024} KB)")

    # Spot-checks
    checks = [
        (52,   5,   "Netherlands",      41),
        ( 0,   0,   "Equator/Greenwich",17),
        ( 0,  90,   "Equator/India",   -63),
        (40, 280,   "USA East Coast",  -34),
    ]
    print("\nSpot-checks (EGM96 undulation):")
    with open(OUT_PATH, "rb") as f:
        for lat, lon, label, expected in checks:
            r, c = 90 - lat, lon % 361
            f.seek((r * 361 + c) * 2)
            v = struct.unpack(">h", f.read(2))[0]
            ok = "✓" if abs(v / 100 - expected) < 5 else "?"
            print(f"  {ok} {label:22s} {lat:+4d}°N {lon:+4d}°E → {v/100:+.1f} m  (≈{expected:+d} m)")

if __name__ == "__main__":
    main()
