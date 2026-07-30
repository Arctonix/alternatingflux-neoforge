package antibluequirk.alternatingflux.gametest;

import antibluequirk.alternatingflux.AlternatingFlux;
import antibluequirk.alternatingflux.block.AFBlocks;
import antibluequirk.alternatingflux.wire.AFWireType;
import antibluequirk.alternatingflux.wire.StrainSpanCoilItem;
import antibluequirk.alternatingflux.wire.StrainSpans;
import blusunrize.immersiveengineering.api.IEProperties;
import blusunrize.immersiveengineering.api.wires.Connection;
import blusunrize.immersiveengineering.api.wires.ConnectionPoint;
import blusunrize.immersiveengineering.api.wires.GlobalWireNetwork;
import blusunrize.immersiveengineering.api.wires.LocalWireNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * What Alternating Flux alone can be held to.
 *
 * AF declares the strain-anchor tag and applies the rule, but ships no hardware
 * that goes in the tag — the dead-ends live in add-ons, and AF is the base mod and
 * must not depend on them. So a bare AF install is the EMPTY-TAG case, and that is
 * exactly what this file pins: with nothing registered as an anchor, the AF coil
 * measures, refuses and connects precisely as it did before the rule existed.
 * That is the guarantee a shipped mod owes worlds that already exist.
 *
 * The doubling itself is proved end-to-end where the anchors actually are, in
 * AF: Interchange's StrainSpanCoilGameTests, which drive this same coil class for
 * both the AF and the UAF tier against real dead-ends.
 */
@GameTestHolder(AlternatingFlux.MODID)
public class StrainSpanGameTests
{
	/**
	 * An empty 104-long corridor: long enough to stand an AF span of 100 blocks in,
	 * past AF's 96-block reach, with a support block outside each end. NeoForge
	 * prefixes the template with the simple class name, so the file on disk is
	 * data/alternatingflux/structure/strainspangametests.span_104x8x5.nbt.
	 */
	private static final String TEMPLATE = "span_104x8x5";

	private static final int RELAY_Y = 3;
	private static final int RELAY_Z = 2;
	private static final int WEST_RELAY_X = 2;

	/**
	 * (a) THE promise to existing worlds. With no anchors registered, the coil's
	 * answer to the only question IE asks it is the wire's own reach — the exact
	 * expression IWireCoil's default returns, which is what wirecoil_af answered
	 * before this class existed.
	 */
	@GameTest(template = TEMPLATE)
	public static void reachIsUnchangedWithoutAnchors(GameTestHelper helper)
	{
		StrainSpanCoilItem coil = coil(helper);
		ItemStack stack = new ItemStack(coil);
		helper.assertTrue(coil.getMaxLength(stack)==AFWireType.AF.getMaxLength(),
				"the AF coil reaches "+coil.getMaxLength(stack)+" with no anchors registered, but AF wire is "
						+AFWireType.AF.getMaxLength()+": a bare AF install has changed behaviour");
		helper.succeed();
	}

	/**
	 * (b) The register is empty in a bare install, and it is empty because AF ships
	 * it empty rather than because the tag failed to load. Both halves matter: a
	 * tag that silently did not exist would look identical from (a) while leaving
	 * the rule unreachable for add-ons.
	 */
	@GameTest(template = TEMPLATE)
	public static void strainAnchorTagExistsAndIsEmptyInBareAf(GameTestHelper helper)
	{
		var tags = helper.getLevel().registryAccess().registryOrThrow(Registries.BLOCK);
		helper.assertTrue(tags.getTag(StrainSpans.STRAIN_ANCHORS).isPresent(),
				"the block tag "+StrainSpans.STRAIN_ANCHORS.location()
						+" did not load at all: no add-on could ever fill it");
		int members = tags.getTag(StrainSpans.STRAIN_ANCHORS).map(t -> t.size()).orElse(0);
		helper.assertTrue(members==0,
				"Alternating Flux alone puts "+members+" block(s) in "+StrainSpans.STRAIN_ANCHORS.location()
						+"; it ships no strain hardware and must contribute none");
		helper.succeed();
	}

