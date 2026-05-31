package antibluequirk.alternatingflux;

import antibluequirk.alternatingflux.block.AFBlocks;
import antibluequirk.alternatingflux.wire.AFWireType;
import blusunrize.immersiveengineering.api.wires.WireApi;
import blusunrize.immersiveengineering.common.items.WireCoilItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Alternating Flux — a long-distance, low-loss super-high-voltage wire tier for
 * Immersive Engineering. Port of AntiBlueQuirk's 1.12 addon to 1.21.1 / NeoForge.
 *
 * Provides the AF wire, the AF Wire Relay, and the AF Transformer (HV&lt;-&gt;AF, 1:1).
 * This class wires up the shared registration: items, the creative tab, the config,
 * and the deferred IE-map injection (see {@link AFBlocks#injectIEMaps()}).
 */
@Mod(AlternatingFlux.MODID)
public class AlternatingFlux
{
    public static final String MODID = "alternatingflux";

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<Item, WireCoilItem> AF_WIRE_COIL =
            ITEMS.register("wirecoil_af", () -> new WireCoilItem(AFWireType.AF));

    public static final DeferredHolder<Item, Item> WIRE_CONSTANTAN =
            ITEMS.register("wire_constantan", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB =
            TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + MODID))
                    .icon(() -> new ItemStack(AF_WIRE_COIL.get()))
                    .displayItems((params, output) -> {
                        output.accept(AF_WIRE_COIL.get());
                        output.accept(WIRE_CONSTANTAN.get());
                        output.accept(AFBlocks.CONNECTOR_AF_RELAY_ITEM.get());
                        output.accept(AFBlocks.TRANSFORMER_AF_ITEM.get());
                    })
                    .build());

    public AlternatingFlux(IEventBus modBus, ModContainer container)
    {
        AFWireType.init();

        ITEMS.register(modBus);
        TABS.register(modBus);
        AFBlocks.register(modBus);

        modBus.addListener(this::commonSetup);

        container.registerConfig(ModConfig.Type.SERVER, Config.SERVER_SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event)
    {
        event.enqueueWork(() -> {
            AFBlocks.injectIEMaps();
            registerFeedthrough();
        });
    }

    /**
     * Register the AF wire for IE feedthroughs, so an AF line can pass through a
     * wall via a feedthrough block (parity with the 1.12 original, which also
     * registered one). Mirrors IE's HV registration: 0.75 connector offset /
     * HV-relay geometry. Uses our dedicated passthrough sprite mapped whole onto
     * the connector face (UV 0..16), as the original did. Must run after block
     * registration (reads the relay's default state); commonSetup is safe.
     */
    private static void registerFeedthrough()
    {
        WireApi.registerFeedthroughForWiretype(
                AFWireType.AF,
                rl("block/passthrough_af"),
                new double[]{0.0, 0.0, 16.0, 16.0},
                0.75,
                AFBlocks.CONNECTOR_AF_RELAY.get().defaultBlockState());
    }

    public static ResourceLocation rl(String path)
    {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
