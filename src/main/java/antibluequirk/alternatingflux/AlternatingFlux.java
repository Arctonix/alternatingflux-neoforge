package antibluequirk.alternatingflux;

import antibluequirk.alternatingflux.block.AFBlocks;
import antibluequirk.alternatingflux.wire.AFWireType;
import antibluequirk.alternatingflux.wire.UAFWireType;
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
 * Alternating Flux — long-distance super-high-voltage wire tiers for Immersive
 * Engineering. Port of AntiBlueQuirk's 1.12 addon to 1.21.1 / NeoForge.
 *
 * Two tiers, each its own wire/relay/transformer network:
 *   - AF  : the base long-distance tier; HV&lt;-&gt;AF step-down via the AF Transformer.
 *   - UAF : the higher "Ultra High AF" tier, bridged to lower tiers by the
 *           HV&lt;-&gt;UAF and AF&lt;-&gt;UAF transformers.
 *
 * This class wires up the shared registration: items, the creative tab, the
 * config, and the deferred IE-map injection (see {@link AFBlocks#injectIEMaps()}).
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

    public static final DeferredHolder<Item, WireCoilItem> UAF_WIRE_COIL =
            ITEMS.register("wirecoil_uaf", () -> new WireCoilItem(UAFWireType.UAF));

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
                        output.accept(UAF_WIRE_COIL.get());
                        output.accept(AFBlocks.CONNECTOR_UAF_RELAY_ITEM.get());
                        output.accept(AFBlocks.TRANSFORMER_UAF_HV_ITEM.get());
                        output.accept(AFBlocks.TRANSFORMER_UAF_AF_ITEM.get());
                    })
                    .build());

    public AlternatingFlux(IEventBus modBus, ModContainer container)
    {
        AFWireType.init();
        UAFWireType.init();

        ITEMS.register(modBus);
        TABS.register(modBus);
        AFBlocks.register(modBus);

        modBus.addListener(this::commonSetup);

        container.registerConfig(ModConfig.Type.SERVER, Config.SERVER_SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event)
    {
        event.enqueueWork(AFBlocks::injectIEMaps);
    }

    public static ResourceLocation rl(String path)
    {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