	/**
	 * (c) The predicate answers for the world, not for a hard-coded list. An AF
	 * relay is the block a player is most likely to expect to count, and it must
	 * not: it carries no tension, and nothing outside the tag is an anchor.
	 */
	@GameTest(template = TEMPLATE)
	public static void nothingIsAnAnchorWithoutTheTag(GameTestHelper helper)
	{
		BlockPos relay = relayPos(0);
		placeRelay(helper, relay, Direction.WEST);
		helper.assertTrue(!StrainSpans.isAnchor(helper.getLevel(), helper.absolutePos(relay)),
				"an AF relay reports as a strain anchor; only blocks in "
						+StrainSpans.STRAIN_ANCHORS.location()+" may");
		helper.assertTrue(!StrainSpans.isAnchor(helper.getLevel(), helper.absolutePos(relay.above(2))),
				"empty air reports as a strain anchor");
		helper.succeed();
	}

	/**
	 * (d) An ordinary span still goes up. The coil subclass sits in front of every
	 * click a player ever makes with it, so the cheapest way for this change to
	 * break AF would be to stop making wire at all.
	 */
	@GameTest(template = TEMPLATE, timeoutTicks = 200)
	public static void ordinarySpanStillConnects(GameTestHelper helper)
	{
		int span = 40;
		BlockPos west = relayPair(helper, span);
		BlockPos east = relayPos(span);

		helper.runAfterDelay(2, () -> {
			helper.assertBlockPresent(AFBlocks.CONNECTOR_AF_RELAY.get(), west);
			helper.assertBlockPresent(AFBlocks.CONNECTOR_AF_RELAY.get(), east);
			runCoil(helper, new ItemStack(coil(helper), 4), west, east);
			helper.runAfterDelay(2, () -> {
				Connection conn = connection(helper, west, east);
				helper.assertTrue(conn!=null,
						"a 40-block AF span between two AF relays was refused; AF wire reaches "
								+AFWireType.AF.getMaxLength());
				helper.assertTrue(conn.type==AFWireType.AF,
						"the span is made of "+conn.type.getUniqueName()+", not AF wire");
				helper.succeed();
			});
		});
	}

	/**
	 * (e) And an over-long one is still refused. 100 blocks is past AF's 96, and
	 * with no anchors in the world there is nothing that could grant the rest —
	 * the doubling must not be reachable by holding the coil and hoping.
	 */
	@GameTest(template = TEMPLATE, timeoutTicks = 200)
	public static void spanBeyondReachStillRefused(GameTestHelper helper)
	{
		int span = 100;
		BlockPos west = relayPair(helper, span);
		BlockPos east = relayPos(span);

		helper.runAfterDelay(2, () -> {
			// Non-vacuity: a refusal proves nothing if there was never a connector
			// at the far end for the coil to refuse, or if the span were short
			// enough to be legal anyway.
			helper.assertBlockPresent(AFBlocks.CONNECTOR_AF_RELAY.get(), west);
			helper.assertBlockPresent(AFBlocks.CONNECTOR_AF_RELAY.get(), east);
			helper.assertTrue(span > AFWireType.AF.getMaxLength(),
					"test is vacuous: "+span+" blocks is within AF's own reach of "
							+AFWireType.AF.getMaxLength());
			runCoil(helper, new ItemStack(coil(helper), 4), west, east);
			helper.runAfterDelay(2, () -> {
				helper.assertTrue(connection(helper, west, east)==null,
						"an AF coil spanned 100 blocks between two ORDINARY RELAYS; AF wire reaches "
								+AFWireType.AF.getMaxLength()+" and nothing here is a strain anchor");
				helper.succeed();
			});
		});
	}

