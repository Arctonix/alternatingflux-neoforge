# Alternating Flux — 1.21.1 / NeoForge port

A port of AntiBlueQuirk's [Alternating Flux](https://github.com/AntiBlueQuirk/alternatingflux)
addon for Immersive Engineering, from 1.12.2/Forge to **Minecraft 1.21.1 / NeoForge**.

Alternating Flux (AF) is a long-distance "super high voltage" wire tier: very low
loss over distance, but it **cannot power machines directly** — you step it down to
HV through an **AF Transformer** at a 1:1 rate. It uses IE's normal Flux/IF energy;
it is not a separate power system.

---

## Status — this is a foundation build, not a finished mod

Done and believed correct (written against the real 1.21.1 IE source):

- Gradle project (ModDevGradle, NeoForge 21.1.164, MC 1.21.1, Java 21, Parchment).
- IE pulled as a dependency via CurseMaven.
- **`AFWireType`** — the AF wire tier as an `IEnergyWire` (the modern equivalent of
  the old `WireType` energy methods). Loss formula mirrors IE's own energy wire.
- **`Config`** — NeoForge `ModConfigSpec` exposing transfer rate, max length, loss
  ratio, and colour.
- **Items** — the AF Wire Coil (reuses IE's own `WireCoilItem`, so wire placement /
  length-checking / consumption all work for free) and Constantan Wire.
- Creative tab, item models, lang, and all original textures + OBJ models ported.

### Remaining work (phase 2 — the heavy lift)

These extend IE's **internal** (non-API) block-entity classes and need to be built
and iterated in a real dev environment with the game running. They were studied but
not yet written:

1. **AF Relay block + `AFRelayBlockEntity`** — model on
   `EnergyConnectorBlockEntity` (relay variant). AF has no machine connector by
   design, so only a relay is needed.
2. **AF Transformer block + `AFTransformerBlockEntity`** — extend
   `AbstractTransformerBlockEntity` (or `TransformerHVBlockEntity`); set
   `getHigherWiretype()` to `"AF"` and `acceptableLowerWires` to `{HV}`.
3. **Feedthrough registration** — call
   `WireApi.registerFeedthroughForWiretype(AFWireType.AF, ...)` in setup, pointing at
   `relay_af.obj` + `passthrough_af` texture (assets already ported).
4. **Block entities, capabilities, blockstates, datagen recipes**, and the IE manual
   entry binding.

---

## Tuning (decided, config-exposed)

| Property       | Value      | Notes                                             |
|----------------|------------|---------------------------------------------------|
| Transfer rate  | 131072 IF/t| 4× modern HV (32768).                              |
| Max length     | 64 blocks  | No hard cap in IE; longer = crosses more chunks.   |
| Loss ratio     | 0.0005     | Below HV's 0.0008 over a longer span → AF wins.    |
| Colour         | 0xf6866c   | Original AF salmon.                                |
| HV ↔ AF        | 1:1        | Via AF Transformer (phase 2).                      |

**Chunk-loading note:** like every IE wire, an AF line only transfers power while
*both* endpoints sit in loaded chunks. This is a Minecraft block-entity reality, not
a length cap — keep long runs chunk-loaded.

---

## Old → new API mapping (1.12 → 1.21.1)

- `WireType` lost its energy methods; energy moved to the `IEnergyWire` interface,
  driven by the `EnergyTransferHandler` that connectors request.
- `getWireCoil()` → `getWireCoil(Connection con)`.
- `ImmersiveNetHandler.Connection` → `api.wires.Connection` + `GlobalWireNetwork`.
- Loss formula unchanged: `lossRatio * length / maxLength`.
- The 1.12 transformer trick (`extends TileEntityTransformerHV`) → now extend
  `AbstractTransformerBlockEntity` / `TransformerHVBlockEntity`.

---

## Building

Requires JDK 21.

```bash
./gradlew build          # produces build/libs/alternatingflux-1.21.1-1.0.0.jar
./gradlew runClient      # launch a dev client with IE + AF loaded
```

Pin the IE version in `gradle.properties` (`curse_ie_file`) to match the IE build you
actually run. The current value targets IE 1.21.1-12.4.x.

## License

The original uses portions of IE's code/assets under "Blu's License of Common Sense",
and AF's own project license is listed as Custom. Fine for personal/server use; check
both and contact the original author before redistributing.
