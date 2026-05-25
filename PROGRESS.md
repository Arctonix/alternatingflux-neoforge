# Alternating Flux 1.21.1 port — PROGRESS

Keep this file in the repo root. Commit it alongside code after each chunk.
To resume in a new chat: say "continue the AF port" and point me at this file +
`git log`. This records the full state so a cold start is possible.

## Target / build
- MC 1.21.1, NeoForge 21.1.164, Java 21, ModDevGradle, Gradle 8.8.
- IE pulled via CurseMaven (pin `curse_ie_file` in gradle.properties to your IE build).
- Dev machine: CachyOS, fish shell. Build: `./gradlew build`; run: `./gradlew runClient`.
- Texture gotcha (already fixed): atlas only stitches `textures/item/` + `textures/block/`
  (singular). Legacy plural `items/` folder = invisible/checkerboard. Keep singular.

## Locked design decisions
- Transfer rate: 131072 IF/t = 4x modern HV (HV is 32768, not the old 4096).
- Max wire length: 64 blocks. Loss ratio: 0.0005. Colour: 0xf6866c (salmon).
- HV <-> AF conversion: 1:1, via the AF Transformer.
- AF category string = "AF". Wire is transmission-only; cannot touch machines.
- Loss model is IE's: lossRatio * length / maxLength (verified, matches EnergyWire).

## DONE — Foundation (proven in-game)
- Gradle project, mods.toml, pack.mcmeta.
- AFWireType implements IEnergyWire. Compiles clean against real IE + NeoForge.
- Config (ModConfigSpec): transferRate / maxLength / lossRatio / wireColour.
- Items: AF Wire Coil (reuses IE WireCoilItem) + Constantan Wire. Creative tab.
- Textures/models/lang ported from original repo. Items render correctly.
- OPTION A TEST PASSED: temporarily set getCategory()="HV"; AF wire connected to
  stock HV connectors and POWER FLOWED over a long run. This proves the energy
  layer (IEnergyWire, transfer rate, loss formula) is correct. Category since
  reverted to "AF".

## KEY IE INTERNALS (researched, drives Chunk 1)
File: common/blocks/metal/EnergyConnectorBlockEntity.java (IE 1.21.1)
- Line ~169: connection compatibility is a STRING MATCH:
  `voltage.equals(cableType.getCategory())`. So a connector whose voltage="AF"
  auto-accepts AF wire and rejects HV. Tier separation is free.
- Line ~75 `registerConnectorTEs`: HARD loop over {LV,MV,HV} only. No addon hook.
  => We must register our own ("AF", relay) entries into the public static maps
     `SPEC_TO_TYPE` and `NAME_TO_SPEC`, plus the `LENGTH` map, ourselves, in setup,
     before any AF connector is constructed.
- Line ~216 `getWireType()`: if/else LV/MV/HV, falls through to STEEL(HV) for
  unknown voltage. Only drives the CONNECTOR's internal rate (borrows HV's, fine).
- Constructor `EnergyConnectorBlockEntity(String voltage, boolean relay, pos, state)`
  takes voltage as a plain string and looks itself up in SPEC_TO_TYPE.
- BasicConnectorBlock.forPower(voltage, relay) builds the block; createBlockState
  adds FACING_ALL + WATERLOGGED.
- AF has NO machine connector by design — only a relay (wire-to-wire).

## TODO
- [ ] Chunk 1: AF Relay
      - Register ("AF",true) [and maybe ("AF",false)] into SPEC_TO_TYPE/NAME_TO_SPEC/LENGTH.
      - AF relay block (BasicConnectorBlock) + BlockItem + blockstate + model
        (reuse relay_af.obj + connector_relay_af.png, ported under assets).
      - Add to creative tab. Build. TEST: place 2 AF relays, string AF wire, confirm
        connect + HV rejected. Commit.
- [ ] Chunk 2: AF Transformer (HV<->AF, 1:1). Hardest part. extend/mirror
      AbstractTransformerBlockEntity / TransformerHVBlockEntity; higher wire = "AF",
      acceptable lower = HV. block+item+blockstate+model (connector_transformer_af.png).
      TEST: power steps HV->AF->HV. Commit.
- [ ] Chunk 3: Feedthrough (registerFeedthroughForWiretype, passthrough_af),
      recipes, IE manual entry, cleanup. Commit.

## Resume protocol
1. `cd ~/Documents/"AF mod"/afmod && git log --oneline` to see last good commit.
2. Read this file's TODO for the next chunk.
3. `./gradlew build` to confirm last state still compiles before adding to it.
