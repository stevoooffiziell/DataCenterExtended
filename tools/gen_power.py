# Power generation content for gen_resources.py (executed inside it with exec(), so all helpers are available).
# PHASE == "assets": textures, models, blockstates, English lang entries (before en_us.json is written).
# PHASE == "data":   tags, loot tables, worldgen, recipes, shop prices, release dates, advancements.
import math

# ============================================================ content definition (must match the Java enums)
GENERATORS = [  # id, name, family, tier, peak FE/t, shop price $, release date or None, description
    ("polycrystalline_solar_panel", "Polycrystalline Solar Panel", "solar", 1, 8, 400, None,
     "Cheap blue cells. Needs open sky, makes nothing at night."),
    ("monocrystalline_solar_panel", "Monocrystalline Solar Panel", "solar", 2, 20, 1200, "2008-01-01",
     "Single-crystal cells, more power per panel."),
    ("perc_solar_panel", "PERC Bifacial Solar Panel", "solar", 3, 40, 3000, "2016-01-01",
     "Passivated rear contact, catches light on both sides."),
    ("perovskite_solar_panel", "Perovskite Tandem Solar Panel", "solar", 4, 80, 8000, "2023-06-01",
     "Perovskite on silicon: the efficiency record holder."),
    ("small_wind_turbine", "Small Wind Turbine", "wind", 1, 24, 900, None,
     "Needs free air (3×3) in front. Higher = more wind."),
    ("medium_wind_turbine", "Medium Wind Turbine", "wind", 2, 80, 4000, "2005-01-01",
     "Needs free air (5×5) in front. Storms add wind."),
    ("large_wind_turbine", "Large Wind Turbine", "wind", 3, 240, 15000, "2014-01-01",
     "Needs free air (7×7) in front. Best above y 160."),
    ("water_wheel", "Water Wheel", "water", 1, 16, 300, None,
     "Put flowing water next to its sides."),
    ("micro_hydro_turbine", "Micro-Hydro Turbine", "water", 2, 60, 1500, None,
     "Uses the water column above it (up to 30 blocks)."),
    ("hydroelectric_turbine", "Hydroelectric Turbine", "water", 3, 192, 7000, None,
     "A dam turbine: up to 32 blocks of water above it."),
    ("combustion_generator", "Combustion Generator", "combustion", 1, 40, 250, None,
     "Burns furnace fuel. Works day and night."),
    ("gas_turbine", "Gas Turbine", "combustion", 2, 120, 2500, "2010-01-01",
     "Twice as much energy per fuel item."),
    ("geothermal_plant", "Geothermal Plant", "geothermal", 1, 36, 1800, None,
     "Heat from lava source blocks on its sides. +50 % in the Nether."),
    ("binary_cycle_plant", "Binary Cycle Plant", "geothermal", 2, 90, 6000, "2012-01-01",
     "Organic Rankine cycle: much more power from the same lava."),
    ("small_modular_reactor", "Small Modular Reactor", "nuclear", 1, 2000, 250000, "2030-01-01",
     "Fuel rods + cooling water on its sides. SCRAMs when too hot."),
]
BATTERIES = [  # id, name, capacity, rate, price, release
    ("lead_acid_battery", "Lead-Acid Battery Bank", 500_000, 256, 300, None),
    ("lithium_ion_battery", "Lithium-Ion Battery Wall", 4_000_000, 1024, 3000, "2015-04-30"),
    ("lfp_battery_bank", "LFP Grid Storage", 20_000_000, 4096, 15000, "2017-12-01"),
]
POWER_CABLES = [  # id, name, FE/t, price per cable, release
    ("copper_power_cable", "Copper Power Cable", 256, 1, None),
    ("aluminium_power_cable", "Aluminium HV Cable", 2048, 6, None),
    ("superconducting_cable", "Superconducting Cable", 16384, 80, "2025-01-01"),
]
GRIDS = [  # id, name, FE/t, price, release
    ("residential_grid_connection", "Residential Grid Connection", 50, 2000, None),
    ("commercial_grid_connection", "Commercial Grid Connection", 500, 20000, "2012-01-01"),
    ("utility_grid_connection", "Utility Grid Connection", 5000, 150000, "2016-01-01"),
]
ORES = [  # id, metal tag, name, deepslate?, base colour, speck colours, needs iron tool
    ("bauxite_ore", "aluminum", "Bauxite Ore", False, 0x7F7F7F, (0xB5532A, 0xD9844A, 0x8E3B1D), False),
    ("deepslate_bauxite_ore", "aluminum", "Deepslate Bauxite Ore", True, 0x4A4A50, (0xB5532A, 0xD9844A, 0x8E3B1D), False),
    ("spodumene_ore", "lithium", "Spodumene Ore", False, 0x7F7F7F, (0xE8C6E0, 0xF6E3F1, 0xC79BC0), False),
    ("deepslate_spodumene_ore", "lithium", "Deepslate Spodumene Ore", True, 0x4A4A50, (0xE8C6E0, 0xF6E3F1, 0xC79BC0), False),
    ("uraninite_ore", "uranium", "Uraninite Ore", False, 0x7F7F7F, (0x23251F, 0x9ACD32, 0x3B3F2E), True),
    ("deepslate_uraninite_ore", "uranium", "Deepslate Uraninite Ore", True, 0x4A4A50, (0x23251F, 0x9ACD32, 0x3B3F2E), True),
]
RAW = {"aluminum": "raw_bauxite", "lithium": "raw_spodumene", "uranium": "raw_uraninite"}
RESOURCE_BLOCKS = [  # id, name, colour
    ("raw_bauxite_block", "Block of Raw Bauxite", 0xB5532A),
    ("raw_spodumene_block", "Block of Raw Spodumene", 0xE0BCD8),
    ("raw_uraninite_block", "Block of Raw Uraninite", 0x2E3127),
    ("aluminium_block", "Block of Aluminium", 0xC9CED4),
    ("steel_block", "Block of Steel", 0x6E747B),
]
MATERIALS = [  # id, name, shop price $ (None = not sold)
    ("raw_bauxite", "Raw Bauxite", None), ("raw_spodumene", "Raw Spodumene", None), ("raw_uraninite", "Raw Uraninite", None),
    ("aluminium_ingot", "Aluminium Ingot", 3), ("aluminium_nugget", "Aluminium Nugget", None),
    ("steel_blend", "Steel Blend", None), ("steel_ingot", "Steel Ingot", 4),
    ("lithium_carbonate", "Lithium Carbonate", 6), ("yellowcake", "Yellowcake", None),
    ("enriched_uranium", "Enriched Uranium", None),
    ("silicon_wafer", "Silicon Wafer", 3), ("n_type_wafer", "N-Type Wafer", None), ("p_type_wafer", "P-Type Wafer", None),
    ("solar_cell", "Solar Cell", 12), ("tempered_glass", "Tempered Glass", 2),
    ("copper_wire", "Copper Wire", 1), ("copper_coil", "Copper Coil", 5), ("permanent_magnet", "Permanent Magnet", 6),
    ("capacitor", "Capacitor", 3), ("inverter", "Inverter", 60), ("charge_controller", "Charge Controller", 45),
    ("steel_plate", "Steel Plate", 5), ("steel_rod", "Steel Rod", 2), ("aluminium_sheet", "Aluminium Sheet", 2),
    ("steel_gear", "Steel Gear", 12), ("bearing", "Bearing", 4), ("gearbox", "Gearbox", 80), ("alternator", "Alternator", 70),
    ("fibreglass", "Fibreglass", 2), ("carbon_fibre", "Carbon Fibre", 8),
    ("fibreglass_blade", "Fibreglass Blade", 15), ("carbon_fibre_blade", "Carbon Fibre Blade", 60),
    ("water_wheel_paddle", "Water Wheel Paddle", 3),
    ("lead_acid_cell", "Lead-Acid Cell", 15), ("li_ion_cell", "Li-Ion Cell", 60), ("lfp_cell", "LFP Cell", 90),
    ("liquid_nitrogen_cell", "Liquid Nitrogen Cell", None), ("control_rod", "Control Rod", None),
    ("reactor_casing", "Reactor Casing", None),
    ("fibreglass_rotor", "Fibreglass Rotor", 60), ("carbon_fibre_rotor", "Carbon Fibre Rotor", 250),
    ("fuel_rod", "Fuel Rod", None), ("spent_fuel_rod", "Spent Fuel Rod", None),
]
# blocks that need a pickaxe; ores need stone (uraninite iron), everything else stone
POWER_BLOCK_IDS = [g[0] for g in GENERATORS] + [b[0] for b in BATTERIES] + [g[0] for g in GRIDS]
ORE_IDS = [o[0] for o in ORES]
RESOURCE_IDS = [r[0] for r in RESOURCE_BLOCKS]


