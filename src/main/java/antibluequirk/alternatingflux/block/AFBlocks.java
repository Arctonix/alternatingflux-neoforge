package antibluequirk.alternatingflux.block;

import antibluequirk.alternatingflux.AlternatingFlux;
import blusunrize.immersiveengineering.common.blocks.BlockItemIE;
import blusunrize.immersiveengineering.common.blocks.generic.ConnectorBlock;
import blusunrize.immersiveengineering.common.blocks.metal.BasicConnectorBlock;
import blusunrize.immersiveengineering.common.blocks.metal.EnergyConnectorBlockEntity;
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
 * Registration for AF connector blocks.
 *
 * Reuses IE {@link EnergyConnectorBlockEntity} with voltage="AF"; our own block +
 * BE-type, injected into IE's public-static SPEC_TO_TYPE / NAME_TO_SPEC maps in
 * {@link #injectIEMaps()} (called from common setup). Compatibility is a string
 * match on category, so an "AF" relay auto-accepts AF wire and rejects HV. AF is
 * transmission-only, so only the RELAY variant exists.
 *
 * Block item MUST be {@link BlockItemIE} (not vanilla BlockItem) so IE's
 * onIEBlockPlacedBy fires and placement facing is applied.
 *
 * The relay's wire-attachment point AND render/collision box come from IE's
 * private static LENGTH map, keyed by (voltage, relay). Without an entry it
 * defaults to 0.5 — which anchors the wire at the connector's MIDDLE and makes
 * the model sit low (HV relay uses 0.875). We reflectively inject ("AF", true)=0.875
 * to match the HV relay's look. The field is private static final, but final only
 * fixes the reference; the map contents are still mutable, so a reflective put is
 * safe and needs no setAccessible-on-final hacks.
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

    // Match HV relay's anchor/length so the wire attaches at the top and the model
    // sits at the correct height.
    private static final float AF_RELAY_LENGTH = 0.875F;

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

    /** Inject AF into IE's connector maps. Call once, during common setup. */
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
            // Non-fatal: without this the relay still works, it just renders with the
            // wire anchored at the middle and sits a bit low. Log and continue.
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
