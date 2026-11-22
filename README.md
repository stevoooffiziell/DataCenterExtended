# DataCenterExtended

NeoForge mod for Minecraft 26.3. Assemble **servers**, mount them in **racks** and turn their performance into money and items.

## Gameplay

1. **Server Workbench:** put a **Server (1U)** item in and install a motherboard, PSU, CPUs, memory, GPUs and storage drives.
   The parts are stored inside the server item, and the tooltip shows its stats.
   - The **motherboard** has a CPU **socket** (LGA775 … LGA4677, AM3+ … AM5, SP3/SP5), a **memory type** (DDR2–DDR5),
     a maximum amount of RAM, and the number of CPU, GPU, memory and storage slots.
     Only matching CPUs and memory modules can be inserted.
   - A server needs a CPU and at least one memory module to boot. The PSU must cover the power draw.
   - **Memory affects compute.** Each CPU recommends an amount of RAM; with less, compute drops (down to 25% with almost none).
     Faster memory (+3% per 1000 MT/s, all modules run at the slowest one's speed) and 2+ modules (+10%, dual channel) add a bonus.
2. **Racks** hold 4 servers each (4 HU). Up to **3 racks of the same type** stacked on top of each other **merge** into one
   tall rack with one GUI (12 servers). The bottom block is the controller.

| Rack | Uses | Produces |
|---|---|---|
| **Mining Rack** | GPU / ASIC hashrate (MH/s) of its own bays, standalone | Bitcoin, Dogecoin or Ethereum (selectable) into the owner's wallet |
| **Hosting Rack** | Compute (KB/s) sold to customers, with a bonus for installed storage | Customer fees (dollars) into the owner's wallet |
| **Fabricating Rack** | Compute (KB/s) | Copies of the target item, by its *file size* (e.g. cobblestone = 1 MB) |

   **Hosting customers:** customers sign up for **Shared** (2 KB/s, $12/h), **VPS** (16 KB/s, $110/h) or **Dedicated**
   (128 KB/s, $900/h) plans while the rack has spare compute. The first customer always comes; later ones sign up
   more often while customers are happy. If the rack can't deliver the compute it sold (no power, overheating, servers
   removed), it is **overloaded**: customers cancel and satisfaction drops, so new customers stay away for a while.

   **ASIC miners** (Antminer S9 / S19 Pro / S21) go straight into a mining rack bay, no server needed. They mine
   **Bitcoin only**, much better than GPUs, but draw a lot of power and get hot.

3. **Data cables** (Coax → Cat5e → Cat6a → Fiber) connect Hosting and Fabricating racks into a **shared compute pool**.
   Each rack uses its own servers first. Spare compute flows to racks that need more, split fairly between them.
   A rack without servers can run on network compute alone.
   **A network's bandwidth is its slowest cable**, so one Coax cable in a Fiber backbone limits everything.
   Right-click a cable or rack with the **Network Tester** to see bandwidth, flow and the bottleneck.

4. **Energy:** each rack block has its own FE buffer (200,000 FE by default; a 3-block rack holds 600,000) that any
   FE cable can fill. Servers use `watts × 0.1 / PSU efficiency` FE per tick. A 500 W server with an 80+ Gold PSU uses ~58 FE/t.
   When the buffer runs low, servers shut down one by one. The rack GUI shows a gauge and the FE/t; hover the gauge for details.
   Turn `energy.requireEnergy` off to play without an energy mod.

   **Electricity bills** (optional, `energy.electricityBills = true`): racks don't use FE. Instead, the owner pays
   for the wall power (server watts / PSU efficiency + cooling) in dollars, by default $40 per kWh per real hour.
   A wallet that can't pay shuts the rack down (*Electricity bill unpaid*) until the debt can be paid in one go.

   **Heat and cooling:** every watt a running server draws becomes heat. Each rack block removes 300 W passively and has
   one **cooling slot** for a **Rack Fan** (800 W), **High-Flow Fan** (2 kW) or **Water Cooling Kit** (12 kW); fans
   and pumps draw a little power themselves. A rack reaches 70 °C when the heat equals its cooling. Above that,
   servers throttle (down to 25% at 95 °C). The biome sets the ambient temperature (desert and Nether hot, snow cold).
   The rack GUI has a thermometer gauge; hover it for details.

   **Hardware wear:** parts of running servers lose about 3% condition per real hour (up to 3x faster when overheating).
   Worn CPUs, GPUs and ASICs are slower (70% speed at 1%), and a part at 0% is broken and stops the server.
   Tooltips show the condition.

