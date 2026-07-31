package antibluequirk.alternatingflux.client;

import antibluequirk.alternatingflux.AlternatingFlux;
import antibluequirk.alternatingflux.wire.StrainSpans;
import blusunrize.immersiveengineering.api.IEApiDataComponents;
import blusunrize.immersiveengineering.api.wires.IWireCoil;
import blusunrize.immersiveengineering.api.wires.WireType;
import blusunrize.immersiveengineering.api.wires.utils.WireLink;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
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

import javax.annotation.Nullable;

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
 *
 * <h2>The far end a client cannot see</h2>
 * The server force-loads the far end's chunk to judge it. A client cannot: past
 * the player's render distance {@code getBlockState} answers air there, and air
 * is not an anchor. At these distances that is not an edge case — a doubled AF
 * span is 192 blocks and a render distance of 8 chunks is 128 — so a naive
 * readout would go red over the last stretch of every long span, which is the
 * exact bug this layer exists to fix, reintroduced one layer up.
 *
 * So the far end is REMEMBERED. The player was standing at it when they made the
 * first click, which is the only way a link is ever stored, so the client has
 * certainly seen that block; it is refreshed on every frame the chunk is still in
 * view and kept when it goes out. See {@link #farEndAnchored}.
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
		// No strain hardware in this game, so there is no second number to state and
		// IE's own line is already telling the truth. A bare AF install sees exactly
		// the HUD it saw in 1.0.5.
		if(!StrainSpans.anchorsExist())
			return;

		for(InteractionHand hand : InteractionHand.values())
		{
			ItemStack held = player.getItemInHand(hand);
			// Any coil, not just ours: the rule applies to every coil in the game
			// (see WireCoilItemMixin), so the readout has to as well or a player
			// holding IE's steel coil between two dead-ends would watch this line go
			// red over the stretch where the connection actually succeeds — which is
			// the exact bug this layer exists to fix.
			if(!(held.getItem() instanceof IWireCoil coil))
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
			ItemStack held, IWireCoil coil, WireLink link
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

		// Not StrainSpans.bothEndsAnchored: that reads the far end out of the world,
		// which is right on a server and blind on a client. The two ends are answered
		// separately here so the far one can come from memory.
		boolean strain = blockAim!=null
				&&link.dimension().equals(level.dimension())
				&&farEndAnchored(level, far)
				&&StrainSpans.isAnchorEnd(level, blockAim.getBlockPos(), wire,
				StrainSpans.targeting(blockAim.getDirection(), blockAim.getBlockPos(), blockAim.getLocation()));

		// The number IE will actually ask the coil for, not the wire's own, so a coil
		// that answers something else for its own reasons is still reported honestly.
		int ordinary = coil.getMaxLength(held);
		int max = strain?StrainSpans.strainSpanLength(ordinary): ordinary;
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
