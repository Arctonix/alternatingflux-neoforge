package antibluequirk.alternatingflux.wire;

import antibluequirk.alternatingflux.AlternatingFlux;
import blusunrize.immersiveengineering.api.IEApiDataComponents;
import blusunrize.immersiveengineering.api.TargetingInfo;
import blusunrize.immersiveengineering.api.wires.IImmersiveConnectable;
import blusunrize.immersiveengineering.api.wires.WireType;
import blusunrize.immersiveengineering.api.wires.utils.WireLink;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
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
 * {@link #bothEndsAnchored} can never be true, and
 * {@link StrainSpanCoilItem#getMaxLength} returns exactly what IE's own coil
 * returns. That is the whole "AF alone behaves as it did in 1.0.5" guarantee, and
 * {@code StrainSpanGameTests} pins it.
 *
 * <h2>Why the rule cannot live anywhere else</h2>
 * IE asks the coil for a length, and asks it with the stack alone:
 * {@code IWireCoil#getMaxLength(ItemStack)}. One end of the span is on that stack
 * (the stored {@code WIRE_LINK} from the first click); the other is the block
 * under the cursor, which the stack has never heard of. So the decision has to be
 * made where BOTH ends are in scope — that is {@code Item#useOn}, which runs
 * synchronously immediately before IE measures. Nothing here patches IE.
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

	public static boolean isAnchor(Level level, BlockPos pos)
	{
		return level.getBlockState(pos).is(STRAIN_ANCHORS);
	}

	/**
	 * IE's own endpoint resolution, in the one form we need it: the block a wire
	 * would actually land on when this one is clicked. Clicking a transformer's
	 * lower half resolves to the block above, and it is that block IE measures
	 * from, so the anchor test has to agree with it or the rule and the connection
	 * would disagree about which block is the endpoint.
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
	 * Would this click complete a span with a strain anchor at BOTH ends?
	 *
	 * False on a first click by construction: with no stored link there is no far
	 * end to be anchored, so the first click of every connection is measured
	 * against the ordinary reach exactly as before.
	 *
	 * The stored end needs no master resolution — IE stores the connection point
	 * it already resolved on the first click, so its position IS the master.
	 */
	public static boolean bothEndsAnchored(Level level, ItemStack coil, WireType wire, BlockPos clicked, TargetingInfo target)
	{
		WireLink stored = coil.get(IEApiDataComponents.WIRE_LINK);
		if(stored==null||!stored.dimension().equals(level.dimension()))
			return false;
		BlockPos far = stored.cp().position();
		// An unloaded far end is not ours to judge: reading its state would force
		// -load the chunk, and IE fails that connection as an invalid point anyway.
		if(!level.isLoaded(far)||!isAnchor(level, far))
			return false;
		BlockPos near = connectionMaster(level, clicked, wire, target);
		return near!=null&&isAnchor(level, near);
	}

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
