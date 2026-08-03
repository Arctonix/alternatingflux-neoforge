# Changelog

All notable changes to **Alternating Flux**.

Alternating Flux ships from one repository across several Minecraft versions, on separate branches.
The `1.0.x` line is the stable line and is the only one still maintained. Version numbers mean the
same thing on every branch: `1.0.5` on 1.16.5 is the same feature set as `1.0.5` on 1.21.1, allowing
for what each Minecraft version can express.

| Minecraft | Loader | Branch | Current |
|---|---|---|---|
| 1.21.1 | NeoForge 21.1.x | `master` | **1.0.6** |
| 1.20.1 | Forge 47.x / NeoForge 20.1.x | `1.20.1-forge-stable` | **1.0.6** |
| 1.19.2 | Forge 43.x | `1.19.2-forge-stable` | 1.0.5 |
| 1.18.2 | Forge 40.x | `1.18.2-forge-stable` | 1.0.5 |
| 1.16.5 | Forge 36.x | `1.16.5-forge-stable` | 1.0.5 |

---

## 1.0.6 — 2026-08-03

*1.21.1 and 1.20.1.*

### Added

- **Strain spans.** A wire anchored at a strain anchor at **both** ends may reach twice as far. One
  anchored end is not enough, and relays, insulator strings and busbars carry no tension, so they
  never grant it. The extra length is paid for in clearance — the slack is unchanged, so a span of
  twice the distance hangs about twice as deep.
- **The public block tag `alternatingflux:strain_anchors`.** This is the entire contract: put a block
  in that tag and it counts as a strain anchor. The rule is enforced where Immersive Engineering
  itself checks span length — in the coil, on the second click — so it applies to **every coil in the
  game**, not only Alternating Flux's. Copper, electrum and steel each double from whatever their own
  tier reaches.
- An Engineer's Manual entry, under *Electrical Grids*.
- A coil readout while a link is held, showing the distance and the reach it is actually working to.

**Alternating Flux ships no strain hardware of its own, and the tag ships empty.** On a plain install
nothing is tagged, every wire reaches exactly as far as it always has, and the mod says nothing about
the rule anywhere in game. The feature exists for add-ons to build on.

This tag is public API. It will not be renamed or re-scoped.

### Fixed

- The **AF Wire Relay's inventory icon** now matches Immersive Engineering's own connectors. Its item
  model inherited both the default block item transforms and the block's translucent render type, so
  the icon drew smaller than every IE relay beside it, sat lower, was half size on the ground and in
  item frames, and drew mostly see-through. The block itself is unchanged — the insulator cups are
  still glassy in world.

### Documentation

- The README now documents the shock behaviour added in 1.0.4, its two config keys, and the
  `strain_anchors` tag.

---

## 1.0.5 — 2026-06-06

*All five Minecraft versions.*

### Fixed

- **Transformer low-side wire height.** The low terminal returned the high terminal's offset through
  a virtual self-call, putting it at 0.75 instead of IE's 0.5625. Wires attached to the low side sat
  visibly too high.
- **Feedthrough attach point and hitbox**, by moving to the six-argument registration so the
  connection length and offset are set independently rather than one value being used for both.
- Immersive Engineering re-pinned to 12.4.2. The previous pin resolved to 12.1.1 despite what the
  comment beside it claimed.
- `transferRate` can no longer be configured to zero, which produced a NaN in the shock calculation.
- Dependency ranges corrected, `update.json` added, and roughly thirteen orphaned assets removed per
  branch.

### Changed

- Continuous integration now attaches the built jar to a GitHub Release automatically when a `v*` tag
  is pushed.

---

## 1.0.4 — 2026-06-06

### Added

- **AF wires shock on contact**, continuing IE's tier progression: radius 0.5 (HV uses 0.3), base
  damage 25 (HV uses 15). Two new config keys, `damageRadius` and `shockDamageBase`; setting the
  damage to `0` disables shocks entirely.

  The 1.12 original never shocked, because it never requested the damage handler. That was a bug in
  the original rather than a design decision, and this fixes it rather than reproducing it.

- A mirrored recipe for the AF wire coil, and both coil recipes grouped.

### Fixed

- Simplified Chinese translations restored on the modern translation keys.

---

## 1.0.3 — 2026-06-01

### Fixed

- **The AF relay's insulator cups are translucent**, matching IE's HV relay. One line on the block
  model. The transformer's cups stay solid, exactly as IE's own transformer does — its green is a
  flush band painted on a solid body, so translucency there would show the body straight through it
  rather than reading as glass.

---

## 1.0.2 — 2026-05-31

### Fixed

- In-mod links pointed at the old repository.

---

## 1.0.1 — 2026-05-31

### Fixed

- **The relay and transformer had no loot tables and dropped nothing when mined.**
- Feedthrough support restored, and its texture upscaled from 8×8 to 16×16.

---

## 1.0.0 — 2026-05-31

First release of the 1.21.1 / NeoForge port.

Alternating Flux adds an above-HV transmission tier to Immersive Engineering: long-distance,
low-loss wires carrying 131,072 IF/t up to 96 blocks per span, which cannot power machines directly
and must be stepped down through an AF Transformer.

Originally created by **AntiBlueQuirk** for Minecraft 1.12.2, and ported here with their permission.

### Deliberate differences from the 1.12 original

These are intentional and track modern Immersive Engineering rather than 1.12-era IE. They are not
bugs:

- Wire loss ratio `0.0005`, where 1.12 used `0.005`. Modern IE lowered its own wire loss ratios, so
  AF is balanced against current IE wires rather than 1.12's.
- Maximum span 96 blocks (was 48) and slack 1.003 (was 1.002).
- The relay recipe uses insulating glass, like modern IE connectors, and the transformer is three
  blocks tall to match modern IE's.

---

## Note on the 1.1 / UAF line

Prereleases tagged `v1.1.0-alpha.*` exist in this repository's history. **That line is abandoned.**
Everything it carried — the UAF tier above AF — moved into a separate add-on,
*Alternating Flux: Interchange*, so that Alternating Flux itself stays the mod AntiBlueQuirk wrote
and Arctonix ported, with no original content mixed in.

Alternating Flux continues on the `1.0.x` line. Do not install the 1.1 alphas; they are superseded.
