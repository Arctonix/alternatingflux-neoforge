package antibluequirk.alternatingflux.block;

import antibluequirk.alternatingflux.AlternatingFlux;
import blusunrize.immersiveengineering.api.wires.WireType;
import blusunrize.immersiveengineering.common.blocks.BlockItemIE;
import blusunrize.immersiveengineering.common.blocks.generic.ConnectorBlock;
import blusunrize.immersiveengineering.common.blocks.metal.BasicConnectorBlock;
import blusunrize.immersiveengineering.common.blocks.metal.EnergyConnectorBlockEntity;
import blusunrize.immersiveengineering.common.blocks.metal.TransformerBlockItem;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.lang.reflect.Field;

/**
 * Registration for AF and UAF connector blocks: the AF/UAF Wire Relays, the AF
 * Transformer, and the two UAF Transformers (HV<->UAF and AF<->UAF).
 *
 * Relays: reuse IE EnergyConnectorBlockEntity with voltage="AF"/"UAF". IE 10.x
 * exposes a public constructor EnergyConnectorBlockEntity(String voltage, boolean
 * relay, BlockPos, BlockState) — identical to 12.x — and the same SPEC_TO_TYPE /
 * NAME_TO_SPEC public-static maps for injection. We register our own BE types and
 * blocks, then push AF/UAF into those maps during common setup. The private LENGTH
 * map (wire anchor height) is poked via reflection.
 *
 * Transformers: AFTransformerBlock / UAFTransformerBlock extend IE ConnectorBlock;
 * the block entities extend IE TransformerBlockEntity with a high wiretype and a
 * set of acceptable lower wires. IE's AbstractTransformerBlockEntity is properly
 * abstract in 10.x and is the friendly extension point. A single UAFTransformer
 * block/BE class backs both UAF transformers; the low-side voltage is supplied
 * per BE (HV for one, AF for the other).
 *
 * Block item for the relays MUST be IE's BlockItemIE so placement facing applies;
 * transformer items MUST be TransformerBlockItem so the multiblock places correctly
 * (AFTransformerBlockItem only reroutes the creative tab — see that class).
 */
