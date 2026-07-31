package antibluequirk.alternatingflux.client;

import antibluequirk.alternatingflux.AlternatingFlux;
import antibluequirk.alternatingflux.wire.StrainSpanCoilItem;
import antibluequirk.alternatingflux.wire.StrainSpans;
import blusunrize.immersiveengineering.api.wires.WireType;
import blusunrize.immersiveengineering.api.wires.utils.WireLink;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

/**
 * A SECOND overlay, drawn beside Immersive Engineering's own held-link readout,
 * that states the reach this coil will actually take.
 *
 * <h2>Why IE's line cannot answer for us</h2>
 * IE's readout sizes its red "too far" tint from the WIRE, not from the coil
 * ({@code ClientEventHandler#onRenderOverlayPost}):
 * <pre>
 *   int max = ((IWireCoil)equipped.getItem()).getWireType(equipped).getMaxLength();
 *   if(d &gt; max*max) col = 0xdd3333;
 * </pre>
 * There is no path from our rule to that expression: the doubling is decided per
 * click from the block under the cursor, and a wire type has no idea what a
 * player is aiming at. So while a strain span is being made, IE's line turns red
 * over the exact stretch where the connection still succeeds — during precisely
 * the activity the rule exists for.
 *
 * <h2>Why a second overlay rather than replacing IE's</h2>
 * On IE 10.2.0 that readout is not a registered overlay at all: it is drawn
 * inline from a {@code RenderGuiOverlayEvent.Post} handler keyed on
 * {@code VanillaGuiOverlay.ITEM_NAME}, in the same block that draws the
 * voltmeter, the revolver, the railgun, the drill, the buzzsaw, the chemthrower,
 * the shield and the fluorescent tube. Suppressing that event to fix one line of
 * text would take all of those with it. Registering our own named overlay through
 * {@code RegisterGuiOverlaysEvent} cannot collide with it at all; the corrected
 * number simply sits one line above IE's, and it is the one that agrees with what
 * the coil will do.
 *
 * Both facts this needs are client-side already, and are the same two IE's own
 * handler uses: the stored wire link gives the far end, and
 * {@code Minecraft#hitResult} gives what the player is aiming at.
 *
 * <h2>The far end a client cannot see</h2>
 * The server force-loads the far end's chunk to judge it. A client cannot: past
 * the player's render distance {@code getBlockState} answers air there, and air
 * is not an anchor. At these distances that is not an edge case — a doubled AF
 * span is 192 blocks and a render distance of 8 chunks is 128 — so a naive
 * readout would go red over the last stretch of every long span, which is the
 * exact bug this overlay exists to fix, reintroduced one line up.
 *
 * So the far end is REMEMBERED. The player was standing at it when they made the
 * first click, which is the only way a link is ever stored, so the client has
 * certainly seen that block; it is refreshed on every frame the chunk is still in
 * view and kept when it goes out. See {@link #farEndAnchored}.
 */
