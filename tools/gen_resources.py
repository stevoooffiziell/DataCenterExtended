"""Generates textures, models, blockstates, lang (en_us), recipes, loot tables, tags and data maps for DataCenterExtended.

Usage:  python tools/gen_resources.py <project root>
de_de.json is maintained by hand and is NOT touched by this script."""
import json
import os
import struct
import sys
import zlib

ROOT = sys.argv[1]
MOD = "datacenterextended"
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", MOD)
DATA = os.path.join(ROOT, "src", "main", "resources", "data")


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


# ============================================================ PNG / pixel helpers
def write_png(path, px, w=16, h=16):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    raw = b""
    for y in range(h):
        raw += b"\x00" + b"".join(struct.pack("BBBB", *px[y][x]) for x in range(w))

    def chunk(t, d):
        c = struct.pack(">I", len(d)) + t + d
        return c + struct.pack(">I", zlib.crc32(t + d) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
    with open(path, "wb") as f:
        f.write(png)


def rgb(h, a=255):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255, a)


def shade(h, f):
    r, g, b = (h >> 16) & 255, (h >> 8) & 255, h & 255
    r, g, b = (min(255, max(0, int(c * f))) for c in (r, g, b))
    return (r << 16) | (g << 8) | b


class Canvas:
    def __init__(self, fill=None):
        self.px = [[(0, 0, 0, 0) if fill is None else rgb(fill) for _ in range(16)] for _ in range(16)]

    def set(self, x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            self.px[y][x] = rgb(c) if isinstance(c, int) else c

    def rect(self, x0, y0, x1, y1, c):  # inclusive
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.set(x, y, c)

    def frame(self, x0, y0, x1, y1, c):
        for x in range(x0, x1 + 1):
            self.set(x, y0, c)
            self.set(x, y1, c)
        for y in range(y0, y1 + 1):
            self.set(x0, y, c)
            self.set(x1, y, c)

    def bevel(self, x0, y0, x1, y1, base):
        self.rect(x0, y0, x1, y1, base)
        for x in range(x0, x1 + 1):
            self.set(x, y0, shade(base, 1.3))
            self.set(x, y1, shade(base, 0.6))
        for y in range(y0, y1 + 1):
            self.set(x0, y, shade(base, 1.2))
            self.set(x1, y, shade(base, 0.65))

    def circle(self, cx, cy, r, c, filled=True):
        for y in range(16):
            for x in range(16):
                d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
                if (filled and d <= r) or (not filled and r - 0.6 <= d <= r + 0.4):
                    self.set(x, y, c)

    def save(self, path):
        write_png(path, self.px)


def noise(x, y, seed=0):
    n = (x * 374761393 + y * 668265263 + seed * 2147483647) & 0xFFFFFFFF
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 255) / 255.0


# ============================================================ item textures
def tex_cpu(brand, tier):
    c = Canvas()
    pin = 0xC9A227
    # pins around the chip
    for i in range(3, 13, 2):
        for (x, y) in ((i, 1), (i, 14), (1, i), (14, i)):
            c.set(x, y, pin)
    c.bevel(2, 2, 13, 13, 0x2E7D32 if brand != "epyc" else 0x1B3B2F)  # substrate
    c.bevel(4, 4, 11, 11, 0xB8BCC2)  # heat spreader
    logo = {"intel": 0x0071C5, "amd": 0xED1C24, "ryzen": 0xF26522, "epyc": 0x00A39A}[brand]
    c.rect(5, 5, 10, 6, logo)
    c.rect(5, 7, 7, 7, shade(logo, 0.7))
    # tier dots
    for t in range(tier):
        c.set(5 + t, 9 if t < 6 else 10, 0xFFD54F)
    return c


def tex_gpu(brand, tier):
    c = Canvas()
    shroud = {"nvidia": 0x2B2B2B, "amd": 0x3A1010}[brand]
    accent = {"nvidia": 0x76B900, "amd": 0xED1C24}[brand]
    c.rect(0, 3, 1, 12, 0xA8ADB3)  # slot bracket
    c.bevel(2, 3, 15, 11, shroud)
    c.rect(2, 12, 15, 12, 0x1E5631)  # pcb edge
    for x in range(5, 14, 2):
        c.set(x, 13, 0xC9A227)  # pcie fingers
    fans = 2 if tier >= 3 else 1
    centers = [(6, 7), (12, 7)] if fans == 2 else [(9, 7)]
    for (cx, cy) in centers:
        c.circle(cx, cy, 2.6, 0x151515)
        c.circle(cx, cy, 2.6, shade(shroud, 1.8), filled=False)
        c.set(cx, cy, 0x777777)
        c.set(cx - 1, cy - 1, 0x555555)
        c.set(cx + 1, cy + 1, 0x555555)
    c.rect(3, 4, 14, 4, accent)
    if tier >= 5:
        c.rect(3, 10, 14, 10, accent)
    return c


def tex_motherboard(pcb, accent):
    c = Canvas()
    c.bevel(1, 1, 14, 14, pcb)
    c.bevel(3, 3, 7, 7, 0xB0B0B0)  # cpu socket
    c.rect(4, 4, 6, 6, 0x6A6A6A)
    for y in (9, 11, 13):
        c.rect(3, y, 12, y, accent)  # pcie slots
    c.rect(9, 3, 9, 7, 0x222222)  # ram slots
    c.rect(11, 3, 11, 7, 0x222222)
    c.rect(13, 3, 13, 7, 0x222222)
    c.set(3, 13, 0xC9A227)
    return c


def tex_psu(label):
    c = Canvas()
    c.bevel(1, 2, 14, 13, 0x3C3C3C)
    c.circle(6.5, 7.5, 4.2, 0x1A1A1A)
    c.circle(6.5, 7.5, 4.2, 0x7A7A7A, filled=False)
    c.circle(6.5, 7.5, 2.2, 0x7A7A7A, filled=False)
    c.set(6, 7, 0x999999)
    c.set(7, 8, 0x999999)
    c.rect(12, 4, 13, 11, label)  # efficiency label
    c.set(12, 12, 0xE0E0E0)
    return c


def tex_credit():
    c = Canvas()
    c.bevel(1, 4, 14, 11, 0x4CAF50)
    c.frame(2, 5, 13, 10, 0x2E7D32)
    c.circle(7.5, 7.5, 1.8, 0xC8E6C9)
    c.set(7, 7, 0x2E7D32)
    c.set(8, 8, 0x2E7D32)
    return c


