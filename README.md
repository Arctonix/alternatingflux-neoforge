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
- **Live to the touch** — like IE's own HV line, an AF wire shocks anything that walks into it, harder than HV does. Both the reach and the damage are configurable, and setting the damage to `0` turns it off.
- **Strain spans** — a line anchored at *both* ends may run twice as far. See below.

Think of it as the EHV transmission backbone of your power grid: bulk power across distance on AF lines, stepped down to HV/MV/LV for actual use.

## Strain spans

Immersive Engineering measures every span against a single number. Alternating Flux adds one exception to it: with a **dead-end at both ends**, a line may reach twice as far. The rule describes the *structure*, not the wire, so it applies to **every coil in the game** — copper, electrum and steel each double from whatever their own tier reaches, exactly as AF does. One anchored end is not enough, and relays, insulator strings and busbars carry no tension, so they never grant it.

The extra length is paid for in clearance: the slack is unchanged, so a span of twice the distance hangs about twice as deep.

**Alternating Flux ships no strain hardware of its own.** With nothing tagged as an anchor — which is exactly how a plain install arrives — every wire in the game reaches precisely as far as it always has, and the mod says nothing about the rule anywhere in game. See *For add-on authors* below.

The Engineer's Manual carries the full entry under *Electrical Grids*.

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

## Download

Get it from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/alternating-flux-neoforge), [Modrinth](https://modrinth.com/mod/alternating-flux-neoforge), or [GitHub Releases](../../releases).

## Installation

Drop the `alternatingflux-<version>.jar` into your `mods/` folder alongside Immersive Engineering and NeoForge. Needed on both client and server.

## Configuration

Server config (`alternatingflux-server.toml`) exposes:

- `transferRate` — AF transfer rate in IF/t (default 131072)
- `maxLength` — max length of a single AF wire in blocks (default 96)
- `lossRatio` — power loss across a full-length run (default 0.0005)
- `wireColour` — RGB colour of the AF wire (default 0xF6866C, the original salmon)
- `damageRadius` — how far from an AF wire the shock reaches, in blocks (default 0.5; IE's HV uses 0.3)
- `shockDamageBase` — base shock damage, scaled by how loaded the line is (default 25; IE's HV uses 15). Set to `0` to disable shocks entirely.

A strain span is always twice `maxLength`; it is not separately configurable.

## For add-on authors

Alternating Flux is meant to be built on. It deliberately ships **no strain hardware**, only the rule and the socket to plug hardware into:

```
alternatingflux:strain_anchors     (block tag)
```

Put a block into that tag and a wire anchored to it at **both** ends may span twice the coil's normal reach. That is the entire contract. The tag ships empty, so a plain install behaves exactly as it did before the feature existed.

The rule is enforced where Immersive Engineering itself checks span length — in the coil, on the second click — which means it applies to every coil, not only AF's, and it can only ever *lengthen* a span. A connector may still refuse a connection for its own reasons.

This tag is public API. It will not be renamed or re-scoped.

## Credits

Alternating Flux was originally created by [AntiBlueQuirk](https://github.com/AntiBlueQuirk) (for Minecraft 1.12.2). The concept, design, and original artwork are theirs — this is an update of their work to 1.21.1, shared publicly with their permission.

- **AntiBlueQuirk** — original creator (mod, concept, design, assets) · [original repo](https://github.com/AntiBlueQuirk/alternatingflux)
- **Arctonix** — 1.21.1 / NeoForge update
- **BluSunrize / Immersive Engineering team** — for IE and its API

## License

Shared with the permission of the original author, AntiBlueQuirk. Free, visible source, credit retained, no paywalling — see the [LICENSE](LICENSE) file for details.
