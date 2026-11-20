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
    c.bevel(2, 2, 13, 13, 0x1B3B2F if brand in ("epyc", "xeon") else 0x2E7D32)  # substrate
    c.bevel(4, 4, 11, 11, 0xB8BCC2)  # heat spreader
    logo = {"intel": 0x0071C5, "amd": 0xED1C24, "ryzen": 0xF26522, "epyc": 0x00A39A, "xeon": 0x5E8EDB}[brand]
    c.rect(5, 5, 10, 6, logo)
    c.rect(5, 7, 7, 7, shade(logo, 0.7))
    # tier dots
    for t in range(tier):
        c.set(5 + t % 6, 9 if t < 6 else 10, 0xFFD54F)
    return c


def tex_gpu(brand, tier):
    c = Canvas()
    shroud = {"nvidia": 0x2B2B2B, "amd": 0x3A1010, "nvidia_dc": 0xB8A04A}[brand]
    accent = {"nvidia": 0x76B900, "amd": 0xED1C24, "nvidia_dc": 0x76B900}[brand]
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


MEMORY_LOOK = {  # pcb, heat spreader (None = bare chips)
    "ddr2": (0x1B5E20, None), "ddr3": (0x1B5E20, 0x37474F), "ddr4": (0x1B5E20, 0x263238), "ddr5": (0x0D47A1, 0x212121),
}


def tex_memory(kind, tier, ecc):
    """A DIMM: PCB with gold contacts, chips or a heat spreader, an accent stripe per tier."""
    c = Canvas()
    pcb, spreader = MEMORY_LOOK[kind]
    c.bevel(0, 4, 15, 11, pcb)
    for x in range(1, 15):
        if x != 7:
            c.set(x, 11, 0xC9A227)  # contacts, with the key notch
    if spreader is None:
        for x in range(1, 15, 3):
            c.rect(x, 6, x + 1, 9, 0x151515)
    else:
        c.bevel(0, 4, 15, 9, spreader)
        c.rect(2, 6, 2 + min(11, 2 * tier), 6, {"ddr3": 0x29B6F6, "ddr4": 0xE53935, "ddr5": 0xFFFFFF}[kind])
    if ecc:
        c.rect(11, 7, 14, 8, 0x00A39A)  # ECC / registered buffer chip
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


def tex_processor_die():
    c = Canvas()
    for i in range(3, 13, 2):
        for (x, y) in ((i, 2), (i, 13), (2, i), (13, i)):
            c.set(x, y, 0xC9A227)
    c.bevel(3, 3, 12, 12, 0x5C6BC0)
    for y in range(5, 11, 2):
        for x in range(5, 11, 2):
            c.set(x, y, 0x9FA8DA)
    c.set(10, 10, 0xFFD54F)
    return c