def tex_bundle():
    c = Canvas()
    for i, y in enumerate((9, 6, 3)):
        c.bevel(1 + i, y, 13 + i, y + 5, shade(0x4CAF50, 0.85 + i * 0.1))
    c.rect(7, 3, 8, 13, 0xD4A017)  # band
    return c


def tex_crate():
    c = Canvas()
    c.bevel(1, 2, 14, 14, 0x8D6E63)
    c.frame(1, 2, 14, 14, 0x4E342E)
    c.rect(1, 7, 14, 8, 0x4E342E)
    c.rect(5, 4, 10, 12, 0x43A047)
    c.rect(7, 4, 8, 12, 0xFFD54F)
    return c


def tex_bitcoin():
    c = Canvas()
    c.circle(7.5, 7.5, 6.6, 0xC77700)
    c.circle(7.5, 7.5, 5.6, 0xF7931A)
    # "B"
    for y in range(4, 12):
        c.set(6, y, 0xFFFFFF)
    for x in (7, 8):
        c.set(x, 4, 0xFFFFFF)
        c.set(x, 7, 0xFFFFFF)
        c.set(x, 11, 0xFFFFFF)
    c.set(9, 5, 0xFFFFFF)
    c.set(9, 6, 0xFFFFFF)
    c.set(9, 8, 0xFFFFFF)
    c.set(9, 9, 0xFFFFFF)
    c.set(9, 10, 0xFFFFFF)
    c.set(7, 3, 0xFFFFFF)
    c.set(7, 12, 0xFFFFFF)
    return c


def tex_silicon():
    c = Canvas()
    c.circle(7.5, 7.5, 6.5, 0x37474F)
    c.circle(7.5, 7.5, 5.5, 0x546E7A)
    for x in range(3, 13, 3):
        for y in range(3, 13, 3):
            c.set(x, y, 0x90A4AE)
    c.rect(6, 14, 9, 14, (0, 0, 0, 0))
    return c


def tex_circuit():
    c = Canvas()
    c.bevel(1, 2, 14, 13, 0x1B5E20)
    for (x0, y0, x1, y1) in ((3, 4, 12, 4), (3, 4, 3, 10), (6, 7, 13, 7), (9, 7, 9, 11), (4, 11, 9, 11)):
        c.rect(x0, y0, x1, y1, 0xC9A227)
    c.rect(10, 9, 12, 11, 0x212121)
    c.rect(5, 8, 6, 9, 0x212121)
    return c


def tex_server_item():
    c = Canvas()
    c.bevel(0, 5, 15, 10, 0x3A3D42)
    c.rect(1, 6, 14, 6, 0x50545B)
    for x in range(4, 10, 2):
        c.rect(x, 7, x, 8, 0x24272B)
    c.set(1, 7, 0x8A8F96)
    c.set(1, 8, 0x8A8F96)
    c.set(13, 8, 0x00E676)
    c.set(12, 8, 0x29B6F6)
    c.rect(1, 11, 14, 11, 0x1A1C1F)  # depth
    return c


def tex_storage(kind):
    c = Canvas()
    if kind == "hdd":
        c.bevel(2, 1, 13, 14, 0xB0B5BB)
        c.circle(7.5, 6.5, 4.2, 0xD9DDE2)
        c.circle(7.5, 6.5, 1.0, 0x7A7F86)
        c.rect(5, 6, 11, 6, 0x5D6066)  # head arm
        c.rect(3, 12, 12, 13, 0x1B5E20)
    elif kind == "ssd":
        c.bevel(2, 2, 13, 13, 0x212121)
        c.rect(3, 4, 12, 8, 0xE53935)
        c.rect(4, 5, 9, 5, 0xFFFFFF)
        c.rect(4, 7, 7, 7, 0xFFCDD2)
    elif kind == "nvme":
        c.bevel(1, 6, 14, 9, 0x1B5E20)
        c.rect(3, 6, 5, 9, 0x212121)
        c.rect(7, 6, 10, 9, 0x212121)
        c.rect(12, 7, 12, 8, 0xC9A227)
        c.set(0, 7, 0xB0B5BB)
    else:  # enterprise U.2
        c.bevel(1, 1, 14, 14, 0x90A4AE)
        c.rect(3, 3, 12, 6, 0x37474F)
        c.rect(4, 4, 10, 4, 0x00A39A)
        for y in range(8, 13, 2):
            c.rect(3, y, 12, y, 0x607D8B)
    return c


def tex_tester():
    c = Canvas()
    c.bevel(3, 1, 12, 14, 0xFFC107)
    c.rect(4, 2, 11, 7, 0x0B1F12)
    for (x, y) in ((4, 6), (5, 5), (6, 6), (7, 4), (8, 5), (9, 3), (10, 4), (11, 3)):
        c.set(x, y, 0x00E676)
    c.rect(5, 9, 6, 10, 0x212121)
    c.rect(9, 9, 10, 10, 0x212121)
    c.rect(7, 12, 8, 15, 0x1E88E5)  # cable
    return c


# ============================================================ block textures
def noisy(base, seed, amp=0.15):
    c = Canvas(base)
    for y in range(16):
        for x in range(16):
            c.set(x, y, shade(base, 0.9 + noise(x, y, seed) * amp))
    return c


RACK_ACCENT = {"mining": 0xF7931A, "hosting": 0x29B6F6, "fabricating": 0xAB47BC}


def tex_rack_front(kind, part):
    """Front of a 4U rack segment: two rails, 4 empty bays; caps on the ends of a merged rack."""
    c = Canvas(0x15171A)
    rail = 0x2E3238
    c.rect(0, 0, 1, 15, rail)
    c.rect(14, 0, 15, 15, rail)
    for y in range(1, 16, 2):
        c.set(1, y, 0x0B0C0E)
        c.set(14, y, 0x0B0C0E)
    for i in range(4):  # bay i = texture rows 4i+1 .. 4i+3 (row 4i is the separator)
        c.rect(2, 4 * i, 13, 4 * i, 0x22252A)
        c.rect(2, 4 * i + 1, 13, 4 * i + 3, 0x0B0C0E)
    acc = RACK_ACCENT[kind]
    if part in ("single", "top"):
        c.rect(0, 0, 15, 0, 0x3A3F46)
        c.rect(5, 0, 10, 0, acc)
    if part in ("single", "bottom"):
        c.rect(0, 15, 15, 15, 0x3A3F46)
        c.set(0, 15, 0x111111)
        c.set(15, 15, 0x111111)
    c.rect(0, 2, 0, 5, acc)  # type stripe on the left rail
    return c