	/**
	 * (f) The multiplier is a rule, not a number two places have to agree on. If
	 * anyone ever makes it configurable or per-tier, this is where they find out
	 * that the manual page, the tooltip and the HUD all quote it.
	 */
	@GameTest(template = TEMPLATE)
	public static void strainSpanIsExactlyTwiceTheOrdinaryOne(GameTestHelper helper)
	{
		StrainSpanCoilItem coil = coil(helper);
		ItemStack stack = new ItemStack(coil);
		helper.assertTrue(StrainSpans.SPAN_MULTIPLIER==2,
				"the strain span multiplier is "+StrainSpans.SPAN_MULTIPLIER
						+"; every page and tooltip that says \"twice\" is now wrong");
		helper.assertTrue(coil.getStrainSpanLength(stack)==AFWireType.AF.getMaxLength()*2,
				"the AF coil advertises a strain span of "+coil.getStrainSpanLength(stack)
						+", expected twice "+AFWireType.AF.getMaxLength());
		helper.succeed();
	}

	// ---- world building --------------------------------------------------

	private static StrainSpanCoilItem coil(GameTestHelper helper)
	{
		helper.assertTrue(AlternatingFlux.AF_WIRE_COIL.get() instanceof StrainSpanCoilItem,
				"wirecoil_af is not a StrainSpanCoilItem: the rule is not on the item at all");
		return (StrainSpanCoilItem)AlternatingFlux.AF_WIRE_COIL.get();
	}

	private static BlockPos relayPos(int offset)
	{
		return new BlockPos(WEST_RELAY_X+offset, RELAY_Y, RELAY_Z);
	}

	/** A relay at each end of the span, each bolted to a block outside it. */
	private static BlockPos relayPair(GameTestHelper helper, int span)
	{
		BlockPos west = relayPos(0);
		BlockPos east = relayPos(span);
		placeRelay(helper, west, Direction.WEST);
		placeRelay(helper, east, Direction.EAST);
		return west;
	}

	/**
	 * IE's connector facing points INTO the block it is mounted on, so the support
	 * goes on that side. Placed with setBlock rather than through the block item
	 * because the facing is the only thing placement would have decided.
	 */
	private static void placeRelay(GameTestHelper helper, BlockPos pos, Direction facing)
	{
		helper.setBlock(pos.relative(facing), Blocks.STONE.defaultBlockState());
		BlockState relay = AFBlocks.CONNECTOR_AF_RELAY.get().defaultBlockState()
				.setValue(IEProperties.FACING_ALL, facing);
		helper.setBlock(pos, relay);
	}

	// ---- driving the coil ------------------------------------------------

	/**
	 * The two clicks that make a wire, in order, through the item's own useOn — the
	 * only path a coil is ever reached by. One mock player carries the stack across
	 * both clicks because the link from the first is stored ON the stack.
	 */
	private static void runCoil(GameTestHelper helper, ItemStack coil, BlockPos from, BlockPos to)
	{
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, coil);
		click(helper, player, from);
		click(helper, player, to);
	}

	private static void click(GameTestHelper helper, Player player, BlockPos rel)
	{
		BlockPos abs = helper.absolutePos(rel);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
		player.getItemInHand(InteractionHand.MAIN_HAND).getItem()
				.useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND, hit));
	}

	// ---- reading the result ----------------------------------------------

	/** The wire between two connectors as the world actually holds it, or null. */
	private static Connection connection(GameTestHelper helper, BlockPos a, BlockPos b)
	{
		GlobalWireNetwork global = GlobalWireNetwork.getNetwork(helper.getLevel());
		ConnectionPoint cpA = new ConnectionPoint(helper.absolutePos(a), 0);
		ConnectionPoint cpB = new ConnectionPoint(helper.absolutePos(b), 0);
		LocalWireNetwork local = global.getNullableLocalNet(cpA);
		if(local==null)
			return null;
		for(Connection c : local.getConnections(cpA))
			if(!c.isInternal()&&c.getOtherEnd(cpA).equals(cpB))
				return c;
		return null;
	}
}
