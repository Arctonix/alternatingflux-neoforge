# Alternating Flux

A long-distance, low-loss power transmission tier for [Immersive Engineering](https://www.curseforge.com/minecraft/mc-mods/immersive-engineering) on **Minecraft 1.21.1 / NeoForge**.

> **Alternating Flux** was originally created by **AntiBlueQuirk** for Minecraft 1.12.2.
> This is an updated port to 1.21.1 / NeoForge by **Arctonix**, with the original
> author's blessing. All credit for the original mod, concept, and assets goes to
> AntiBlueQuirk — see [the original mod](https://github.com/AntiBlueQuirk/alternatingflux).

---

## What it does

Immersive Engineering's HV wires are great, but they top out at short distances — you end up planting relays every 32 blocks. **Alternating Flux (AF)** adds a "super high voltage" wire tier built for hauling power across large distances:

- **Long range** — AF wires reach up to **96 blocks** per span (configurable), far beyond HV.
- **Low loss** — much lower power loss over distance than standard wires.
- **High throughput** — carries **131,072 IF/t** (4× modern HV).
- **Transmission only** — AF cannot power machines directly. You step down to HV through an **AF Transformer**, exactly like a real substation steps transmission voltage down to distribution.

Think of it as the EHV transmission backbone of your power grid: bulk power across distance on AF lines, stepped down to HV/MV/LV for actual use.

## How to use it

1. Craft **Constantan Wire** (constantan plate + wirecutter), then an **AF Wire Coil**.
2. Place **AF Wire Relays** as your towers/poles and string AF coil between them for long-distance runs.
3. To get power onto an AF line, build an **AF Transformer** — connect HV on the low side and AF on the high side. It converts Flux ↔ Alternating Flux at 1:1.
4. At the far end, another AF Transformer steps it back down to HV to feed your machines.

> **Note:** Like all IE wires, power only flows while the wire's endpoint connectors (your substations/sources/sinks) are in loaded chunks. Long AF lines cross more chunks — keep the endpoints chunk-loaded.

## Requirements

- Minecraft **1.21.1**
- **NeoForge** 21.1.x
- **Immersive Engineering** (required dependency — install it alongside this mod)

## Installation

Drop the `alternatingflux-x.x.x.jar` into your `mods/` folder alongside Immersive Engineering and NeoForge. Needed on both client and server.

## Configuration

Server config (`alternatingflux-server.toml`) exposes:

- `transferRate` — AF transfer rate in IF/t (default 131072)
- `maxLength` — max length of a single AF wire in blocks (default 96)
- `lossRatio` — power loss across a full-length run (default 0.0005)
- `wireColour` — RGB colour of the AF wire (default 0xF6866C, the original salmon)

## Credits

Alternating Flux was originally created by [AntiBlueQuirk](https://github.com/AntiBlueQuirk) (for Minecraft 1.12.2). The concept, design, and original artwork are theirs — this is an update of their work to 1.21.1, shared publicly with their permission.

- **AntiBlueQuirk** — original creator (mod, concept, design, assets) · [original repo](https://github.com/AntiBlueQuirk/alternatingflux)
- **Arctonix** — 1.21.1 / NeoForge update
- **BluSunrize / Immersive Engineering team** — for IE and its API

## License

Shared with the permission of the original author, AntiBlueQuirk. Free, visible source, credit retained, no paywalling — see the [LICENSE](LICENSE) file for details.