def tex_rack_side(part):
    c = noisy(0x24272B, 11)
    c.rect(0, 0, 0, 15, 0x1A1C1F)
    c.rect(15, 0, 15, 15, 0x1A1C1F)
    for y in range(2, 14, 3):
        c.rect(4, y, 11, y, 0x15171A)  # vents
    if part in ("single", "top"):
        c.rect(0, 0, 15, 0, 0x3A3F46)
    if part in ("single", "bottom"):
        c.rect(0, 15, 15, 15, 0x3A3F46)
    return c


def tex_rack_top():
    c = noisy(0x24272B, 13)
    c.frame(0, 0, 15, 15, 0x3A3F46)
    for y in range(3, 13, 2):
        for x in range(3, 13, 2):
            c.set(x, y, 0x101112)
    return c


def tex_server_plate(on):
    """1U server front, repeated in every bay band so the auto-UV of each plate element picks it up."""
    c = Canvas()
    for i in range(4):
        y = 4 * i + 1
        c.rect(2, y, 13, y + 2, 0x3A3D42)
        c.rect(2, y, 13, y, 0x50545B)
        for x in range(5, 11, 2):
            c.set(x, y + 1, 0x24272B)  # drive bays
        c.set(3, y + 1, 0x8A8F96)  # handle
        c.set(12, y + 1, 0x00E676 if on else 0x3B1F1F)
        c.set(11, y + 1, (0x29B6F6 if i % 2 == 0 else 0x00E676) if on else 0x263238)
    return c


def tex_workbench_top():
    c = noisy(0x2E7D32, 21, 0.1)  # ESD mat
    c.frame(0, 0, 15, 15, 0x9E9E9E)
    c.rect(3, 3, 7, 7, 0x1B5E20)  # a motherboard lying on the mat
    c.rect(4, 4, 5, 5, 0xB0B0B0)
    c.rect(10, 4, 12, 4, 0xFFC107)  # screwdriver
    c.rect(12, 4, 13, 4, 0xD32F2F)
    c.rect(9, 10, 12, 12, 0x3A3D42)  # a server lid
    return c


def tex_workbench_front():
    c = noisy(0x5D6066, 23)
    c.rect(0, 0, 15, 2, 0x2E7D32)
    for y in (4, 8, 12):
        c.bevel(2, y, 13, y + 2, 0x6F737A)
        c.rect(7, y + 1, 8, y + 1, 0xCFD3D8)
    return c


def tex_workbench_side():
    c = noisy(0x5D6066, 25)
    c.rect(0, 0, 15, 2, 0x2E7D32)
    c.rect(1, 3, 2, 15, 0x3A3D42)
    c.rect(13, 3, 14, 15, 0x3A3D42)
    c.rect(3, 9, 12, 10, 0x3A3D42)
    return c


CABLE_COLORS = {  # base, stripe
    "coax": (0x1A1A1A, 0xC77B30),
    "cat5e": (0x1E88E5, 0x90CAF9),
    "cat6a": (0x43A047, 0xA5D6A7),
    "fiber": (0xFFB300, 0xFFF59D),
}


