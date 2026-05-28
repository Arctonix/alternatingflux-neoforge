package antibluequirk.alternatingflux.block;

import antibluequirk.alternatingflux.AlternatingFlux;
import blusunrize.immersiveengineering.common.blocks.BlockItemIE;
import blusunrize.immersiveengineering.common.blocks.generic.ConnectorBlock;
import blusunrize.immersiveengineering.common.blocks.metal.BasicConnectorBlock;
import blusunrize.immersiveengineering.common.blocks.metal.EnergyConnectorBlockEntity;
import blusunrize.immersiveengineering.common.blocks.metal.TransformerBlockItem;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.lang.reflect.Field;
import java.util.Set;

/**
 * Registration for AF connector blocks: the AF Wire Relay (Chunk 1) and the
 * AF Transformer (Chunk 2).
 *
 * Relay: reuses IE EnergyConnectorBlockEntity with voltage="AF"; our own block +
 * BE-type, injected into IE's public-static SPEC_TO_TYPE / NAME_TO_SPEC maps, plus
 * a reflective inject into the private LENGTH map (wire anchor / render height).
 * Block item MUST be BlockItemIE so placement facing is applied.
 *
 * Transformer: our own AFTransformerBlock (extends IE ConnectorBlock) + 
 * AFTransformerBlockEntity (extends IE TransformerBlockEntity, high=AF / low=HV).
 * 3-tall multiblock; item is IE's TransformerBlockItem (multiblock-aware).
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
    private static final float AF_RELAY_LENGTH = 0.875F; // match HV relay anchor/height

    public static final String UAF_VOLTAGE = "UAF";
    public static final Pair<String, Boolean> UAF_RELAY_SPEC = Pair.of(UAF_VOLTAGE, true);
    // Wire-attach height tracks the model top: IE's getConnectionOffset gives
    // attachY = LENGTH - renderDiameter/2. The UAF relay model is exactly 0.5 taller
    // than the AF relay (maxY 1.36875 vs 0.86875) and both wires share the same
    // renderDiameter, so AF's 0.875 + 0.5 puts the wire at the UAF top cup the same
    // way 0.875 does for the AF relay.
    private static final float UAF_RELAY_LENGTH = 1.375F; // AF's 0.875 + 0.5 (model is 0.5 taller)

    // ---- AF Wire Relay (Chunk 1) -----------------------------------------

    public static final DeferredBlock<BasicConnectorBlock<EnergyConnectorBlockEntity>> CONNECTOR_AF_RELAY =
            BLOCKS.register("connector_af_relay", () ->
                    new BasicConnectorBlock<>(ConnectorBlock.PROPERTIES.get(), AFBlocks::relayType));

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

    // ---- AF Transformer (Chunk 2) ----------------------------------------

    public static final DeferredBlock<AFTransformerBlock> TRANSFORMER_AF =
            BLOCKS.register("connector_af_transformer", () ->
                    new AFTransformerBlock(ConnectorBlock.PROPERTIES.get()));

    public static final DeferredHolder<Item, TransformerBlockItem> TRANSFORMER_AF_ITEM =
            AlternatingFlux.ITEMS.register("connector_af_transformer",
                    () -> new TransformerBlockItem(TRANSFORMER_AF.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AFTransformerBlockEntity>> TRANSFORMER_AF_BE =
            BLOCK_ENTITIES.register("connector_af_transformer", () ->
                    new BlockEntityType<>(
                            AFTransformerBlockEntity::new,
                            Set.of(TRANSFORMER_AF.get()),
                            null));

    // ---- UAF Wire Relay --------------------------------------------------

    public static final DeferredBlock<BasicConnectorBlock<EnergyConnectorBlockEntity>> CONNECTOR_UAF_RELAY =
            BLOCKS.register("connector_uaf_relay", () ->
                    new BasicConnectorBlock<>(ConnectorBlock.PROPERTIES.get(), AFBlocks::uafRelayType));

    public static final DeferredHolder<Item, BlockItemIE> CONNECTOR_UAF_RELAY_ITEM =
            AlternatingFlux.ITEMS.register("connector_uaf_relay",
                    () -> new BlockItemIE(CONNECTOR_UAF_RELAY.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyConnectorBlockEntity>> CONNECTOR_UAF_RELAY_BE =
            BLOCK_ENTITIES.register("connector_uaf_relay", () ->
                    new BlockEntityType<>(
                            (pos, state) -> new EnergyConnectorBlockEntity(UAF_VOLTAGE, true, pos, state),
                            Set.of(CONNECTOR_UAF_RELAY.get()),
                            null));

    private static BlockEntityType<EnergyConnectorBlockEntity> uafRelayType()
    {
        return CONNECTOR_UAF_RELAY_BE.get();
    }

    // ---- UAF Transformers (HV<->UAF and AF<->UAF) ------------------------

    // HV <-> UAF
    public static final DeferredBlock<UAFTransformerBlock> TRANSFORMER_UAF_HV =
            BLOCKS.register("connector_uaf_transformer_hv", () ->
                    new UAFTransformerBlock(ConnectorBlock.PROPERTIES.get(), AFBlocks::uafHvType));

    public static final DeferredHolder<Item, TransformerBlockItem> TRANSFORMER_UAF_HV_ITEM =
            AlternatingFlux.ITEMS.register("connector_uaf_transformer_hv",
                    () -> new TransformerBlockItem(TRANSFORMER_UAF_HV.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<UAFTransformerBlockEntity>> TRANSFORMER_UAF_HV_BE =
            BLOCK_ENTITIES.register("connector_uaf_transformer_hv", () ->
                    new BlockEntityType<>(
                            (pos, state) -> new UAFTransformerBlockEntity(
                                    uafHvType(),
                                    blusunrize.immersiveengineering.api.wires.WireType.HV_CATEGORY,
                                    pos, state),
                            Set.of(TRANSFORMER_UAF_HV.get()),
                            null));

    private static BlockEntityType<UAFTransformerBlockEntity> uafHvType()
    {
        return TRANSFORMER_UAF_HV_BE.get();
    }

    // AF <-> UAF
    public static final DeferredBlock<UAFTransformerBlock> TRANSFORMER_UAF_AF =
            BLOCKS.register("connector_uaf_transformer_af", () ->
                    new UAFTransformerBlock(ConnectorBlock.PROPERTIES.get(), AFBlocks::uafAfType));

    public static final DeferredHolder<Item, TransformerBlockItem> TRANSFORMER_UAF_AF_ITEM =
            AlternatingFlux.ITEMS.register("connector_uaf_transformer_af",
                    () -> new TransformerBlockItem(TRANSFORMER_UAF_AF.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<UAFTransformerBlockEntity>> TRANSFORMER_UAF_AF_BE =
            BLOCK_ENTITIES.register("connector_uaf_transformer_af", () ->
                    new BlockEntityType<>(
                            (pos, state) -> new UAFTransformerBlockEntity(
                                    uafAfType(),
                                    AF_VOLTAGE,
                                    pos, state),
                            Set.of(TRANSFORMER_UAF_AF.get()),
                            null));

    private static BlockEntityType<UAFTransformerBlockEntity> uafAfType()
    {
        return TRANSFORMER_UAF_AF_BE.get();
    }

    // ---- IE map injection (relay) ----------------------------------------

    /** Inject AF relay into IE's connector maps. Call once, during common setup. */
    public static void injectIEMaps()
    {
        EnergyConnectorBlockEntity.SPEC_TO_TYPE.put(AF_RELAY_SPEC, CONNECTOR_AF_RELAY_BE::get);
        EnergyConnectorBlockEntity.NAME_TO_SPEC.put(
                AlternatingFlux.rl("connector_af_relay"), AF_RELAY_SPEC);
        EnergyConnectorBlockEntity.SPEC_TO_TYPE.put(UAF_RELAY_SPEC, CONNECTOR_UAF_RELAY_BE::get);
        EnergyConnectorBlockEntity.NAME_TO_SPEC.put(
                AlternatingFlux.rl("connector_uaf_relay"), UAF_RELAY_SPEC);
        injectLength();
    }

    @SuppressWarnings("unchecked")
    private static void injectLength()
    {
        try
        {
            Field f = EnergyConnectorBlockEntity.class.getDeclaredField("LENGTH");
            f.setAccessible(true);
            Object2FloatMap<Pair<String, Boolean>> length =
                    (Object2FloatMap<Pair<String, Boolean>>)f.get(null);
            length.put(AF_RELAY_SPEC, AF_RELAY_LENGTH);
            length.put(UAF_RELAY_SPEC, UAF_RELAY_LENGTH);
        }
        catch(ReflectiveOperationException e)
        {
            org.slf4j.LoggerFactory.getLogger(AlternatingFlux.MODID).warn(
                    "Could not inject AF/UAF connector LENGTH; relay wire anchor will use the 0.5 default.", e);
        }
    }

    public static void register(IEventBus modBus)
    {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }

    private AFBlocks() {}
}
