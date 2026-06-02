package antibluequirk.alternatingflux.client;

import antibluequirk.alternatingflux.AlternatingFlux;
import antibluequirk.alternatingflux.Config;
import blusunrize.immersiveengineering.api.ManualHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-only manual hook. Registers a config getter with IE so the AF/UAF manual
 * entries can show live spec values through IE's {@code <config;TYPE;KEY>} tokens
 * (the same statistics-page idea the 1.12 original used). The getter returns null
 * for any key we don't own, so IE's own config tokens keep resolving.
 *
 * Forge equivalent of the 1.21.1 NeoForge AFManual: a MOD-bus EventBusSubscriber
 * scoped to the client dist.
 */
@Mod.EventBusSubscriber(modid = AlternatingFlux.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class AFManual
{
    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event)
    {
        event.enqueueWork(() -> ManualHelper.addConfigGetter(AFManual::configValue));
    }

    /** key -> value for our {@code <config;...>} tokens; null for keys we don't handle. */
    private static Object configValue(String key)
    {
        return switch(key)
        {
            case "af_transfer_rate"  -> Config.SERVER.transferRate.get();
            case "af_max_length"     -> Config.SERVER.maxLength.get();
            case "af_loss_percent"   -> Config.SERVER.lossRatio.get() * 100.0;
            case "uaf_transfer_rate" -> Config.SERVER.uafTransferRate.get();
            case "uaf_max_length"    -> Config.SERVER.uafMaxLength.get();
            case "uaf_loss_percent"  -> Config.SERVER.uafLossRatio.get() * 100.0;
            default -> null;
        };
    }

    private AFManual() {}
}
