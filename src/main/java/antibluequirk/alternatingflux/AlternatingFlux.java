package antibluequirk.alternatingflux;

import antibluequirk.alternatingflux.wire.AFWireType;
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
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Alternating Flux — long-distance super-high-voltage wire tier for Immersive
 * Engineering. Port of AntiBlueQuirk's 1.12 addon to 1.21.1 / NeoForge.
 *
 * FOUNDATION milestone: wire type, config, items, creative tab. The AF relay
 * and AF transformer blocks (the heavy lift against IE's connector internals)
 * are the next phase — see README "Remaining work".
 */
@Mod(AlternatingFlux.MODID)
public class AlternatingFlux
{
    public static final String MODID = "alternatingflux";

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Reuse IE's own wire-coil item for AF — this gives us correct wire placement,
    // length checking and consumption for free. AFWireType.init() must run before
    // this supplier is invoked (we call it in the constructor, below).
    public static final DeferredHolder<Item, WireCoilItem> AF_WIRE_COIL =
            ITEMS.register("wirecoil_af", () -> new WireCoilItem(AFWireType.AF));

    // Crafting material — Constantan wire (cut from a Constantan plate).
    public static final DeferredHolder<Item, Item> WIRE_CONSTANTAN =
            ITEMS.register("wire_constantan", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB =
            TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + MODID))
                    .icon(() -> new ItemStack(AF_WIRE_COIL.get()))
                    .displayItems((params, output) -> {
                        output.accept(AF_WIRE_COIL.get());
                        output.accept(WIRE_CONSTANTAN.get());
                        // TODO phase 2: add AF relay + AF transformer block items here.
                    })
                    .build());

    public AlternatingFlux(IEventBus modBus, ModContainer container)
    {
        // Register the AF wire type during construction, before item registration
        // resolves AF_WIRE_COIL (which references AFWireType.AF).
        AFWireType.init();

        ITEMS.register(modBus);
        TABS.register(modBus);

        container.registerConfig(ModConfig.Type.SERVER, Config.SERVER_SPEC);

        // TODO phase 2: register AF relay + transformer blocks, block entities,
        // capabilities, and the AF feedthrough via WireApi.registerFeedthroughForWiretype.
    }

    public static ResourceLocation rl(String path)
    {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