public final class AFBlocks
{
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, AlternatingFlux.MODID);
    // ForgeRegistries key: net.minecraft.core.registries.Registries is 1.19.3+.
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, AlternatingFlux.MODID);

    public static final String AF_VOLTAGE = "AF";
    public static final Pair<String, Boolean> AF_RELAY_SPEC = Pair.of(AF_VOLTAGE, true);
    private static final float AF_RELAY_LENGTH = 0.875F; // match HV relay anchor/height

    public static final String UAF_VOLTAGE = "UAF";
    public static final Pair<String, Boolean> UAF_RELAY_SPEC = Pair.of(UAF_VOLTAGE, true);
    // The UAF relay model is exactly 0.5 taller than the AF relay and both wires
    // share the same renderDiameter, so AF's 0.875 + 0.5 puts the wire at the UAF
    // top cup the same way 0.875 does for the AF relay.
    private static final float UAF_RELAY_LENGTH = 1.375F;

    // ---- AF Wire Relay ---------------------------------------------------

    public static final RegistryObject<BlockEntityType<EnergyConnectorBlockEntity>> CONNECTOR_AF_RELAY_BE =
            BLOCK_ENTITIES.register("connector_af_relay", () ->
                    BlockEntityType.Builder.<EnergyConnectorBlockEntity>of(
                            (pos, state) -> new EnergyConnectorBlockEntity(AF_VOLTAGE, true, pos, state),
                            AFBlocks.CONNECTOR_AF_RELAY.get()
                    ).build(null));

    public static final RegistryObject<BasicConnectorBlock<EnergyConnectorBlockEntity>> CONNECTOR_AF_RELAY =
            BLOCKS.register("connector_af_relay", () ->
                    new BasicConnectorBlock<>(ConnectorBlock.PROPERTIES.get(), CONNECTOR_AF_RELAY_BE));

    public static final RegistryObject<BlockItemIE> CONNECTOR_AF_RELAY_ITEM =
            AlternatingFlux.ITEMS.register("connector_af_relay",
                    () -> new BlockItemIE(AFBlocks.CONNECTOR_AF_RELAY.get(),
                            new Item.Properties().tab(AlternatingFlux.TAB)));

    // ---- AF Transformer --------------------------------------------------

    public static final RegistryObject<AFTransformerBlock> TRANSFORMER_AF =
            BLOCKS.register("connector_af_transformer", () ->
                    new AFTransformerBlock(ConnectorBlock.PROPERTIES.get()));

    public static final RegistryObject<BlockEntityType<AFTransformerBlockEntity>> TRANSFORMER_AF_BE =
            BLOCK_ENTITIES.register("connector_af_transformer", () ->
                    BlockEntityType.Builder.of(
                            AFTransformerBlockEntity::new,
                            TRANSFORMER_AF.get()
                    ).build(null));

    public static final RegistryObject<TransformerBlockItem> TRANSFORMER_AF_ITEM =
            AlternatingFlux.ITEMS.register("connector_af_transformer",
                    () -> new AFTransformerBlockItem(TRANSFORMER_AF.get()));

    // ---- UAF Wire Relay --------------------------------------------------

    public static final RegistryObject<BlockEntityType<EnergyConnectorBlockEntity>> CONNECTOR_UAF_RELAY_BE =
            BLOCK_ENTITIES.register("connector_uaf_relay", () ->
                    BlockEntityType.Builder.<EnergyConnectorBlockEntity>of(
                            (pos, state) -> new EnergyConnectorBlockEntity(UAF_VOLTAGE, true, pos, state),
                            AFBlocks.CONNECTOR_UAF_RELAY.get()
                    ).build(null));

    public static final RegistryObject<BasicConnectorBlock<EnergyConnectorBlockEntity>> CONNECTOR_UAF_RELAY =
            BLOCKS.register("connector_uaf_relay", () ->
                    new BasicConnectorBlock<>(ConnectorBlock.PROPERTIES.get(), CONNECTOR_UAF_RELAY_BE));

    public static final RegistryObject<BlockItemIE> CONNECTOR_UAF_RELAY_ITEM =
            AlternatingFlux.ITEMS.register("connector_uaf_relay",
                    () -> new BlockItemIE(AFBlocks.CONNECTOR_UAF_RELAY.get(),
                            new Item.Properties().tab(AlternatingFlux.TAB)));

    // ---- UAF Transformers (HV<->UAF and AF<->UAF) ------------------------

    // HV <-> UAF. BE declared before the block (mirrors the relay above) so the
    // block's reference to the BE type is backward; the BE factory's self-reference
    // to its own type is routed through uafHvType() to avoid an illegal
    // self-reference in the field initializer, and the forward reference to the
    // block is qualified (AFBlocks.) like the relay's.
    public static final RegistryObject<BlockEntityType<UAFTransformerBlockEntity>> TRANSFORMER_UAF_HV_BE =
            BLOCK_ENTITIES.register("connector_uaf_transformer_hv", () ->
                    BlockEntityType.Builder.<UAFTransformerBlockEntity>of(
                            (pos, state) -> new UAFTransformerBlockEntity(
                                    uafHvType(), WireType.HV_CATEGORY, pos, state),
                            AFBlocks.TRANSFORMER_UAF_HV.get()
                    ).build(null));

    public static final RegistryObject<UAFTransformerBlock> TRANSFORMER_UAF_HV =
            BLOCKS.register("connector_uaf_transformer_hv", () ->
                    new UAFTransformerBlock(ConnectorBlock.PROPERTIES.get(), TRANSFORMER_UAF_HV_BE));

    public static final RegistryObject<TransformerBlockItem> TRANSFORMER_UAF_HV_ITEM =
            AlternatingFlux.ITEMS.register("connector_uaf_transformer_hv",
                    () -> new AFTransformerBlockItem(TRANSFORMER_UAF_HV.get()));

    // AF <-> UAF.
    public static final RegistryObject<BlockEntityType<UAFTransformerBlockEntity>> TRANSFORMER_UAF_AF_BE =
            BLOCK_ENTITIES.register("connector_uaf_transformer_af", () ->
                    BlockEntityType.Builder.<UAFTransformerBlockEntity>of(
                            (pos, state) -> new UAFTransformerBlockEntity(
                                    uafAfType(), AF_VOLTAGE, pos, state),
                            AFBlocks.TRANSFORMER_UAF_AF.get()
                    ).build(null));

    public static final RegistryObject<UAFTransformerBlock> TRANSFORMER_UAF_AF =
            BLOCKS.register("connector_uaf_transformer_af", () ->
                    new UAFTransformerBlock(ConnectorBlock.PROPERTIES.get(), TRANSFORMER_UAF_AF_BE));

    public static final RegistryObject<TransformerBlockItem> TRANSFORMER_UAF_AF_ITEM =
            AlternatingFlux.ITEMS.register("connector_uaf_transformer_af",
                    () -> new AFTransformerBlockItem(TRANSFORMER_UAF_AF.get()));

    private static BlockEntityType<UAFTransformerBlockEntity> uafHvType()
    {
        return TRANSFORMER_UAF_HV_BE.get();
    }

    private static BlockEntityType<UAFTransformerBlockEntity> uafAfType()
    {
        return TRANSFORMER_UAF_AF_BE.get();
    }

    // ---- IE map injection (relays) ---------------------------------------

    /** Inject AF/UAF relays into IE's connector maps. Call once, during common setup. */
    public static void injectIEMaps()
    {
        EnergyConnectorBlockEntity.SPEC_TO_TYPE.put(AF_RELAY_SPEC, CONNECTOR_AF_RELAY_BE);
        EnergyConnectorBlockEntity.NAME_TO_SPEC.put(
                AlternatingFlux.rl("connector_af_relay"), AF_RELAY_SPEC);
        EnergyConnectorBlockEntity.SPEC_TO_TYPE.put(UAF_RELAY_SPEC, CONNECTOR_UAF_RELAY_BE);
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
                    (Object2FloatMap<Pair<String, Boolean>>) f.get(null);
            length.put(AF_RELAY_SPEC, AF_RELAY_LENGTH);
            length.put(UAF_RELAY_SPEC, UAF_RELAY_LENGTH);
        }
        catch (ReflectiveOperationException e)
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
