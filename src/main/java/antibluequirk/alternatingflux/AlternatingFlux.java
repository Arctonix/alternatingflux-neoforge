package antibluequirk.alternatingflux;

import antibluequirk.alternatingflux.block.AFBlocks;
import antibluequirk.alternatingflux.wire.AFWireCoilItem;
import antibluequirk.alternatingflux.wire.AFWireType;
import antibluequirk.alternatingflux.wire.UAFWireType;
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
 * Alternating Flux — long-distance super-high-voltage wire tiers for Immersive
 * Engineering. Port of AntiBlueQuirk's 1.12 addon to 1.19.2 / Forge (Forge-only:
 * NeoForge does not exist for 1.19.2).
 *
 * Two tiers, each its own wire/relay/transformer network:
 *   - AF  : the base long-distance tier; HV<->AF step-down via the AF Transformer.
 *   - UAF : the higher "Ultra High AF" tier, bridged to lower tiers by the
 *           HV<->UAF and AF<->UAF transformers.
 *
 * Backport of the published 1.21.1 / NeoForge port at
 * https://github.com/Arctonix/alternatingflux-neoforge, via the 1.20.1-forge
 * branch. Differences from 1.20.1 are purely platform: IE 9.x API, the
 * pre-1.19.3 creative-tab idiom, and TextureStitchEvent sprite stitching
 * instead of atlas JSONs.
 */
@Mod(AlternatingFlux.MODID)
public class AlternatingFlux
{
    public static final String MODID = "alternatingflux";

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public static final RegistryObject<WireCoilItem> AF_WIRE_COIL =
            ITEMS.register("wirecoil_af", () -> new AFWireCoilItem(AFWireType.AF));

    public static final RegistryObject<WireCoilItem> UAF_WIRE_COIL =
            ITEMS.register("wirecoil_uaf", () -> new AFWireCoilItem(UAFWireType.UAF));

    // TAB is declared below; the qualified reference sidesteps the illegal-
    // forward-reference rule for simple names in field-initializer lambdas
    // (same idiom as the AFBlocks.* forward references).
    public static final RegistryObject<Item> WIRE_CONSTANTAN =
            ITEMS.register("wire_constantan",
                    () -> new Item(new Item.Properties().tab(AlternatingFlux.TAB)));

    /**
     * 1.19.2 creative tab: the pre-1.19.3 idiom — an anonymous CreativeModeTab
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
        // The wire types register themselves with WireApi via their constructors.
        AFWireType.init();
        UAFWireType.init();

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
     * Register the AF and UAF wires for IE feedthroughs, so a line can pass through
     * a wall via a feedthrough block (parity with the 1.12 original, which registered
     * one for AF). Each tier's dedicated passthrough sprite is mapped whole onto the
     * connector face (UV 0..16). Must run after block registration (reads the relay's
     * default state); commonSetup is safe.
     *
     * The (connLength, connOffset) pair must match each relay's actual model tip so
     * the through-wire meets the connector cup instead of floating below it: the AF
     * relay cup tops out at 0.86875 (-> 0.875), the UAF relay 0.5 taller at 1.36875
     * (-> 1.375). We use IE 9.x's six-arg overload that takes connLength and
     * connOffset separately (verified by javap: registerFeedthroughForWiretype(
     * WireType, ResourceLocation, double[], double, double, BlockState)); the older
     * five-arg overload feeds a single value into both, which left the wire short.
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

        WireApi.registerFeedthroughForWiretype(
                UAFWireType.UAF,
                rl("block/passthrough_uaf"),
                new double[]{0.0, 0.0, 16.0, 16.0},
                1.375,
                1.375,
                AFBlocks.CONNECTOR_UAF_RELAY.get().defaultBlockState());
    }

    public static ResourceLocation rl(String path)
    {
        return new ResourceLocation(MODID, path);
    }
}
