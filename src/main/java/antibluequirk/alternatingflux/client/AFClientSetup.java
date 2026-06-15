package antibluequirk.alternatingflux.client;

import antibluequirk.alternatingflux.AlternatingFlux;
import antibluequirk.alternatingflux.block.AFBlocks;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client setup for AF blocks. On Forge 1.16.5 the {@code "render_type"} model
 * field is a no-op, so the AF relay's translucent cup must be wired through
 * {@link RenderTypeLookup#setRenderLayer} instead. (This restores the same
 * glassy relay cup the 1.21.1 / NeoForge build gets from its model render_type,
 * matching the 1.18.2 down-port's AFClientSetup. {@code RenderTypeLookup} is the
 * 1.16.5 name; it was renamed to {@code ItemBlockRenderTypes} in 1.17.)
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
                RenderTypeLookup.setRenderLayer(AFBlocks.CONNECTOR_AF_RELAY.get(), RenderType.translucent()));
    }

    private AFClientSetup() {}
}
