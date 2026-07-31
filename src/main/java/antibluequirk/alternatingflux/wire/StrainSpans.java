package antibluequirk.alternatingflux.wire;

import antibluequirk.alternatingflux.AlternatingFlux;
import blusunrize.immersiveengineering.api.IEApiDataComponents;
import blusunrize.immersiveengineering.api.TargetingInfo;
import blusunrize.immersiveengineering.api.wires.ConnectionPoint;
import blusunrize.immersiveengineering.api.wires.IImmersiveConnectable;
import blusunrize.immersiveengineering.api.wires.IWireCoil;
import blusunrize.immersiveengineering.api.wires.WireType;
import blusunrize.immersiveengineering.api.wires.utils.WireLink;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * The one rule: a wire strung between two STRAIN ANCHORS may reach twice as far.
 *
 * <h2>Why a tag, and why the tag is empty here</h2>
 * Alternating Flux ships no strain hardware of its own. The dead-ends this rule
 * is about live in add-ons, and AF is the base mod — it must not depend on any of
 * them. So the anchor set is a BLOCK TAG that AF declares and tests, and that
 * add-ons fill in from their own data. Tags merge across mods, so an add-on adds
 * its dead-ends (and other mods' dead-ends) without AF ever naming them and
 * without touching those mods' files.
 *
 * With the tag empty — a bare AF install — {@link #isAnchor} is false everywhere,
 * {@link #bothEndsAnchored} can never be true, {@link #reachFor} hands IE back the
 * number it came in with and {@link #tooFarReason} hands it back its own message.
 *
 * That is half of the "AF alone behaves as it did in 1.0.5" guarantee. The other
 * half is that it must not TALK about the rule either: a player with AF and IE
 * and nothing else has no dead-end block in their game, so a tooltip promising a
 * span "between two dead-ends" or a refusal asking for one names hardware that
 * does not exist and displaces a message IE had already got right. Everything the
 * rule says is therefore gated on {@link #anchorsExist()}, and
 * {@code StrainSpanGameTests} pins both halves — now for IE's own coils too, which
 * this rule newly reaches.
 *
 * <h2>Where the rule is applied</h2>
 * In {@code WireCoilItemMixin}, on IE's own coil-use path. IE asks the coil for a
 * length, and asks it with the stack alone ({@code IWireCoil#getMaxLength}); one
 * end of a span is on that stack and the other is the block under the cursor, so
 * no coil can answer "are BOTH ends anchored?" from what it is given. The one
 * place both ends are in scope is inside {@code WireCoilItem#doCoilUse}, which is
 * where IE measures — and which every coil in the game funnels through, ours and
 * IE's alike. See that class for why it is a mixin and how it fails soft.
 *
 * <h2>Sag</h2>
 * Untouched, and it still grows: {@code Connection#makeCatenaryData} computes
 * {@code wireLength = delta.length()*slack} and slack is a RATIO, so a doubled
 * span hangs about twice as deep on its own. The extra reach is paid for in
 * clearance, not bought for free.
 */
public final class StrainSpans
{
	/**
	 * Blocks that carry the pull of a wire rather than passing it along — the
	 * dead-ends of the add-ons. Declared here, filled in by whoever ships the
	 * hardware.
	 */
	public static final TagKey<Block> STRAIN_ANCHORS =
			TagKey.create(Registries.BLOCK, AlternatingFlux.rl("strain_anchors"));

	/**
	 * How much further a strain span reaches. Not a config knob: it is a rule a
	 * player learns once and can then read off any coil's tooltip, and a server
	 * that could set it to 5 would make every manual page and every HUD readout in
	 * the ecosystem wrong about the same wire.
	 */
	public static final int SPAN_MULTIPLIER = 2;

	/** Chat key for a span that would have fitted if both ends were anchored. */
	public static final String CHAT_NEEDS_BOTH_ANCHORS = "chat.alternatingflux.warning.needsBothAnchors";
	/** Tooltip key: the two reaches of a coil, ordinary and strain. */
	public static final String TOOLTIP_SPAN = "desc.alternatingflux.info.coilSpan";
	/** Held-link readout while the span is an ordinary one. */
	public static final String HUD_REACH = "desc.alternatingflux.info.reach";
	/** Held-link readout while both ends are anchors and the doubled reach applies. */
	public static final String HUD_STRAIN_REACH = "desc.alternatingflux.info.strainReach";

	/** The doubled reach of a wire whose ordinary reach is {@code ordinary}. */
	public static int strainSpanLength(int ordinary)
	{
		return ordinary*SPAN_MULTIPLIER;
	}

	public static boolean isAnchor(Level level, BlockPos pos)
	{
		return level.getBlockState(pos).is(STRAIN_ANCHORS);
	}

	/**
	 * Does this game contain any strain hardware at all?
	 *
	 * The rule is silent when the answer is no, and that silence is the whole of
	 * the promise to a bare AF install. It is not enough for the doubling to be
	 * unreachable with an empty tag — the rule must also not SPEAK about it. A
	 * player with AF and IE and nothing else has no dead-end block in their game,
	 * so a tooltip promising twice the reach "between two dead-ends", or a refusal
	 * telling them to build one, names hardware that does not exist and replaces a
	 * message (IE's own "Too far") that was correct. Everything the rule says to a
	 * player is gated on this, and so is every world read it would otherwise do.
	 *
	 * Read off {@code BuiltInRegistries.BLOCK} rather than off a level, because the
	 * two callers that need it most have no level to hand: a tooltip drawn in the
	 * inventory and the held-link overlay. Block tags are bound onto the static
	 * registry on both sides — the server on datapack reload, the client on the tag
	 * sync that follows login — so this is the same object, and the same answer,
	 * that {@code level.registryAccess().registryOrThrow(Registries.BLOCK)} gives.
	 * Before any tags are bound at all (the main menu) it answers false, which is
	 * the safe direction: the rule stays quiet rather than advertising early.
	 */
	public static boolean anchorsExist()
	{
		return BuiltInRegistries.BLOCK.getTag(STRAIN_ANCHORS).map(anchors -> anchors.size() > 0).orElse(false);
	}

	// ---- what the mixin asks -------------------------------------------------

	/**
	 * The reach IE should use for this click: {@code ordinary}, doubled only when
	 * both ends of the span land on a strain anchor.
	 *
	 * A plain block distance, because IE squares it on the very next instruction.
	 *
	 * {@code nearMaster} is IE's own {@code masterPos} — the block a wire would
	 * actually land on, which is not always the block clicked (a transformer's
	 * lower half resolves to the block above). It has to be that one, or the rule
	 * and the connection would disagree about which block the endpoint is.
	 *
	 * <h2>Why the far end is read, not merely peeked at if convenient</h2>
	 * This once refused to judge a far end whose chunk was not loaded, on the
	 * grounds that reading it would force-load the chunk. That was backwards on
	 * both counts.
	 *
	 * It is backwards on cost, because IE force-loads that exact chunk two lines
	 * earlier regardless: {@code doCoilUse} calls
	 * {@code world.getBlockEntity(storedLink.cp().position())} BEFORE it asks the
	 * coil for a length, and {@code Level#getBlockEntity} goes through
	 * {@code getChunkAt}, which loads on the server. Declining the read saved
	 * nothing; it only meant deciding the rule on less information than IE decides
	 * the connection on.
	 *
	 * And it is backwards on reach, because the far end being unloaded is the
	 * NORMAL case for exactly the spans this rule exists for. The player stands at
	 * the near end when the second click lands, and a dedicated server's default
	 * view distance is 10 chunks — 160 blocks — while a doubled AF span is 192. So
	 * every span in the last stretch of its own range silently lost the doubling
	 * and was refused with a message telling the player to build the dead-end they
	 * were standing at. On a view-distance-8 server it was two thirds of the extra
	 * range.
	 *
	 * The client reads the same expression and cannot force-load anything: an
	 * unloaded chunk answers air there, which is not an anchor. That divergence is
	 * the overlay's to handle, and it does — see {@code StrainSpanOverlay}.
	 */
	public static int reachFor(Level level, BlockPos nearMaster, @Nullable WireLink stored, int ordinary)
	{
		if(!anchorsExist())
			return ordinary;
		return bothEndsAnchored(level, nearMaster, stored)?strainSpanLength(ordinary): ordinary;
	}

	/**
	 * Our own words for a span IE is about to refuse as too far, or null to leave
	 * IE's "Too far" exactly as it is.
	 *
	 * IE's word for a span it will not take is "Too far" — true, and useless to a
	 * player who has just watched this same coil make a longer one somewhere else.
	 * Where the ONLY thing standing between the click and a wire is the missing
	 * anchor, say that instead.
	 *
	 * Deliberately narrow, and every one of the four ways out returns IE's own
	 * message:
	 * <ul>
	 * <li>No strain hardware in this game. Nothing was ever withheld, so there is
	 * nothing to explain, and sending a bare AF player looking for a block their
	 * game does not contain would be worse than saying nothing.</li>
	 * <li>The span fits. Then this is not the refusal we are looking at.</li>
	 * <li>The span is longer than even a strain span reaches. No anchor would have
	 * helped, so "too far" is the whole truth.</li>
	 * <li>Both ends ARE anchored. Then the doubled reach was already granted and
	 * ran out; there is nothing further to build.</li>
	 * </ul>
	 * The last of those can only be reached when the doubling itself did not
	 * apply — see {@code WireCoilItemMixin} on failing soft. It is checked anyway,
	 * so that a degraded install says nothing false rather than telling a player to
	 * anchor two ends that are already anchored.
	 */
	@Nullable
	public static Component tooFarReason(
			Level level, int ordinary, BlockPos nearMaster, @Nullable WireLink stored, int distanceSq
	)
	{
		if(!anchorsExist())
			return null;
		int doubled = strainSpanLength(ordinary);
		if(distanceSq <= (long)ordinary*ordinary||distanceSq > (long)doubled*doubled)
			return null;
		if(bothEndsAnchored(level, nearMaster, stored))
			return null;
		// Uncoloured, like every other line IE puts in that same action bar. This
		// replaces one of IE's messages in place; it should not announce itself as
		// coming from somewhere else.
		return Component.translatable(CHAT_NEEDS_BOTH_ANCHORS, ordinary, doubled);
	}

	// ---- the two ends --------------------------------------------------------

	/**
	 * Would this span have a strain anchor at BOTH ends?
	 *
	 * False whenever there is no stored link, by construction: with no far end
	 * there is no span, so the first click of every connection is measured against
	 * the ordinary reach exactly as before.
	 *
	 * The stored end needs no master resolution — IE stores the connection point it
	 * already resolved on the first click, so its position IS the master. The
	 * dimension is checked here because IE checks it AFTER measuring, so at the
	 * moment we are asked a cross-dimension link is still live on the stack.
	 */
	public static boolean bothEndsAnchored(Level level, BlockPos nearMaster, @Nullable WireLink stored)
	{
		if(stored==null||!stored.dimension().equals(level.dimension()))
			return false;
		return isAnchor(level, stored.cp().position())&&isAnchor(level, nearMaster);
	}

	/** The same question asked of a click that has not been made yet. */
	public static boolean bothEndsAnchored(
			Level level, ItemStack coil, WireType wire, BlockPos clicked, TargetingInfo target
	)
	{
		BlockPos near = connectionMaster(level, clicked, wire, target);
		return near!=null&&bothEndsAnchored(level, near, coil.get(IEApiDataComponents.WIRE_LINK));
	}

	/**
	 * IE's own endpoint resolution, in the one form we need it: the block a wire
	 * would actually land on when this one is clicked. Clicking a transformer's
	 * lower half resolves to the block above, and it is that block IE measures
	 * from, so the anchor test has to agree with it.
	 *
	 * Null when the click is not on a connector at all — there is nothing for
	 * either IE or us to do with it.
	 */
	@Nullable
	public static BlockPos connectionMaster(Level level, BlockPos clicked, WireType wire, TargetingInfo target)
	{
		if(!(level.getBlockEntity(clicked) instanceof IImmersiveConnectable iic)||!iic.canConnect())
			return null;
		return iic.getConnectionMaster(wire, target);
	}

	/**
	 * Is the connector this click would land on a strain anchor?
	 *
	 * Split out because the two ends are known in different ways and, on the
	 * client, with different confidence: the clicked end is always under the
	 * player's cursor and therefore always loaded, while the stored end may be
	 * hundreds of blocks behind them.
	 */
	public static boolean isAnchorEnd(Level level, BlockPos clicked, WireType wire, TargetingInfo target)
	{
		BlockPos near = connectionMaster(level, clicked, wire, target);
		return near!=null&&isAnchor(level, near);
	}

	// ---- asking what a click would be told -----------------------------------

	/**
	 * What {@link #tooFarReason} would say about this click, without making it.
	 *
	 * The same decision, reached the same way, from the outside: this replicates
	 * the tests IE runs before it measures — not a connector, wrong cable, wrong
	 * dimension, same connection — because each of those has a better message than
	 * ours and IE reaches them first. Returns null to mean "IE should answer this
	 * click", never to mean "allow".
	 *
	 * Exists so a gametest can ask what a player would be told without reading
	 * chat, and it answers for ANY coil, IE's included, because the rule now does.
	 */
	@Nullable
	public static Component refusalFor(UseOnContext ctx)
	{
		Player player = ctx.getPlayer();
		Level level = ctx.getLevel();
		ItemStack stack = ctx.getItemInHand();
		if(player==null||!(stack.getItem() instanceof IWireCoil coil))
			return null;
		if(!anchorsExist())
			return null;

		WireType wire = coil.getWireType(stack);
		TargetingInfo target = targeting(ctx);

		// Only ever a SECOND click: with nothing stored there is no span to measure.
		WireLink stored = stack.get(IEApiDataComponents.WIRE_LINK);
		if(stored==null||!stored.dimension().equals(level.dimension()))
			return null; // IE: passes the click on, or says "wrongDimension"

		BlockPos clicked = ctx.getClickedPos();
		if(!(level.getBlockEntity(clicked) instanceof IImmersiveConnectable iic)||!iic.canConnect())
			return null; // IE: PASS, not a connector
		BlockPos masterPos = iic.getConnectionMaster(wire, target);
		Vec3i masterOffset = clicked.subtract(masterPos);
		if(!(level.getBlockEntity(masterPos) instanceof IImmersiveConnectable master)||!master.canConnect())
			return null; // IE: PASS
		ConnectionPoint cp = master.getTargetedPoint(target, masterOffset);
		if(cp==null||!master.canConnectCable(wire, cp, masterOffset)
				||!coil.canConnectCable(stack, level.getBlockEntity(masterPos)))
			return null; // IE: "wrongCable", which is the more useful thing to hear

		BlockPos farPos = stored.cp().position();
		if(farPos.equals(masterPos))
			return null; // IE: "sameConnection"

		// IE's own measurement, to the integer: doCoilUse ceils the squared distance
		// and compares it against the squared reach.
		int distanceSq = (int)Math.ceil(farPos.distSqr(masterPos));
		return tooFarReason(level, coil.getMaxLength(stack), masterPos, stored, distanceSq);
	}

	// ---- odds and ends -------------------------------------------------------

	/** The TargetingInfo IE builds for a click, from the pieces a UseOnContext carries. */
	public static TargetingInfo targeting(UseOnContext ctx)
	{
		return targeting(ctx.getClickedFace(), ctx.getClickedPos(), ctx.getClickLocation());
	}

	public static TargetingInfo targeting(Direction face, BlockPos pos, Vec3 hit)
	{
		return new TargetingInfo(face, (float)hit.x-pos.getX(), (float)hit.y-pos.getY(), (float)hit.z-pos.getZ());
	}

	private StrainSpans() {}
}
