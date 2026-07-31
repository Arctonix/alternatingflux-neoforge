package antibluequirk.alternatingflux.client;

import antibluequirk.alternatingflux.AlternatingFlux;
import antibluequirk.alternatingflux.Config;
import antibluequirk.alternatingflux.wire.StrainSpans;
import blusunrize.immersiveengineering.api.ManualHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-only manual hook. Registers a config getter with IE so AF's manual entry
 * can print live spec values through IE's {@code <config;TYPE;KEY>} tokens instead
 * of numbers baked into the page — a server that retunes the wire retunes what the
 * manual says about it.
 *
 * The getter returns null for any key it does not own, which is how IE chains
 * several getters: IE's own tokens, and any add-on's, keep resolving.
 */
@Mod.EventBusSubscriber(modid = AlternatingFlux.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class AFManual
{
    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event)
    {
        event.enqueueWork(() -> ManualHelper.addConfigGetter(AFManual::configValue));
    }

    /** key -&gt; value for our {@code <config;...>} tokens; null for keys we don't handle. */
    private static Object configValue(String key)
    {
        return switch(key)
        {
            case "af_transfer_rate" -> Config.SERVER.transferRate.get();
            case "af_max_length" -> Config.SERVER.maxLength.get();
            // Derived rather than a second setting, so the page can never quote a
            // doubled reach the coil would not actually take.
            case "af_strain_length" -> Config.SERVER.maxLength.get()*StrainSpans.SPAN_MULTIPLIER;
            case "af_loss_percent" -> Config.SERVER.lossRatio.get()*100.0;
            default -> null;
        };
    }

    private AFManual() {}
}
