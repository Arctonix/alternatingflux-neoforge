package antibluequirk.alternatingflux.client;

import antibluequirk.alternatingflux.AlternatingFlux;
import antibluequirk.alternatingflux.wire.StrainSpanCoilItem;
import antibluequirk.alternatingflux.wire.StrainSpans;
import blusunrize.immersiveengineering.api.IEApiDataComponents;
import blusunrize.immersiveengineering.api.wires.WireType;
import blusunrize.immersiveengineering.api.wires.utils.WireLink;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * A SECOND overlay layer, drawn beside Immersive Engineering's own held-link
 * readout, that states the reach this coil will actually take.
 *
 * <h2>Why IE's line cannot answer for us</h2>
 * IE's readout sizes its red "too far" tint from the WIRE, not from the coil
 * ({@code ItemOverlayUtils#renderWireCoilOverlay}):
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
 * <h2>Why a second layer rather than replacing IE's</h2>
 * IE registers its overlays as one layer at {@code GuiLayers.ITEMS}, and that
 * single layer draws the voltmeter, the revolver, the drill, the chemthrower and
 * the rest. Cancelling it through {@code RenderGuiLayerEvent.Pre} to fix one line
 * of text would take all of those with it. Registering our own layer through the
 * same {@code RegisterGuiLayersEvent} costs IE nothing and leaves every other
 * overlay alone; the corrected number simply sits one line above IE's, and it is
 * the one that agrees with what the coil will do.
 *
 * Both facts this needs are client-side already, and are the same two IE's own
 * layer uses: the stored {@code WIRE_LINK} component gives the far end, and
 * {@code Minecraft#hitResult} gives what the player is aiming at.
 */
@EventBusSubscriber(modid = AlternatingFlux.MODID, bus = Bus.MOD, value = Dist.CLIENT)
public final class StrainSpanOverlay
{
	private static final ResourceLocation LAYER = AlternatingFlux.rl("strain_span");

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

	@SubscribeEvent
	static void register(RegisterGuiLayersEvent ev)
	{
		// Anchored on the vanilla debug overlay rather than on IE's layer id: this
		// is the same slot IE registers into, and it exists whatever order the mods
		// happen to load in.
		ev.registerBelow(VanillaGuiLayers.DEBUG_OVERLAY, LAYER, StrainSpanOverlay::render);
	}

	private static void render(GuiGraphics graphics, DeltaTracker delta)
	{
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		Level level = mc.level;
		if(player==null||level==null||mc.options.hideGui)
			return;

		for(InteractionHand hand : InteractionHand.values())
		{
			ItemStack held = player.getItemInHand(hand);
			if(!(held.getItem() instanceof StrainSpanCoilItem coil))
				continue;
			WireLink link = held.get(IEApiDataComponents.WIRE_LINK);
			if(link==null)
				continue; // nothing is being spanned yet; IE draws nothing either
			draw(graphics, mc, player, level, held, coil, link);
			return; // one line: a player can only aim at one thing
		}
	}

	private static void draw(
			GuiGraphics graphics, Minecraft mc, Player player, Level level,
			ItemStack held, StrainSpanCoilItem coil, WireLink link
	)
	{
		BlockPos far = link.cp().position();
		WireType wire = coil.getWireType(held);

		// The same distance IE measures, taken the same way: the block under the
		// cursor if there is one, the player otherwise.
		HitResult aim = mc.hitResult;
		BlockHitResult blockAim = aim instanceof BlockHitResult hit?hit: null;
		double distanceSq = blockAim!=null
				?blockAim.getBlockPos().distSqr(far)
				:player.distanceToSqr(far.getX(), far.getY(), far.getZ());

		boolean strain = blockAim!=null&&StrainSpans.bothEndsAnchored(
				level, held, wire, blockAim.getBlockPos(),
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
				mc.getWindow().getGuiScaledWidth()/2,
				mc.getWindow().getGuiScaledHeight()-LINE_ABOVE_IE-mc.gui.leftHeight,
				colour);
	}

	private StrainSpanOverlay() {}
}