# ============================================================ textures
def tex_ore(base, specks, deepslate, seed):
    c = noisy(base, seed, 0.25)
    if deepslate:
        for y in range(0, 16, 4):
            for x in range(16):
                if noise(x, y, seed + 1) > 0.6:
                    c.set(x, y, shade(base, 0.8))
    spots = [(3, 3), (10, 2), (6, 7), (12, 9), (2, 11), (8, 13), (13, 13)]
    for i, (sx, sy) in enumerate(spots):
        col = specks[i % len(specks)]
        c.set(sx, sy, col)
        c.set(sx + 1, sy, shade(col, 0.85))
        c.set(sx, sy + 1, shade(col, 1.1))
        if i % 2 == 0:
            c.set(sx + 1, sy + 1, specks[(i + 1) % len(specks)])
    return c


def tex_storage_block(col, seed, rivets=False):
    c = noisy(col, seed, 0.12)
    c.frame(0, 0, 15, 15, shade(col, 0.7))
    c.frame(1, 1, 14, 14, shade(col, 1.15))
    if rivets:
        for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
            c.set(x, y, shade(col, 1.4))
    return c


def tex_raw_block(col, seed):
    c = noisy(col, seed, 0.35)
    for y in range(16):
        for x in range(16):
            if noise(x, y, seed + 7) > 0.8:
                c.set(x, y, shade(col, 0.65))
    return c


def tex_raw(col):
    c = Canvas()
    pts = [(5, 4, 3), (9, 6, 3.5), (6, 9, 3), (10, 10, 2.5)]
    for (cx, cy, r) in pts:
        c.circle(cx, cy, r, col)
    for y in range(16):
        for x in range(16):
            if c.px[y][x][3] and noise(x, y, col & 255) > 0.6:
                c.set(x, y, shade(col, 0.75))
    c.set(5, 3, shade(col, 1.35))
    c.set(9, 5, shade(col, 1.35))
    return c


def tex_ingot(col):
    c = Canvas()
    for y in range(5, 12):
        inset = (y - 5) // 3
        c.rect(2 + inset, y, 13 - (2 - inset), y, col)
    c.rect(3, 5, 12, 5, shade(col, 1.35))
    c.rect(2, 11, 13, 11, shade(col, 0.6))
    c.rect(4, 7, 9, 7, shade(col, 1.2))
    return c


def tex_nugget(col):
    c = Canvas()
    c.rect(6, 6, 9, 9, col)
    c.set(6, 6, shade(col, 1.3))
    c.set(9, 9, shade(col, 0.6))
    c.rect(10, 10, 11, 11, shade(col, 0.9))
    return c


def tex_dust(col, speck=None):
    c = Canvas()
    for y in range(7, 13):
        half = (y - 6)
        c.rect(8 - half, y, 7 + half, y, col)
    for y in range(7, 13):
        for x in range(16):
            if c.px[y][x][3] and noise(x, y, 5) > 0.7:
                c.set(x, y, speck if speck is not None else shade(col, 0.8))
    c.set(8, 7, shade(col, 1.2))
    return c


def tex_pellet(col, glow):
    c = Canvas()
    c.rect(5, 4, 10, 11, col)
    c.frame(5, 4, 10, 11, shade(col, 0.6))
    c.rect(6, 5, 7, 10, glow)
    c.set(9, 6, shade(glow, 1.2))
    return c


def tex_wafer(col, dots=None):
    c = Canvas()
    c.circle(7.5, 7.5, 6.5, col)
    c.circle(7.5, 7.5, 6.5, shade(col, 0.7), filled=False)
    for i in range(3, 8):
        c.set(i, 11 - i, shade(col, 1.4))
    if dots:
        for (x, y) in ((5, 5), (9, 6), (7, 9), (10, 10), (4, 9)):
            c.set(x, y, dots)
    c.rect(6, 14, 9, 14, (0, 0, 0, 0))  # flat edge
    return c


def tex_solar_cell():
    c = Canvas()
    c.rect(2, 2, 13, 13, 0x1E3A8A)
    for x in (5, 9):
        c.rect(x, 2, x, 13, 0xCBD5E1)
    for y in range(4, 13, 3):
        c.rect(2, y, 13, y, 0x64748B)
    c.frame(2, 2, 13, 13, 0x94A3B8)
    return c


def tex_glass_pane():
    c = Canvas()
    c.rect(2, 2, 13, 13, (200, 230, 240, 120))
    c.frame(2, 2, 13, 13, 0xB0BEC5)
    for i in range(4, 9):
        c.set(i, 12 - i, (255, 255, 255, 200))
    return c


def tex_wire(col):
    c = Canvas()
    for r in (5.5, 4.0, 2.5):
        c.circle(7.5, 7.5, r, col, filled=False)
    c.set(13, 7, shade(col, 1.3))
    c.set(14, 8, shade(col, 1.3))
    return c


def tex_coil():
    c = Canvas()
    c.rect(3, 6, 12, 9, 0x9E9E9E)
    for x in range(4, 12, 2):
        c.rect(x, 4, x, 11, 0xC77B30)
        c.set(x, 4, 0xE5A060)
    c.rect(2, 7, 2, 8, 0x616161)
    c.rect(13, 7, 13, 8, 0x616161)
    return c


def tex_magnet():
    c = Canvas()
    c.rect(4, 3, 11, 12, 0xB0B6BC)
    c.rect(4, 3, 7, 12, 0xD32F2F)
    c.rect(8, 3, 11, 12, 0x1976D2)
    c.frame(4, 3, 11, 12, 0x424242)
    return c


def tex_capacitor():
    c = Canvas()
    c.rect(5, 2, 10, 10, 0x1565C0)
    c.rect(5, 2, 10, 2, 0xB0BEC5)
    c.rect(9, 3, 9, 9, 0x90CAF9)
    c.rect(6, 11, 6, 14, 0xB0BEC5)
    c.rect(9, 11, 9, 13, 0xB0BEC5)
    return c


def tex_box(body, accent, fins=False, leds=None):
    c = Canvas()
    c.bevel(2, 3, 13, 12, body)
    if fins:
        for x in range(3, 13, 2):
            c.rect(x, 3, x, 5, shade(body, 0.6))
    c.rect(4, 7, 9, 10, 0x0B1016)
    c.rect(5, 8, 8, 8, accent)
    for i, col in enumerate(leds or []):
        c.set(11, 7 + 2 * i, col)
    return c


def tex_plate(col):
    c = Canvas()
    c.bevel(2, 4, 13, 11, col)
    for (x, y) in ((3, 5), (12, 5), (3, 10), (12, 10)):
        c.set(x, y, shade(col, 0.7))
    return c


def tex_rod(col):
    c = Canvas()
    for i in range(3, 13):
        c.set(i, 15 - i, col)
        c.set(i + 1, 15 - i, shade(col, 0.7))
        c.set(i, 14 - i, shade(col, 1.3))
    return c


def tex_gear(col):
    c = Canvas()
    c.circle(7.5, 7.5, 5, col)
    for a in range(8):
        ang = a * math.pi / 4
        x, y = 7.5 + math.cos(ang) * 6.3, 7.5 + math.sin(ang) * 6.3
        c.rect(int(round(x)) - 1, int(round(y)) - 1, int(round(x)), int(round(y)), col)
    c.circle(7.5, 7.5, 1.6, (0, 0, 0, 0))
    c.circle(7.5, 7.5, 5, shade(col, 0.7), filled=False)
    return c


def tex_bearing():
    c = Canvas()
    c.circle(7.5, 7.5, 6, 0x9E9E9E)
    c.circle(7.5, 7.5, 4, 0x616161)
    c.circle(7.5, 7.5, 2.4, (0, 0, 0, 0))
    for a in range(6):
        ang = a * math.pi / 3
        c.set(int(round(7.5 + math.cos(ang) * 4.6)), int(round(7.5 + math.sin(ang) * 4.6)), 0xE0E0E0)
    return c


def tex_gearbox():
    c = Canvas()
    c.bevel(2, 3, 13, 12, 0x546E7A)
    g = tex_gear(0xB0BEC5)
    for y in range(16):
        for x in range(16):
            if g.px[y][x][3] and 4 <= x <= 11 and 5 <= y <= 11:
                c.set(x, y, g.px[y][x])
    return c


