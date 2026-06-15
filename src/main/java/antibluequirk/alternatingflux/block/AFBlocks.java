package antibluequirk.alternatingflux.block;

import antibluequirk.alternatingflux.AlternatingFlux;
import blusunrize.immersiveengineering.common.blocks.BlockItemIE;
import blusunrize.immersiveengineering.common.blocks.metal.BasicConnectorBlock;
import blusunrize.immersiveengineering.common.blocks.metal.EnergyConnectorTileEntity;
import blusunrize.immersiveengineering.common.blocks.metal.TransformerBlockItem;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.Field;

/**
 * Registration for AF connector blocks: the AF Wire Relay and the AF Transformer.
 *
 * Relay: reuses IE EnergyConnectorTileEntity with voltage="AF". IE 1.16.5 (5.x)
 * exposes a public two-arg constructor EnergyConnectorTileEntity(String voltage,
 * boolean relay) — no BlockPos/BlockState (those come from the type chain).
 * The SPEC_TO_TYPE and NAME_TO_SPEC public-static maps are injected during common
 * setup. The private LENGTH map (wire anchor height) is poked via reflection.
 *
 * Transformer: AFTransformerBlock extends IE ConnectorBlock; AFTransformerBlockEntity
 * extends IE TransformerTileEntity with getHigherWiretype()="AF" and
 * acceptableLowerWires={HV}.
 *
 * Block item for the relay MUST be IE's BlockItemIE so placement facing applies;
 * transformer item MUST be TransformerBlockItem so the multiblock places correctly.
 *
 * Field ordering: CONNECTOR_AF_RELAY_BE is declared before CONNECTOR_AF_RELAY, yet
 * its lazy supplier references AFBlocks.CONNECTOR_AF_RELAY.get(). A qualified static
 * reference inside a lambda is not an illegal forward reference, and the lambda only
 * runs at registration time (after CONNECTOR_AF_RELAY is registered), so the block is
 * bound into the TileEntityType. This MUST be set: the relay's BE is a server-tickable
 * EnergyConnector, and a no-block TileEntityType would be dropped on placement/chunk
 * load (no power, no persistence). Matches the 1.18.2/1.19.2/1.20.1 siblings.
 */
public final class AFBlocks
{
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, AlternatingFlux.MODID);
    public static final DeferredRegister<TileEntityType<?>> TILE_ENTITIES =
            DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, AlternatingFlux.MODID);

    public static final String AF_VOLTAGE = "AF";
    // In IE 1.16.5 SPEC_TO_TYPE uses Pair (org.apache.commons.lang3).
    public static final Pair<String, Boolean> AF_RELAY_SPEC = Pair.of(AF_VOLTAGE, true);
    private static final float AF_RELAY_LENGTH = 0.875F; // match HV relay anchor/height

    // ---- AF Wire Relay ---------------------------------------------------
    // Declaration order: CONNECTOR_AF_RELAY_BE first, then CONNECTOR_AF_RELAY.
    // The BE's lazy supplier binds the block via AFBlocks.CONNECTOR_AF_RELAY.get()
    // (qualified static ref inside a lambda that runs at registration time — legal,
    // not an illegal forward reference). The block MUST be bound or the TileEntityType
    // is dropped on placement and the relay never ticks / joins the wire network.

    public static final RegistryObject<TileEntityType<EnergyConnectorTileEntity>> CONNECTOR_AF_RELAY_BE =
            TILE_ENTITIES.register("connector_af_relay", () ->
                    TileEntityType.Builder.<EnergyConnectorTileEntity>of(
                            () -> new EnergyConnectorTileEntity(AF_VOLTAGE, true),
                            AFBlocks.CONNECTOR_AF_RELAY.get()
                    ).build(null));

    public static final RegistryObject<BasicConnectorBlock<EnergyConnectorTileEntity>> CONNECTOR_AF_RELAY =
            BLOCKS.register("connector_af_relay", () ->
                    new BasicConnectorBlock<>("connector_af_relay", CONNECTOR_AF_RELAY_BE));

    public static final RegistryObject<BlockItemIE> CONNECTOR_AF_RELAY_ITEM =
            AlternatingFlux.ITEMS.register("connector_af_relay",
                    () -> new BlockItemIE(AFBlocks.CONNECTOR_AF_RELAY.get(),
                            new Item.Properties().tab(AlternatingFlux.TAB)));

    // ---- AF Transformer --------------------------------------------------

    public static final RegistryObject<AFTransformerBlock> TRANSFORMER_AF =
            BLOCKS.register("connector_af_transformer", () ->
                    new AFTransformerBlock("connector_af_transformer"));

    public static final RegistryObject<TileEntityType<AFTransformerBlockEntity>> TRANSFORMER_AF_BE =
            TILE_ENTITIES.register("connector_af_transformer", () ->
                    TileEntityType.Builder.<AFTransformerBlockEntity>of(
                            AFTransformerBlockEntity::new,
                            AFBlocks.TRANSFORMER_AF.get()
                    ).build(null));

    public static final RegistryObject<TransformerBlockItem> TRANSFORMER_AF_ITEM =
            AlternatingFlux.ITEMS.register("connector_af_transformer",
                    () -> new TransformerBlockItem(TRANSFORMER_AF.get(),
                            new Item.Properties().tab(AlternatingFlux.TAB)));

    // ---- IE map injection (relay) ----------------------------------------

    /** Inject AF relay into IE's connector maps. Call once, during common setup. */
    public static void injectIEMaps()
    {
        EnergyConnectorTileEntity.SPEC_TO_TYPE.put(AF_RELAY_SPEC, CONNECTOR_AF_RELAY_BE);
        EnergyConnectorTileEntity.NAME_TO_SPEC.put(
                AlternatingFlux.rl("connector_af_relay"), AF_RELAY_SPEC);
        injectLength();
    }

    @SuppressWarnings("unchecked")
    private static void injectLength()
    {
        try
        {
            Field f = EnergyConnectorTileEntity.class.getDeclaredField("LENGTH");
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
        TILE_ENTITIES.register(modBus);
    }

    private AFBlocks() {}
}