@Mod.EventBusSubscriber(modid = AlternatingFlux.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class StrainSpanOverlay
{
    private static final String OVERLAY_ID = "strain_span";

    /** In range, and in range only because both ends are anchored. */
    private static final int COLOUR_STRAIN = 0x55dd55;
    /** IE's own out-of-range red. */
    private static final int COLOUR_TOO_FAR = 0xdd3333;

    /**
     * One text line above IE's, which sits at {@code height-20-leftHeight}.
     * {@code leftHeight} is the vanilla HUD's own left-column cursor, read the same
     * way IE reads it, so the pair rise and fall together as the status bars do.
     */
    private static final int LINE_ABOVE_IE = 30;

    /**
     * The last far end this client actually looked at, and what it was.
     *
     * One link is held at a time and one player is looking at it, so one slot is
     * the whole of the state. It is only ever consulted for the position it was
     * recorded at, in the dimension it was recorded in, so a stale entry cannot be
     * mistaken for a fresh one — it can only fail to apply, and then the readout
     * falls back to reading the world and under-promises rather than inventing.
     */
    @Nullable
    private static ResourceKey<Level> rememberedDimension;
    @Nullable
    private static BlockPos rememberedFarEnd;
    private static boolean rememberedAnchored;

    @SubscribeEvent
    static void register(RegisterGuiOverlaysEvent ev)
    {
        // Anchored on the vanilla debug overlay rather than on anything of IE's:
        // IE registers no named overlay on this version, and the debug slot exists
        // whatever order the mods happen to load in.
        ev.registerBelow(VanillaGuiOverlay.DEBUG_TEXT.id(), OVERLAY_ID, StrainSpanOverlay::render);
    }

    private static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight)
    {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Level level = mc.level;
        if(player==null||level==null||mc.options.hideGui)
            return;
        // No strain hardware in this game, so there is no second number to state and
        // IE's own line is already telling the truth. A bare AF install sees exactly
        // the HUD it saw in 1.0.5.
        if(!StrainSpans.anchorsExist())
            return;

        for(InteractionHand hand : InteractionHand.values())
        {
            ItemStack held = player.getItemInHand(hand);
            if(!(held.getItem() instanceof StrainSpanCoilItem coil))
                continue;
            WireLink link = StrainSpans.storedLink(held);
            if(link==null)
                continue; // nothing is being spanned yet; IE draws nothing either
            draw(graphics, mc, gui, player, level, held, coil, link, screenWidth, screenHeight);
            return; // one line: a player can only aim at one thing
        }
    }

    private static void draw(
            GuiGraphics graphics, Minecraft mc, ForgeGui gui, Player player, Level level,
            ItemStack held, StrainSpanCoilItem coil, WireLink link, int screenWidth, int screenHeight
    )
    {
        BlockPos far = link.cp.position();
        WireType wire = coil.getWireType(held);

        // The same distance IE measures, taken the same way: the block under the
        // cursor if there is one, the player otherwise.
        HitResult aim = mc.hitResult;
        BlockHitResult blockAim = aim instanceof BlockHitResult hit?hit: null;
        double distanceSq = blockAim!=null
                ?blockAim.getBlockPos().distSqr(far)
                :player.distanceToSqr(far.getX(), far.getY(), far.getZ());

        // Not StrainSpans.bothEndsAnchored: that reads the far end out of the world,
        // which is right on a server and blind on a client. The two ends are answered
        // separately here so the far one can come from memory.
        boolean strain = blockAim!=null
                &&link.dimension.equals(level.dimension())
                &&farEndAnchored(level, far)
                &&StrainSpans.isAnchorEnd(level, blockAim.getBlockPos(), wire,
                StrainSpans.targeting(blockAim.getDirection(), blockAim.getBlockPos(), blockAim.getLocation()));

        int max = strain?coil.getStrainSpanLength(held): wire.getMaxLength();
        int distance = Mth.ceil(Math.sqrt(distanceSq));
        // IE's own "in range" colour for its held-link line, so the two agree.
        // Read here rather than into a constant: WireType.ELECTRUM is a static
        // field IE fills in during mod construction, and this class is loaded by
        // the event-bus scan, which can happen first.
        int inRange = WireType.ELECTRUM.getColour(null);
        int colour = distanceSq > (double)max*max?COLOUR_TOO_FAR
                :strain?COLOUR_STRAIN: inRange;

        Component text = Component.translatable(
                strain?StrainSpans.HUD_STRAIN_REACH: StrainSpans.HUD_REACH, distance, max);
        graphics.drawCenteredString(
                mc.font, text,
                screenWidth/2,
                screenHeight-LINE_ABOVE_IE-gui.leftHeight,
                colour);
    }

    /**
     * Was the far end of the held link an anchor, as best this client can know?
     *
     * While its chunk is in view the world is the answer and the memory is
     * refreshed from it. Once it drops out of view the world would answer air —
     * {@code ClientChunkCache} hands back an empty chunk for anything it has not
     * been sent — and the remembered answer is used instead, because the block did
     * not change when the player walked away from it.
     *
     * The memory is certain to have been filled: a link only exists because the
     * player clicked that block, from arm's length, some seconds ago. The one case
     * it has not is a reconnect while already holding a link, where the coil
     * survives on the stack and this class's memory does not. There the readout
     * falls back to what the client can see, which is the pre-existing behaviour and
     * errs towards the shorter reach — a green line that should have been red would
     * be the harmful direction, and this cannot produce one.
     */
    private static boolean farEndAnchored(Level level, BlockPos far)
    {
        if(level.isLoaded(far))
        {
            boolean anchored = StrainSpans.isAnchor(level, far);
            rememberedDimension = level.dimension();
            rememberedFarEnd = far;
            rememberedAnchored = anchored;
            return anchored;
        }
        return far.equals(rememberedFarEnd)&&level.dimension().equals(rememberedDimension)&&rememberedAnchored;
    }

    private StrainSpanOverlay() {}
}
