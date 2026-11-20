# DataCenterExtended

NeoForge mod for Minecraft 26.3. Assemble **servers**, mount them in **racks** and turn their performance into money and items.

## Gameplay

1. **Server Workbench:** put a **Server (1U)** item in and install a motherboard, PSU, CPUs, GPUs and storage drives.
   The parts are stored inside the server item, and the tooltip shows its stats.
   The motherboard decides how many CPU sockets, GPU slots and storage bays are usable. The PSU must cover the power draw.
2. **Racks** hold 4 servers each (4 HU). Up to **3 racks of the same type** stacked on top of each other **merge** into one
   tall rack with one GUI (12 servers). The bottom block is the controller.

| Rack | Uses | Produces |
|---|---|---|
| **Mining Rack** | GPU hashrate (MH/s) of its own servers, standalone | Bitcoin, which you sell at the **Trading Terminal** |
| **Hosting Rack** | Compute (KB/s), with a bonus for installed storage | Credits |
| **Fabricating Rack** | Compute (KB/s) | Copies of the target item, by its *file size* (e.g. cobblestone = 1 MB) |

3. **Data cables** (Coax → Cat5e → Cat6a → Fiber) connect Hosting and Fabricating racks into a **shared compute pool**.
   Each rack uses its own servers first. Spare compute flows to racks that need more, split fairly between them.
   A rack without servers can run on network compute alone.
   **A network's bandwidth is its slowest cable**, so one Coax cable in a Fiber backbone limits everything.
   Right-click a cable or rack with the **Network Tester** to see bandwidth, flow and the bottleneck.

Credits come as Credit (1), Credit Bundle (9) and Credit Crate (81). Better hardware costs credits and the previous tier.

## Configuration

### Global settings: `config/datacenterextended-synced.toml`
The server sends these settings to clients. Sections:
- `general`: compute and hashrate multipliers, PSU limit
- `energy`: optional Forge Energy requirement
- `mining`, `hosting`, `fabrication`, `trading`: rates and blacklist
- `network`: capacity of each cable tier, `maxCablesPerNetwork`
- `racks`: fabricating intake and hosting capacity per segment, storage bonus

### Per-item values: data maps (datapack-overridable, synced to clients)
Any item, including items from other mods, can be given hardware stats or a file size.

`data/<any_namespace>/data_maps/item/hardware.json`
```json
{ "values": {
    "datacenterextended:amd_fx_4300": { "type": "cpu", "compute": 10, "power": 95 },
    "datacenterextended:geforce_gtx_460": { "type": "gpu", "hashrate": 1.0, "power": 160 },
    "datacenterextended:office_motherboard": { "type": "motherboard", "cpu_sockets": 1, "gpu_slots": 1, "storage_slots": 1, "power": 15 },
    "datacenterextended:psu_550w": { "type": "psu", "power": 550, "efficiency": 0.82 },
    "datacenterextended:ssd_1tb": { "type": "storage", "storage": 1024, "power": 5 }
} }
```

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