def tex_alternator():
    c = Canvas()
    c.rect(3, 4, 12, 11, 0x37474F)
    for x in range(4, 12, 2):
        c.rect(x, 4, x, 11, 0x546E7A)
    c.rect(1, 7, 2, 8, 0xB0BEC5)
    c.rect(13, 6, 14, 9, 0xC77B30)
    c.frame(3, 4, 12, 11, 0x263238)
    return c


def tex_weave(a, b):
    c = Canvas()
    for y in range(3, 13):
        for x in range(3, 13):
            c.set(x, y, a if ((x // 2) + (y // 2)) % 2 == 0 else b)
    c.frame(3, 3, 12, 12, shade(a, 0.7))
    return c


def tex_blade(col):
    c = Canvas()
    for i in range(2, 14):
        w = 1 if i < 4 or i > 11 else 2
        for k in range(w):
            c.set(i, 15 - i - k, col)
        c.set(i, 16 - i, shade(col, 0.7))
    c.set(2, 13, 0x616161)
    c.set(3, 13, 0x616161)
    return c


def tex_rotor(col):
    """Three blades at 120° around a hub, drawn so that the hub is centred (rendered spinning on wind turbines)."""
    c = Canvas()
    for k in range(3):
        ang = -math.pi / 2 + k * 2 * math.pi / 3
        for step in range(2, 15):
            r = step * 0.5
            x, y = 7.5 + math.cos(ang) * r, 7.5 + math.sin(ang) * r
            perp = (-math.sin(ang), math.cos(ang))
            width = 1.2 if r < 5 else 0.7
            for t in (-width, 0, width):
                c.set(int(round(x + perp[0] * t * 0.8)), int(round(y + perp[1] * t * 0.8)), col if t <= 0 else shade(col, 0.82))
    c.circle(7.5, 7.5, 1.6, 0x9E9E9E)
    c.set(7, 7, 0xE0E0E0)
    return c


def tex_paddle():
    c = Canvas()
    c.rect(4, 2, 11, 9, 0x8D6E63)
    for y in (4, 7):
        c.rect(4, y, 11, y, 0x6D4C41)
    c.rect(7, 10, 8, 14, 0x9E9E9E)
    c.frame(4, 2, 11, 9, 0x5D4037)
    return c


def tex_cell(body, cap, band):
    c = Canvas()
    c.rect(5, 3, 10, 13, body)
    c.rect(5, 3, 10, 3, cap)
    c.rect(6, 2, 9, 2, 0xB0BEC5)
    c.rect(5, 8, 10, 9, band)
    c.rect(9, 4, 9, 12, shade(body, 1.25))
    return c


def tex_long_rod(body, band, glow=None):
    c = Canvas()
    c.rect(7, 1, 8, 14, body)
    c.set(7, 1, shade(body, 1.3))
    for y in range(3, 14, 3):
        c.rect(7, y, 8, y, band)
    if glow is not None:
        for y in (4, 7, 10):
            c.rect(6, y, 6, y + 1, glow)
            c.rect(9, y, 9, y + 1, glow)
    return c


def tex_casing():
    c = Canvas()
    c.bevel(2, 2, 13, 13, 0x9E9E9E)
    c.rect(4, 4, 11, 11, 0xBDBDBD)
    for i in range(4, 12, 2):
        c.set(i, 4, 0xFBC02D)
        c.set(i + 1, 4, 0x212121)
    return c


def tex_canister():
    c = Canvas()
    c.rect(5, 3, 10, 13, 0xE3F2FD)
    c.rect(5, 3, 10, 4, 0x90A4AE)
    c.rect(6, 7, 9, 10, 0x64B5F6)
    c.set(7, 2, 0x90A4AE)
    c.set(8, 2, 0x90A4AE)
    return c


# ---------------------------------------------------------------- block textures
def tex_frame_side(col=0xB0BEC5):
    c = noisy(col, 41, 0.08)
    c.frame(0, 0, 15, 15, shade(col, 0.7))
    return c


SOLAR_LOOK = {  # cell colour, grid colour, frame
    1: (0x2F5AA8, 0xB8C7E0, 0xC0C6CC),
    2: (0x111827, 0x4B5563, 0xC0C6CC),
    3: (0x172554, 0x60A5FA, 0x94A3B8),
    4: (0x3B0764, 0x2DD4BF, 0x64748B),
}


def tex_solar_top(tier):
    cell, grid, frame = SOLAR_LOOK[tier]
    c = Canvas(cell)
    for y in range(16):
        for x in range(16):
            if tier == 1 and noise(x, y, 77) > 0.7:
                c.set(x, y, shade(cell, 1.25))
            if tier == 4:
                c.set(x, y, shade(cell, 0.9 + 0.3 * ((x + y) % 5) / 5))
    for v in (5, 10):
        c.rect(v, 0, v, 15, grid)
        c.rect(0, v, 15, v, grid)
    c.frame(0, 0, 15, 15, frame)
    return c


def tex_nacelle(face):
    c = noisy(0xECEFF1, 51, 0.06)
    c.frame(0, 0, 15, 15, 0xB0BEC5)
    if face == "front":
        c.circle(7.5, 7.5, 3, 0xCFD8DC)
        c.circle(7.5, 7.5, 3, 0x90A4AE, filled=False)
    elif face == "side":
        for x in range(3, 13, 3):
            c.rect(x, 6, x + 1, 7, 0xB0BEC5)
    return c


def tex_machine(body, face, accent, style):
    c = noisy(body, 61 + len(style), 0.1)
    c.frame(0, 0, 15, 15, shade(body, 0.6))
    if face == "front":
        if style == "wheel":
            c = Canvas(0x6D4C41)
            c.circle(7.5, 7.5, 7, 0x8D6E63)
            c.circle(7.5, 7.5, 7, 0x5D4037, filled=False)
            for a in range(8):
                ang = a * math.pi / 4
                for r in range(2, 8):
                    c.set(int(round(7.5 + math.cos(ang) * r)), int(round(7.5 + math.sin(ang) * r)), 0x4E342E)
            c.circle(7.5, 7.5, 1.5, 0x9E9E9E)
        elif style == "turbine":
            c.circle(7.5, 7.5, 5.5, 0x263238)
            for a in range(6):
                ang = a * math.pi / 3
                for r in range(1, 6):
                    c.set(int(round(7.5 + math.cos(ang + r * 0.25) * r)), int(round(7.5 + math.sin(ang + r * 0.25) * r)), accent)
        elif style == "fire":
            c.rect(3, 8, 12, 13, 0x1A1A1A)
            for x in range(4, 12):
                c.set(x, 12, 0xFF7043 if x % 2 else 0xFFB74D)
                c.set(x, 11, 0xFF5722 if x % 3 == 0 else 0x1A1A1A)
            for x in range(3, 13, 2):
                c.rect(x, 2, x, 6, shade(body, 0.6))
        elif style == "steam":
            for y in range(3, 13, 3):
                c.rect(2, y, 13, y + 1, accent)
            c.rect(6, 2, 9, 13, shade(body, 0.75))
        elif style == "reactor":
            c.rect(4, 3, 11, 12, 0x263238)
            c.rect(5, 4, 10, 11, 0x0D47A1)
            c.rect(6, 5, 9, 10, 0x4FC3F7)
            for x in range(0, 16, 2):
                c.set(x, 14, 0xFBC02D)
                c.set(x + 1, 14, 0x212121)
        elif style == "battery":
            c.rect(3, 3, 12, 12, shade(body, 0.8))
            for i in range(4):
                c.rect(4 + 2 * i, 10 - i * 0, 5 + 2 * i, 11, accent)
            c.rect(4, 4, 5, 5, 0xE53935)
            c.rect(10, 4, 11, 5, 0x212121)
        elif style == "meter":
            c.circle(7.5, 6.5, 4, 0xECEFF1)
            c.circle(7.5, 6.5, 4, 0x455A64, filled=False)
            c.rect(7, 4, 7, 7, 0xD32F2F)
            c.rect(4, 12, 11, 13, accent)
        elif style == "hv":
            c.rect(3, 3, 12, 12, 0x212121)
            for (x, y) in ((8, 3), (7, 4), (6, 5), (5, 6), (7, 6), (8, 6), (9, 6), (8, 7), (7, 8), (6, 9), (5, 10)):
                c.set(x, y, accent)
        elif style == "geo":
            for y in range(3, 13, 4):
                c.rect(2, y, 13, y + 1, accent)
            c.rect(2, 13, 13, 13, 0xFF7043)
    elif face == "top":
        if style in ("fire", "steam", "geo", "reactor"):
            c.circle(7.5, 7.5, 3.5, shade(body, 0.5))
            c.circle(7.5, 7.5, 2.5, 0x1A1A1A)
        else:
            c.frame(3, 3, 12, 12, shade(body, 0.8))
    else:
        if style in ("turbine", "steam", "geo"):
            c.rect(0, 6, 15, 9, accent)
            c.rect(0, 7, 15, 7, shade(accent, 1.25))
        else:
            for y in range(4, 13, 4):
                c.rect(3, y, 12, y, shade(body, 0.75))
    return c


MACHINE_LOOK = {  # id: body, accent, style
    "water_wheel": (0x6D4C41, 0x8D6E63, "wheel"),
    "micro_hydro_turbine": (0x607D8B, 0x29B6F6, "turbine"),
    "hydroelectric_turbine": (0x455A64, 0x0288D1, "turbine"),
    "combustion_generator": (0x424242, 0xFF7043, "fire"),
    "gas_turbine": (0x9E9E9E, 0xFFA726, "steam"),
    "geothermal_plant": (0x5D4037, 0xC77B30, "geo"),
    "binary_cycle_plant": (0x00695C, 0x4DB6AC, "geo"),
    "small_modular_reactor": (0xBDBDBD, 0x4FC3F7, "reactor"),
    "lead_acid_battery": (0x263238, 0xE53935, "battery"),
    "lithium_ion_battery": (0xECEFF1, 0x43A047, "battery"),
    "lfp_battery_bank": (0x00796B, 0x80CBC4, "battery"),
    "residential_grid_connection": (0x90A4AE, 0x43A047, "meter"),
    "commercial_grid_connection": (0x2E7D32, 0xFDD835, "hv"),
    "utility_grid_connection": (0x37474F, 0xFDD835, "hv"),
}

POWER_CABLE_LOOK = {"copper_power_cable": (0x3E2723, 0xC77B30), "aluminium_power_cable": (0x37474F, 0xCFD8DC),
                    "superconducting_cable": (0xE0F7FA, 0x26C6DA)}


def tex_power_cable(cid):
    base, core = POWER_CABLE_LOOK[cid]
    c = noisy(base, 33, 0.1)
    for x in range(16):
        c.set(x, 7, core if x % 4 else shade(core, 1.3))
        c.set(x, 8, shade(core, 0.8))
    return c


ITEM_TEX_POWER = {
    "raw_bauxite": tex_raw(0xB5532A), "raw_spodumene": tex_raw(0xE0BCD8), "raw_uraninite": tex_raw(0x3A3D2E),
    "aluminium_ingot": tex_ingot(0xC9CED4), "aluminium_nugget": tex_nugget(0xC9CED4),
    "steel_blend": tex_dust(0x5F6368, 0x111111), "steel_ingot": tex_ingot(0x7A8088),
    "lithium_carbonate": tex_dust(0xF5F5F5, 0xD0D0D0), "yellowcake": tex_dust(0xE6C229, 0xB8941A),
    "enriched_uranium": tex_pellet(0x33691E, 0xB2FF59),
    "silicon_wafer": tex_wafer(0x8E99A8), "n_type_wafer": tex_wafer(0x8E99A8, 0xE53935), "p_type_wafer": tex_wafer(0x8E99A8, 0xFDD835),
    "solar_cell": tex_solar_cell(), "tempered_glass": tex_glass_pane(),
    "copper_wire": tex_wire(0xC77B30), "copper_coil": tex_coil(), "permanent_magnet": tex_magnet(),
    "capacitor": tex_capacitor(), "inverter": tex_box(0x37474F, 0x4ADE80, fins=True),
    "charge_controller": tex_box(0x1565C0, 0x39C5E0, leds=[0x4ADE80, 0xFBBF24]),
    "steel_plate": tex_plate(0x7A8088), "steel_rod": tex_rod(0x8A9098), "aluminium_sheet": tex_plate(0xD5DADF),
    "steel_gear": tex_gear(0x8A9098), "bearing": tex_bearing(), "gearbox": tex_gearbox(), "alternator": tex_alternator(),
    "fibreglass": tex_weave(0xE8F5E9, 0xC8E6C9), "carbon_fibre": tex_weave(0x212121, 0x424242),
    "fibreglass_blade": tex_blade(0xF5F5F5), "carbon_fibre_blade": tex_blade(0x2B2B2B),
    "fibreglass_rotor": tex_rotor(0xF5F5F5), "carbon_fibre_rotor": tex_rotor(0x303030),
    "water_wheel_paddle": tex_paddle(),
    "lead_acid_cell": tex_cell(0x263238, 0xE53935, 0x455A64), "li_ion_cell": tex_cell(0x1E88E5, 0xB0BEC5, 0x43A047),
    "lfp_cell": tex_cell(0x00796B, 0xB0BEC5, 0x80CBC4), "liquid_nitrogen_cell": tex_canister(),
    "control_rod": tex_long_rod(0x616161, 0x212121), "reactor_casing": tex_casing(),
    "fuel_rod": tex_long_rod(0x9E9E9E, 0x616161, 0x76FF03), "spent_fuel_rod": tex_long_rod(0x5D5D5D, 0x3A3A3A, 0x33691E),
}

if PHASE == "assets":
    for name, canvas in ITEM_TEX_POWER.items():
        canvas.save(os.path.join(ASSETS, "textures", "item", name + ".png"))
        write_json(os.path.join(ASSETS, "models", "item", name + ".json"),
                   {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{name}"}})
        write_json(os.path.join(ASSETS, "items", name + ".json"), {"model": {"type": "minecraft:model", "model": f"{MOD}:item/{name}"}})

    def cube_all(name, tex):
        write_json(BLOCK_MODEL(name), {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MOD}:block/{tex}"}})
        write_json(os.path.join(ASSETS, "blockstates", name + ".json"), {"variants": {"": {"model": f"{MOD}:block/{name}"}}})
        block_item(name, name)

    def facing_multipart(name, model):
        write_json(os.path.join(ASSETS, "blockstates", name + ".json"), {"multipart": [
            {"when": {"facing": f}, "apply": variant(f"{MOD}:block/{model}", f)} for f in ROT]})

    # ores and storage blocks
    for i, (oid, metal, oname, deep, base, specks, iron) in enumerate(ORES):
        tex_ore(base, specks, deep, 90 + i).save(BLOCK_TEX(oid))
        cube_all(oid, oid)
    for i, (rid, rname, col) in enumerate(RESOURCE_BLOCKS):
        if rid.startswith("raw_"):
            tex_raw_block(col, 120 + i).save(BLOCK_TEX(rid))
        else:
            tex_storage_block(col, 130 + i, rivets=rid == "steel_block").save(BLOCK_TEX(rid))
        cube_all(rid, rid)

    # generators
    tex_frame_side().save(BLOCK_TEX("solar_panel_side"))
    tex_storage_block(0x9AA3AB, 140).save(BLOCK_TEX("solar_panel_bottom"))
    for face in ("front", "side", "top"):
        tex_nacelle(face).save(BLOCK_TEX(f"wind_turbine_{face}"))
    tex_storage_block(0xB0BEC5, 141).save(BLOCK_TEX("wind_turbine_tower"))
    for (gid, gname, family, tier, peak, price, release, desc) in GENERATORS:
        if family == "solar":
            tex_solar_top(tier).save(BLOCK_TEX(gid + "_top"))
            t = {"top": f"{MOD}:block/{gid}_top", "side": f"{MOD}:block/solar_panel_side",
                 "bottom": f"{MOD}:block/solar_panel_bottom", "particle": f"{MOD}:block/{gid}_top"}
            write_json(BLOCK_MODEL(gid), {"parent": "minecraft:block/block", "textures": t, "elements": [{
                "from": [0, 0, 0], "to": [16, 6, 16], "faces": {
                    "up": {"texture": "#top", "uv": [0, 0, 16, 16]}, "down": {"texture": "#bottom", "uv": [0, 0, 16, 16], "cullface": "down"},
                    **{f: {"texture": "#side", "uv": [0, 10, 16, 16], "cullface": f} for f in ("north", "south", "east", "west")}}}]})
        elif family == "wind":
            t = {"front": f"{MOD}:block/wind_turbine_front", "side": f"{MOD}:block/wind_turbine_side",
                 "top": f"{MOD}:block/wind_turbine_top", "tower": f"{MOD}:block/wind_turbine_tower",
                 "particle": f"{MOD}:block/wind_turbine_side"}
            all6 = lambda tex: {f: {"texture": tex} for f in ("north", "south", "east", "west", "up", "down")}
            write_json(BLOCK_MODEL(gid), {"parent": "minecraft:block/block", "textures": t, "elements": [
                {"from": [6, 0, 6], "to": [10, 10, 10], "faces": all6("#tower")},
                {"from": [4, 10, 2], "to": [12, 16, 16], "faces": {
                    "north": {"texture": "#front"}, "south": {"texture": "#side"}, "east": {"texture": "#side"},
                    "west": {"texture": "#side"}, "up": {"texture": "#top"}, "down": {"texture": "#side"}}},
                {"from": [6.5, 11, 0.5], "to": [9.5, 15, 2], "faces": all6("#tower")},
            ]})
        else:
            body, accent, style = MACHINE_LOOK[gid]
            for face in ("front", "side", "top"):
                tex_machine(body, face, accent, style).save(BLOCK_TEX(f"{gid}_{face}"))
            orientable(gid, f"{gid}_front", f"{gid}_side", f"{gid}_top")
        facing_multipart(gid, gid)
        block_item(gid, gid)
    for (bid, *_r) in BATTERIES + GRIDS:
        body, accent, style = MACHINE_LOOK[bid]
        for face in ("front", "side", "top"):
            tex_machine(body, face, accent, style).save(BLOCK_TEX(f"{bid}_{face}"))
        orientable(bid, f"{bid}_front", f"{bid}_side", f"{bid}_top")
        facing_blockstate(bid, bid)
        block_item(bid, bid)

    # power cables: thinner than data cables
    for (cid, cname, rate, price, release) in POWER_CABLES:
        tex_power_cable(cid).save(BLOCK_TEX(cid))
        t = {"cable": f"{MOD}:block/{cid}", "particle": f"{MOD}:block/{cid}"}
        faces6 = {f: {"texture": "#cable"} for f in ("north", "south", "east", "west", "up", "down")}
        write_json(BLOCK_MODEL(cid + "_core"), {"textures": t, "elements": [{"from": [6, 6, 6], "to": [10, 10, 10], "faces": faces6}]})
        write_json(BLOCK_MODEL(cid + "_side"), {"textures": t, "elements": [{"from": [6, 6, 0], "to": [10, 10, 6],
                                                                             "faces": {f: {"texture": "#cable"} for f in ("north", "east", "west", "up", "down")}}]})
        write_json(BLOCK_MODEL(cid + "_inventory"), {"parent": "minecraft:block/block", "textures": t,
                                                     "elements": [{"from": [0, 6, 6], "to": [16, 10, 10], "faces": faces6}]})
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

    # ---------------------------------------------------------------- lang (English)
    for (gid, gname, family, tier, peak, price, release, desc) in GENERATORS:
        lang[f"block.{MOD}.{gid}"] = gname
        lang[f"tooltip.{MOD}.generator.{gid}"] = desc
    for (bid, name, *_r) in BATTERIES + POWER_CABLES + GRIDS:
        lang[f"block.{MOD}.{bid}"] = name
    for (oid, metal, oname, *_r) in ORES:
        lang[f"block.{MOD}.{oid}"] = oname
    for (rid, rname, col) in RESOURCE_BLOCKS:
        lang[f"block.{MOD}.{rid}"] = rname
    for (mid, mname, price) in MATERIALS:
        lang[f"item.{MOD}.{mid}"] = mname
    lang.update({
        "itemGroup.datacenterextended.energy": "DataCenter – Energy",
        "itemGroup.datacenterextended.materials": "DataCenter – Materials",
        "generator.datacenterextended.family.solar": "Solar",
        "generator.datacenterextended.family.wind": "Wind",
        "generator.datacenterextended.family.water": "Hydro",
        "generator.datacenterextended.family.combustion": "Combustion",
        "generator.datacenterextended.family.geothermal": "Geothermal",
        "generator.datacenterextended.family.nuclear": "Nuclear",
        "status.datacenterextended.generator.running": "● Producing",
        "status.datacenterextended.generator.weather": "◐ Clouds – reduced",
        "status.datacenterextended.generator.throttled": "◐ Hot – control rods in",
        "status.datacenterextended.generator.buffer_full": "◐ Buffer full – connect a consumer",
        "status.datacenterextended.generator.night": "◌ Night",
        "status.datacenterextended.generator.no_sky": "✖ No open sky",
        "status.datacenterextended.generator.too_low": "◐ Too low – build higher",
        "status.datacenterextended.generator.blocked": "◐ Rotor blocked – clear the area in front",
        "status.datacenterextended.generator.no_water": "✖ No water",
        "status.datacenterextended.generator.no_fuel": "✖ No fuel",
        "status.datacenterextended.generator.no_lava": "✖ No lava source next to it",
        "status.datacenterextended.generator.no_rods": "✖ No fuel rods",
        "status.datacenterextended.generator.scram": "✖ SCRAM – cool down and restart",
        "status.datacenterextended.no_owner_short": "✖ No owner",
        "gui.datacenterextended.power.output": "Output",
        "gui.datacenterextended.power.equivalent": "Electrical power",
        "gui.datacenterextended.power.buffer": "Buffer",
        "gui.datacenterextended.power.core_heat": "Core heat",
        "gui.datacenterextended.power.burn_time": "Fuel left",
        "gui.datacenterextended.power.tier": "Tier %s",
        "gui.datacenterextended.power.hint.solar": "needs sky",
        "gui.datacenterextended.power.hint.wind": "higher is better",
        "gui.datacenterextended.power.hint.water": "needs water",
        "gui.datacenterextended.power.hint.geothermal": "needs lava",
        "gui.datacenterextended.power.hint.combustion": "burns fuel",
        "gui.datacenterextended.power.hint.nuclear": "needs cooling",
        "gui.datacenterextended.power.restart": "Restart",
        "gui.datacenterextended.power.charging": "▲ Charging",
        "gui.datacenterextended.power.discharging": "▼ Discharging",
        "gui.datacenterextended.power.idle": "◌ Idle",
        "gui.datacenterextended.power.exporting": "● Exporting",
        "gui.datacenterextended.power.flow": "Net flow",
        "gui.datacenterextended.power.time": "Time",
        "gui.datacenterextended.power.full_in": "Full in %s",
        "gui.datacenterextended.power.empty_in": "Empty in %s",
        "gui.datacenterextended.power.max_rate": "Max. rate",
        "gui.datacenterextended.power.export": "Export",
        "gui.datacenterextended.power.tariff": "Feed-in tariff",
        "gui.datacenterextended.power.per_kwh": "%s / kWh",
        "gui.datacenterextended.power.income": "Income",
        "gui.datacenterextended.power.per_hour": "%s / h",
        "gui.datacenterextended.power.earned": "Earned",
        "gui.datacenterextended.power.grid_hint": "Sells up to %s. Racks are worth far more.",
        "gui.datacenterextended.rack.power_bill_self": "Bill %s/h · %s%% own power",
        "gui.datacenterextended.shop.energy": "Energy",
        "gui.datacenterextended.shop.materials": "Materials",
        "gui.datacenterextended.stats.grid": "Grid",
        "tooltip.datacenterextended.generator.peak": "Peak output: %s FE/t (%s)",
        "tooltip.datacenterextended.battery.stored": "Stored: %s / %s FE",
        "tooltip.datacenterextended.battery.rate": "Charge / discharge: %s FE/t (out of the front)",
        "tooltip.datacenterextended.power_cable": "Moves up to %s FE/t per network",
        "tooltip.datacenterextended.grid": "Exports up to %s",
        "tooltip.datacenterextended.grid.hint": "Sells surplus energy – far less than running servers earns",
        "tooltip.datacenterextended.fuel_rod": "Fuel left: %s%%",
        "jade.datacenterextended.generator.output": "Output %s / %s FE/t",
        "jade.datacenterextended.battery.flow": "Net flow %s FE/t",
        "jade.datacenterextended.grid.export": "Export %s / %s FE/t",
        "message.datacenterextended.reactor.scram": "☢ Reactor at %s: SCRAM! It overheated and shut down. Add cooling water, let it cool and restart it.",
        "message.datacenterextended.meter.generator": "Generator: %s / %s FE/t",
        "message.datacenterextended.meter.none": "Not part of a power network (use power cables)",
        "message.datacenterextended.meter.header": "Power network: %s cables · %s generators · %s batteries · %s consumers · %s grid",
        "message.datacenterextended.meter.limit": "Transfer limit %s FE/t (weakest cable: %s)",
        "message.datacenterextended.meter.flow": "Producing %s · to consumers %s · to batteries %s · to grid %s FE/t",
        "message.datacenterextended.meter.storage": "Batteries %s / %s FE (%s%%)",
        "message.datacenterextended.meter.limited": "Limited by the cables – upgrade the %s",
        "advancements.datacenterextended.first_generator.title": "Let There Be Light",
        "advancements.datacenterextended.first_generator.description": "Place your first generator",
        "advancements.datacenterextended.aluminium.title": "Light Metal",
        "advancements.datacenterextended.aluminium.description": "Smelt bauxite into an aluminium ingot",
        "advancements.datacenterextended.grid_storage.title": "Grid-Scale Storage",
        "advancements.datacenterextended.grid_storage.description": "Fill an LFP Grid Storage bank completely",
        "advancements.datacenterextended.feeding_the_grid.title": "Feeding the Grid",
        "advancements.datacenterextended.feeding_the_grid.description": "Get paid for exported energy",
        "advancements.datacenterextended.splitting_atoms.title": "Splitting Atoms",
        "advancements.datacenterextended.splitting_atoms.description": "Run a Small Modular Reactor",
        "book.datacenterextended.page.19": "§lPower§r\n\nRacks need FE. Generators fill their buffer and push FE into blocks next to them.\n\nPower cables connect many generators, batteries and racks. A network moves as much as its weakest cable allows.",
        "book.datacenterextended.page.20": "§lGenerators§r\n\n• Solar: open sky, daytime\n• Wind: free air in front, high up\n• Hydro: flowing water or a water column\n• Combustion: furnace fuel\n• Geothermal: lava on its sides\n• Nuclear: fuel rods + water",
        "book.datacenterextended.page.21": "§lMaterials§r\n\nBauxite (y 32–100) → aluminium, spodumene → lithium, uraninite (deep) → uranium. Steel = iron + coal, blast furnace.\n\nSolar cells are n- and p-doped silicon wafers. Turbines need alternators: copper coils and magnets.",
        "book.datacenterextended.page.22": "§lStorage & Grid§r\n\nBatteries store energy for the night. A grid connection sells surplus energy – much less than servers earn.\n\nThe reactor gets hot: water on its sides cools it. Too hot = SCRAM, restart it in its GUI.",
    })

# ============================================================ data
if PHASE == "data":
    # ---------------------------------------------------------------- tags
    mineable = HEAVY + [M(x) for x in POWER_BLOCK_IDS + ORE_IDS + RESOURCE_IDS]
    iron_tool = [M(o[0]) for o in ORES if o[6]] + [M("raw_uraninite_block")]
    stone_tool = HEAVY + [M(x) for x in POWER_BLOCK_IDS + ORE_IDS + RESOURCE_IDS if M(x) not in iron_tool]
    write_json(os.path.join(DATA, "minecraft", "tags", "block", "mineable", "pickaxe.json"), {"values": mineable})
    write_json(os.path.join(DATA, "minecraft", "tags", "block", "needs_stone_tool.json"), {"values": stone_tool})
    write_json(os.path.join(DATA, "minecraft", "tags", "block", "needs_iron_tool.json"), {"values": iron_tool})

    def ctag(kind, path, values):
        for registry in kind:
            write_json(os.path.join(DATA, "c", "tags", registry, *path.split("/")) + ".json", {"values": values})

    BOTH = ("block", "item")
    for metal in ("aluminum", "lithium", "uranium"):
        ctag(BOTH, f"ores/{metal}", [M(o[0]) for o in ORES if o[1] == metal])
    ctag(BOTH, "ores", [f"#c:ores/{m}" for m in ("aluminum", "lithium", "uranium")])
    ctag(BOTH, "ores_in_ground/stone", [M(o[0]) for o in ORES if not o[3]])
    ctag(BOTH, "ores_in_ground/deepslate", [M(o[0]) for o in ORES if o[3]])
    ctag(BOTH, "ore_rates/singular", [M(o[0]) for o in ORES])
    ctag(("item",), "raw_materials/aluminum", [M("raw_bauxite")])
    ctag(("item",), "raw_materials/lithium", [M("raw_spodumene")])
    ctag(("item",), "raw_materials/uranium", [M("raw_uraninite")])
    ctag(("item",), "raw_materials", ["#c:raw_materials/aluminum", "#c:raw_materials/lithium", "#c:raw_materials/uranium"])
    ctag(BOTH, "storage_blocks/raw_aluminum", [M("raw_bauxite_block")])
    ctag(BOTH, "storage_blocks/raw_lithium", [M("raw_spodumene_block")])
    ctag(BOTH, "storage_blocks/raw_uranium", [M("raw_uraninite_block")])
    ctag(BOTH, "storage_blocks/aluminum", [M("aluminium_block")])
    ctag(BOTH, "storage_blocks/steel", [M("steel_block")])
    ctag(BOTH, "storage_blocks", [f"#c:storage_blocks/{x}" for x in ("raw_aluminum", "raw_lithium", "raw_uranium", "aluminum", "steel")])
    ctag(("item",), "ingots/aluminum", [M("aluminium_ingot")])
    ctag(("item",), "ingots/steel", [M("steel_ingot")])
    ctag(("item",), "ingots", ["#c:ingots/aluminum", "#c:ingots/steel"])
    ctag(("item",), "nuggets/aluminum", [M("aluminium_nugget")])
    ctag(("item",), "nuggets", ["#c:nuggets/aluminum"])
    ctag(("item",), "plates/steel", [M("steel_plate")])
    ctag(("item",), "plates/aluminum", [M("aluminium_sheet")])
    ctag(("item",), "rods/steel", [M("steel_rod")])
    ctag(("item",), "gears/steel", [M("steel_gear")])
    ctag(("item",), "wires/copper", [M("copper_wire")])
    ctag(("item",), "dusts/lithium", [M("lithium_carbonate")])

    # ---------------------------------------------------------------- loot tables
    def loot(block, entry, explosion=True):
        pool = {"entries": [entry], "rolls": 1}
        if explosion:
            pool = {"condition": {"type": "minecraft:survives_explosion"}, **pool}
        write_json(os.path.join(DATA, MOD, "loot_table", "blocks", block + ".json"), {
            "type": "minecraft:block", "pools": [pool], "random_sequence": f"{MOD}:blocks/{block}"})

    simple = lambda b: {"type": "minecraft:item", "name": M(b),
                        "modifier": [{"type": "minecraft:copy_components", "include": ["minecraft:custom_name"], "source": "block_entity"}]}
    for b in [g[0] for g in GENERATORS] + [g[0] for g in GRIDS]:
        loot(b, simple(b))
    for b in [c[0] for c in POWER_CABLES] + RESOURCE_IDS:
        loot(b, {"type": "minecraft:item", "name": M(b)})
    for b in [x[0] for x in BATTERIES]:
        loot(b, {"type": "minecraft:item", "name": M(b), "modifier": [
            {"type": "minecraft:copy_components", "include": ["minecraft:custom_name", f"{MOD}:stored_energy"], "source": "block_entity"}]})
    for (oid, metal, *_r) in ORES:
        loot(oid, {"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "condition": "minecraft:tool/can_silk_touch", "name": M(oid)},
            {"type": "minecraft:item", "name": M(RAW[metal]), "modifier": [
                {"type": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                {"type": "minecraft:explosion_decay"}]}]}, explosion=False)

    # ---------------------------------------------------------------- worldgen
    WG = os.path.join(DATA, MOD, "worldgen")
    ORE_GEN = [  # feature, stone ore, deepslate ore, vein size, veins per chunk, min y, max y, height shape
        ("ore_bauxite", "bauxite_ore", "deepslate_bauxite_ore", 10, 8, 32, 100, "uniform"),
        ("ore_spodumene", "spodumene_ore", "deepslate_spodumene_ore", 7, 5, -32, 40, "trapezoid"),
        ("ore_uraninite", "uraninite_ore", "deepslate_uraninite_ore", 4, 2, -64, 0, "trapezoid"),
    ]
    for (feat, stone, deep, size, count, lo, hi, shape) in ORE_GEN:
        write_json(os.path.join(WG, "feature", feat + ".json"), {
            "type": "minecraft:ore", "discard_chance_on_air_exposure": 0.0, "size": size, "targets": [
                {"state": M(stone), "target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"}},
                {"state": M(deep), "target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables"}}]})
        write_json(os.path.join(WG, "placed_feature", feat + ".json"), {
            "feature": M(feat), "placement": [
                {"type": "minecraft:count", "count": count}, {"type": "minecraft:in_square"},
                {"type": "minecraft:height_range", "height": {"type": f"minecraft:{shape}",
                                                              "min_inclusive": {"absolute": lo}, "max_inclusive": {"absolute": hi}}},
                {"type": "minecraft:biome"}]})
    write_json(os.path.join(DATA, MOD, "neoforge", "biome_modifier", "add_ores.json"), {
        "type": "neoforge:add_features", "biomes": "#minecraft:is_overworld",
        "features": [M(f[0]) for f in ORE_GEN], "step": "underground_ores"})

    # ---------------------------------------------------------------- recipes
    def cook(name, kind, ingredient, result, time, xp=0.7):
        write_json(os.path.join(RECIPES, name + ".json"), {"type": f"minecraft:{kind}", "category": "misc", "cookingtime": time,
                                                          "experience": xp, "ingredient": ingredient, "result": {"id": result}})

    def smelt_and_blast(name, ingredient, result, xp=0.7):
        cook(name + "_from_smelting", "smelting", ingredient, result, 200, xp)
        cook(name + "_from_blasting", "blasting", ingredient, result, 100, xp)

    smelt_and_blast("aluminium_ingot_raw", M("raw_bauxite"), M("aluminium_ingot"))
    smelt_and_blast("aluminium_ingot_ore", "#c:ores/aluminum", M("aluminium_ingot"))
    smelt_and_blast("lithium_carbonate_raw", M("raw_spodumene"), M("lithium_carbonate"))
    smelt_and_blast("lithium_carbonate_ore", "#c:ores/lithium", M("lithium_carbonate"))
    smelt_and_blast("yellowcake_raw", M("raw_uraninite"), M("yellowcake"), 1.0)
    smelt_and_blast("yellowcake_ore", "#c:ores/uranium", M("yellowcake"), 1.0)
    cook("steel_ingot", "blasting", M("steel_blend"), M("steel_ingot"), 200, 0.5)
    cook("silicon_wafer", "blasting", M("silicon"), M("silicon_wafer"), 100, 0.2)
    cook("tempered_glass", "blasting", "minecraft:glass", M("tempered_glass"), 100, 0.1)
    cook("yellowcake_from_spent_fuel", "blasting", M("spent_fuel_rod"), M("yellowcake"), 200, 0.5)

    # storage blocks / nuggets
    for (block, unit) in (("aluminium_block", "aluminium_ingot"), ("steel_block", "steel_ingot"), ("raw_bauxite_block", "raw_bauxite"),
                          ("raw_spodumene_block", "raw_spodumene"), ("raw_uraninite_block", "raw_uraninite")):
        shaped(block, ["###", "###", "###"], {"#": M(unit)}, M(block), category="building")
        shapeless(unit + "_from_" + block, [M(block)], M(unit), 9)
    shaped("aluminium_ingot_from_nuggets", ["###", "###", "###"], {"#": M("aluminium_nugget")}, M("aluminium_ingot"))
    shapeless("aluminium_nugget", [M("aluminium_ingot")], M("aluminium_nugget"), 9)

    AL, ST, CU, FE = "#c:ingots/aluminum", "#c:ingots/steel", "#c:ingots/copper", "#c:ingots/iron"
    shapeless("steel_blend", [FE, "#minecraft:coals", "#minecraft:coals"], M("steel_blend"))
    shaped("enriched_uranium", ["YYY", "YRY", "YYY"], {"Y": M("yellowcake"), "R": "#c:dusts/redstone"}, M("enriched_uranium"))
    shapeless("n_type_wafer", [M("silicon_wafer"), "#c:dusts/redstone"], M("n_type_wafer"))
    shapeless("p_type_wafer", [M("silicon_wafer"), "#c:dusts/glowstone"], M("p_type_wafer"))
    shapeless("solar_cell", [M("n_type_wafer"), M("p_type_wafer"), M("copper_wire")], M("solar_cell"))
    shaped("copper_wire", ["CCC"], {"C": CU}, M("copper_wire"), 6)
    shaped("copper_coil", ["WWW", "WIW", "WWW"], {"W": M("copper_wire"), "I": FE}, M("copper_coil"))
    shaped("permanent_magnet", [" R ", "SDS", " R "], {"R": "#c:dusts/redstone", "S": ST, "D": FE}, M("permanent_magnet"), 2)
    shaped("capacitor", ["SPS", "W W"], {"S": M("aluminium_sheet"), "P": "minecraft:paper", "W": M("copper_wire")}, M("capacitor"), 2)
    shaped("inverter", ["AKA", "CBC", "AWA"], {"A": M("aluminium_sheet"), "K": M("capacitor"), "C": M("copper_coil"),
                                               "B": M("circuit_board"), "W": M("copper_wire")}, M("inverter"))
    shaped("charge_controller", ["WKW", "PBP"], {"W": M("copper_wire"), "K": M("capacitor"), "P": M("processor_die"),
                                                 "B": M("circuit_board")}, M("charge_controller"))
    shaped("steel_plate", ["SS", "SS"], {"S": ST}, M("steel_plate"), 2)
    shaped("steel_rod", ["S", "S"], {"S": ST}, M("steel_rod"), 4)
    shaped("aluminium_sheet", ["AAA"], {"A": AL}, M("aluminium_sheet"), 4)
    shaped("steel_gear", [" P ", "PRP", " P "], {"P": M("steel_plate"), "R": M("steel_rod")}, M("steel_gear"))
    shaped("bearing", [" N ", "NRN", " N "], {"N": "#c:nuggets/iron", "R": M("steel_rod")}, M("bearing"), 2)
    shaped("gearbox", ["PGP", "BRB", "PGP"], {"P": M("steel_plate"), "G": M("steel_gear"), "B": M("bearing"), "R": M("steel_rod")}, M("gearbox"))
    shaped("alternator", ["PCP", "MRM", "PCP"], {"P": M("steel_plate"), "C": M("copper_coil"), "M": M("permanent_magnet"),
                                                 "R": M("steel_rod")}, M("alternator"))
    shapeless("fibreglass", ["minecraft:glass", "#c:strings", "#c:slime_balls"], M("fibreglass"), 2)
    shaped("carbon_fibre", ["CSC", "SRS", "CSC"], {"C": "#minecraft:coals", "S": "#c:strings", "R": "#c:slime_balls"}, M("carbon_fibre"), 2)
    shaped("fibreglass_blade", ["  F", " F ", "R  "], {"F": M("fibreglass"), "R": M("steel_rod")}, M("fibreglass_blade"))
    shaped("carbon_fibre_blade", ["  F", " F ", "R  "], {"F": M("carbon_fibre"), "R": M("steel_rod")}, M("carbon_fibre_blade"))
    shaped("fibreglass_rotor", [" B ", "BHB"], {"B": M("fibreglass_blade"), "H": M("bearing")}, M("fibreglass_rotor"))
    shaped("carbon_fibre_rotor", [" B ", "BHB"], {"B": M("carbon_fibre_blade"), "H": M("bearing")}, M("carbon_fibre_rotor"))
    shaped("water_wheel_paddle", ["P", "P", "S"], {"P": "#minecraft:planks", "S": M("steel_rod")}, M("water_wheel_paddle"), 2)
    shaped("lead_acid_cell", ["IGI", "CWC", "III"], {"I": FE, "G": "#c:gunpowders", "C": CU, "W": "minecraft:water_bucket"}, M("lead_acid_cell"), 2)
    shaped("li_ion_cell", ["SLS", "CGC", "SLS"], {"S": M("aluminium_sheet"), "L": M("lithium_carbonate"), "C": CU,
                                                  "G": "#minecraft:coals"}, M("li_ion_cell"), 2)
    shaped("lfp_cell", ["IBI", "BCB", "IBI"], {"I": FE, "B": "minecraft:bone_meal", "C": M("li_ion_cell")}, M("lfp_cell"))
    shaped("liquid_nitrogen_cell", ["SIS", "IPI", "SIS"], {"S": M("steel_plate"), "I": "minecraft:blue_ice",
                                                           "P": "minecraft:powder_snow_bucket"}, M("liquid_nitrogen_cell"), 2)
    shaped("control_rod", ["R", "G", "R"], {"R": M("steel_rod"), "G": "#c:ingots/gold"}, M("control_rod"))
    shaped("fuel_rod", ["P", "E", "P"], {"P": M("steel_plate"), "E": M("enriched_uranium")}, M("fuel_rod"))
    shaped("reactor_casing", ["PCP", "C C", "PCP"], {"P": M("steel_plate"), "C": "#c:concretes"}, M("reactor_casing"))

    G = lambda gid: M(gid)
    shaped("polycrystalline_solar_panel", ["TTT", "CCC", "AWA"], {"T": M("tempered_glass"), "C": M("solar_cell"),
                                                                 "A": M("aluminium_sheet"), "W": M("copper_wire")}, G("polycrystalline_solar_panel"), category="redstone")
    shaped("monocrystalline_solar_panel", ["CCC", "CPC", "AIA"], {"C": M("solar_cell"), "P": G("polycrystalline_solar_panel"),
                                                                 "A": M("aluminium_sheet"), "I": M("inverter")}, G("monocrystalline_solar_panel"), category="redstone")
    shaped("perc_solar_panel", ["CCC", "CPC", "AKA"], {"C": M("solar_cell"), "P": G("monocrystalline_solar_panel"),
                                                      "A": M("aluminium_sheet"), "K": M("charge_controller")}, G("perc_solar_panel"), category="redstone")
    shaped("perovskite_solar_panel", ["QCQ", "CPC", "DKD"], {"Q": "#c:gems/quartz", "C": M("solar_cell"), "P": G("perc_solar_panel"),
                                                            "D": M("processor_die"), "K": M("charge_controller")}, G("perovskite_solar_panel"), category="redstone")
    shaped("small_wind_turbine", [" R ", "AGA", "BSB"], {"R": M("fibreglass_rotor"), "A": M("aluminium_sheet"), "G": M("alternator"),
                                                        "B": M("bearing"), "S": M("steel_rod")}, G("small_wind_turbine"), category="redstone")
    shaped("medium_wind_turbine", [" R ", "GWG", "PIP"], {"R": M("fibreglass_rotor"), "G": M("alternator"), "W": G("small_wind_turbine"),
                                                         "P": M("steel_plate"), "I": M("inverter")}, G("medium_wind_turbine"), category="redstone")
    shaped("large_wind_turbine", [" R ", "XWX", "PIP"], {"R": M("carbon_fibre_rotor"), "X": M("gearbox"), "W": G("medium_wind_turbine"),
                                                        "P": M("steel_block"), "I": M("inverter")}, G("large_wind_turbine"), category="redstone")
    shaped("water_wheel", ["PBP", "PGP", "PBP"], {"P": M("water_wheel_paddle"), "B": M("bearing"), "G": M("alternator")}, G("water_wheel"), category="redstone")
    shaped("micro_hydro_turbine", ["SBS", "XGX", "SSS"], {"S": M("steel_plate"), "B": M("bearing"), "X": M("steel_gear"),
                                                         "G": M("alternator")}, G("micro_hydro_turbine"), category="redstone")
    shaped("hydroelectric_turbine", ["SXS", "GMG", "SIS"], {"S": M("steel_plate"), "X": M("gearbox"), "G": M("alternator"),
                                                           "M": G("micro_hydro_turbine"), "I": M("inverter")}, G("hydroelectric_turbine"), category="redstone")
    shaped("combustion_generator", ["PPP", "PFP", "WGW"], {"P": M("steel_plate"), "F": "minecraft:furnace", "W": M("copper_wire"),
                                                          "G": M("alternator")}, G("combustion_generator"), category="redstone")
    shaped("gas_turbine", ["PBP", "XCX", "PIP"], {"P": M("steel_plate"), "B": "minecraft:blast_furnace", "X": M("gearbox"),
                                                 "C": G("combustion_generator"), "I": M("inverter")}, G("gas_turbine"), category="redstone")
    shaped("geothermal_plant", ["PFP", "CGC", "PFP"], {"P": M("steel_plate"), "F": M("fibreglass"), "C": "#c:storage_blocks/copper",
                                                      "G": M("alternator")}, G("geothermal_plant"), category="redstone")
    shaped("binary_cycle_plant", ["PIP", "CGC", "PXP"], {"P": M("steel_plate"), "I": M("inverter"), "C": "#c:storage_blocks/copper",
                                                        "G": G("geothermal_plant"), "X": M("gearbox")}, G("binary_cycle_plant"), category="redstone")
    shaped("small_modular_reactor", ["CKC", "RXR", "CTC"], {"C": M("reactor_casing"), "K": M("charge_controller"), "R": M("control_rod"),
                                                           "X": M("steel_block"), "T": G("gas_turbine")}, G("small_modular_reactor"), category="redstone")
    shaped("lead_acid_battery", ["LWL", "LLL", "PPP"], {"L": M("lead_acid_cell"), "W": M("copper_wire"), "P": M("steel_plate")},
           M("lead_acid_battery"), category="redstone")
    shaped("lithium_ion_battery", ["CKC", "CAC", "CCC"], {"C": M("li_ion_cell"), "K": M("charge_controller"), "A": M("aluminium_block")},
           M("lithium_ion_battery"), category="redstone")
    shaped("lfp_battery_bank", ["FKF", "FBF", "PIP"], {"F": M("lfp_cell"), "K": M("charge_controller"), "B": M("lithium_ion_battery"),
                                                      "P": M("steel_plate"), "I": M("inverter")}, M("lfp_battery_bank"), category="redstone")
    shaped("copper_power_cable", ["KKK", "WWW", "KKK"], {"K": "minecraft:dried_kelp", "W": M("copper_wire")}, M("copper_power_cable"), 8, "redstone")
    shaped("aluminium_power_cable", ["CCC", "ASA", "CCC"], {"C": M("copper_power_cable"), "A": AL, "S": M("steel_rod")},
           M("aluminium_power_cable"), 8, "redstone")
    shaped("superconducting_cable", ["CCC", "NDN", "CCC"], {"C": M("aluminium_power_cable"), "N": M("liquid_nitrogen_cell"),
                                                           "D": "#c:gems/diamond"}, M("superconducting_cable"), 8, "redstone")
    shaped("residential_grid_connection", ["PMP", "WCW", "PPP"], {"P": M("steel_plate"), "M": "minecraft:clock", "W": M("copper_power_cable"),
                                                                 "C": M("circuit_board")}, M("residential_grid_connection"), category="redstone")
    shaped("commercial_grid_connection", ["PIP", "CXC", "PPP"], {"P": M("steel_plate"), "I": M("inverter"), "C": M("copper_coil"),
                                                                "X": M("residential_grid_connection")}, M("commercial_grid_connection"), category="redstone")
    shaped("utility_grid_connection", ["PIP", "CXC", "SSS"], {"P": M("steel_plate"), "I": M("inverter"), "C": M("copper_coil"),
                                                             "X": M("commercial_grid_connection"), "S": M("steel_block")}, M("utility_grid_connection"), category="redstone")

    # ---------------------------------------------------------------- shop prices and release dates
    for (gid, gname, family, tier, peak, price, release, desc) in GENERATORS:
        prices[M(gid)] = price
        if release:
            RELEASE[gid] = release
    for (bid, name, a, b, price, release) in BATTERIES:
        prices[M(bid)] = price
        if release:
            RELEASE[bid] = release
    for (cid, name, rate, price, release) in POWER_CABLES + GRIDS:
        prices[M(cid)] = price
        if release:
            RELEASE[cid] = release
    for (mid, mname, price) in MATERIALS:
        if price is not None:
            prices[M(mid)] = price

    # ---------------------------------------------------------------- advancements
    ADV = os.path.join(DATA, MOD, "advancement")

    def advancement(name, parent, icon, criteria=None, frame=None):
        adv = {"parent": M(parent), "criteria": criteria or {"done": {"trigger": "minecraft:impossible"}},
               "display": {"icon": {"id": M(icon)}, "title": {"translate": f"advancements.{MOD}.{name}.title"},
                           "description": {"translate": f"advancements.{MOD}.{name}.description"}},
               "requirements": [["done"]]}
        if frame:
            adv["display"]["frame"] = frame
        write_json(os.path.join(ADV, name + ".json"), adv)

    advancement("first_generator", "root", "polycrystalline_solar_panel")
    advancement("aluminium", "first_generator", "aluminium_ingot",
                {"done": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": M("aluminium_ingot")}]}}})
    advancement("grid_storage", "first_generator", "lfp_battery_bank", frame="goal")
    advancement("feeding_the_grid", "first_generator", "residential_grid_connection")
    advancement("splitting_atoms", "first_generator", "small_modular_reactor", frame="challenge")