5. **Wallet:** press **K** (rebindable under Controls) to open your wallet. It shows your dollar balance, your coins,
   live prices with a chart, and lets you sell coins (25% / 50% / all or a typed amount, minus a 2% fee).
   Racks pay the player who placed them, even while that player is offline. New players start with $250.
   Coin prices follow real history for the calendar date (see 7.), with a little noise every minute, so timing your
   sales matters. The wallet chart shows the next Bitcoin halving and The Merge; everyone online gets a chat message
   when a halving happens.

   **Service tab:** the wallet's third tab repairs worn parts or whole servers (cost: shop price × lost condition × 50%)
   and buys hardware second-hand (shop price × condition × 40%).

6. **Hardware Shop:** buy parts, racks, cables and the workbench with dollars, at a **Hardware Shop** block or from the
   Shop tab of the wallet screen. The $250 start covers a first hosting setup (Pentium 4 server + Hosting Rack).
   Crafting recipes for parts still exist, but they are expensive chains (each part needs the previous one plus processor
   dies, gold/diamond blocks or echo shards), so the shop is the normal way early on.

7. **Calendar & real history:** the world has a date, starting at **1 Jul 2010**; one day passes every 2 real minutes
   (`calendar.startDate`, `calendar.ticksPerDay`, `/datacenter date [set <YYYY-MM-DD> | speed <ticks>]`).
   - **Crypto prices** follow approximate real monthly prices (built-in table up to Oct 2025, then a configurable yearly
     drift) with a little random noise. Coins can't be mined or sold before their launch (BTC 2009, DOGE Dec 2013,
     ETH Jul 2015). Difficulty follows the price (`difficultyExponent`), so mining stays playable from $0.07 to $100k.
   - **Halvings** happen on Bitcoin's real dates (2012, 2016, 2020, 2024, then every 4 years). After **The Merge**
     (15 Sep 2022) Ethereum can still be mined, but pays only 10% (`postMergeReward`).
   - **Hardware** appears in the shop on its real release date (data map `release_date`) and gets 15% cheaper per year
     (down to 30%). Second-hand values and repairs follow today's price.
   - 50+ new parts (Athlon 64 X2 to Threadripper PRO 7995WX and EPYC 9965, GeForce 8800 GTX to B200, AM2 to Z890/X870E
     boards, DDR2-4 GB to DDR5-256 GB, 400–1600 W PSUs) and **prebuilt servers** (Office 2004 … AI Server 2023) that
     arrive assembled and cost 35% more than their parts (`shop.prebuiltMarkup`).

8. **Monitoring & redstone:**
   - **Server Monitor** block: placed next to a rack, its front shows status, date, temperature, income/h and customers.
   - **Comparator output** on racks and monitors. The ⚡ button in the rack GUI switches the mode: Status (15 running,
     8 warning, 0 stopped), Heat (0 at ambient to 15 at the critical temperature) or Energy (FE fill / bill paid).
   - **Wear warnings**: when a part drops below 10% (`wear.warningThreshold`) or breaks, the owner gets a chat message
     and a toast (`wear.toasts`).
   - **Stats tab** in the wallet: hosting, mining, electricity and shop spending over 1 day / 1 week / 1 month / 1 year,
     as a bar chart with a profit line and hover details.

