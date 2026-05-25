package antibluequirk.alternatingflux.block;

import antibluequirk.alternatingflux.AlternatingFlux;
import blusunrize.immersiveengineering.common.blocks.BlockItemIE;
import blusunrize.immersiveengineering.common.blocks.generic.ConnectorBlock;
import blusunrize.immersiveengineering.common.blocks.metal.BasicConnectorBlock;
import blusunrize.immersiveengineering.common.blocks.metal.EnergyConnectorBlockEntity;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;

/**
 * Registration for AF connector blocks.
 *
 * Strategy (see PROGRESS.md "KEY IE INTERNALS"): reuse {@link EnergyConnectorBlockEntity}
 * verbatim, but register our OWN block + block-entity-type that construct it with
 * voltage="AF". Inject the ("AF", true) entry into IE's public-static SPEC_TO_TYPE /
 * NAME_TO_SPEC maps in {@link #injectIEMaps()} (called from common setup).
 *
 * Compatibility is decided by string-matching the connector's voltage against the
 * wire's category, so an "AF"-voltage relay auto-accepts AF wire and rejects HV.
 * AF is transmission-only, so only the RELAY variant exists (relay=true).
 *
 * IMPORTANT: the block ITEM must be {@link BlockItemIE}, not a vanilla BlockItem.
 * IE applies placement facing in Block#onIEBlockPlacedBy, which is only invoked by
 * BlockItemIE (see BlockItemIE line ~105). A plain BlockItem skips it, leaving the
 * connector stuck at default facing (NORTH) — which both locks placement to one
 * direction AND makes it pop off when neighbours change (it thinks its mount, the
 * block to its north, has vanished).
 */
public final class AFBlocks
{
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(AlternatingFlux.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE,
                    AlternatingFlux.MODID);

    public static final String AF_VOLTAGE = "AF";
    public static final Pair<String, Boolean> AF_RELAY_SPEC = Pair.of(AF_VOLTAGE, true);

    public static final DeferredBlock<BasicConnectorBlock<EnergyConnectorBlockEntity>> CONNECTOR_AF_RELAY =
            BLOCKS.register("connector_af_relay", () ->
                    new BasicConnectorBlock<>(ConnectorBlock.PROPERTIES.get(), AFBlocks::relayType));

    // Block item MUST be BlockItemIE so onIEBlockPlacedBy fires and facing is set.
    public static final DeferredHolder<Item, BlockItemIE> CONNECTOR_AF_RELAY_ITEM =
            AlternatingFlux.ITEMS.register("connector_af_relay",
                    () -> new BlockItemIE(CONNECTOR_AF_RELAY.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyConnectorBlockEntity>> CONNECTOR_AF_RELAY_BE =
            BLOCK_ENTITIES.register("connector_af_relay", () ->
                    new BlockEntityType<>(
                            (pos, state) -> new EnergyConnectorBlockEntity(AF_VOLTAGE, true, pos, state),
                            Set.of(CONNECTOR_AF_RELAY.get()),
                            null));

    private static BlockEntityType<EnergyConnectorBlockEntity> relayType()
    {
        return CONNECTOR_AF_RELAY_BE.get();
    }

    /** Inject AF into IE's connector maps. Call once, during common setup. */
    public static void injectIEMaps()
    {
        EnergyConnectorBlockEntity.SPEC_TO_TYPE.put(AF_RELAY_SPEC, CONNECTOR_AF_RELAY_BE::get);
        EnergyConnectorBlockEntity.NAME_TO_SPEC.put(
                AlternatingFlux.rl("connector_af_relay"), AF_RELAY_SPEC);
    }

    public static void register(IEventBus modBus)
    {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }

    private AFBlocks() {}
}
