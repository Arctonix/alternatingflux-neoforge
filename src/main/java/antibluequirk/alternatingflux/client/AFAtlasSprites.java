package antibluequirk.alternatingflux.client;

import antibluequirk.alternatingflux.AlternatingFlux;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Stitches the feedthrough sprite into the block atlas. 1.18.2 predates atlas
 * JSONs (assets/&lt;ns&gt;/atlases/ is 1.19.3+), so sprites not directly
 * referenced by a model — ours is consumed only by IE's feedthrough model via
 * WireApi.INFOS — must be added through TextureStitchEvent.Pre instead.
 */
@Mod.EventBusSubscriber(modid = AlternatingFlux.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class AFAtlasSprites
{
    @SubscribeEvent
    static void onTextureStitch(TextureStitchEvent.Pre event)
    {
        if(event.getAtlas().location().equals(InventoryMenu.BLOCK_ATLAS))
        {
            event.addSprite(AlternatingFlux.rl("block/passthrough_af"));
        }
    }

    private AFAtlasSprites() {}
}
