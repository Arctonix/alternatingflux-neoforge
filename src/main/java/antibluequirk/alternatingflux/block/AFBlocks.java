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

    // ---- IE map injection (relay) ----------------------------------------

    /** Inject AF relay into IE's connector maps. Call once, during common setup. */
    public static void injectIEMaps()
    {
        EnergyConnectorBlockEntity.SPEC_TO_TYPE.put(AF_RELAY_SPEC, CONNECTOR_AF_RELAY_BE::get);
        EnergyConnectorBlockEntity.NAME_TO_SPEC.put(
                AlternatingFlux.rl("connector_af_relay"), AF_RELAY_SPEC);
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
        }
        catch(ReflectiveOperationException e)
        {
            org.slf4j.LoggerFactory.getLogger(AlternatingFlux.MODID).warn(
                    "Could not inject AF connector LENGTH; relay wire anchor will use the 0.5 default.", e);
        }
    }

    public static void register(IEventBus modBus)
    {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }

    private AFBlocks() {}
}
