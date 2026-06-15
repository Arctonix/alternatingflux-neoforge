package antibluequirk.alternatingflux.block;

import antibluequirk.alternatingflux.AlternatingFlux;
import blusunrize.immersiveengineering.common.blocks.metal.EnergyConnectorTileEntity;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
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
 * IE 1.16.5 (5.x) registration model — IE's block base classes (IEBaseBlock -&gt;
 * ConnectorBlock) SELF-REGISTER in their constructor: they call setRegistryName() and
 * add themselves to IEContent.registeredIEBlocks, and auto-create a BlockItem that they
 * add to IEContent.registeredIEItems. IE then registers everything in those lists during
 * its OWN RegistryEvents (the pre-DeferredRegister pattern). So we must NOT also register
 * these blocks/items through our own DeferredRegister — that double-sets the registry
 * name and crashes load_registries ("Attempted to set registry name with existing
 * registry name! New: alternatingflux:connector_af_relay Old: immersiveengineering:..."),
 * which is exactly what a naive port of the 1.18.2+ DeferredRegister approach does here.
 *
 * Therefore:
 *   - The blocks are plain static fields, constructed at class-init (which happens from
 *     {@link #register} during mod construction, before IE's registry events fire). Each
 *     block (see {@link AFRelayBlock}, {@link AFTransformerBlock}) overrides
 *     createRegistryName() to the alternatingflux: namespace — IEBaseBlock hardcodes
 *     immersiveengineering: — and supplies a custom item factory so the auto-created
 *     BlockItem is the right type and lands in AF's creative tab.
 *   - We keep our own DeferredRegister ONLY for the TileEntityTypes (built via Builder,
 *     not self-registering, so no conflict). Each TE type binds its block so the
 *     server-tickable EnergyConnector BE is not dropped at placement/chunk-load.
 *
 * Field ordering matters: each TileEntityType RegistryObject is declared BEFORE its block
 * (the block constructor reads the TE-type field), while the TE-type Builder references
 * the block through a lambda that only runs at registry-event time (after class-init has
 * eagerly constructed the block) — a qualified forward reference inside a lambda is legal.
 *
 * Relay: reuses IE EnergyConnectorTileEntity with voltage="AF" (public two-arg ctor
 * EnergyConnectorTileEntity(String voltage, boolean relay)). SPEC_TO_TYPE/NAME_TO_SPEC are
 * injected during common setup; the private LENGTH map (wire anchor height) via reflection.
 *
 * Transformer: AFTransformerBlock extends IE ConnectorBlock; AFTransformerBlockEntity
 * extends IE TransformerTileEntity (getHigherWiretype()="AF", acceptableLowerWires={HV}).
 */
public final class AFBlocks
{
    public static final DeferredRegister<TileEntityType<?>> TILE_ENTITIES =
            DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, AlternatingFlux.MODID);

    public static final String AF_VOLTAGE = "AF";
    // In IE 1.16.5 SPEC_TO_TYPE uses Pair (org.apache.commons.lang3).
    public static final Pair<String, Boolean> AF_RELAY_SPEC = Pair.of(AF_VOLTAGE, true);
    private static final float AF_RELAY_LENGTH = 0.875F; // match HV relay anchor/height

    // ---- AF Wire Relay ---------------------------------------------------

    public static final RegistryObject<TileEntityType<EnergyConnectorTileEntity>> CONNECTOR_AF_RELAY_BE =
            TILE_ENTITIES.register("connector_af_relay", () ->
                    TileEntityType.Builder.<EnergyConnectorTileEntity>of(
                            () -> new EnergyConnectorTileEntity(AF_VOLTAGE, true),
                            AFBlocks.CONNECTOR_AF_RELAY
                    ).build(null));

    /** Constructed eagerly; IE registers it (and its auto BlockItem) during its RegistryEvents. */
    public static final AFRelayBlock CONNECTOR_AF_RELAY =
            new AFRelayBlock("connector_af_relay", CONNECTOR_AF_RELAY_BE);

    // ---- AF Transformer --------------------------------------------------

    public static final RegistryObject<TileEntityType<AFTransformerBlockEntity>> TRANSFORMER_AF_BE =
            TILE_ENTITIES.register("connector_af_transformer", () ->
                    TileEntityType.Builder.<AFTransformerBlockEntity>of(
                            AFTransformerBlockEntity::new,
                            AFBlocks.TRANSFORMER_AF
                    ).build(null));

    /** Constructed eagerly; IE registers it (and its auto TransformerBlockItem). */
    public static final AFTransformerBlock TRANSFORMER_AF =
            new AFTransformerBlock("connector_af_transformer");

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
        // Touching this class has already constructed the blocks above (class-init), which
        // self-added them to IEContent's lists; IE registers them during its RegistryEvents.
        // We only register our own TileEntityTypes.
        TILE_ENTITIES.register(modBus);
    }

    private AFBlocks() {}
}
