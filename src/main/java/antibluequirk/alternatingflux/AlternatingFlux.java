package antibluequirk.alternatingflux;

import antibluequirk.alternatingflux.block.AFBlocks;
import antibluequirk.alternatingflux.wire.AFWireType;
import blusunrize.immersiveengineering.common.items.WireCoilItem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Alternating Flux — long-distance super-high-voltage wire tier for Immersive
 * Engineering. Port of AntiBlueQuirk's 1.12 addon to 1.20.1 / Forge.
 *
 * Backport of the published 1.21.1 / NeoForge port at
 * https://github.com/Arctonix/alternating-flux-neoforge. Same wire tier, same
 * relay, same HV<->AF transformer; differences are purely loader/platform
 * (Forge DeferredRegister style, RegistryObject, ForgeConfigSpec, IE 10.x API).
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
                    })
                    .build());

    public AlternatingFlux()
    {
        // The wire type registers itself with WireApi via its constructor.
        AFWireType.init();

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ITEMS.register(modBus);
        TABS.register(modBus);
        AFBlocks.register(modBus);

        modBus.addListener(this::commonSetup);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SERVER_SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event)
    {
        event.enqueueWork(AFBlocks::injectIEMaps);
    }

    public static ResourceLocation rl(String path)
    {
        return new ResourceLocation(MODID, path);
    }
}
