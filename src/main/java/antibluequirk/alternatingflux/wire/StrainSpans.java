package antibluequirk.alternatingflux.wire;

import antibluequirk.alternatingflux.AlternatingFlux;
import blusunrize.immersiveengineering.api.TargetingInfo;
import blusunrize.immersiveengineering.api.wires.IImmersiveConnectable;
import blusunrize.immersiveengineering.api.wires.WireType;
import blusunrize.immersiveengineering.api.wires.utils.WireLink;
import blusunrize.immersiveengineering.api.wires.utils.WirecoilUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
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
 * The tag id is {@code alternatingflux:strain_anchors} on every Minecraft version
 * AF ships for, so one add-on file targets all of them.
 *
 * With the tag empty — a bare AF install — {@link #isAnchor} is false everywhere,
 * {@link #bothEndsAnchored} can never be true, and
 * {@link StrainSpanCoilItem#getMaxLength} returns exactly what IE's own coil
 * returns.
 *
 * That is half of the "AF alone behaves as it did in 1.0.5" guarantee. The other
 * half is that it must not TALK about the rule either: a player with AF and IE
 * and nothing else has no dead-end block in their game, so a tooltip promising a
 * span "between two dead-ends" or a refusal asking for one names hardware that
 * does not exist and displaces a message IE had already got right. Everything the
 * rule says is therefore gated on {@link #anchorsExist()}, and
 * {@code StrainSpanGameTests} pins both halves.
 *
 * <h2>Why the rule cannot live anywhere else</h2>
 * IE asks the coil for a length, and asks it with the stack alone:
 * {@code IWireCoil#getMaxLength(ItemStack)}. One end of the span is on that stack
 * (the stored wire link from the first click); the other is the block under the
 * cursor, which the stack has never heard of. So the decision has to be made where
 * BOTH ends are in scope — that is {@code Item#useOn}, which runs synchronously
 * immediately before IE measures. Nothing here patches IE.
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
     * Does this game contain any strain hardware at all?
     *
     * The rule is silent when the answer is no, and that silence is the whole of
     * the promise to a bare AF install. It is not enough for the doubling to be
     * unreachable with an empty tag — {@link StrainSpanCoilItem} must also not
     * SPEAK about it. A player with AF and IE and nothing else has no dead-end
     * block in their game, so a tooltip promising twice the reach "between two
     * dead-ends", or a refusal telling them to build one, names hardware that does
     * not exist and replaces a message (IE's own "Too far") that was correct.
     * Everything the rule says to a player is gated on this.
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
    // Forge deprecates the vanilla static registries in favour of its own, but its
    // own are not where block tags are bound — this is the registry the tag loader
    // binds to, and the one a BlockState's own is(TagKey) consults.
    @SuppressWarnings("deprecation")
    public static boolean anchorsExist()
    {
        return BuiltInRegistries.BLOCK.getTag(STRAIN_ANCHORS).map(anchors -> anchors.size() > 0).orElse(false);
    }

    /**
     * The wire link stored on a coil by its first click, or null if it holds none.
     *
     * IE 10.x keeps it in item NBT rather than in a data component, and
     * {@code WireLink#readFromItem} on a stack that has no link parses an empty tag
     * into a {@code ResourceLocation("")} and throws. So the presence check is not
     * a convenience here, it is the guard.
     */
    @Nullable
    public static WireLink storedLink(ItemStack coil)
    {
        return WirecoilUtils.hasWireLink(coil)?WireLink.readFromItem(coil): null;
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
     *
     * <h2>Why the far end is read, not merely peeked at if convenient</h2>
     * This once refused to judge a far end whose chunk was not loaded, on the
     * grounds that reading it would force-load the chunk. That was backwards on
     * both counts.
     *
     * It is backwards on cost, because IE force-loads that exact chunk two lines
     * later regardless: {@code WireCoilItem#doCoilUse} calls
     * {@code world.getBlockEntity(otherLink.cp.position())} BEFORE it asks the coil
     * for a length, and {@code Level#getBlockEntity} goes through
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
    public static boolean bothEndsAnchored(Level level, ItemStack coil, WireType wire, BlockPos clicked, TargetingInfo target)
    {
        WireLink stored = storedLink(coil);
        if(stored==null||!stored.dimension.equals(level.dimension()))
            return false;
        return isAnchor(level, stored.cp.position())&&isAnchorEnd(level, clicked, wire, target);
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

    /**
     * The TargetingInfo IE builds for a click, from the pieces a UseOnContext
     * carries.
     *
     * Written out rather than delegated to IE 10.x's own
     * {@code TargetingInfo(UseOnContext)} constructor, which takes the hit vector
     * ABSOLUTE and so does not agree with {@code doCoilUse}, the one place the
     * answer has to match. doCoilUse subtracts the clicked position, and a
     * TargetingInfo built the other way would resolve a different connection point
     * on any block that has more than one.
     */
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
