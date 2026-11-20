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
| **Mining Rack** | GPU hashrate (MH/s) of its own servers, standalone | Bitcoin, Dogecoin or Ethereum (selectable) into the owner's wallet |
| **Hosting Rack** | Compute (KB/s), with a bonus for installed storage | Dollars into the owner's wallet |
| **Fabricating Rack** | Compute (KB/s) | Copies of the target item, by its *file size* (e.g. cobblestone = 1 MB) |

3. **Data cables** (Coax → Cat5e → Cat6a → Fiber) connect Hosting and Fabricating racks into a **shared compute pool**.
   Each rack uses its own servers first. Spare compute flows to racks that need more, split fairly between them.
   A rack without servers can run on network compute alone.
   **A network's bandwidth is its slowest cable**, so one Coax cable in a Fiber backbone limits everything.
   Right-click a cable or rack with the **Network Tester** to see bandwidth, flow and the bottleneck.

4. **Energy:** each rack block has its own FE buffer (200,000 FE by default; a 3-block rack holds 600,000) that any
   FE cable can fill. Servers use `watts × 0.1 / PSU efficiency` FE per tick. A 500 W server with an 80+ Gold PSU uses ~58 FE/t.
   When the buffer runs low, servers shut down one by one. The rack GUI shows a gauge and the FE/t; hover the gauge for details.
   Turn `energy.requireEnergy` off to play without an energy mod.

5. **Wallet:** press **K** (rebindable under Controls) to open your wallet. It shows your dollar balance, your coins,
   live prices with a chart, and lets you sell coins (25% / 50% / all or a typed amount, minus a 2% fee).
   Racks pay the player who placed them, even while that player is offline. New players start with $250.
   Coin prices follow a mean-reverting random walk per world (updated every minute), so timing your sales matters.

6. **Hardware Shop:** buy parts, racks, cables and the workbench with dollars, at a **Hardware Shop** block or from the
   Shop tab of the wallet screen. The $250 start covers a first hosting setup (Pentium 4 server + Hosting Rack).
   Crafting recipes for parts still exist, but they are expensive chains (each part needs the previous one plus processor
   dies, gold/diamond blocks or echo shards), so the shop is the normal way early on.

## Configuration

### Global settings: `config/datacenterextended-synced.toml`
The server sends these settings to clients. Sections:
- `general`: compute and hashrate multipliers, PSU limit
- `energy`: FE requirement (on by default), FE per watt, buffer size and input rate
- `economy`: starting balance, sell fee
- `market`: update interval; per coin base price, volatility, mean reversion and difficulty (`hashesPerCoin`)
- `shop`: remote shop on/off (`remoteShop = false` means buying only at a Hardware Shop block), price multiplier
- `memory`: RAM requirement, starved factor, speed and multi-channel bonus
- `mining`, `hosting`, `fabrication`: CPU mining, `bytesPerDollar`, fabrication speed and blacklist
- `network`: capacity of each cable tier, `maxCablesPerNetwork`
- `racks`: fabricating intake and hosting capacity per segment, storage bonus

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
python tools/gen_resources.py . # regenerate textures, models, recipes, data maps, en_us.json
```
`de_de.json` is maintained by hand; the generator does not touch it.
