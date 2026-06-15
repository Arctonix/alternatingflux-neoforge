package antibluequirk.alternatingflux;

import antibluequirk.alternatingflux.block.AFBlocks;
import antibluequirk.alternatingflux.wire.AFWireCoilItem;
import antibluequirk.alternatingflux.wire.AFWireType;
import blusunrize.immersiveengineering.api.wires.WireApi;
import blusunrize.immersiveengineering.common.items.WireCoilItem;
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
 * Alternating Flux — a long-distance, low-loss super-high-voltage wire tier for
 * Immersive Engineering. Port of AntiBlueQuirk's 1.12 addon to 1.18.2 / Forge.
 *
 * Provides the AF wire, the AF Wire Relay, and the AF Transformer (HV<->AF, 1:1).
 * AF-only stable v1.0.5 — parity with the 1.21.1 NeoForge v1.0.5 release.
 *
 * Uses the pre-1.19.3 anonymous-class creative tab idiom (CreativeModeTab.builder()
 * does not exist in 1.18.2). TextureStitchEvent stitches the passthrough sprite
 * instead of atlases/blocks.json (atlases/ was added in 1.19.3).
 */
@Mod(AlternatingFlux.MODID)
public class AlternatingFlux
{
    public static final String MODID = "alternatingflux";

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public static final RegistryObject<WireCoilItem> AF_WIRE_COIL =
            ITEMS.register("wirecoil_af", () -> new AFWireCoilItem(AFWireType.AF));

    // TAB is declared below; the qualified reference sidesteps the illegal-
    // forward-reference rule for simple names in field-initializer lambdas.
    public static final RegistryObject<Item> WIRE_CONSTANTAN =
            ITEMS.register("wire_constantan",
                    () -> new Item(new Item.Properties().tab(AlternatingFlux.TAB)));

    /**
     * 1.18.2 creative tab: the pre-1.19.3 idiom — an anonymous CreativeModeTab
     * (Forge's String ctor appends it to the global tab array). Title comes from
     * the existing "itemGroup.alternatingflux" lang key. Items opt in via
     * Item.Properties#tab; the IE item classes whose constructors pin IE's own
     * tab (WireCoilItem, TransformerBlockItem) are rerouted here through small
     * subclasses overriding fillItemCategory.
     */
    public static final CreativeModeTab TAB = new CreativeModeTab(MODID)
    {
        @Override
        public ItemStack makeIcon()
        {
            return new ItemStack(AF_WIRE_COIL.get());
        }
    };

    public AlternatingFlux()
    {
        // The wire type registers itself with WireApi via its constructor.
        AFWireType.init();

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ITEMS.register(modBus);
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
     * Register the AF wire for IE feedthroughs, so an AF line can pass through a
     * wall via a feedthrough block (parity with the 1.12 original). The dedicated
     * passthrough sprite is mapped whole onto the connector face (UV 0..16). The
     * connLength/connOffset pair must match the relay model so the wire stub meets
     * the cup at the right height: relay_af.obj tips out at 0.86875 (so 0.875).
     * Must run after block registration (reads the relay's default state);
     * commonSetup is safe.
     */
    private static void registerFeedthrough()
    {
        WireApi.registerFeedthroughForWiretype(
                AFWireType.AF,
                rl("block/passthrough_af"),
                new double[]{0.0, 0.0, 16.0, 16.0},
                0.875,
                0.875,
                AFBlocks.CONNECTOR_AF_RELAY.get().defaultBlockState());
    }

    public static ResourceLocation rl(String path)
    {
        return new ResourceLocation(MODID, path);
    }
}