def tex_shop_front():
    """Shop counter: striped awning, a screen with a shopping cart."""
    c = Canvas(0x37474F)
    for x in range(16):
        c.rect(x, 0, x, 2, 0xE53935 if (x // 2) % 2 == 0 else 0xFAFAFA)
    c.rect(0, 3, 15, 3, 0x263238)
    c.rect(2, 5, 13, 11, 0x0B1F12)
    c.rect(4, 7, 10, 7, 0x00E676)  # cart basket
    c.rect(5, 8, 9, 9, 0x00E676)
    c.set(3, 6, 0x00E676)
    c.set(5, 10, 0xFFD54F)
    c.set(9, 10, 0xFFD54F)
    c.rect(11, 6, 12, 6, 0xF7931A)  # coin
    c.rect(1, 13, 14, 14, 0x546E7A)
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
# rarity: C common, U uncommon, R rare, E epic (must match ModItems)
CPUS = [  # id, name, brand, tier, compute KB/s, watts, socket, recommended RAM GB, rarity
    ("pentium_4", "Intel Pentium 4 HT", "intel", 1, 4, 85, "lga775", 1, "C"),
    ("core_2_quad_q6600", "Intel Core 2 Quad Q6600", "intel", 1, 8, 105, "lga775", 4, "C"),
    ("amd_fx_4300", "AMD FX-4300", "amd", 2, 10, 95, "am3plus", 4, "C"),
    ("amd_fx_8350", "AMD FX-8350", "amd", 2, 18, 125, "am3plus", 8, "C"),
    ("core_i5_2500k", "Intel Core i5-2500K", "intel", 3, 24, 95, "lga1155", 8, "C"),
    ("core_i7_3770k", "Intel Core i7-3770K", "intel", 3, 32, 77, "lga1155", 16, "C"),
    ("xeon_e5_2690", "Intel Xeon E5-2690", "xeon", 3, 40, 135, "lga2011", 32, "U"),
    ("core_i7_8700k", "Intel Core i7-8700K", "intel", 4, 72, 95, "lga1151", 16, "U"),
    ("core_i9_9900k", "Intel Core i9-9900K", "intel", 5, 128, 95, "lga1151", 32, "U"),
    ("ryzen_5_3600", "AMD Ryzen 5 3600", "ryzen", 4, 64, 65, "am4", 16, "U"),
    ("ryzen_7_5800x3d", "AMD Ryzen 7 5800X3D", "ryzen", 5, 160, 105, "am4", 32, "U"),
    ("epyc_7742", "AMD EPYC 7742", "epyc", 6, 1024, 225, "sp3", 256, "R"),
    ("core_i5_13600k", "Intel Core i5-13600K", "intel", 5, 220, 181, "lga1700", 32, "R"),
    ("core_i9_13900k", "Intel Core i9-13900K", "intel", 6, 400, 253, "lga1700", 64, "R"),
    ("ryzen_7_9800x3d", "AMD Ryzen 7 9800X3D", "ryzen", 6, 360, 120, "am5", 32, "R"),
    ("ryzen_9_7950x", "AMD Ryzen 9 7950X", "ryzen", 6, 512, 170, "am5", 64, "R"),
    ("epyc_9654", "AMD EPYC 9654", "epyc", 7, 2048, 360, "sp5", 384, "E"),
    ("xeon_platinum_8480", "Intel Xeon Platinum 8480+", "xeon", 7, 1536, 350, "lga4677", 256, "E"),
]
GPUS = [  # id, name, brand, tier, MH/s, watts, rarity
    ("geforce_gtx_460", "NVIDIA GeForce GTX 460", "nvidia", 1, 1.0, 160, "C"),
    ("radeon_hd_7970", "AMD Radeon HD 7970", "amd", 2, 2.5, 250, "C"),
    ("geforce_gtx_970", "NVIDIA GeForce GTX 970", "nvidia", 2, 3.0, 145, "C"),
    ("radeon_rx_580", "AMD Radeon RX 580", "amd", 3, 4.0, 185, "U"),
    ("geforce_gtx_1080_ti", "NVIDIA GeForce GTX 1080 Ti", "nvidia", 4, 8.0, 250, "U"),
    ("geforce_rtx_2080_ti", "NVIDIA GeForce RTX 2080 Ti", "nvidia", 4, 12.0, 250, "U"),
    ("radeon_rx_6800_xt", "AMD Radeon RX 6800 XT", "amd", 5, 14.0, 300, "R"),
    ("geforce_rtx_3090", "NVIDIA GeForce RTX 3090", "nvidia", 5, 20.0, 350, "R"),
    ("radeon_rx_7900_xtx", "AMD Radeon RX 7900 XTX", "amd", 6, 28.0, 355, "R"),
    ("geforce_rtx_4090", "NVIDIA GeForce RTX 4090", "nvidia", 6, 40.0, 450, "E"),
    ("geforce_rtx_5090", "NVIDIA GeForce RTX 5090", "nvidia", 7, 60.0, 575, "E"),
    ("nvidia_h100", "NVIDIA H100 PCIe", "nvidia_dc", 7, 100.0, 700, "E"),
]
BOARDS = [  # id, name, pcb, accent, socket, memory type, cpu, gpu, storage, memory slots, max RAM GB, watts, rarity
    ("office_motherboard", "Office Motherboard (LGA775)", 0x2E7D32, 0x1B1B1B, "lga775", "ddr2", 1, 1, 1, 2, 8, 15, "C"),
    ("am3_motherboard", "970 Motherboard (AM3+)", 0x3E2723, 0xED1C24, "am3plus", "ddr3", 1, 2, 2, 4, 32, 20, "C"),
    ("z77_motherboard", "Z77 Motherboard (LGA1155)", 0x263238, 0x0071C5, "lga1155", "ddr3", 1, 2, 2, 4, 32, 20, "C"),
    ("workstation_motherboard", "Dual-Xeon Workstation Motherboard (LGA2011)", 0x0D47A1, 0xE0E0E0, "lga2011", "ddr3", 2, 3, 4, 8, 256, 45, "U"),
    ("gaming_motherboard", "Z390 Gaming Motherboard (LGA1151)", 0x212121, 0xD32F2F, "lga1151", "ddr4", 1, 2, 3, 4, 128, 25, "U"),
    ("mining_motherboard", "Mining Motherboard (LGA1151)", 0x4E342E, 0xF7931A, "lga1151", "ddr4", 1, 6, 1, 2, 32, 35, "U"),
    ("x570_motherboard", "X570 Motherboard (AM4)", 0x1A1A1A, 0xF26522, "am4", "ddr4", 1, 3, 4, 4, 128, 30, "U"),
    ("epyc_sp3_motherboard", "EPYC Server Motherboard (SP3)", 0x1B3B2F, 0x4DB6AC, "sp3", "ddr4", 2, 4, 4, 8, 2048, 60, "R"),
    ("z790_motherboard", "Z790 Motherboard (LGA1700)", 0x111827, 0x29B6F6, "lga1700", "ddr5", 1, 3, 4, 4, 192, 35, "R"),
    ("x670e_motherboard", "X670E Motherboard (AM5)", 0x1C1C1C, 0xFF7043, "am5", "ddr5", 1, 3, 4, 4, 192, 35, "R"),
    ("server_motherboard", "EPYC Server Motherboard (SP5)", 0x1B3B2F, 0x00A39A, "sp5", "ddr5", 2, 4, 4, 8, 3072, 70, "E"),
    ("xeon_quad_motherboard", "Quad-Xeon Server Motherboard (LGA4677)", 0x0B2545, 0x5E8EDB, "lga4677", "ddr5", 4, 4, 4, 8, 4096, 90, "E"),
]
MEMORY = [  # id, name, type, GB, MT/s, watts, ecc, rarity
    ("ddr2_1gb", "1 GB DDR2-667", "ddr2", 1, 667, 2, False, "C"),
    ("ddr2_2gb", "2 GB DDR2-800", "ddr2", 2, 800, 2, False, "C"),
    ("ddr3_4gb", "4 GB DDR3-1333", "ddr3", 4, 1333, 3, False, "C"),
    ("ddr3_8gb", "8 GB DDR3-1600", "ddr3", 8, 1600, 3, False, "C"),
    ("ddr3_32gb_ecc", "32 GB DDR3-1600 ECC RDIMM", "ddr3", 32, 1600, 5, True, "U"),
    ("ddr4_8gb", "8 GB DDR4-2400", "ddr4", 8, 2400, 3, False, "U"),
    ("ddr4_16gb", "16 GB DDR4-3200", "ddr4", 16, 3200, 4, False, "U"),
    ("ddr4_64gb_ecc", "64 GB DDR4-3200 ECC RDIMM", "ddr4", 64, 3200, 6, True, "R"),
    ("ddr5_16gb", "16 GB DDR5-4800", "ddr5", 16, 4800, 4, False, "R"),
    ("ddr5_32gb", "32 GB DDR5-6000", "ddr5", 32, 6000, 5, False, "R"),
    ("ddr5_128gb_ecc", "128 GB DDR5-4800 ECC RDIMM", "ddr5", 128, 4800, 8, True, "E"),
]
SOCKETS = {"lga775": "LGA775", "am3plus": "AM3+", "lga1155": "LGA1155", "lga2011": "LGA2011", "lga1151": "LGA1151",
           "am4": "AM4", "sp3": "SP3", "lga1700": "LGA1700", "am5": "AM5", "sp5": "SP5", "lga4677": "LGA4677"}
STORAGE = [  # id, name, texture kind, GB, watts, rarity
    ("hdd_500gb", "500 GB Hard Drive", "hdd", 500, 8, "C"),
    ("ssd_1tb", "1 TB SATA SSD", "ssd", 1024, 5, "U"),
    ("nvme_2tb", "2 TB NVMe SSD", "nvme", 2048, 7, "R"),
    ("enterprise_ssd_8tb", "8 TB Enterprise SSD", "enterprise", 8192, 12, "E"),
]
RACK_KINDS = [("mining", "Mining Rack"), ("hosting", "Hosting Rack"), ("fabricating", "Fabricating Rack")]
RACK_PARTS = ["single", "bottom", "middle", "top"]
CABLES = [("coax", "Coax Cable"), ("cat5e", "Cat5e Cable"), ("cat6a", "Cat6a Cable"), ("fiber", "Fiber Cable")]
PSUS = [  # id, name, label colour, watts, efficiency, rarity
    ("psu_300w", "300W Power Supply", 0xE0E0E0, 300, 0.75, "C"),
    ("psu_550w", "550W Power Supply (80+ Bronze)", 0xCD7F32, 550, 0.82, "C"),
    ("psu_850w", "850W Power Supply (80+ Gold)", 0xFFC107, 850, 0.87, "U"),
    ("psu_1200w", "1200W Power Supply (80+ Platinum)", 0xB0BEC5, 1200, 0.90, "U"),
    ("psu_2000w", "2000W Power Supply (80+ Titanium)", 0x78909C, 2000, 0.92, "R"),
    ("psu_3200w", "3200W Industrial Power Supply", 0x7E57C2, 3200, 0.94, "E"),
]

ITEM_TEXTURES = {
    "processor_die": tex_processor_die(),
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
for (i, n, kind, gb, mts, w, ecc, rar) in MEMORY:
    ITEM_TEXTURES[i] = tex_memory(kind, 1 + "CURE".index(rar) * 2 + (gb >= 16), ecc)

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


# hardware shop
tex_shop_front().save(BLOCK_TEX("hardware_shop_front"))
tex_terminal_side().save(BLOCK_TEX("hardware_shop_side"))
orientable("hardware_shop", "hardware_shop_front", "hardware_shop_side", "hardware_shop_side")
facing_blockstate("hardware_shop", "hardware_shop")
block_item("hardware_shop", "hardware_shop")

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
    # bay i spans texture rows 4i+1..4i+3 -> model y 12-4i .. 15-4i
    # explicit UVs: the auto-UV of an element sticking out of the block reaches outside the 16x16 texture
    y0, y1 = 12 - 4 * i, 15 - 4 * i
    v0, v1 = 1 + 4 * i, 4 + 4 * i  # texture rows of bay i
    uvs = {"north": [2, v0, 14, v1], "east": [2, v0, 3, v1], "west": [2, v0, 3, v1],
           "up": [2, v0, 14, v0 + 1], "down": [2, v1 - 1, 14, v1]}
    for on in (False, True):
        tex = f"{MOD}:block/rack_server" + ("_on" if on else "")
        write_json(BLOCK_MODEL(f"rack_server_{i}" + ("_on" if on else "")), {
            "textures": {"plate": tex, "particle": tex},
            "elements": [{"from": [2, y0, -0.5], "to": [14, y1, 0.5],
                          "faces": {f: {"texture": "#plate", "uv": uv} for f, uv in uvs.items()}}]})

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
    "block.datacenterextended.hardware_shop": "Hardware Shop",
    "container.datacenterextended.server_workbench": "Server Workbench",
    "item.datacenterextended.server": "Server (1U)",
    "item.datacenterextended.network_tester": "Network Tester",
    "item.datacenterextended.processor_die": "Processor Die",
    "key.datacenterextended.wallet": "Open Wallet / Shop",
    "key.category.datacenterextended.main": "DataCenterExtended",
    "coin.datacenterextended.bitcoin": "Bitcoin",
    "coin.datacenterextended.dogecoin": "Dogecoin",
    "coin.datacenterextended.ethereum": "Ethereum",
    "status.datacenterextended.no_owner": "✖ No owner – break and place it again",
    "gui.datacenterextended.wallet.title": "Wallet",
    "gui.datacenterextended.wallet.tab": "Wallet",
    "gui.datacenterextended.wallet.all": "All",
    "gui.datacenterextended.wallet.amount": "Amount",
    "gui.datacenterextended.wallet.sell": "Sell",
    "gui.datacenterextended.wallet.fee": "Fee %s%% · %s",
    "gui.datacenterextended.wallet.hint": "Click a coin to show its chart",
    "gui.datacenterextended.wallet.chart": "%s price",
    "gui.datacenterextended.wallet.no_history": "No price history yet – check back in a minute",
    "gui.datacenterextended.shop.tab": "Shop",
    "gui.datacenterextended.shop.infrastructure": "Infrastructure",
    "gui.datacenterextended.shop.quantity": "Quantity",
    "gui.datacenterextended.shop.buy": "Buy",
    "gui.datacenterextended.shop.price": "%s each",
    "gui.datacenterextended.shop.total": "= %s",
    "gui.datacenterextended.shop.select": "Select an item to buy",
    "gui.datacenterextended.shop.remote": "Remote order",
    "gui.datacenterextended.income.per_hour": "Income per hour",
    "gui.datacenterextended.income.per_day": "%s / MC day",
    "gui.datacenterextended.income.wallet": "→ owner's wallet",
    "gui.datacenterextended.rack.storage_bonus": "Storage bonus +%s%%",
    "message.datacenterextended.sold_coin": "Sold %s for %s",
    "message.datacenterextended.shop.bought": "Bought %sx %s for %s",
    "message.datacenterextended.shop.too_expensive": "Not enough money (%s needed)",
    "message.datacenterextended.shop.no_access": "Use a Hardware Shop block to buy (remote shop is disabled)",
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
    "problem.datacenterextended.no_cpu": "✖ No CPU",
    "problem.datacenterextended.incompatible_cpu": "✖ CPU doesn't fit the socket",
    "problem.datacenterextended.incompatible_memory": "✖ Wrong memory type",
    "problem.datacenterextended.no_memory": "✖ No memory (RAM)",
    "problem.datacenterextended.psu_overloaded": "✖ PSU overloaded",
    "problem.datacenterextended.no_compute": "✖ No compute (needs CPUs)",
    "problem.datacenterextended.no_hashrate": "✖ No hashrate (needs GPUs)",
    "problem.datacenterextended.no_energy": "✖ Not enough FE",
    "gui.datacenterextended.performance": "CPU %s · GPU %s",
    "gui.datacenterextended.power_fe": "Power %s W / %s W · %s FE/t",
    "gui.datacenterextended.memory": "RAM %s / %s · %s · ×%s",
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
    "gui.datacenterextended.rack.power_fe": "Power %s W · %s FE/t",
    "gui.datacenterextended.energy.stored": "%s / %s FE",
    "gui.datacenterextended.energy.usage": "Using %s FE/t",
    "gui.datacenterextended.fabricating": "%s / %s · %s",
    "gui.datacenterextended.slot.server": "Server",
    "gui.datacenterextended.slot.server_bay": "Server bay (1U)",
    "gui.datacenterextended.slot.target": "Item to fabricate (not consumed)",
    "gui.datacenterextended.slot.output": "Output",
    "gui.datacenterextended.slot.locked": "Not supported by this motherboard",
    "gui.datacenterextended.slot.incompatible": "Doesn't fit this motherboard",
    "gui.datacenterextended.slot.cpu": "%s · Socket %s",
    "gui.datacenterextended.slot.memory": "%s · %s, up to %s",
    "hardware.datacenterextended.type.motherboard": "Motherboard",
    "hardware.datacenterextended.type.cpu": "CPU",
    "hardware.datacenterextended.type.gpu": "Graphics Card",
    "hardware.datacenterextended.type.psu": "Power Supply",
    "hardware.datacenterextended.type.storage": "Storage",
    "hardware.datacenterextended.type.memory": "Memory",
    "tooltip.datacenterextended.compute": "Compute: %s",
    "tooltip.datacenterextended.hashrate": "Hashrate: %s",
    "tooltip.datacenterextended.power": "Power draw: %s W",
    "tooltip.datacenterextended.capacity": "Capacity: %s W",
    "tooltip.datacenterextended.efficiency": "Efficiency: %s%%",
    "tooltip.datacenterextended.slots": "CPU %s · GPU %s · RAM %s · Storage %s",
    "tooltip.datacenterextended.board": "Socket %s · %s · max %s",
    "tooltip.datacenterextended.socket": "Socket: %s",
    "tooltip.datacenterextended.recommended_memory": "Recommended RAM: %s",
    "tooltip.datacenterextended.memory_module": "%s %s-%s",
    "tooltip.datacenterextended.psu_fe": "Up to %s FE/t at full load",
    "tooltip.datacenterextended.storage": "Storage: %s",
    "tooltip.datacenterextended.file_size": "File size: %s",
    "tooltip.datacenterextended.server.empty": "Empty – assemble it at a Server Workbench",
    "message.datacenterextended.tester.header": "Compute network: %s cables, %s racks",
    "message.datacenterextended.tester.bandwidth": "Bandwidth %s (slowest cable: %s)",
    "message.datacenterextended.tester.flow": "Spare %s · wanted %s · moving %s",
    "message.datacenterextended.tester.limited": "Limited by bandwidth – upgrade the %s",
    "message.datacenterextended.tester.truncated": "Network too large – cut off at maxCablesPerNetwork",
    "message.datacenterextended.tester.none": "Not part of a compute network",
    "message.datacenterextended.tester.standalone": "Mining racks work standalone and don't use the network",
}
for group in (CPUS, GPUS, BOARDS, PSUS, STORAGE, MEMORY):
    for row in group:
        lang[f"item.{MOD}.{row[0]}"] = row[1]
for sid, name in SOCKETS.items():
    lang[f"socket.{MOD}.{sid}"] = name
for kind, name in RACK_KINDS:
    lang[f"block.{MOD}.{kind}_rack"] = name
    lang[f"container.{MOD}.{kind}_rack"] = name
for tier, name in CABLES:
    lang[f"block.{MOD}.{tier}_cable"] = name
write_json(os.path.join(ASSETS, "lang", "en_us.json"), lang)

# ============================================================ data: hardware data map
hw = {}
for (i, n, b, t, kbs, w, sock, rec, rar) in CPUS:
    hw[f"{MOD}:{i}"] = {"type": "cpu", "socket": sock, "compute": kbs, "power": w, "recommended_memory": rec}
for (i, n, b, t, mhs, w, rar) in GPUS:
    hw[f"{MOD}:{i}"] = {"type": "gpu", "hashrate": mhs, "power": w}
for (i, n, pcb, acc, sock, mem, cpu, gpu, bays, mslots, maxmem, w, rar) in BOARDS:
    hw[f"{MOD}:{i}"] = {"type": "motherboard", "socket": sock, "memory_type": mem, "cpu_sockets": cpu, "gpu_slots": gpu,
                        "storage_slots": bays, "memory_slots": mslots, "max_memory": maxmem, "power": w}
for (i, n, kind, gb, mts, w, ecc, rar) in MEMORY:
    hw[f"{MOD}:{i}"] = {"type": "memory", "memory_type": kind, "memory": gb, "speed": mts, "power": w}
for (i, n, lab, w, eff, rar) in PSUS:
    hw[f"{MOD}:{i}"] = {"type": "psu", "power": w, "efficiency": eff}
for (i, n, kind, gb, w, rar) in STORAGE:
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
not_fab = [f"{MOD}:{r[0]}" for g in (CPUS, GPUS, BOARDS, PSUS, STORAGE, MEMORY) for r in g]
not_fab += [f"{MOD}:{x}" for x in ("processor_die", "server", "network_tester", "server_workbench", "hardware_shop")]
not_fab += RACK_IDS + CABLE_IDS
write_json(os.path.join(DATA, MOD, "tags", "item", "not_fabricable.json"), {"values": not_fab})
HEAVY = [f"{MOD}:server_workbench", f"{MOD}:hardware_shop"] + RACK_IDS
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
for test, ticks in (("network_sharing", 200), ("rack_merging", 100), ("hosting_storage_bonus", 100),
                    ("rack_energy", 100), ("memory_and_sockets", 20), ("wallet_payout", 200), ("shop_and_sell", 20),
                    ("part_recipes", 20)):
    write_json(os.path.join(DATA, MOD, "test_instance", test + ".json"), {
        "type": "minecraft:function", "environment": "minecraft:default", "function": f"{MOD}:{test}",
        "max_ticks": ticks, "setup_ticks": 1, "required": True, "structure": "minecraft:empty"})

# ============================================================ data: recipes
RECIPES = os.path.join(DATA, MOD, "recipe")


def shaped(name, pattern, key, result, count=1, category="misc", conditions=None):
    r = {"type": "minecraft:crafting_shaped", "category": category, "key": key, "pattern": pattern,
         "result": {"id": result, **({"count": count} if count > 1 else {})}}
    if conditions:
        r = {"neoforge:conditions": conditions, **r}
    write_json(os.path.join(RECIPES, name + ".json"), r)


def shapeless(name, ingredients, result, count=1):
    write_json(os.path.join(RECIPES, name + ".json"), {
        "type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": ingredients,
        "result": {"id": result, **({"count": count} if count > 1 else {})}})


M = lambda n: f"{MOD}:{n}"
shapeless("silicon", ["#c:sands", "#c:gems/quartz"], M("silicon"), 2)
shaped("circuit_board", ["CRC", "SGS", "CRC"], {"C": "#c:ingots/copper", "R": "#c:dusts/redstone", "S": M("silicon"), "G": "minecraft:green_dye"}, M("circuit_board"), 2)
shaped("processor_die", ["NRN", "SGS", "NRN"], {"N": "#c:nuggets/gold", "R": "#c:dusts/redstone", "S": M("silicon"),
                                                  "G": "#c:dusts/glowstone"}, M("processor_die"))
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
shaped("hardware_shop", ["IPI", "CEC", "III"], {"I": "#c:ingots/iron", "P": "#c:glass_panes", "C": M("circuit_board"), "E": "#c:gems/emerald"}, M("hardware_shop"), category="redstone")

# Each category is an upgrade chain: a part needs the previous part of its category plus expensive materials by
# rarity, so buying in the shop is the normal way early on. Only the first part of each chain is cheap (bootstrap).
# All part recipes carry a condition: enablePartRecipes = false in the startup config removes them.
MATERIAL = {"C": "#c:ingots/iron", "U": "#c:ingots/gold", "R": "#c:gems/diamond", "E": "#c:ingots/netherite"}
COST = {"C": M("processor_die"), "U": "#c:storage_blocks/gold", "R": "#c:storage_blocks/diamond", "E": "minecraft:echo_shard"}
PART_CONDITION = [{"type": f"{MOD}:part_recipes_enabled"}]
CHAINS = [  # parts, side ingredient
    (CPUS, M("silicon")),
    (GPUS, "#c:ingots/copper"),
    (BOARDS, M("circuit_board")),
    (MEMORY, M("silicon")),
    (PSUS, "#c:dusts/redstone"),
    (STORAGE, M("silicon")),
]
for parts, side in CHAINS:
    previous = None
    for row in parts:
        rid, rarity = row[0], row[-1]
        core = previous or M("circuit_board")
        cost = COST[rarity] if previous else "#c:dusts/redstone"
        shaped(rid, ["MSM", "$X$", "MSM"], {"M": MATERIAL[rarity], "S": side, "$": cost, "X": core}, M(rid),
               conditions=PART_CONDITION)
        previous = M(rid)

# ============================================================ data: shop prices (dollars)
def nice(x):
    """Rounds to two significant digits, e.g. 1234 -> 1200, 37.4 -> 37."""
    if x < 10:
        return round(x, 2)
    digits = len(str(int(x))) - 2
    return int(round(x / 10 ** digits) * 10 ** digits)


prices = {}
for (i, n, b, t, kbs, w, sock, rec, rar) in CPUS:
    prices[M(i)] = nice(8 * kbs ** 1.15)
for (i, n, b, t, mhs, w, rar) in GPUS:
    prices[M(i)] = nice(30 * mhs ** 1.15)
for (i, n, pcb, acc, sock, mem, cpu, gpu, bays, mslots, maxmem, w, rar) in BOARDS:
    prices[M(i)] = nice({"C": 40, "U": 300, "R": 2500, "E": 15000}[rar] * (1 + 0.25 * (cpu - 1)) * (1.5 if gpu >= 6 else 1))
for (i, n, kind, gb, mts, w, ecc, rar) in MEMORY:
    prices[M(i)] = nice(max(8, 5 * gb * mts / 1000))
for (row, price) in zip(PSUS, (25, 60, 150, 400, 1500, 6000)):
    prices[M(row[0])] = price
for (row, price) in zip(STORAGE, (30, 120, 500, 4000)):
    prices[M(row[0])] = price
prices.update({M("server"): 20, M("server_workbench"): 60, M("mining_rack"): 100, M("hosting_rack"): 100,
               M("fabricating_rack"): 500, M("coax_cable"): 2, M("cat5e_cable"): 8, M("cat6a_cable"): 40,
               M("fiber_cable"): 200, M("network_tester"): 30, M("circuit_board"): 4, M("silicon"): 1,
               M("processor_die"): 12})
write_json(os.path.join(DATA, MOD, "data_maps", "item", "shop_price.json"), {"values": prices})

print("generated", len(ITEM_TEXTURES), "item textures,", len(os.listdir(RECIPES)), "recipes")