def tex_cable(tier):
    base, stripe = CABLE_COLORS[tier]
    c = noisy(base, 31, 0.12)
    for x in range(16):
        c.set(x, 7, shade(base, 1.25))
        c.set(x, 8, stripe if (x // 2) % 2 == 0 else shade(base, 1.1))
    return c


def tex_terminal_front():
    c = Canvas(0x37474F)
    c.frame(0, 0, 15, 15, 0x263238)
    c.rect(2, 2, 13, 9, 0x0B1F12)
    pts = [(2, 8), (4, 7), (6, 7), (8, 5), (10, 6), (12, 3), (13, 3)]
    for (x, y) in pts:
        c.set(x, y, 0x00E676)
    c.set(3, 8, 0x00E676)
    c.set(7, 6, 0x00E676)
    c.set(11, 4, 0x00E676)
    c.rect(2, 11, 13, 13, 0x263238)
    for x in range(3, 13, 2):
        c.set(x, 12, 0x90A4AE)
    c.set(12, 12, 0xF7931A)
    return c


def tex_terminal_side():
    c = Canvas(0x455A64)
    for y in range(16):
        for x in range(16):
            c.set(x, y, shade(0x455A64, 0.9 + noise(x, y, 7) * 0.15))
    c.frame(0, 0, 15, 15, 0x263238)
    return c


# ============================================================ content definition
CPUS = [  # id, name, brand, tier, compute KB/s, watts
    ("pentium_4", "Intel Pentium 4", "intel", 1, 4, 85),
    ("amd_fx_4300", "AMD FX-4300", "amd", 2, 10, 95),
    ("core_i5_2500k", "Intel Core i5-2500K", "intel", 3, 24, 95),
    ("ryzen_5_3600", "AMD Ryzen 5 3600", "ryzen", 4, 64, 65),
    ("core_i9_9900k", "Intel Core i9-9900K", "intel", 5, 128, 95),
    ("ryzen_9_7950x", "AMD Ryzen 9 7950X", "ryzen", 6, 512, 170),
    ("epyc_9654", "AMD EPYC 9654", "epyc", 7, 2048, 360),
]
GPUS = [  # id, name, brand, tier, MH/s, watts
    ("geforce_gtx_460", "NVIDIA GeForce GTX 460", "nvidia", 1, 1.0, 160),
    ("radeon_hd_7970", "AMD Radeon HD 7970", "amd", 2, 2.5, 250),
    ("radeon_rx_580", "AMD Radeon RX 580", "amd", 3, 4.0, 185),
    ("geforce_gtx_1080_ti", "NVIDIA GeForce GTX 1080 Ti", "nvidia", 4, 8.0, 250),
    ("geforce_rtx_3090", "NVIDIA GeForce RTX 3090", "nvidia", 5, 20.0, 350),
    ("geforce_rtx_4090", "NVIDIA GeForce RTX 4090", "nvidia", 6, 40.0, 450),
]
BOARDS = [  # id, name, pcb, accent, cpu, gpu, watts, storage bays
    ("office_motherboard", "Office Motherboard", 0x2E7D32, 0x1B1B1B, 1, 1, 15, 1),
    ("gaming_motherboard", "Gaming Motherboard", 0x212121, 0xD32F2F, 1, 2, 25, 2),
    ("workstation_motherboard", "Workstation Motherboard", 0x0D47A1, 0xE0E0E0, 2, 3, 40, 4),
    ("mining_motherboard", "Mining Motherboard", 0x4E342E, 0xF7931A, 1, 6, 35, 1),
    ("server_motherboard", "Server Motherboard", 0x1B3B2F, 0x00A39A, 4, 4, 60, 4),
]
STORAGE = [  # id, name, texture kind, GB, watts
    ("hdd_500gb", "500 GB Hard Drive", "hdd", 500, 8),
    ("ssd_1tb", "1 TB SATA SSD", "ssd", 1024, 5),
    ("nvme_2tb", "2 TB NVMe SSD", "nvme", 2048, 7),
    ("enterprise_ssd_8tb", "8 TB Enterprise SSD", "enterprise", 8192, 12),
]
RACK_KINDS = [("mining", "Mining Rack"), ("hosting", "Hosting Rack"), ("fabricating", "Fabricating Rack")]
RACK_PARTS = ["single", "bottom", "middle", "top"]
CABLES = [("coax", "Coax Cable"), ("cat5e", "Cat5e Cable"), ("cat6a", "Cat6a Cable"), ("fiber", "Fiber Cable")]
PSUS = [  # id, name, label colour, watts, efficiency
    ("psu_300w", "300W Power Supply", 0xE0E0E0, 300, 0.75),
    ("psu_550w", "550W Power Supply (80+ Bronze)", 0xCD7F32, 550, 0.82),
    ("psu_850w", "850W Power Supply (80+ Gold)", 0xFFC107, 850, 0.87),
    ("psu_1200w", "1200W Power Supply (80+ Platinum)", 0xB0BEC5, 1200, 0.90),
    ("psu_2000w", "2000W Power Supply (80+ Titanium)", 0x78909C, 2000, 0.92),
    ("psu_3200w", "3200W Industrial Power Supply", 0x7E57C2, 3200, 0.94),
]

ITEM_TEXTURES = {
    "credit": tex_credit(), "credit_bundle": tex_bundle(), "credit_crate": tex_crate(), "bitcoin": tex_bitcoin(),
    "silicon": tex_silicon(), "circuit_board": tex_circuit(), "server": tex_server_item(),
    "network_tester": tex_tester(),
}
for (i, n, b, t, *_r) in CPUS:
    ITEM_TEXTURES[i] = tex_cpu(b, t)
for (i, n, b, t, *_r) in GPUS:
    ITEM_TEXTURES[i] = tex_gpu(b, t)
for (i, n, pcb, acc, *_r) in BOARDS:
    ITEM_TEXTURES[i] = tex_motherboard(pcb, acc)
for (i, n, lab, *_r) in PSUS:
    ITEM_TEXTURES[i] = tex_psu(lab)
for (i, n, kind, *_r) in STORAGE:
    ITEM_TEXTURES[i] = tex_storage(kind)

# ============================================================ assets
for name, canvas in ITEM_TEXTURES.items():
    canvas.save(os.path.join(ASSETS, "textures", "item", name + ".png"))
    write_json(os.path.join(ASSETS, "models", "item", name + ".json"),
               {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{name}"}})
    write_json(os.path.join(ASSETS, "items", name + ".json"),
               {"model": {"type": "minecraft:model", "model": f"{MOD}:item/{name}"}})

BLOCK_TEX = lambda n: os.path.join(ASSETS, "textures", "block", n + ".png")
BLOCK_MODEL = lambda n: os.path.join(ASSETS, "models", "block", n + ".json")
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}


def variant(model, facing):
    v = {"model": model}
    if ROT[facing]:
        v["y"] = ROT[facing]
    return v


def orientable(name, front, side, top):
    write_json(BLOCK_MODEL(name), {"parent": "minecraft:block/orientable", "textures": {
        "front": f"{MOD}:block/{front}", "side": f"{MOD}:block/{side}", "top": f"{MOD}:block/{top}"}})


def block_item(name, model):
    write_json(os.path.join(ASSETS, "items", name + ".json"), {"model": {"type": "minecraft:model", "model": f"{MOD}:block/{model}"}})


def facing_blockstate(name, model):
    write_json(os.path.join(ASSETS, "blockstates", name + ".json"), {"variants": {
        f"facing={f}": variant(f"{MOD}:block/{model}", f) for f in ROT}})


# trading terminal
tex_terminal_front().save(BLOCK_TEX("trading_terminal_front"))
tex_terminal_side().save(BLOCK_TEX("trading_terminal_side"))
orientable("trading_terminal", "trading_terminal_front", "trading_terminal_side", "trading_terminal_side")
facing_blockstate("trading_terminal", "trading_terminal")
block_item("trading_terminal", "trading_terminal")

# server workbench
tex_workbench_top().save(BLOCK_TEX("server_workbench_top"))
tex_workbench_front().save(BLOCK_TEX("server_workbench_front"))
tex_workbench_side().save(BLOCK_TEX("server_workbench_side"))
orientable("server_workbench", "server_workbench_front", "server_workbench_side", "server_workbench_top")
facing_blockstate("server_workbench", "server_workbench")
block_item("server_workbench", "server_workbench")

# racks: one frame model per type & part, plus a 1U server plate per bay (on/off) as multipart overlay
for part in RACK_PARTS:
    tex_rack_side(part).save(BLOCK_TEX(f"rack_side_{part}"))
tex_rack_top().save(BLOCK_TEX("rack_top"))
tex_server_plate(False).save(BLOCK_TEX("rack_server"))
tex_server_plate(True).save(BLOCK_TEX("rack_server_on"))

for i in range(4):
    # bay i spans texture rows 4i+1..4i+3 -> model y 12-4i .. 15-4i; auto-UV picks the matching band
    y0, y1 = 12 - 4 * i, 15 - 4 * i
    for on in (False, True):
        tex = f"{MOD}:block/rack_server" + ("_on" if on else "")
        write_json(BLOCK_MODEL(f"rack_server_{i}" + ("_on" if on else "")), {
            "textures": {"plate": tex, "particle": tex},
            "elements": [{"from": [2, y0, -0.5], "to": [14, y1, 0.5],
                          "faces": {f: {"texture": "#plate"} for f in ("north", "east", "west", "up", "down")}}]})

for kind, _name in RACK_KINDS:
    rid = f"{kind}_rack"
    for part in RACK_PARTS:
        tex_rack_front(kind, part).save(BLOCK_TEX(f"{rid}_front_{part}"))
        orientable(f"{rid}_{part}", f"{rid}_front_{part}", f"rack_side_{part}", "rack_top")
    multipart = []
    for f in ROT:
        for part in RACK_PARTS:
            multipart.append({"when": {"facing": f, "part": part}, "apply": variant(f"{MOD}:block/{rid}_{part}", f)})
        for i in range(4):
            for on in ("false", "true"):
                model = f"{MOD}:block/rack_server_{i}" + ("_on" if on == "true" else "")
                multipart.append({"when": {"facing": f, f"server_{i}": "true", "running": on}, "apply": variant(model, f)})
    write_json(os.path.join(ASSETS, "blockstates", rid + ".json"), {"multipart": multipart})
    block_item(rid, f"{rid}_single")

# data cables: core + one arm per connected side
for tier, _name in CABLES:
    cid = f"{tier}_cable"
    tex_cable(tier).save(BLOCK_TEX(cid))
    t = {"cable": f"{MOD}:block/{cid}", "particle": f"{MOD}:block/{cid}"}
    all_faces = lambda: {f: {"texture": "#cable"} for f in ("north", "south", "east", "west", "up", "down")}
    write_json(BLOCK_MODEL(cid + "_core"), {"textures": t, "elements": [{"from": [5, 5, 5], "to": [11, 11, 11], "faces": all_faces()}]})
    write_json(BLOCK_MODEL(cid + "_side"), {"textures": t, "elements": [{"from": [5, 5, 0], "to": [11, 11, 5],
                                                                         "faces": {f: {"texture": "#cable"} for f in ("north", "east", "west", "up", "down")}}]})
    write_json(BLOCK_MODEL(cid + "_inventory"), {"parent": "minecraft:block/block", "textures": t,
                                                 "elements": [{"from": [0, 5, 5], "to": [16, 11, 11], "faces": all_faces()}]})
    side = f"{MOD}:block/{cid}_side"
    write_json(os.path.join(ASSETS, "blockstates", cid + ".json"), {"multipart": [
        {"apply": {"model": f"{MOD}:block/{cid}_core"}},
        {"when": {"north": "true"}, "apply": {"model": side}},
        {"when": {"east": "true"}, "apply": {"model": side, "y": 90}},
        {"when": {"south": "true"}, "apply": {"model": side, "y": 180}},
        {"when": {"west": "true"}, "apply": {"model": side, "y": 270}},
        {"when": {"up": "true"}, "apply": {"model": side, "x": 270}},
        {"when": {"down": "true"}, "apply": {"model": side, "x": 90}},
    ]})
    block_item(cid, cid + "_inventory")

# ============================================================ lang
lang = {
    "itemGroup.datacenterextended": "DataCenterExtended",
    "block.datacenterextended.server_workbench": "Server Workbench",
    "block.datacenterextended.trading_terminal": "Trading Terminal",
    "container.datacenterextended.server_workbench": "Server Workbench",
    "item.datacenterextended.server": "Server (1U)",
    "item.datacenterextended.network_tester": "Network Tester",
    "item.datacenterextended.credit": "Credit",
    "item.datacenterextended.credit_bundle": "Credit Bundle (9)",
    "item.datacenterextended.credit_crate": "Credit Crate (81)",
    "item.datacenterextended.bitcoin": "Bitcoin",
    "item.datacenterextended.silicon": "Silicon",
    "item.datacenterextended.circuit_board": "Circuit Board",
    "status.datacenterextended.running": "● Online",
    "status.datacenterextended.exporting": "● Sharing compute with the network",
    "status.datacenterextended.no_servers": "✖ No working server",
    "status.datacenterextended.no_energy": "✖ Not enough FE",
    "status.datacenterextended.no_target": "✖ Insert an item to fabricate",
    "status.datacenterextended.not_fabricable": "✖ Item has no file size",
    "status.datacenterextended.output_full": "✖ Output full",
    "status.datacenterextended.idle": "◌ Idle",
    "problem.datacenterextended.none": "● Running",
    "problem.datacenterextended.no_motherboard": "✖ No motherboard",
    "problem.datacenterextended.no_psu": "✖ No power supply",
    "problem.datacenterextended.psu_overloaded": "✖ PSU overloaded",
    "problem.datacenterextended.no_compute": "✖ No compute (needs CPUs)",
    "problem.datacenterextended.no_hashrate": "✖ No hashrate (needs GPUs)",
    "problem.datacenterextended.no_energy": "✖ Not enough FE",
    "gui.datacenterextended.performance": "CPU %s · GPU %s",
    "gui.datacenterextended.power": "Power %s W / %s W",
    "gui.datacenterextended.storage": "Storage %s",
    "gui.datacenterextended.workbench.insert": "Insert a server to assemble it",
    "gui.datacenterextended.workbench.boots": "● Boots",
    "gui.datacenterextended.server.running": "● Running",
    "gui.datacenterextended.rack.hashrate": "Hashrate %s",
    "gui.datacenterextended.rack.compute": "Compute %s · using %s",
    "gui.datacenterextended.rack.standalone": "Standalone (no network)",
    "gui.datacenterextended.rack.no_network": "No data cable connected",
    "gui.datacenterextended.rack.network": "Net ↓%s ↑%s (%s)",
    "gui.datacenterextended.rack.power": "Power %s W",
    "gui.datacenterextended.next_bitcoin": "Next Bitcoin in %s",
    "gui.datacenterextended.next_credit_bonus": "Next credit in %s (+%s%%)",
    "gui.datacenterextended.fabricating": "%s / %s · %s",
    "gui.datacenterextended.slot.server": "Server",
    "gui.datacenterextended.slot.server_bay": "Server bay (1U)",
    "gui.datacenterextended.slot.target": "Item to fabricate (not consumed)",
    "gui.datacenterextended.slot.output": "Output",
    "gui.datacenterextended.slot.locked": "Not supported by this motherboard",
    "hardware.datacenterextended.type.motherboard": "Motherboard",
    "hardware.datacenterextended.type.cpu": "CPU",
    "hardware.datacenterextended.type.gpu": "Graphics Card",
    "hardware.datacenterextended.type.psu": "Power Supply",
    "hardware.datacenterextended.type.storage": "Storage",
    "tooltip.datacenterextended.compute": "Compute: %s",
    "tooltip.datacenterextended.hashrate": "Hashrate: %s",
    "tooltip.datacenterextended.power": "Power draw: %s W",
    "tooltip.datacenterextended.capacity": "Capacity: %s W",
    "tooltip.datacenterextended.efficiency": "Efficiency: %s%%",
    "tooltip.datacenterextended.slots": "CPU %s · GPU %s · Storage %s",
    "tooltip.datacenterextended.storage": "Storage: %s",
    "tooltip.datacenterextended.file_size": "File size: %s",
    "tooltip.datacenterextended.server.empty": "Empty – assemble it at a Server Workbench",
    "message.datacenterextended.sold_bitcoin": "Sold %s Bitcoin for %s credits.",
    "message.datacenterextended.bitcoin_rate": "Current rate: 1 Bitcoin = %s credits. Right-click with Bitcoin to sell.",
    "message.datacenterextended.tester.header": "Compute network: %s cables, %s racks",
    "message.datacenterextended.tester.bandwidth": "Bandwidth %s (slowest cable: %s)",
    "message.datacenterextended.tester.flow": "Spare %s · wanted %s · moving %s",
    "message.datacenterextended.tester.limited": "Limited by bandwidth – upgrade the %s",
    "message.datacenterextended.tester.truncated": "Network too large – cut off at maxCablesPerNetwork",
    "message.datacenterextended.tester.none": "Not part of a compute network",
    "message.datacenterextended.tester.standalone": "Mining racks work standalone and don't use the network",
}
for group in (CPUS, GPUS, BOARDS, PSUS, STORAGE):
    for row in group:
        lang[f"item.{MOD}.{row[0]}"] = row[1]
for kind, name in RACK_KINDS:
    lang[f"block.{MOD}.{kind}_rack"] = name
    lang[f"container.{MOD}.{kind}_rack"] = name
for tier, name in CABLES:
    lang[f"block.{MOD}.{tier}_cable"] = name
write_json(os.path.join(ASSETS, "lang", "en_us.json"), lang)

# ============================================================ data: hardware data map
hw = {}
for (i, n, b, t, kbs, w) in CPUS:
    hw[f"{MOD}:{i}"] = {"type": "cpu", "compute": kbs, "power": w}
for (i, n, b, t, mhs, w) in GPUS:
    hw[f"{MOD}:{i}"] = {"type": "gpu", "hashrate": mhs, "power": w}
for (i, n, pcb, acc, cpu, gpu, w, bays) in BOARDS:
    hw[f"{MOD}:{i}"] = {"type": "motherboard", "cpu_sockets": cpu, "gpu_slots": gpu, "storage_slots": bays, "power": w}
for (i, n, lab, w, eff) in PSUS:
    hw[f"{MOD}:{i}"] = {"type": "psu", "power": w, "efficiency": eff}
for (i, n, kind, gb, w) in STORAGE:
    hw[f"{MOD}:{i}"] = {"type": "storage", "storage": gb, "power": w}
write_json(os.path.join(DATA, MOD, "data_maps", "item", "hardware.json"), {"values": hw})

# ============================================================ data: file size data map
# Generic tags first, specific ones later. Conflicts are resolved by specificity
# (item id > c:ingots/iron > c:ingots), so other mods' items get sane defaults via the c: tags.
sizes = {
    # terrain
    "#minecraft:dirt": "1 MB", "#c:cobblestones": "1 MB", "#c:stones": "2 MB", "#c:sands": "1 MB",
    "#c:gravels": "1 MB", "#c:netherracks": "1 MB", "#c:end_stones": "4 MB", "#c:obsidians": "64 MB",
    "#c:glass_blocks": "2 MB", "minecraft:clay_ball": "1 MB", "minecraft:snowball": "256 KB", "#c:ice": "2 MB",
    "minecraft:cobblestone": "1 MB",
    # wood & plants
    "#minecraft:logs": "4 MB", "#minecraft:planks": "1 MB", "#minecraft:saplings": "2 MB", "#minecraft:leaves": "512 KB",
    "#c:crops": "2 MB", "#c:seeds": "512 KB", "#c:flowers": "2 MB", "#c:mushrooms": "2 MB", "#c:dyes": "4 MB",
    "minecraft:sugar_cane": "1 MB", "minecraft:cactus": "1 MB", "minecraft:bamboo": "512 KB",
    # ores (generic -> specific)
    "#c:ores": "32 MB", "#c:ores/coal": "12 MB", "#c:ores/copper": "24 MB", "#c:ores/iron": "48 MB",
    "#c:ores/gold": "96 MB", "#c:ores/redstone": "32 MB", "#c:ores/lapis": "32 MB", "#c:ores/quartz": "24 MB",
    "#c:ores/diamond": "768 MB", "#c:ores/emerald": "640 MB", "#c:ores/netherite_scrap": "4 GB",
    "#c:raw_materials": "24 MB", "#c:raw_materials/copper": "16 MB", "#c:raw_materials/iron": "32 MB", "#c:raw_materials/gold": "64 MB",
    "#c:ingots": "48 MB", "#c:ingots/copper": "16 MB", "#c:ingots/iron": "32 MB", "#c:ingots/gold": "64 MB", "#c:ingots/netherite": "8 GB",
    "#c:nuggets": "4 MB", "#c:nuggets/copper": "1.8 MB", "#c:nuggets/iron": "3.6 MB", "#c:nuggets/gold": "7.2 MB",
    "#c:gems": "128 MB", "#c:gems/lapis": "8 MB", "#c:gems/quartz": "12 MB", "#c:gems/amethyst": "16 MB",
    "#c:gems/prismarine": "16 MB", "#c:gems/emerald": "384 MB", "#c:gems/diamond": "512 MB",
    "#c:dusts": "16 MB", "#c:dusts/redstone": "8 MB", "#c:dusts/glowstone": "12 MB",
    "minecraft:coal": "8 MB", "minecraft:charcoal": "6 MB", "minecraft:netherite_scrap": "1 GB",
    "#c:storage_blocks": "432 MB", "#c:storage_blocks/coal": "72 MB", "#c:storage_blocks/copper": "144 MB",
    "#c:storage_blocks/iron": "288 MB", "#c:storage_blocks/gold": "576 MB", "#c:storage_blocks/redstone": "72 MB",
    "#c:storage_blocks/lapis": "72 MB", "#c:storage_blocks/emerald": "3.4 GB", "#c:storage_blocks/diamond": "4.5 GB",
    "#c:storage_blocks/netherite": "72 GB",
    # common modded metals (tags are simply empty when no mod provides them)
    "#c:ingots/tin": "24 MB", "#c:ingots/zinc": "20 MB", "#c:ingots/aluminum": "24 MB", "#c:ingots/lead": "32 MB",
    "#c:ingots/nickel": "40 MB", "#c:ingots/osmium": "48 MB", "#c:ingots/silver": "64 MB", "#c:ingots/bronze": "28 MB",
    "#c:ingots/steel": "64 MB", "#c:ingots/uranium": "128 MB", "#c:ingots/platinum": "160 MB",
    "#c:ores/tin": "36 MB", "#c:ores/zinc": "30 MB", "#c:ores/aluminum": "36 MB", "#c:ores/lead": "48 MB",
    "#c:ores/nickel": "60 MB", "#c:ores/osmium": "72 MB", "#c:ores/silver": "96 MB", "#c:ores/uranium": "192 MB",
    "#c:ores/platinum": "240 MB",
    # mob drops & misc
    "#c:strings": "4 MB", "#c:feathers": "4 MB", "#c:bones": "8 MB", "#c:leathers": "16 MB", "#c:gunpowders": "24 MB",
    "#c:slime_balls": "32 MB", "#c:ender_pearls": "256 MB", "#c:rods/blaze": "192 MB", "minecraft:ghast_tear": "256 MB",
    "minecraft:rotten_flesh": "2 MB", "minecraft:spider_eye": "8 MB", "minecraft:phantom_membrane": "64 MB",
    "minecraft:shulker_shell": "512 MB", "#c:nether_stars": "64 GB", "minecraft:echo_shard": "1 GB",
    "minecraft:wheat": "2 MB", "minecraft:egg": "2 MB",
}
write_json(os.path.join(DATA, MOD, "data_maps", "item", "file_size.json"), {"values": sizes})

# ============================================================ data: tags
RACK_IDS = [f"{MOD}:{k}_rack" for k, _ in RACK_KINDS]
CABLE_IDS = [f"{MOD}:{t}_cable" for t, _ in CABLES]
not_fab = [f"{MOD}:{r[0]}" for g in (CPUS, GPUS, BOARDS, PSUS, STORAGE) for r in g]
not_fab += [f"{MOD}:{x}" for x in ("credit", "credit_bundle", "credit_crate", "bitcoin", "server", "network_tester",
                                   "server_workbench", "trading_terminal")]
not_fab += RACK_IDS + CABLE_IDS
write_json(os.path.join(DATA, MOD, "tags", "item", "not_fabricable.json"), {"values": not_fab})
HEAVY = [f"{MOD}:server_workbench", f"{MOD}:trading_terminal"] + RACK_IDS
write_json(os.path.join(DATA, "minecraft", "tags", "block", "mineable", "pickaxe.json"), {"values": HEAVY})
write_json(os.path.join(DATA, "minecraft", "tags", "block", "needs_stone_tool.json"), {"values": HEAVY})

# ============================================================ data: loot tables
for full in HEAVY + CABLE_IDS:
    b = full.split(":")[1]
    write_json(os.path.join(DATA, MOD, "loot_table", "blocks", b + ".json"), {
        "type": "minecraft:block",
        "pools": [{"condition": {"type": "minecraft:survives_explosion"},
                   "entries": [{"type": "minecraft:item",
                                "modifier": {"type": "minecraft:copy_components", "include": ["minecraft:custom_name"], "source": "block_entity"},
                                "name": full}],
                   "rolls": 1}],
        "random_sequence": f"{MOD}:blocks/{b}"})

# ============================================================ data: game tests (./gradlew runGameTestServer)
for test, ticks in (("network_sharing", 200), ("rack_merging", 100), ("hosting_storage_bonus", 100)):
    write_json(os.path.join(DATA, MOD, "test_instance", test + ".json"), {
        "type": "minecraft:function", "environment": "minecraft:default", "function": f"{MOD}:{test}",
        "max_ticks": ticks, "setup_ticks": 1, "required": True, "structure": "minecraft:empty"})

# ============================================================ data: recipes
RECIPES = os.path.join(DATA, MOD, "recipe")


def shaped(name, pattern, key, result, count=1, category="misc"):
    r = {"type": "minecraft:crafting_shaped", "category": category, "key": key, "pattern": pattern,
         "result": {"id": result, **({"count": count} if count > 1 else {})}}
    write_json(os.path.join(RECIPES, name + ".json"), r)


def shapeless(name, ingredients, result, count=1):
    write_json(os.path.join(RECIPES, name + ".json"), {
        "type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": ingredients,
        "result": {"id": result, **({"count": count} if count > 1 else {})}})


M = lambda n: f"{MOD}:{n}"
shapeless("silicon", ["#c:sands", "#c:gems/quartz"], M("silicon"), 2)
shaped("circuit_board", ["CRC", "SGS", "CRC"], {"C": "#c:ingots/copper", "R": "#c:dusts/redstone", "S": M("silicon"), "G": "minecraft:green_dye"}, M("circuit_board"), 2)
shapeless("credit_bundle", [M("credit")] * 9, M("credit_bundle"))
shapeless("credit_from_bundle", [M("credit_bundle")], M("credit"), 9)
shapeless("credit_crate", [M("credit_bundle")] * 9, M("credit_crate"))
shapeless("credit_bundle_from_crate", [M("credit_crate")], M("credit_bundle"), 9)
shaped("server", ["III", "PCP", "III"], {"I": "#c:ingots/iron", "P": "#c:glass_panes", "C": M("circuit_board")}, M("server"), category="redstone")
shaped("server_workbench", ["III", "CTC", "I I"], {"I": "#c:ingots/iron", "C": M("circuit_board"), "T": "minecraft:crafting_table"}, M("server_workbench"), category="redstone")
shaped("network_tester", ["G", "C", "I"], {"G": "#c:glass_panes", "C": M("circuit_board"), "I": "#c:ingots/iron"}, M("network_tester"), category="equipment")
RACK_CORE = {"mining": "#c:ingots/gold", "hosting": M("circuit_board"), "fabricating": "minecraft:crafter"}
for kind, _n in RACK_KINDS:
    shaped(f"{kind}_rack", ["IBI", "IXI", "IBI"], {"I": "#c:ingots/iron", "B": "minecraft:iron_bars", "X": RACK_CORE[kind]},
           M(f"{kind}_rack"), category="redstone")
shaped("coax_cable", ["CCC", "RSR", "CCC"], {"C": "#c:ingots/copper", "R": "#c:dusts/redstone", "S": "#c:strings"}, M("coax_cable"), 8, "redstone")
shaped("cat5e_cable", ["XXX", "XCX", "XXX"], {"X": M("coax_cable"), "C": M("circuit_board")}, M("cat5e_cable"), 8, "redstone")
shaped("cat6a_cable", ["XXX", "XGX", "XXX"], {"X": M("cat5e_cable"), "G": "#c:ingots/gold"}, M("cat6a_cable"), 8, "redstone")
shaped("fiber_cable", ["XXX", "PDP", "XXX"], {"X": M("cat6a_cable"), "P": "#c:glass_panes", "D": "#c:gems/diamond"}, M("fiber_cable"), 8, "redstone")
shaped("trading_terminal", ["IPI", "CEC", "III"], {"I": "#c:ingots/iron", "P": "#c:glass_panes", "C": M("circuit_board"), "E": "#c:gems/emerald"}, M("trading_terminal"), category="redstone")

# tier-1 parts need no credits (bootstrap); later tiers need the previous tier + money
T1_MONEY = "#c:dusts/redstone"
hardware_recipes = {
    # id: (material M, side S, money $, core X)
    "pentium_4": ("#c:ingots/iron", M("silicon"), T1_MONEY, M("circuit_board")),
    "amd_fx_4300": ("#c:ingots/copper", M("silicon"), M("credit"), M("pentium_4")),
    "core_i5_2500k": ("#c:ingots/gold", M("silicon"), M("credit_bundle"), M("amd_fx_4300")),
    "ryzen_5_3600": ("#c:storage_blocks/redstone", M("silicon"), M("credit_bundle"), M("core_i5_2500k")),
    "core_i9_9900k": ("#c:gems/diamond", M("silicon"), M("credit_bundle"), M("ryzen_5_3600")),
    "ryzen_9_7950x": ("#c:gems/emerald", M("silicon"), M("credit_crate"), M("core_i9_9900k")),
    "epyc_9654": ("#c:ingots/netherite", M("silicon"), M("credit_crate"), M("ryzen_9_7950x")),

    "geforce_gtx_460": ("#c:ingots/iron", "#c:ingots/copper", T1_MONEY, M("circuit_board")),
    "radeon_hd_7970": ("#c:ingots/gold", "#c:ingots/copper", M("credit"), M("geforce_gtx_460")),
    "radeon_rx_580": ("#c:storage_blocks/redstone", "#c:ingots/copper", M("credit_bundle"), M("radeon_hd_7970")),
    "geforce_gtx_1080_ti": ("#c:gems/diamond", "#c:ingots/copper", M("credit_bundle"), M("radeon_rx_580")),
    "geforce_rtx_3090": ("#c:gems/emerald", "#c:ingots/copper", M("credit_crate"), M("geforce_gtx_1080_ti")),
    "geforce_rtx_4090": ("#c:ingots/netherite", "#c:ingots/copper", M("credit_crate"), M("geforce_rtx_3090")),

    "office_motherboard": ("#c:ingots/iron", M("circuit_board"), T1_MONEY, M("silicon")),
    "gaming_motherboard": ("#c:ingots/copper", M("circuit_board"), M("credit"), M("office_motherboard")),
    "workstation_motherboard": ("#c:ingots/gold", M("circuit_board"), M("credit_bundle"), M("gaming_motherboard")),
    "mining_motherboard": ("#c:ingots/gold", M("circuit_board"), M("credit_bundle"), M("geforce_gtx_460")),
    "server_motherboard": ("#c:gems/diamond", M("circuit_board"), M("credit_crate"), M("workstation_motherboard")),

    "psu_300w": ("#c:ingots/iron", "#c:dusts/redstone", "#c:ingots/copper", M("circuit_board")),
    "psu_550w": ("#c:ingots/copper", "#c:dusts/redstone", M("credit"), M("psu_300w")),
    "psu_850w": ("#c:ingots/gold", "#c:dusts/redstone", M("credit_bundle"), M("psu_550w")),
    "psu_1200w": ("#c:storage_blocks/iron", "#c:dusts/redstone", M("credit_bundle"), M("psu_850w")),
    "psu_2000w": ("#c:gems/diamond", "#c:dusts/redstone", M("credit_crate"), M("psu_1200w")),
    "psu_3200w": ("#c:ingots/netherite", "#c:dusts/redstone", M("credit_crate"), M("psu_2000w")),

    "hdd_500gb": ("#c:ingots/iron", "#c:dusts/redstone", "#c:ingots/copper", M("circuit_board")),
    "ssd_1tb": ("#c:ingots/gold", M("silicon"), M("credit"), M("hdd_500gb")),
    "nvme_2tb": ("#c:gems/diamond", M("silicon"), M("credit_bundle"), M("ssd_1tb")),
    "enterprise_ssd_8tb": ("#c:gems/emerald", M("silicon"), M("credit_crate"), M("nvme_2tb")),
}
for rid, (m, s, money, core) in hardware_recipes.items():
    shaped(rid, ["MSM", "$X$", "MSM"], {"M": m, "S": s, "$": money, "X": core}, M(rid))

print("generated", len(ITEM_TEXTURES), "item textures,", len(os.listdir(RECIPES)), "recipes")
