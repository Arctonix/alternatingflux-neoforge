package antibluequirk.alternatingflux;

import antibluequirk.alternatingflux.block.AFBlocks;
import antibluequirk.alternatingflux.wire.AFWireType;
import antibluequirk.alternatingflux.wire.UAFWireType;
import blusunrize.immersiveengineering.api.wires.WireApi;
import blusunrize.immersiveengineering.common.items.WireCoilItem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Alternating Flux — long-distance super-high-voltage wire tiers for Immersive
 * Engineering. Port of AntiBlueQuirk's 1.12 addon to 1.20.1 / Forge (also loads
 * on NeoForge 1.20.1, which keeps the Forge API surface).
 *
 * Two tiers, each its own wire/relay/transformer network:
 *   - AF  : the base long-distance tier; HV<->AF step-down via the AF Transformer.
 *   - UAF : the higher "Ultra High AF" tier, bridged to lower tiers by the
 *           HV<->UAF and AF<->UAF transformers.
 *
 * Backport of the published 1.21.1 / NeoForge port at
 * https://github.com/Arctonix/alternatingflux-neoforge. Differences are purely
 * loader/platform (Forge DeferredRegister style, RegistryObject, ForgeConfigSpec,
 * IE 10.x API).
 */
@Mod(AlternatingFlux.MODID)
public class AlternatingFlux
{
    public static final String MODID = "alternatingflux";

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<WireCoilItem> AF_WIRE_COIL =
            ITEMS.register("wirecoil_af", () -> new WireCoilItem(AFWireType.AF));

    public static final RegistryObject<WireCoilItem> UAF_WIRE_COIL =
            ITEMS.register("wirecoil_uaf", () -> new WireCoilItem(UAFWireType.UAF));

    public static final RegistryObject<Item> WIRE_CONSTANTAN =
            ITEMS.register("wire_constantan", () -> new Item(new Item.Properties()));

    public static final RegistryObject<CreativeModeTab> TAB =
            TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + MODID))
                    .icon(() -> new ItemStack(AF_WIRE_COIL.get()))
                    .displayItems((params, output) -> {
                        output.accept(AF_WIRE_COIL.get());
                        output.accept(WIRE_CONSTANTAN.get());
                        output.accept(AFBlocks.CONNECTOR_AF_RELAY_ITEM.get());
                        output.accept(AFBlocks.TRANSFORMER_AF_ITEM.get());
                        output.accept(UAF_WIRE_COIL.get());
                        output.accept(AFBlocks.CONNECTOR_UAF_RELAY_ITEM.get());
                        output.accept(AFBlocks.TRANSFORMER_UAF_HV_ITEM.get());
                        output.accept(AFBlocks.TRANSFORMER_UAF_AF_ITEM.get());
                    })
                    .build());

    public AlternatingFlux()
    {
        // The wire types register themselves with WireApi via their constructors.
        AFWireType.init();
        UAFWireType.init();

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ITEMS.register(modBus);
        TABS.register(modBus);
        AFBlocks.register(modBus);

        modBus.addListener(this::commonSetup);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SERVER_SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event)
    {
        event.enqueueWork(() -> {
            AFBlocks.injectIEMaps();
            registerFeedthrough();
        });
    }

    /**
     * Register the AF and UAF wires for IE feedthroughs, so a line can pass through
     * a wall via a feedthrough block (parity with the 1.12 original, which registered
     * one for AF). Mirrors IE's HV registration: 0.75 connector offset / HV-relay
     * geometry, with each tier's dedicated passthrough sprite mapped whole onto the
     * connector face (UV 0..16). Must run after block registration (reads the relay's
     * default state); commonSetup is safe.
     */
    private static void registerFeedthrough()
    {
        WireApi.registerFeedthroughForWiretype(
                AFWireType.AF,
                rl("block/passthrough_af"),
                new double[]{0.0, 0.0, 16.0, 16.0},
                0.75,
                AFBlocks.CONNECTOR_AF_RELAY.get().defaultBlockState());

        WireApi.registerFeedthroughForWiretype(
                UAFWireType.UAF,
                rl("block/passthrough_uaf"),
                new double[]{0.0, 0.0, 16.0, 16.0},
                0.75,
                AFBlocks.CONNECTOR_UAF_RELAY.get().defaultBlockState());
    }

    public static ResourceLocation rl(String path)
    {
        return new ResourceLocation(MODID, path);
    }
}
