# Alternating Flux

Long-distance, low-loss power transmission tiers for [Immersive Engineering](https://www.curseforge.com/minecraft/mc-mods/immersive-engineering) on **Minecraft 1.19.2 / Forge**.

> **Alternating Flux** was originally created by **AntiBlueQuirk** for Minecraft 1.12.2.
> This is an updated port by **Arctonix**, with the original author's blessing. All
> credit for the original mod, concept, and assets goes to AntiBlueQuirk — see
> [the original mod](https://github.com/AntiBlueQuirk/alternatingflux).

> **Alpha:** this 1.19.2 line is a backport of the 1.21.1 / NeoForge release and is
> still alpha. The UAF tier in particular is new and may change.

---

## What it does

Immersive Engineering's HV wires are great, but they top out at short distances — you end up planting relays every 32 blocks. **Alternating Flux (AF)** adds a "super high voltage" wire tier built for hauling power across large distances, and **Ultra High Alternating Flux (UAF)** adds a second tier above it for the biggest trunk lines:

- **Long range** — AF and UAF wires reach up to **96 blocks** per span (configurable), far beyond HV.
- **Low loss** — much lower power loss over distance than standard wires; UAF lower still than AF.
- **High throughput** — AF carries **131,072 IF/t** (4× modern HV); UAF carries **524,288 IF/t** (16× HV, 4× AF).
- **Transmission only** — neither tier can power machines directly. You step down through a transformer, exactly like a real substation steps transmission voltage down to distribution.
- **Live wires shock** — like IE's HV lines, an energised AF or UAF wire shocks creatures that touch it, scaled by how much power it is carrying.

Think of it as the EHV/UHV transmission backbone of your power grid: bulk power across distance on AF/UAF lines, stepped down to HV/MV/LV for actual use.

## How to use it

### AF tier

1. Craft **Constantan Wire** (constantan plate + wirecutter), then an **AF Wire Coil**.
2. Place **AF Wire Relays** as your towers/poles and string AF coil between them for long-distance runs.
3. To get power onto an AF line, build an **AF Transformer** — connect HV on the low side and AF on the high side. It converts Flux ↔ Alternating Flux at 1:1.
4. At the far end, another AF Transformer steps it back down to HV to feed your machines.

### UAF tier

1. Refine a finished AF coil with extra Constantan and Graphite into a **UAF Wire Coil**.
2. Place the taller **UAF Wire Relays** and string UAF coil between them.
3. Bridge UAF to the lower tiers with one of two transformers: the **HV–UAF Transformer** (steps between HV and UAF) or the **AF–UAF Transformer** (steps between AF and UAF). Both convert at 1:1.

> **Note:** Like all IE wires, power only flows while the wire's endpoint connectors (your substations/sources/sinks) are in loaded chunks. Long AF/UAF lines cross more chunks — keep the endpoints chunk-loaded.

## Requirements

- Minecraft **1.19.2**
- **Forge** 43.x
- **Immersive Engineering** 9.2.x (required dependency — install it alongside this mod)

## Installation

Drop the `alternatingflux-x.x.x.jar` into your `mods/` folder alongside Immersive Engineering and Forge. Needed on both client and server.

## Configuration

Server config (`alternatingflux-server.toml`) exposes, per tier:

**AF** (`[alternating_flux]`):

- `transferRate` — AF transfer rate in IF/t (default 131072)
- `maxLength` — max length of a single AF wire in blocks (default 96)
- `lossRatio` — power loss across a full-length run (default 0.0005)
- `wireColour` — RGB colour of the AF wire (default 0xF6866C, the original salmon)
- `damageRadius` — how close an entity must get to a live AF wire to be shocked (default 0.5)
- `shockDamageBase` — base shock damage of a fully-loaded AF wire (default 25; set 0 to disable)

**UAF** (`[ultra_high_alternating_flux]`):

- `transferRate` — UAF transfer rate in IF/t (default 524288)
- `maxLength` — max length of a single UAF wire in blocks (default 96)
- `lossRatio` — power loss across a full-length run (default 0.0001)
- `wireColour` — RGB colour of the UAF wire (default 0x8B3FD6, purple)
- `damageRadius` — shock proximity for a live UAF wire (default 0.75)
- `shockDamageBase` — base shock damage of a fully-loaded UAF wire (default 40; set 0 to disable)

## Credits

- **AntiBlueQuirk** — original creator of Alternating Flux. The concept, design, textures, and models are theirs.
- **Arctonix** — port and the UAF tier.
- **BluSunrize and the Immersive Engineering team** — for IE and its API, which this addon builds on.

## License

This project follows the original's licensing under Blu's License of Common Sense.
Free to use, visible source, credit retained. Not for sale or paywalling.