9. **Power generation** (creative tabs *DataCenter – Energy* and *DataCenter – Materials*):
   - **Generators** push FE into blocks next to them (racks, batteries, other mods' machines):
     | Family | Tiers (peak FE/t) | Needs |
     |---|---|---|
     | Solar | Polycrystalline 8, Monocrystalline 20, PERC 40, Perovskite tandem 80 | open sky; follows the sun, clouds −65 % |
     | Wind | Small 24, Medium 80, Large 240 (rotor drawn spinning) | free air (3×3 / 5×5 / 7×7) in front, height above sea level |
     | Hydro | Water wheel 16, Micro-hydro 60, Hydroelectric 192 | flowing water at the sides / water column above |
     | Combustion | Combustion generator 40, Gas turbine 120 | furnace fuel (gas turbine: 2× energy per item) |
     | Geothermal | Geothermal 36, Binary cycle 90 | lava sources on its sides, +50 % in the Nether |
     | Nuclear | Small Modular Reactor 2000 | fuel rods (7 calendar days each) and cooling water; SCRAMs when too hot |
     1 FE/t ≈ 10 W with the default `energy.fePerWatt`; a Pentium 4 server needs ~20 FE/t.
   - **Battery banks** (lead-acid 500k, Li-ion 4M, LFP 20M FE) charge from every side, discharge through the front and keep
     their charge when broken. **Power cables** (copper 256, aluminium 2,048, superconducting 16,384 FE/t) link generators,
     batteries and racks into networks: consumers first, then batteries, then grid connections.
   - **Grid connection** (residential 5 kW, commercial 50 kW, utility 500 kW): sells surplus FE to the owner's wallet at a
     feed-in tariff of $3/kWh in 2010, rising ~6.5 %/year – a few percent of what the same energy earns in a server, so
     mining and hosting stay the way to make money. Shown as *Grid* in the Stats tab.
   - In electricity bill mode, FE in a rack's buffer is used first and only the rest goes on the bill.
   - **Ores** (world generation): bauxite → aluminium (y 32–100), spodumene → lithium carbonate (y −32–40),
     uraninite → yellowcake → enriched uranium (deep, rare, iron pickaxe). Steel = iron + 2 coal, blast furnace.
   - Recipes use real components: doped silicon wafers → solar cells, copper coils + magnets → alternators, fibreglass and
     carbon-fibre blades → rotors, capacitors → inverters and charge controllers, lithium cells → battery banks.
     Common `c:` tags are used, so other mods' aluminium, steel etc. work too. Everything is also in the shop.
   - The network tester works as a multimeter on power cables and power blocks; Jade shows generators and batteries.

10. **Progression:** new players get the **Data Center Handbook**, a guide book (also in the shop and craftable from
   a book + circuit board). The advancement tab *Data Center* tracks the milestones: first server online, a full
   12-server rack, overheating, water cooling, first coins, ASICs, a halving, first and Dedicated customers, fiber,
   repairs, $10,000 and $1,000,000, the first generator, aluminium, a full LFP bank, grid export and a running reactor.

11. **Jade:** with [Jade](https://modrinth.com/mod/jade) installed, looking at any rack block shows the merged rack's
   status, income, customers, FE (or electricity bill), temperature and, while sneaking, the owner. Jade is optional.

## Configuration

### Global settings: `config/datacenterextended-synced.toml`
The server sends these settings to clients. Sections:
- `calendar`: `startDate`, `ticksPerDay`
- `general`: compute and hashrate multipliers, PSU limit, `giveHandbook`
- `energy`: FE requirement (on by default), FE per watt, buffer size and input rate, `electricityBills`, `pricePerKWh`
- `cooling`: heat on/off, passive cooling per rack block, throttle / critical temperature, minimum speed, biome ambient
- `wear`: wear on/off, loss per hour, heat multiplier, minimum performance, repair cost and resale factors,
  `warningThreshold`, `toasts`
- `economy`: starting balance, sell fee
- `market`: update interval; per coin difficulty reference (`basePrice`, `hashesPerCoin`, `difficultyExponent`),
  price noise (`volatility`, `reversion`), `futureDrift`, `postMergeReward`
- `shop`: remote shop on/off (`remoteShop = false` means buying and servicing only at a Hardware Shop block), price multiplier,
  `releaseDates`, `yearlyPriceDrop`, `minPriceFactor`, `prebuiltMarkup`
- `memory`: RAM requirement, starved factor, speed and multi-channel bonus
- `mining`, `fabrication`: CPU mining, fabrication speed and blacklist
- `hosting`: customer check interval, sign-up / overload-cancel / churn chances, and per plan (`shared`, `vps`,
  `dedicated`) compute, price per hour and weight
- `network`: capacity of each cable tier, `maxCablesPerNetwork`
- `racks`: fabricating intake and hosting capacity per segment, storage bonus
- `power`: output multiplier per generator family, `cableTransferMultiplier`, `maxCablesPerNetwork`, `fuelRodDays`,
  `billSelfConsumption`, `gridExportPricePerKWh`, `gridYearlyGrowth`, `gridStandingCharge`

### Startup settings: `config/datacenterextended-startup.toml`
- `enablePartRecipes`: set to `false` to remove all part crafting recipes (shop only). Needs a restart.

### Per-item values: data maps (datapack-overridable, synced to clients)
Any item, including items from other mods, can be given hardware stats or a file size.

`data/<any_namespace>/data_maps/item/hardware.json`
```json
{ "values": {
    "datacenterextended:amd_fx_4300": { "type": "cpu", "socket": "am3plus", "compute": 10, "power": 95, "recommended_memory": 4 },
    "datacenterextended:geforce_gtx_460": { "type": "gpu", "hashrate": 1.0, "power": 160 },
    "datacenterextended:x570_motherboard": { "type": "motherboard", "socket": "am4", "memory_type": "ddr4", "max_memory": 128,
                                             "cpu_sockets": 1, "gpu_slots": 3, "memory_slots": 4, "storage_slots": 4, "power": 30 },
    "datacenterextended:ddr4_16gb": { "type": "memory", "memory_type": "ddr4", "memory": 16, "speed": 3200, "power": 4 },
    "datacenterextended:psu_550w": { "type": "psu", "power": 550, "efficiency": 0.82 },
    "datacenterextended:ssd_1tb": { "type": "storage", "storage": 1024, "power": 5 }
} }
```

Sockets and memory types are free-form ids; a part or board without one fits everything. Add a lang key
`socket.datacenterextended.<id>` to give a new socket a display name.

`data/<any_namespace>/data_maps/item/file_size.json`: numbers (bytes) or strings such as `"512 KB"`, `"1 MB"`, `"2.5 GB"`
```json
{ "values": {
    "minecraft:cobblestone": "1 MB",
    "#c:ingots": "48 MB",
    "#c:ingots/tin": "24 MB"
} }
```
When an item matches several entries, the most specific one wins: item id > `#c:ingots/iron` > `#c:ingots`.
Add `"replace": true` to an entry to force it. The item tag `#datacenterextended:not_fabricable` blocks fabrication.

ASIC miners use `"type": "asic"` with `hashrate`, `power` and `efficiency` (their built-in PSU); they go into rack bays.

`data/<any_namespace>/data_maps/item/cooling.json`: any item with an entry fits a rack's cooling slot
```json
{ "values": { "datacenterextended:rack_fan": { "capacity": 800, "power": 20 } } }
```

`data/<any_namespace>/data_maps/item/shop_price.json`: price in dollars; any item with an entry appears in the shop
```json
{ "values": { "datacenterextended:pentium_4": 39, "minecraft:diamond": 250 } }
```

### Add-ons
Other mods can join compute networks by exposing the `datacenterextended:compute_node` block capability
(`de.f4ked.datacenterextended.network.ComputeNode`) and registering the block position with
`ComputeNetworkManager.registerNode(level, pos)`.

## Development
```
./gradlew build                # build the jar + run unit tests
./gradlew runClient            # start a dev client
./gradlew runGameTestServer    # headless server: loads all data and runs the in-world game tests
python tools/gen_resources.py . # regenerate models, recipes, data maps, release dates, en_us.json
                               # (existing PNGs are kept; add --force-textures to redraw them)
```
`de_de.json` is maintained by hand; the generator does not touch it. The textures of the ASICs, cooling items and the
handbook are hand-drawn PNGs (the generator only writes their models). Advancements (`data/datacenterextended/advancement/`)
are hand-written as well; code-granted ones use a single `minecraft:impossible` criterion named `done`
(see `ModAdvancements`).
