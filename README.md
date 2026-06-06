# Alternating Flux

A long-distance, low-loss power transmission tier for [Immersive Engineering](https://www.curseforge.com/minecraft/mc-mods/immersive-engineering) on **Minecraft 1.21.1 / NeoForge**.

> **This is the 1.1 alpha line** (`uaf-tier` branch): it adds the **Ultra High Alternating Flux (UAF)** tier on top of the stable AF release. Expect rough edges — please report issues.

> **Alternating Flux** was originally created by **AntiBlueQuirk** for Minecraft 1.12.2.
> This is an updated port to 1.21.1 / NeoForge by **Arctonix**, with the original
> author's blessing. All credit for the original mod, concept, and assets goes to
> AntiBlueQuirk — see [the original mod](https://github.com/AntiBlueQuirk/alternatingflux).

---

## What it does

Immersive Engineering's HV wires are great, but they top out at short distances — you end up planting relays every 32 blocks. **Alternating Flux (AF)** adds a "super high voltage" wire tier built for hauling power across large distances, and **Ultra High Alternating Flux (UAF)** stacks an even higher trunk-line tier on top of it:

- **Long range** — AF and UAF wires reach up to **96 blocks** per span (configurable), far beyond HV.
- **Low loss** — much lower power loss over distance than standard wires; UAF lower still.
- **High throughput** — AF carries **131,072 IF/t** (4× modern HV); UAF carries **524,288 IF/t** (4× AF, 16× HV).
- **Transmission only** — neither tier can power machines directly. You step down through a transformer, exactly like a real substation steps transmission voltage down to distribution.
- **Live lines shock** — like uninsulated HV, an energised AF or UAF wire shocks entities that touch it, harder the more power it carries. An idle line is harmless.

Think of it as the EHV/UHV transmission backbone of your power grid: bulk power across distance on AF/UAF lines, stepped down to HV/MV/LV for actual use.

## How to use it

1. Craft **Constantan Wire** (constantan plate + wirecutter), then an **AF Wire Coil**.
2. Place **AF Wire Relays** as your towers/poles and string AF coil between them for long-distance runs.
3. To get power onto an AF line, build an **AF Transformer** — connect HV on the low side and AF on the high side. It converts Flux ↔ Alternating Flux at 1:1.
4. At the far end, another AF Transformer steps it back down to HV to feed your machines.
5. For even bigger trunk lines, craft **UAF Wire Coils** and **UAF Wire Relays**, then bridge into the UAF network with the **HV–UAF Transformer** (straight from HV) or the **AF–UAF Transformer** (from an existing AF line).

> **Note:** Like all IE wires, power only flows while the wire's endpoint connectors (your substations/sources/sinks) are in loaded chunks. Long AF lines cross more chunks — keep the endpoints chunk-loaded.

## Requirements

- Minecraft **1.21.1**
- **NeoForge** 21.1.x
- **Immersive Engineering** (required dependency — install it alongside this mod)

## Download

Get it from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/alternating-flux-neoforge), [Modrinth](https://modrinth.com/mod/alternating-flux-neoforge), or [GitHub Releases](../../releases).

## Installation

Drop the `alternatingflux-<version>.jar` into your `mods/` folder alongside Immersive Engineering and NeoForge. Needed on both client and server.

## Configuration

Server config (`alternatingflux-server.toml`) exposes:

**AF** (section `alternating_flux`)

- `transferRate` — AF line carrying capacity in IF/t (default 131072)
- `maxLength` — max length of a single AF wire in blocks (default 96)
- `lossRatio` — power loss across a full-length run (default 0.0005)
- `wireColour` — RGB colour of the AF wire (default 0xF6866C, the original salmon)
- `damageRadius` — how close an entity must get to a live AF wire to be shocked (default 0.5; 0 disables)
- `shockDamageBase` — base shock damage of a fully-loaded AF wire (default 25; 0 disables)

**UAF** (section `ultra_high_alternating_flux`)

- `transferRate` — UAF line carrying capacity in IF/t (default 524288)
- `maxLength` — max length of a single UAF wire in blocks (default 96)
- `lossRatio` — power loss across a full-length run (default 0.0001)
- `wireColour` — RGB colour of the UAF wire (default 0x8B3FD6, purple)
- `damageRadius` — how close an entity must get to a live UAF wire to be shocked (default 0.75; 0 disables)
- `shockDamageBase` — base shock damage of a fully-loaded UAF wire (default 40; 0 disables)

## Credits

Alternating Flux was originally created by [AntiBlueQuirk](https://github.com/AntiBlueQuirk) (for Minecraft 1.12.2). The concept, design, and original artwork are theirs — this is an update of their work to 1.21.1, shared publicly with their permission.

- **AntiBlueQuirk** — original creator (mod, concept, design, assets) · [original repo](https://github.com/AntiBlueQuirk/alternatingflux)
- **Arctonix** — 1.21.1 / NeoForge update
- **BluSunrize / Immersive Engineering team** — for IE and its API

## License

Shared with the permission of the original author, AntiBlueQuirk. Free, visible source, credit retained, no paywalling — see the [LICENSE](LICENSE) file for details.
