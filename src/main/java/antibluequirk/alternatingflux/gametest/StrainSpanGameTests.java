package antibluequirk.alternatingflux.gametest;

import antibluequirk.alternatingflux.AlternatingFlux;
import antibluequirk.alternatingflux.block.AFBlocks;
import antibluequirk.alternatingflux.wire.AFWireType;
import antibluequirk.alternatingflux.wire.StrainSpans;
import blusunrize.immersiveengineering.api.IEApiDataComponents;
import blusunrize.immersiveengineering.api.IEProperties;
import blusunrize.immersiveengineering.api.wires.Connection;
import blusunrize.immersiveengineering.api.wires.ConnectionPoint;
import blusunrize.immersiveengineering.api.wires.GlobalWireNetwork;
import blusunrize.immersiveengineering.api.wires.IWireCoil;
import blusunrize.immersiveengineering.api.wires.LocalWireNetwork;
import blusunrize.immersiveengineering.api.wires.WireType;
import blusunrize.immersiveengineering.api.wires.utils.WireLink;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The strain-span rule, in both installs Alternating Flux has to work in.
 *
 * <h2>The two installs</h2>
 * AF declares the strain-anchor tag and applies the rule, but ships no hardware
 * that goes in the tag — the dead-ends live in add-ons, and AF is the base mod. So
 * there are exactly two shapes a game can have, and a datapack is chosen once when
 * a server starts, so they are two RUNS and not two states:
 * <ul>
 * <li>BARE — {@code ./gradlew runGameTestServer}. Nothing in the tag; what every
 * player who installs only AF and IE has. Here the whole rule must be invisible:
 * every coil in the game, ours and IE's, measures, refuses and describes itself
 * precisely as it did before the rule existed. That is the guarantee owed to the
 * five MC lines 1.0.5 is already live on.</li>
 * <li>ANCHORED — {@code ./gradlew runGameTestServerAnchored}. {@link
 * StrainSpanFixture} mounts a test-only datapack that stands in for an add-on and
 * puts two connectors in the tag. Here the doubling itself is proved.</li>
 * </ul>
 * {@code ./gradlew gameTest} runs both. Each test below declares which install it
 * is about and hands itself in when the run is the other one; it asks the RUN, not
 * the tag, so a fixture that failed to load cannot quietly turn a doubling test
 * into a no-op — {@link #runModeMatchesFixture} is what pins the two together.
 *
 * <h2>Why IE's own coils are tested here</h2>
 * Because the rule now reaches them. It used to live on a coil subclass, which
 * covered only the two coils AF and AF: Interchange register; it now lives on IE's
 * one length check, which every coil in the game funnels through. Copper, electrum
 * and steel are registered by IE, so nobody else's test suite is going to notice if
 * AF breaks them — in either direction, by failing to double or by changing
 * anything at all when the tag is empty.
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

	/** IE's own word for a span it will not take. The bare install must still hear it. */
	private static final String IE_TOO_FAR = "chat.immersiveengineering.warning.tooFar";

	private static final String IE = "immersiveengineering";
	/** Tagged by the fixture: the anchor at each end of an IE strain span. */
	private static final String HV_RELAY = "connector_hv_relay";
	/** Never tagged: an ordinary HV connector, for the one-anchor control. */
	private static final String HV_CONNECTOR = "connector_hv";
	private static final String STEEL_COIL = "wirecoil_steel";

	// ======================================================================
	//  Both installs
	// ======================================================================

	/**
	 * ★ THE RUN IS THE INSTALL IT CLAIMS TO BE.
	 *
	 * Every other test in this file hands itself in when the run is not the one it
	 * is about, and it decides that from {@link StrainSpanFixture#installed()} — a
	 * system property. A conditional test suite has exactly one catastrophic failure
	 * mode, and this is it: if that property never reached the JVM, the anchored run
	 * would mount no fixture, every test about the doubling would quietly declare
	 * itself out of scope, and the suite would report all green having proved
	 * nothing at all. Both runs would pass, identically, forever.
	 *
	 * So the run's identity is taken from somewhere the property cannot reach — the
	 * run directory the build gives it — and the property, the directory and the tag
	 * are made to agree. Any one of the three going missing fails here, loudly, in
	 * the run it went missing from.
	 */
	@GameTest(template = TEMPLATE)
	public static void runModeMatchesFixture(GameTestHelper helper)
	{
		Path runDirectory = helper.getLevel().getServer().getServerDirectory();
		String name = runDirectory.toAbsolutePath().getFileName().toString();
		boolean anchoredRun = StrainSpanFixture.ANCHORED_RUN_DIRECTORY.equals(name);

		helper.assertTrue(StrainSpanFixture.installed()==anchoredRun,
				anchoredRun
						?"this is the anchored run ("+runDirectory+") but "+StrainSpanFixture.PROPERTY
						+" did not reach the JVM. Every test about the doubling would have declared"
						+" itself out of scope and the suite would have gone green proving nothing"
						:"a run outside "+StrainSpanFixture.ANCHORED_RUN_DIRECTORY+" ("+runDirectory
						+") has "+StrainSpanFixture.PROPERTY+" set; the bare install is not being tested");
		helper.assertTrue(StrainSpans.anchorsExist()==anchoredRun,
				anchoredRun
						?"the anchored run mounted no strain anchors: "+StrainSpans.STRAIN_ANCHORS.location()
						+" is empty, so the fixture datapack did not load"
						:"the bare run has strain anchors in "+StrainSpans.STRAIN_ANCHORS.location()
						+"; the fixture datapack has leaked into an install that must not have it");
		helper.succeed();
	}

	/**
	 * (a) THE ITEM NEVER DOUBLES — only a click does.
	 *
	 * {@code IWireCoil#getMaxLength(ItemStack)} is handed the stack and nothing
	 * else, so it cannot know what is under the cursor and must never answer
	 * anything but the ordinary reach. This held when the rule was a coil subclass
	 * carrying a scoped flag, and it holds more plainly now that the rule is not on
	 * the item at all; either way, anything that reads a coil outside a click — a
	 * tooltip, another mod, IE's own overlay — gets the honest number.
	 *
	 * True in both installs, and the anchored one is the stronger claim.
	 */
	@GameTest(template = TEMPLATE)
	public static void coilItselfAlwaysReportsTheOrdinaryReach(GameTestHelper helper)
	{
		IWireCoil af = coil(helper);
		ItemStack stack = new ItemStack((Item)af);
		helper.assertTrue(af.getMaxLength(stack)==AFWireType.AF.getMaxLength(),
				"the AF coil reports a reach of "+af.getMaxLength(stack)+", but AF wire is "
						+AFWireType.AF.getMaxLength()+": the doubling has leaked onto the item");

		IWireCoil steel = steelCoil(helper);
		ItemStack steelStack = new ItemStack((Item)steel);
		helper.assertTrue(steel.getMaxLength(steelStack)==WireType.STEEL.getMaxLength(),
				"IE's steel coil reports a reach of "+steel.getMaxLength(steelStack)+", but steel wire is "
						+WireType.STEEL.getMaxLength());
		helper.succeed();
	}

	/**
	 * (d) An ordinary span still goes up. The rule sits in front of every click a
	 * player ever makes with any coil, so the cheapest way for this change to break
	 * Immersive Engineering would be to stop making wire at all.
	 */
	@GameTest(template = TEMPLATE, timeoutTicks = 200)
	public static void ordinarySpanStillConnects(GameTestHelper helper)
	{
		int span = 40;
		BlockPos west = afRelayPair(helper, span);
		BlockPos east = relayPos(span);

		helper.runAfterDelay(2, () -> {
			helper.assertBlockPresent(AFBlocks.CONNECTOR_AF_RELAY.get(), west);
			helper.assertBlockPresent(AFBlocks.CONNECTOR_AF_RELAY.get(), east);
			runCoil(helper, new ItemStack((Item)coil(helper), 4), west, east);
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
	 * (f) The multiplier is a rule, not a number two places have to agree on. If
	 * anyone ever makes it configurable or per-tier, this is where they find out
	 * that the manual page, the tooltip and the HUD all quote it.
	 */
	@GameTest(template = TEMPLATE)
	public static void strainSpanIsExactlyTwiceTheOrdinaryOne(GameTestHelper helper)
	{
		helper.assertTrue(StrainSpans.SPAN_MULTIPLIER==2,
				"the strain span multiplier is "+StrainSpans.SPAN_MULTIPLIER
						+"; every page and tooltip that says \"twice\" is now wrong");
		helper.assertTrue(StrainSpans.strainSpanLength(AFWireType.AF.getMaxLength())
						==AFWireType.AF.getMaxLength()*2,
				"a strain span of AF wire is "+StrainSpans.strainSpanLength(AFWireType.AF.getMaxLength())
						+", expected twice "+AFWireType.AF.getMaxLength());
		helper.succeed();
	}

	/**
	 * (h) ★ THE FAR END IS READ, NOT SKIPPED WHEN IT IS INCONVENIENT.
	 *
	 * The rule used to decline to judge a far end whose chunk was not loaded. That
	 * quietly cost the feature most of its own range: the player stands at the near
	 * end when the second click lands, and a dedicated server's default view
	 * distance is 160 blocks while a doubled AF span is 192.
	 *
	 * This pins the absence of that guard without depending on chunk-unload timing.
	 * The far end is put a kilometre away, in a chunk nothing in this test holds a
	 * ticket on, and the assertion is that asking the question LOADED it: a
	 * reintroduced {@code isLoaded} check would return before the read and leave the
	 * chunk absent. That IE force-loads the very same chunk moments later --
	 * {@code doCoilUse} calls {@code getBlockEntity} on the stored position before it
	 * asks the coil for a length -- is why the guard bought nothing in the first
	 * place.
	 *
	 * The answer itself is false, as it must be: a kilometre of untouched world is
	 * not an anchor, in either install. It is the loading that is under test.
	 */
	@GameTest(template = TEMPLATE)
	public static void unloadedFarEndIsStillJudged(GameTestHelper helper)
	{
		ServerLevel level = helper.getLevel();
		BlockPos near = helper.absolutePos(relayPos(0));
		BlockPos far = near.offset(1024, 0, 0);
		helper.assertTrue(!level.isLoaded(far),
				"harness broken: the far chunk at "+far+" was already loaded, so this test"
						+" cannot tell a read from a skipped read");

		WireLink stored = WireLink.create(
				new ConnectionPoint(far, 0), level, BlockPos.ZERO,
				StrainSpans.targeting(Direction.UP, far, Vec3.atCenterOf(far)));

		helper.assertTrue(!StrainSpans.bothEndsAnchored(level, near, stored),
				"empty world a kilometre away reported as a strain anchor");
		helper.assertTrue(level.isLoaded(far),
				"the rule refused to look at a far end whose chunk was not loaded."
						+" On a default server that is every span past 160 blocks -- the last"
						+" stretch of the doubled reach -- refused with a message telling the"
						+" player to build the dead-end they are standing at");
		helper.succeed();
	}

	// ======================================================================
	//  Bare install only: nothing changed, for anyone
	// ======================================================================

	/**
	 * (b) The register is empty in a bare install, and it is empty because AF ships
	 * it empty rather than because the tag failed to load. Both halves matter: a
	 * tag that silently did not exist would look identical from outside while
	 * leaving the rule unreachable for add-ons.
	 */
	@GameTest(template = TEMPLATE)
	public static void strainAnchorTagExistsAndIsEmptyInBareAf(GameTestHelper helper)
	{
		if(!inBareInstall(helper))
			return;
		var blocks = helper.getLevel().registryAccess().registryOrThrow(Registries.BLOCK);
		helper.assertTrue(blocks.getTag(StrainSpans.STRAIN_ANCHORS).isPresent(),
				"the block tag "+StrainSpans.STRAIN_ANCHORS.location()
						+" did not load at all: no add-on could ever fill it");
		int members = blocks.getTag(StrainSpans.STRAIN_ANCHORS).map(t -> t.size()).orElse(0);
		helper.assertTrue(members==0,
				"Alternating Flux alone puts "+members+" block(s) in "+StrainSpans.STRAIN_ANCHORS.location()
						+"; it ships no strain hardware and must contribute none");
		helper.succeed();
	}

	/**
	 * (c) The predicate answers for the world, not for a hard-coded list. An AF
	 * relay is the block a player is most likely to expect to count, and in a bare
	 * install it must not: it carries no tension, and nothing outside the tag is an
	 * anchor.
	 */
	@GameTest(template = TEMPLATE)
	public static void nothingIsAnAnchorWithoutTheTag(GameTestHelper helper)
	{
		if(!inBareInstall(helper))
			return;
		BlockPos relay = relayPos(0);
		placeConnector(helper, relay, Direction.WEST, AFBlocks.CONNECTOR_AF_RELAY.get());
		helper.assertTrue(!StrainSpans.isAnchor(helper.getLevel(), helper.absolutePos(relay)),
				"an AF relay reports as a strain anchor; only blocks in "
						+StrainSpans.STRAIN_ANCHORS.location()+" may");
		helper.assertTrue(!StrainSpans.isAnchor(helper.getLevel(), helper.absolutePos(relay.above(2))),
				"empty air reports as a strain anchor");
		helper.succeed();
	}

	/**
	 * (e) An over-long span is still refused. 100 blocks is past AF's 96, and with
	 * no anchors in the world there is nothing that could grant the rest — the
	 * doubling must not be reachable by holding the coil and hoping.
	 */
	@GameTest(template = TEMPLATE, timeoutTicks = 200)
	public static void spanBeyondReachStillRefused(GameTestHelper helper)
	{
		if(!inBareInstall(helper))
			return;
		int span = 100;
		BlockPos west = afRelayPair(helper, span);
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
			runCoil(helper, new ItemStack((Item)coil(helper), 4), west, east);
			helper.runAfterDelay(2, () -> {
				helper.assertTrue(connection(helper, west, east)==null,
						"an AF coil spanned 100 blocks between two ORDINARY RELAYS; AF wire reaches "
								+AFWireType.AF.getMaxLength()+" and nothing here is a strain anchor");
				helper.succeed();
			});
		});
	}

	/**
	 * (g) ★ AND IT SAYS NOTHING. A bare AF install must be unchanged in what the
	 * player is TOLD, not only in which wires go up — this is a mod that has already
	 * shipped, and a player with AF and IE and nothing else has no dead-end block in
	 * their game at all.
	 *
	 * Both halves are checked on the same 100-block span (e) refuses: the message
	 * the player actually receives must be IE's own "Too far", read out of a player
	 * that records what it is shown rather than inferred from a predicate; and the
	 * tooltip must not carry the two-reaches line. Either one would have a bare
	 * install quoting a 192-block span it can never take and naming hardware from a
	 * mod that is not installed.
	 */
	@GameTest(template = TEMPLATE, timeoutTicks = 200)
	public static void bareAfSaysNothingAboutStrain(GameTestHelper helper)
	{
		if(!inBareInstall(helper))
			return;
		int span = 100;
		BlockPos west = afRelayPair(helper, span);
		BlockPos east = relayPos(span);

		helper.runAfterDelay(2, () -> {
			IWireCoil coil = coil(helper);
			ItemStack stack = new ItemStack((Item)coil, 4);

			// 100 blocks is inside the band the rule speaks in -- past AF's 96, within
			// the doubled 192 -- so this is precisely the click that would be answered
			// with "build a dead-end at both ends" if the rule were reachable here.
			helper.assertTrue(span > AFWireType.AF.getMaxLength()
							&&span <= StrainSpans.strainSpanLength(AFWireType.AF.getMaxLength()),
					"test is vacuous: "+span+" blocks is outside the band the rule speaks in");

			RecordingPlayer player = recordingPlayer(helper);
			player.setItemInHand(InteractionHand.MAIN_HAND, stack);
			click(helper, player, west);
			helper.assertTrue(stack.has(IEApiDataComponents.WIRE_LINK),
					"harness broken: the first click stored no link, so there is no span to refuse");
			player.said.clear();
			click(helper, player, east);

			assertSaid(helper, player, IE_TOO_FAR,
					"with no strain hardware installed a refused AF span no longer says IE's own"
							+" \"too far\"; a bare install has been sent looking for a block its game"
							+" does not contain");
			helper.assertTrue(StrainSpans.refusalFor(context(helper, player, east))==null,
					"the rule claims it would speak about a missing dead-end in a game with none");

			List<Component> lines = tooltip(helper, (Item)coil);
			helper.assertTrue(!statesSpan(lines),
					"the AF coil's tooltip advertises a "
							+StrainSpans.strainSpanLength(AFWireType.AF.getMaxLength())
							+"-block span between dead-ends in a game with no dead-ends in it");
			helper.succeed();
		});
	}

	/**
	 * (g2) ★ AND IT LEAVES IMMERSIVE ENGINEERING'S OWN COILS ALONE.
	 *
	 * This is the one the mixin newly puts at risk. The rule is applied inside
	 * {@code WireCoilItem#doCoilUse}, which is every coil in the game — so an empty
	 * tag now has to leave copper, electrum and steel byte-for-byte as IE shipped
	 * them, not merely leave AF's own coil alone. A steel span past steel's reach
	 * must still fail, still say "Too far" in IE's own words, and the steel coil
	 * must still describe itself exactly as IE wrote it.
	 *
	 * Without this test the bare-install guarantee would cover two coils out of
	 * eight, and the five live MC lines are the ones holding the other six.
	 */
	@GameTest(template = TEMPLATE, timeoutTicks = 200)
	public static void bareAfLeavesIeCoilsAlone(GameTestHelper helper)
	{
		if(!inBareInstall(helper))
			return;
		int steelMax = WireType.STEEL.getMaxLength();
		int span = steelMax*3/2;
		Block relay = ieBlock(helper, HV_RELAY);
		BlockPos west = connectorPair(helper, span, relay, relay);
		BlockPos east = relayPos(span);

		helper.runAfterDelay(2, () -> {
			helper.assertTrue(span > steelMax&&span <= StrainSpans.strainSpanLength(steelMax),
					"test is vacuous: "+span+" blocks is outside the band the rule would speak in"
							+" for steel wire, which reaches "+steelMax);
			helper.assertBlockPresent(relay, west);
			helper.assertBlockPresent(relay, east);

			Item steel = (Item)steelCoil(helper);
			ItemStack stack = new ItemStack(steel, 4);
			RecordingPlayer player = recordingPlayer(helper);
			player.setItemInHand(InteractionHand.MAIN_HAND, stack);
			click(helper, player, west);
			helper.assertTrue(stack.has(IEApiDataComponents.WIRE_LINK),
					"harness broken: the first click with IE's steel coil stored no link");
			player.said.clear();
			click(helper, player, east);

			helper.assertTrue(connection(helper, west, east)==null,
					"IE's steel coil spanned "+span+" blocks between two HV relays in a game with"
							+" no strain anchors in it; steel wire reaches "+steelMax);
			assertSaid(helper, player, IE_TOO_FAR,
					"a refused steel span no longer says IE's own \"too far\": AF has changed what"
							+" Immersive Engineering tells its own players with nothing installed"
							+" that could justify it");

			List<Component> lines = tooltip(helper, steel);
			helper.assertTrue(!statesSpan(lines),
					"IE's steel coil now advertises a strain span; AF has written on another mod's"
							+" item in a game that contains no dead-ends");
			helper.succeed();
		});
	}

	// ======================================================================
	//  Anchored install only: the doubling itself
	// ======================================================================

	/**
	 * (i) AF's own coil reaches twice as far between two anchors. 100 blocks is past
	 * AF's 96 and inside its doubled 192, so this wire can only exist because both
	 * ends are in the tag — and (e) is the same span in the same place with the
	 * anchors taken away, refused.
	 */
	@GameTest(template = TEMPLATE, timeoutTicks = 200)
	public static void afCoilReachesTwiceBetweenAnchors(GameTestHelper helper)
	{
		if(!inAnchoredInstall(helper))
			return;
		int afMax = AFWireType.AF.getMaxLength();
		int span = 100;
		BlockPos west = afRelayPair(helper, span);
		BlockPos east = relayPos(span);

		helper.runAfterDelay(2, () -> {
			helper.assertTrue(span > afMax&&span <= StrainSpans.strainSpanLength(afMax),
					"test is vacuous: "+span+" blocks is not inside the band only a strain span"
							+" reaches ("+afMax+" ordinary, "+StrainSpans.strainSpanLength(afMax)+" anchored)");
			helper.assertTrue(StrainSpans.isAnchor(helper.getLevel(), helper.absolutePos(west))
							&&StrainSpans.isAnchor(helper.getLevel(), helper.absolutePos(east)),
					"harness broken: the fixture did not put the AF relay in "
							+StrainSpans.STRAIN_ANCHORS.location());
			runCoil(helper, new ItemStack((Item)coil(helper), 4), west, east);
			helper.runAfterDelay(2, () -> {
				Connection conn = connection(helper, west, east);
				helper.assertTrue(conn!=null,
						"a "+span+"-block AF span between two strain anchors was refused; AF wire"
								+" reaches "+afMax+" ordinarily and "+StrainSpans.strainSpanLength(afMax)
								+" anchored at both ends");
				helper.assertTrue(conn.type==AFWireType.AF,
						"the strain span is made of "+conn.type.getUniqueName()+", not AF wire");
				helper.succeed();
			});
		});
	}

	/**
	 * (j) ★ AND SO DOES IMMERSIVE ENGINEERING'S. This is the whole reason the rule
	 * moved off our coil and onto IE's length check.
	 *
	 * A steel coil is registered by IE, not by us; before this change no subclass of
	 * ours could ever reach it, and a player who built two dead-ends and strung
	 * ordinary HV between them saw nothing happen — which is exactly what was
	 * reported. 48 blocks is past steel's 32 and inside its doubled 64, so this wire
	 * can only exist because the rule reached a coil we do not own.
	 */
	@GameTest(template = TEMPLATE, timeoutTicks = 200)
	public static void ieSteelCoilReachesTwiceBetweenAnchors(GameTestHelper helper)
	{
		if(!inAnchoredInstall(helper))
			return;
		int steelMax = WireType.STEEL.getMaxLength();
		int span = steelMax*3/2;
		Block relay = ieBlock(helper, HV_RELAY);
		BlockPos west = connectorPair(helper, span, relay, relay);
		BlockPos east = relayPos(span);

		helper.runAfterDelay(2, () -> {
			helper.assertTrue(span > steelMax&&span <= StrainSpans.strainSpanLength(steelMax),
					"test is vacuous: "+span+" blocks is not inside the band only a strain span"
							+" reaches for steel ("+steelMax+" ordinary, "
							+StrainSpans.strainSpanLength(steelMax)+" anchored)");
			helper.assertTrue(StrainSpans.isAnchor(helper.getLevel(), helper.absolutePos(west))
							&&StrainSpans.isAnchor(helper.getLevel(), helper.absolutePos(east)),
					"harness broken: the fixture did not put "+IE+":"+HV_RELAY+" in "
							+StrainSpans.STRAIN_ANCHORS.location());
			runCoil(helper, new ItemStack((Item)steelCoil(helper), 4), west, east);
			helper.runAfterDelay(2, () -> {
				Connection conn = connection(helper, west, east);
				helper.assertTrue(conn!=null,
						"IE's own steel coil was refused a "+span+"-block span between two strain"
								+" anchors. The rule reaches only the coils AF registers again, which"
								+" is the bug it was moved onto IE's length check to fix");
				helper.assertTrue(conn.type==WireType.STEEL,
						"the strain span is made of "+conn.type.getUniqueName()+", not steel wire");
				helper.succeed();
			});
		});
	}

	/**
	 * (k) One anchor is not enough, on IE's coil as on ours. The far end is a tagged
	 * HV relay and the near end an ordinary HV connector at the same distance, so
	 * the only difference between this and (j) is the tag — which is the non-vacuity
	 * (j) needs, and the guarantee that the doubled reach is something a player has
	 * to build for rather than something every HV line quietly gained.
	 */
	@GameTest(template = TEMPLATE, timeoutTicks = 200)
	public static void ieCoilStillRefusedWithOneAnchor(GameTestHelper helper)
	{
		if(!inAnchoredInstall(helper))
			return;
		int steelMax = WireType.STEEL.getMaxLength();
		int span = steelMax*3/2;
		Block relay = ieBlock(helper, HV_RELAY);
		Block plain = ieBlock(helper, HV_CONNECTOR);
		BlockPos west = connectorPair(helper, span, relay, plain);
		BlockPos east = relayPos(span);

		helper.runAfterDelay(2, () -> {
			helper.assertTrue(StrainSpans.isAnchor(helper.getLevel(), helper.absolutePos(west)),
					"harness broken: the west HV relay is not in "+StrainSpans.STRAIN_ANCHORS.location());
			helper.assertTrue(!StrainSpans.isAnchor(helper.getLevel(), helper.absolutePos(east)),
					"an ordinary HV connector is in "+StrainSpans.STRAIN_ANCHORS.location()
							+"; the doubled reach is available without building any strain structure");

			Item steel = (Item)steelCoil(helper);
			ItemStack stack = new ItemStack(steel, 4);
			RecordingPlayer player = recordingPlayer(helper);
			player.setItemInHand(InteractionHand.MAIN_HAND, stack);
			click(helper, player, west);
			player.said.clear();
			click(helper, player, east);

			helper.runAfterDelay(2, () -> {
				helper.assertTrue(connection(helper, west, east)==null,
						"a "+span+"-block steel span went up with an anchor at only one end; steel"
								+" wire reaches "+steelMax);
				// (l) The refusal, in the same click: it must name both numbers rather
				// than IE's bare "too far", which teaches a player nothing when the same
				// coil just made a longer span two dead-ends away.
				Component said = assertSaid(helper, player, StrainSpans.CHAT_NEEDS_BOTH_ANCHORS,
						"a steel span that a second anchor would have allowed was refused with IE's"
								+" bare \"too far\"; the player is told nothing about the reach they"
								+" could have had");
				TranslatableContents t = (TranslatableContents)said.getContents();
				helper.assertTrue(Integer.valueOf(steelMax).equals(t.getArgs()[0])
								&&Integer.valueOf(StrainSpans.strainSpanLength(steelMax)).equals(t.getArgs()[1]),
						"the refusal quotes "+t.getArgs()[0]+" and "+t.getArgs()[1]
								+" blocks; steel reaches "+steelMax+" and "
								+StrainSpans.strainSpanLength(steelMax)+" between anchors");
				helper.succeed();
			});
		});
	}

	/**
	 * (m) And it stops at double — the upper bound, which matters twice over.
	 *
	 * Past the doubled reach no anchor helps, so IE's own "too far" is the honest
	 * answer and must be what comes back; anything else would be telling a player to
	 * build hardware that would have made no difference.
	 *
	 * And 68 blocks is past steel's doubled 64 but well inside a QUADRUPLED 128, so
	 * this is also where a rule applied twice — the old subclass override and the new
	 * mixin both live — would show up as a wire that should not exist. (j) can only
	 * see that the doubling happened; this is what sees that it happened once.
	 */
	@GameTest(template = TEMPLATE, timeoutTicks = 200)
	public static void ieCoilStopsAtDoubleLength(GameTestHelper helper)
	{
		if(!inAnchoredInstall(helper))
			return;
		int steelMax = WireType.STEEL.getMaxLength();
		int span = StrainSpans.strainSpanLength(steelMax)+4;
		Block relay = ieBlock(helper, HV_RELAY);
		BlockPos west = connectorPair(helper, span, relay, relay);
		BlockPos east = relayPos(span);

		helper.runAfterDelay(2, () -> {
			Item steel = (Item)steelCoil(helper);
			ItemStack stack = new ItemStack(steel, 4);
			RecordingPlayer player = recordingPlayer(helper);
			player.setItemInHand(InteractionHand.MAIN_HAND, stack);
			click(helper, player, west);
			player.said.clear();
			click(helper, player, east);

			helper.runAfterDelay(2, () -> {
				helper.assertTrue(connection(helper, west, east)==null,
						"a "+span+"-block steel span went up between two strain anchors; even doubled,"
								+" steel reaches only "+StrainSpans.strainSpanLength(steelMax));
				assertSaid(helper, player, IE_TOO_FAR,
						"a span past even the doubled reach was refused with a message about missing"
								+" anchors, at two ends that are both already anchored");
				helper.succeed();
			});
		});
	}

	/**
	 * (n) ★ EVERY COIL SAYS IT, AND SAYS IT ONCE.
	 *
	 * Two claims in one, and the second is the more dangerous.
	 *
	 * That IE's steel coil states the rule at all is the point of moving it off our
	 * subclass: the rule is invisible otherwise, and a player holding a steel coil
	 * has nowhere else to learn that its reach changes with what is on the far end.
	 *
	 * That it states it EXACTLY once, on our coil as much as on IE's, is the guard
	 * against the failure mode this whole change invites. The rule used to be an
	 * override on {@code StrainSpanCoilItem}; it is now an injection into the method
	 * that override called. If both were ever live at once the tooltip would print
	 * twice — and the same double application on the reach hook would quietly grant
	 * FOUR times the span, which no test that only checks "at least double" could
	 * see. The tooltip is where it shows up first and cheapest.
	 */
	@GameTest(template = TEMPLATE)
	public static void everyCoilStatesItsStrainSpanExactlyOnce(GameTestHelper helper)
	{
		if(!inAnchoredInstall(helper))
			return;
		assertStatesSpanOnce(helper, (Item)steelCoil(helper), WireType.STEEL.getMaxLength(),
				"IE's own steel coil");
		assertStatesSpanOnce(helper, (Item)coil(helper), AFWireType.AF.getMaxLength(),
				"the AF coil, which used to carry the rule as an override of this very method");
		helper.succeed();
	}

	private static void assertStatesSpanOnce(GameTestHelper helper, Item item, int ordinary, String what)
	{
		List<TranslatableContents> spans = new ArrayList<>();
		for(Component line : tooltip(helper, item))
			if(line.getContents() instanceof TranslatableContents t&&StrainSpans.TOOLTIP_SPAN.equals(t.getKey()))
				spans.add(t);

		helper.assertTrue(!spans.isEmpty(),
				what+" states no strain span on its tooltip, so the only way to learn that it"
						+" reaches "+StrainSpans.strainSpanLength(ordinary)+" blocks between dead-ends"
						+" is to try it");
		helper.assertTrue(spans.size()==1,
				what+" states its strain span "+spans.size()+" times. The rule is being applied more"
						+" than once -- an override and the mixin both live -- which on the reach hook"
						+" would mean "+(ordinary*(1 << spans.size()))+" blocks instead of "
						+StrainSpans.strainSpanLength(ordinary));
		TranslatableContents span = spans.get(0);
		helper.assertTrue(Integer.valueOf(ordinary).equals(span.getArgs()[0])
						&&Integer.valueOf(StrainSpans.strainSpanLength(ordinary)).equals(span.getArgs()[1]),
				what+" quotes "+span.getArgs()[0]+" and "+span.getArgs()[1]+" blocks; it reaches "
						+ordinary+" and "+StrainSpans.strainSpanLength(ordinary));
	}

	// ======================================================================
	//  Which install is this?
	// ======================================================================

	/** True when this run is the bare install; hands the test in when it is not. */
	private static boolean inBareInstall(GameTestHelper helper)
	{
		return forInstall(helper, false);
	}

	/** True when this run is the anchored install; hands the test in when it is not. */
	private static boolean inAnchoredInstall(GameTestHelper helper)
	{
		return forInstall(helper, true);
	}

	/**
	 * Read off the RUN, not off the tag. Asking the tag would make a test that needs
	 * anchors pass silently in a run whose fixture failed to mount, which is the one
	 * failure mode a conditional test must not have; {@link #runModeMatchesFixture}
	 * is where the run and the tag are made to agree.
	 */
	private static boolean forInstall(GameTestHelper helper, boolean anchored)
	{
		if(StrainSpanFixture.installed()==anchored)
			return true;
		helper.succeed();
		return false;
	}

	// ======================================================================
	//  World building
	// ======================================================================

	private static IWireCoil coil(GameTestHelper helper)
	{
		helper.assertTrue(AlternatingFlux.AF_WIRE_COIL.get() instanceof IWireCoil,
				"wirecoil_af is not an IWireCoil at all");
		return (IWireCoil)AlternatingFlux.AF_WIRE_COIL.get();
	}

	/** IE's steel coil — a coil AF does not register and cannot subclass. */
	private static IWireCoil steelCoil(GameTestHelper helper)
	{
		Item item = ieItem(helper, STEEL_COIL);
		helper.assertTrue(item instanceof IWireCoil,
				IE+":"+STEEL_COIL+" is not an IWireCoil; this test no longer drives a real IE coil");
		return (IWireCoil)item;
	}

	private static Block ieBlock(GameTestHelper helper, String path)
	{
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath(IE, path);
		Block block = BuiltInRegistries.BLOCK.get(id);
		helper.assertTrue(block!=Blocks.AIR, "Immersive Engineering has no block "+id
				+"; this test cannot stand a wire between blocks that do not exist");
		return block;
	}

	private static Item ieItem(GameTestHelper helper, String path)
	{
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath(IE, path);
		Item item = BuiltInRegistries.ITEM.get(id);
		helper.assertTrue(item!=Items.AIR, "Immersive Engineering has no item "+id);
		return item;
	}

	private static BlockPos relayPos(int offset)
	{
		return new BlockPos(WEST_RELAY_X+offset, RELAY_Y, RELAY_Z);
	}

	/** An AF relay at each end of the span, each bolted to a block outside it. */
	private static BlockPos afRelayPair(GameTestHelper helper, int span)
	{
		Block relay = AFBlocks.CONNECTOR_AF_RELAY.get();
		return connectorPair(helper, span, relay, relay);
	}

	private static BlockPos connectorPair(GameTestHelper helper, int span, Block west, Block east)
	{
		placeConnector(helper, relayPos(0), Direction.WEST, west);
		placeConnector(helper, relayPos(span), Direction.EAST, east);
		return relayPos(0);
	}

	/**
	 * IE's connector facing points INTO the block it is mounted on, so the support
	 * goes on that side. Placed with setBlock rather than through the block item
	 * because the facing is the only thing placement would have decided.
	 */
	private static void placeConnector(GameTestHelper helper, BlockPos pos, Direction facing, Block connector)
	{
		helper.setBlock(pos.relative(facing), Blocks.STONE.defaultBlockState());
		BlockState state = connector.defaultBlockState().setValue(IEProperties.FACING_ALL, facing);
		helper.setBlock(pos, state);
	}

	// ======================================================================
	//  Driving a coil, and hearing what it says
	// ======================================================================

	/**
	 * A player that remembers what it was shown.
	 *
	 * {@code GameTestHelper#makeMockPlayer} hands back a plain {@link Player}, whose
	 * {@code displayClientMessage} is empty — so a test built on it can only ever
	 * ask the rule what it WOULD say, never read what IE actually said. That gap is
	 * exactly where a refusal message can be right in the predicate and wrong on the
	 * screen, which is the half of this change that touches IE's own coils.
	 */
	private static final class RecordingPlayer extends Player
	{
		private final GameType mode;
		final List<Component> said = new ArrayList<>();

		RecordingPlayer(Level level, GameType mode)
		{
			super(level, BlockPos.ZERO, 0.0F, new GameProfile(UUID.randomUUID(), "af-recording-player"));
			this.mode = mode;
		}

		@Override
		public void displayClientMessage(Component message, boolean actionBar)
		{
			said.add(message);
		}

		@Override
		public boolean isSpectator()
		{
			return mode==GameType.SPECTATOR;
		}

		@Override
		public boolean isCreative()
		{
			return mode.isCreative();
		}

		@Override
		public boolean isLocalPlayer()
		{
			return true;
		}
	}

	private static RecordingPlayer recordingPlayer(GameTestHelper helper)
	{
		return new RecordingPlayer(helper.getLevel(), GameType.SURVIVAL);
	}

	/** The one message the player was shown, asserted to be {@code key}. */
	private static Component assertSaid(GameTestHelper helper, RecordingPlayer player, String key, String why)
	{
		helper.assertTrue(!player.said.isEmpty(),
				why+" -- in fact the player was told nothing at all");
		Component last = player.said.get(player.said.size()-1);
		helper.assertTrue(last.getContents() instanceof TranslatableContents t&&key.equals(t.getKey()),
				why+" -- the player was shown "+describe(last)+", expected "+key);
		return last;
	}

	private static String describe(Component component)
	{
		return component.getContents() instanceof TranslatableContents t?t.getKey(): component.getString();
	}

	private static List<Component> tooltip(GameTestHelper helper, Item item)
	{
		List<Component> lines = new ArrayList<>();
		item.appendHoverText(new ItemStack(item), Item.TooltipContext.of(helper.getLevel()),
				lines, TooltipFlag.NORMAL);
		return lines;
	}

	private static boolean statesSpan(List<Component> lines)
	{
		for(Component line : lines)
			if(line.getContents() instanceof TranslatableContents t&&StrainSpans.TOOLTIP_SPAN.equals(t.getKey()))
				return true;
		return false;
	}

	/**
	 * The two clicks that make a wire, in order, through the item's own useOn — the
	 * only path a coil is ever reached by. One player carries the stack across both
	 * clicks because the link from the first is stored ON the stack.
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
		player.getItemInHand(InteractionHand.MAIN_HAND).getItem().useOn(context(helper, player, rel));
	}

	/** The context one click would build, without making the click. */
	private static UseOnContext context(GameTestHelper helper, Player player, BlockPos rel)
	{
		BlockPos abs = helper.absolutePos(rel);
		return new UseOnContext(player, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
	}

	// ======================================================================
	//  Reading the result
	// ======================================================================

	/** The wire between two connectors as the world actually holds it, or null. */
	@Nullable
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
