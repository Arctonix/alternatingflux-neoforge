package antibluequirk.alternatingflux.block;

import antibluequirk.alternatingflux.AlternatingFlux;
import blusunrize.immersiveengineering.common.blocks.BlockItemIE;
import blusunrize.immersiveengineering.common.blocks.generic.ConnectorBlock;
import blusunrize.immersiveengineering.common.blocks.metal.BasicConnectorBlock;
import blusunrize.immersiveengineering.common.blocks.metal.EnergyConnectorBlockEntity;
import blusunrize.immersiveengineering.common.blocks.metal.TransformerBlockItem;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.lang.reflect.Field;

/**
 * Registration for AF connector blocks: the AF Wire Relay and the AF Transformer.
 *
 * Relay: reuses IE EnergyConnectorBlockEntity with voltage="AF". IE 10.x exposes
 * a public constructor EnergyConnectorBlockEntity(String voltage, boolean relay,
 * BlockPos, BlockState) — identical to 12.x — and the same SPEC_TO_TYPE /
 * NAME_TO_SPEC public-static maps for injection. We register our own BE type
 * and block, then push AF into those maps during common setup. The private
 * LENGTH map (wire anchor height) is poked via reflection.
 *
 * Transformer: AFTransformerBlock extends IE ConnectorBlock; AFTransformerBlockEntity
 * extends IE TransformerBlockEntity with getHigherWiretype()="AF" and
 * acceptableLowerWires={HV}. IE's AbstractTransformerBlockEntity is properly
 * abstract in 10.x and is the friendly extension point.
 *
 * Block item for the relay MUST be IE's BlockItemIE so placement facing applies;
 * transformer item MUST be TransformerBlockItem so the multiblock places correctly.
 */
public final class AFBlocks
{
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, AlternatingFlux.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AlternatingFlux.MODID);

    public static final String AF_VOLTAGE = "AF";
    public static final Pair<String, Boolean> AF_RELAY_SPEC = Pair.of(AF_VOLTAGE, true);
    private static final float AF_RELAY_LENGTH = 0.875F; // match HV relay anchor/height

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
                    () -> new BlockItemIE(AFBlocks.CONNECTOR_AF_RELAY.get(), new Item.Properties()));

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
                    () -> new TransformerBlockItem(TRANSFORMER_AF.get()));

    // ---- IE map injection (relay) ----------------------------------------

    /** Inject AF relay into IE's connector maps. Call once, during common setup. */
    public static void injectIEMaps()
    {
        EnergyConnectorBlockEntity.SPEC_TO_TYPE.put(AF_RELAY_SPEC, CONNECTOR_AF_RELAY_BE);
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
                    (Object2FloatMap<Pair<String, Boolean>>) f.get(null);
            length.put(AF_RELAY_SPEC, AF_RELAY_LENGTH);
        }
        catch (ReflectiveOperationException e)
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
