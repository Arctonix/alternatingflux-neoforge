package antibluequirk.alternatingflux.client;

import antibluequirk.alternatingflux.AlternatingFlux;
import antibluequirk.alternatingflux.block.AFBlocks;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client setup for AF blocks. On Forge 1.18.2 the {@code "render_type"} model
 * field is a no-op, so the AF relay's translucent cup must be wired through
 * {@link ItemBlockRenderTypes#setRenderLayer} instead. (This restores the same
 * glassy relay cup the 1.21.1 / NeoForge build gets from its model render_type.)
 *
 * setRenderLayer mutates a static map and is not thread-safe, so the call is
 * deferred onto the client thread via {@link FMLClientSetupEvent#enqueueWork}.
 */
@Mod.EventBusSubscriber(modid = AlternatingFlux.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class AFClientSetup
{
    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event)
    {
        event.enqueueWork(() ->
                ItemBlockRenderTypes.setRenderLayer(AFBlocks.CONNECTOR_AF_RELAY.get(), RenderType.translucent()));
    }

    private AFClientSetup() {}
}
