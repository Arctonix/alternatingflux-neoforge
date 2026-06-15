package antibluequirk.alternatingflux;

import antibluequirk.alternatingflux.block.AFBlocks;
import antibluequirk.alternatingflux.wire.AFWireCoilItem;
import antibluequirk.alternatingflux.wire.AFWireType;
import blusunrize.immersiveengineering.api.wires.WireApi;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Alternating Flux — a long-distance, low-loss super-high-voltage wire tier for
 * Immersive Engineering. Port of AntiBlueQuirk's 1.12 addon to 1.16.5 / Forge.
 *
 * Provides the AF wire, the AF Wire Relay, and the AF Transformer (HV&lt;-&gt;AF, 1:1).
 * This class wires up the shared registration: items, the creative tab, the config,
 * and the deferred IE-map injection (see {@link AFBlocks#injectIEMaps()}).
 */
@Mod(AlternatingFlux.MODID)
public class AlternatingFlux
{
    public static final String MODID = "alternatingflux";

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    /**
     * Creative tab for AF items. In 1.16.5, the DeferredRegister&lt;CreativeModeTab&gt;
     * pattern (introduced in 1.19.3) does not exist. Use ItemGroup anonymous subclass.
     * The tab label key is "itemGroup.alternatingflux" matching the lang file entry.
     * Declared before AF_WIRE_COIL/WIRE_CONSTANTAN so their lambdas can reference TAB
     * without a forward reference.
     */
    public static final ItemGroup TAB = new ItemGroup(MODID)
    {
        @Override
        public ItemStack makeIcon()
        {
            // AF_WIRE_COIL.get() is only called at render time (not at static init),
            // so the forward reference here is safe.
            return new ItemStack(AF_WIRE_COIL.get());
        }
    };

    /**
     * AF Wire Coil. Uses AFWireCoilItem (extends Item + IWireCoil directly) instead
     * of IE's WireCoilItem, because IE 1.16.5's IEBaseItem sets the registry name to
     * immersiveengineering:&lt;name&gt; in the constructor, which would conflict with our
     * registration under the alternatingflux namespace.
     */
    public static final RegistryObject<AFWireCoilItem> AF_WIRE_COIL =
            ITEMS.register("wirecoil_af", () -> new AFWireCoilItem(AFWireType.AF,
                    new Item.Properties().tab(TAB)));

    public static final RegistryObject<Item> WIRE_CONSTANTAN =
            ITEMS.register("wire_constantan", () -> new Item(new Item.Properties().tab(TAB)));

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
     * Register the AF wire for IE feedthroughs. In IE 1.16.5 the signature is:
     * (WireType, ResourceLocation, float[], double, double, Supplier&lt;BlockState&gt;)
     * — note float[] (not double[]) and Supplier&lt;BlockState&gt; (not plain BlockState).
     * The relay model tips out at 0.875, so connLength and connOffset are both 0.875.
     */
    private static void registerFeedthrough()
    {
        WireApi.registerFeedthroughForWiretype(
                AFWireType.AF,
                rl("block/passthrough_af"),
                new float[]{0.0f, 0.0f, 16.0f, 16.0f},
                0.875,
                0.875,
                () -> AFBlocks.CONNECTOR_AF_RELAY.get().defaultBlockState());
    }

    public static ResourceLocation rl(String path)
    {
        return new ResourceLocation(MODID, path);
    }
}
