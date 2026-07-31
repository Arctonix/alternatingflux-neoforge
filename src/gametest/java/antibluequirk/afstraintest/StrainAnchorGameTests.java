package antibluequirk.afstraintest;

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
import blusunrize.immersiveengineering.api.wires.utils.WirecoilUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * The strain-span rule itself, proved end to end in a world that HAS strain
 * hardware in it.
 *
 * {@code StrainSpanGameTests} over in AF proper pins the other half — that a bare
 * install, with the tag empty, is byte-for-byte the mod that shipped as 1.0.5.
 * The two cannot share a world: every test there depends on the tag being empty
 * and every test here depends on it not being. So this suite runs in its own
 * gametest namespace, in its own world:
 *
 * <pre>./gradlew runGameTestServer -PstrainAnchors</pre>
 *
 * The anchor is {@link AFStrainTest#STRAIN_ANCHOR}, a test-only stand-in for an
 * add-on's dead-end; the ordinary end is AF's own wire relay, which is not in the
 * tag and must not become one.
 */
@GameTestHolder(AFStrainTest.MODID)
public class StrainAnchorGameTests
{
    /**
     * 208 long for the spans, and 20 TALL for the sag. A wire hangs at a fixed
     * slack ratio, so a doubled span hangs about twice as deep: 120 blocks of AF
     * wire at slack 1.003 dips roughly four blocks below its ends. Hung any lower,
     * the line would clip the floor of the test and IE would refuse it as
     * obstructed — a real constraint on a real strain span, and one this file has
     * to leave room for rather than trip over.
     */
    private static final String TEMPLATE = "span_208x20x5";

    /*
     * Every test below names a batch of its own. The framework runs the tests
     * WITHIN a batch concurrently and the batches one after another, and each of
     * these worlds is 208 blocks long — eight of them standing at once is a
     * hundred-odd chunks of generated, wire-laden world held open simultaneously,
     * for no gain in what is proved. One at a time.
     */

    /** The row the spans are strung on; supports stand one block outside each end. */
    private static final int WIRE_Y = 10;
    private static final int WIRE_Z = 2;
    private static final int WEST_X = 2;

    /** A second row, well below the first, for the tests that need two spans. */
    private static final int LOWER_Y = 3;

    /** Past AF's ordinary 96, inside its doubled 192. */
    private static final int STRAIN_SPAN = 120;
    /** Past the doubled 192. */
    private static final int OVERLONG_SPAN = 200;
    /** Comfortably inside the ordinary reach. */
    private static final int ORDINARY_SPAN = 40;
    /** Past the ordinary reach and inside the doubled one — the interesting band. */
    private static final int UNANCHORED_SPAN = 100;

    /**
     * (a) THE FEATURE. 120 blocks is past AF wire's 96 and inside its doubled 192,
     * so this wire can only exist because both ends are strain anchors.
     *
     * The wire type is asserted too: the doubling must not have quietly turned an
     * AF span into something else. It is the same AF conductor, reaching further.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 400, batch = "strainSpanConnectsBetweenTwoAnchors")
    public static void strainSpanConnectsBetweenTwoAnchors(GameTestHelper helper)
    {
        BlockPos west = pos(0, WIRE_Y);
        BlockPos east = pos(STRAIN_SPAN, WIRE_Y);
        placeAnchor(helper, west, Direction.WEST);
        placeAnchor(helper, east, Direction.EAST);

        helper.runAfterDelay(2, () -> {
            assertBothEndsAreAnchors(helper, west, east);
            helper.assertTrue(STRAIN_SPAN > AFWireType.AF.getMaxLength()
                            &&STRAIN_SPAN <= AFWireType.AF.getMaxLength()*StrainSpans.SPAN_MULTIPLIER,
                    "test is vacuous: "+STRAIN_SPAN+" blocks is not inside the band the rule governs");
            runCoil(helper, coilStack(helper), west, east);
            helper.runAfterDelay(2, () -> {
                Connection conn = connection(helper, west, east);
                helper.assertTrue(conn!=null,
                        "a "+STRAIN_SPAN+"-block AF span between two strain anchors was refused;"
                                +" AF wire reaches "+AFWireType.AF.getMaxLength()+" and anchored at both"
                                +" ends it must reach "
                                +AFWireType.AF.getMaxLength()*StrainSpans.SPAN_MULTIPLIER);
                helper.assertTrue(conn.type==AFWireType.AF,
                        "the strain span is made of "+conn.type.getUniqueName()+", not AF wire");
                helper.succeed();
            });
        });
    }

    /**
     * (b) One anchor is not enough, and the coil says why. A wire pulls at each end
     * equally, so anchoring one side has only halved the problem.
     *
     * Both halves are checked on the same span: that no wire appears, and that the
     * player is told which two numbers the coil is working to rather than IE's bare
     * "Too far", which is what sent them looking in the first place.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 400, batch = "spanRefusedWithAnOrdinaryRelayAtTheFarEnd")
    public static void spanRefusedWithAnOrdinaryRelayAtTheFarEnd(GameTestHelper helper)
    {
        BlockPos west = pos(0, WIRE_Y);
        BlockPos east = pos(STRAIN_SPAN, WIRE_Y);
        placeAnchor(helper, west, Direction.WEST);
        placeRelay(helper, east, Direction.EAST);

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(StrainSpans.isAnchor(helper.getLevel(), helper.absolutePos(west)),
                    "harness broken: the west end is not an anchor, so nothing is being tested");
            helper.assertTrue(!StrainSpans.isAnchor(helper.getLevel(), helper.absolutePos(east)),
                    "AF's own wire relay is in "+StrainSpans.STRAIN_ANCHORS.location()
                            +"; then every AF span is a strain span and the rule means nothing");

            StrainSpanCoilItem coil = coil(helper);
            ItemStack stack = coilStack(helper);
            Player player = helper.makeMockSurvivalPlayer();
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            // A first click has no span yet, so there is nothing for us to explain.
            helper.assertTrue(coil.strainRefusal(context(helper, player, west))==null,
                    "the coil explained a refusal on a FIRST click, before any span existed");
            click(helper, player, west);
            helper.assertTrue(WirecoilUtils.hasWireLink(stack),
                    "harness broken: the first click stored no link, so there is no span to refuse");

            Component refusal = coil.strainRefusal(context(helper, player, east));
            helper.assertTrue(refusal!=null,
                    "a "+STRAIN_SPAN+"-block span to an ordinary relay was left to IE's bare \"too far\"");
            helper.assertTrue(refusal.getContents() instanceof TranslatableContents t
                            &&StrainSpans.CHAT_NEEDS_BOTH_ANCHORS.equals(t.getKey()),
                    "the refusal is not "+StrainSpans.CHAT_NEEDS_BOTH_ANCHORS
                            +" but "+refusal.getContents());
            helper.assertTrue(refusal.getContents() instanceof TranslatableContents t
                            &&t.getArgs().length==2
                            &&Integer.valueOf(AFWireType.AF.getMaxLength()).equals(t.getArgs()[0])
                            &&Integer.valueOf(AFWireType.AF.getMaxLength()*StrainSpans.SPAN_MULTIPLIER)
                            .equals(t.getArgs()[1]),
                    "the refusal does not quote the two reaches, so it says no more than \"too far\"");

            click(helper, player, east);
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(connection(helper, west, east)==null,
                        "an AF coil spanned "+STRAIN_SPAN+" blocks with an anchor at ONE end;"
                                +" AF wire reaches "+AFWireType.AF.getMaxLength());
                helper.succeed();
            });
        });
    }

    /**
     * (c) And the same span, refused the same way, with the ends clicked in the
     * other order. The rule reads one end off the stack and the other off the world,
     * by two different routes, so "both ends" has to be proved from both sides —
     * a check that only looked at the clicked end would pass (b) and fail here.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 400, batch = "spanRefusedWithTheOrdinaryRelayClickedFirst")
    public static void spanRefusedWithTheOrdinaryRelayClickedFirst(GameTestHelper helper)
    {
        BlockPos west = pos(0, WIRE_Y);
        BlockPos east = pos(STRAIN_SPAN, WIRE_Y);
        placeRelay(helper, west, Direction.WEST);
        placeAnchor(helper, east, Direction.EAST);

        helper.runAfterDelay(2, () -> {
            StrainSpanCoilItem coil = coil(helper);
            ItemStack stack = coilStack(helper);
            Player player = helper.makeMockSurvivalPlayer();
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            click(helper, player, west);
            Component refusal = coil.strainRefusal(context(helper, player, east));
            helper.assertTrue(refusal!=null&&refusal.getContents() instanceof TranslatableContents t
                            &&StrainSpans.CHAT_NEEDS_BOTH_ANCHORS.equals(t.getKey()),
                    "with the ordinary relay clicked FIRST the coil did not name the missing anchor;"
                            +" the stored end is read by a different route than the clicked one");
            click(helper, player, east);
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(connection(helper, west, east)==null,
                        "an AF coil spanned "+STRAIN_SPAN+" blocks from an ordinary relay to an anchor");
                helper.succeed();
            });
        });
    }

    /** (d) Twice, not "as far as you like": 200 blocks is past AF's doubled 192. */
    @GameTest(template = TEMPLATE, timeoutTicks = 400, batch = "strainSpanStopsAtDoubleLength")
    public static void strainSpanStopsAtDoubleLength(GameTestHelper helper)
    {
        BlockPos west = pos(0, WIRE_Y);
        BlockPos east = pos(OVERLONG_SPAN, WIRE_Y);
        placeAnchor(helper, west, Direction.WEST);
        placeAnchor(helper, east, Direction.EAST);

        helper.runAfterDelay(2, () -> {
            assertBothEndsAreAnchors(helper, west, east);
            helper.assertTrue(OVERLONG_SPAN > AFWireType.AF.getMaxLength()*StrainSpans.SPAN_MULTIPLIER,
                    "test is vacuous: "+OVERLONG_SPAN+" is inside the doubled reach");

            // And IE answers it, not us: past the doubled reach no anchor would have
            // helped, so "too far" is the whole truth and our message would be a lie.
            StrainSpanCoilItem coil = coil(helper);
            ItemStack stack = coilStack(helper);
            Player player = helper.makeMockSurvivalPlayer();
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            click(helper, player, west);
            helper.assertTrue(coil.strainRefusal(context(helper, player, east))==null,
                    "the coil blamed a missing anchor for a span past even the doubled reach");
            click(helper, player, east);

            helper.runAfterDelay(2, () -> {
                helper.assertTrue(connection(helper, west, east)==null,
                        "an AF coil spanned "+OVERLONG_SPAN+" blocks between two anchors; doubled AF is "
                                +AFWireType.AF.getMaxLength()*StrainSpans.SPAN_MULTIPLIER);
                helper.succeed();
            });
        });
    }

    /**
     * (e) Everything that is not a strain span is untouched, even with strain
     * hardware installed. An ordinary 40-block AF line between two ordinary relays
     * is the wire a player already has strung across their base; a 100-block one is
     * the wire they have always been refused. Both still behave exactly as in 1.0.5.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 400, batch = "ordinarySpansAreUnchangedWithAnchorsInstalled")
    public static void ordinarySpansAreUnchangedWithAnchorsInstalled(GameTestHelper helper)
    {
        BlockPos shortWest = pos(0, WIRE_Y);
        BlockPos shortEast = pos(ORDINARY_SPAN, WIRE_Y);
        placeRelay(helper, shortWest, Direction.WEST);
        placeRelay(helper, shortEast, Direction.EAST);

        BlockPos longWest = pos(0, LOWER_Y);
        BlockPos longEast = pos(UNANCHORED_SPAN, LOWER_Y);
        placeRelay(helper, longWest, Direction.WEST);
        placeRelay(helper, longEast, Direction.EAST);

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(StrainSpans.anchorsExist(),
                    "no strain hardware is registered in this world; this suite is testing nothing");
            helper.assertTrue(UNANCHORED_SPAN > AFWireType.AF.getMaxLength(),
                    "test is vacuous: "+UNANCHORED_SPAN+" is within AF's own reach");
            runCoil(helper, coilStack(helper), shortWest, shortEast);
            runCoil(helper, coilStack(helper), longWest, longEast);
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(connection(helper, shortWest, shortEast)!=null,
                        "a "+ORDINARY_SPAN+"-block AF span between two ordinary relays was refused;"
                                +" installing strain hardware broke ordinary wiring");
                helper.assertTrue(connection(helper, longWest, longEast)==null,
                        "an AF coil spanned "+UNANCHORED_SPAN+" blocks between two ORDINARY RELAYS;"
                                +" the doubling is reachable without anchors");
                helper.succeed();
            });
        });
    }

    /**
     * (f) ★ THE DOUBLING DOES NOT OUTLIVE THE CLICK THAT EARNED IT.
     *
     * The extra reach is handed to IE through a flag set for the duration of one
     * {@code useOn} and dropped in a finally, precisely so that it cannot be carried
     * forward; nothing is written to the stack. If it ever were — or if the finally
     * were dropped — the coil that just made a strain span would keep the doubled
     * reach for the player's next click, and 192-block spans between ordinary relays
     * would quietly become legal.
     *
     * Both readings are checked, on the same coil, in order: the number the coil
     * answers with once the click is over, and the wire that click's successor is
     * actually allowed to make.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 400, batch = "theDoublingDoesNotOutliveTheClickThatEarnedIt")
    public static void theDoublingDoesNotOutliveTheClickThatEarnedIt(GameTestHelper helper)
    {
        BlockPos anchorWest = pos(0, WIRE_Y);
        BlockPos anchorEast = pos(STRAIN_SPAN, WIRE_Y);
        placeAnchor(helper, anchorWest, Direction.WEST);
        placeAnchor(helper, anchorEast, Direction.EAST);

        BlockPos relayWest = pos(0, LOWER_Y);
        BlockPos relayEast = pos(UNANCHORED_SPAN, LOWER_Y);
        placeRelay(helper, relayWest, Direction.WEST);
        placeRelay(helper, relayEast, Direction.EAST);

        helper.runAfterDelay(2, () -> {
            StrainSpanCoilItem coil = coil(helper);
            ItemStack stack = coilStack(helper);
            Player player = helper.makeMockSurvivalPlayer();
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);

            click(helper, player, anchorWest);
            click(helper, player, anchorEast);

            helper.runAfterDelay(2, () -> {
                helper.assertTrue(connection(helper, anchorWest, anchorEast)!=null,
                        "the strain span was never made, so nothing has been earned and this test"
                                +" cannot tell whether it leaks");
                helper.assertTrue(coil.getMaxLength(stack)==AFWireType.AF.getMaxLength(),
                        "after making a strain span the coil still answers "+coil.getMaxLength(stack)
                                +" blocks instead of "+AFWireType.AF.getMaxLength()
                                +": the doubling outlived the click that justified it");

                // The same coil, still in the same hand, on two ordinary relays.
                click(helper, player, relayWest);
                click(helper, player, relayEast);
                helper.runAfterDelay(2, () -> {
                    helper.assertTrue(connection(helper, relayWest, relayEast)==null,
                            "the click after a strain span spanned "+UNANCHORED_SPAN
                                    +" blocks between two ORDINARY RELAYS; the doubled reach carried over");
                    helper.succeed();
                });
            });
        });
    }

    /**
     * (g) A strain span is a wire in the world, not a trick of the click that made
     * it — so it has to come back after a restart. This round-trips the network the
     * way a server restart does, through IE's own
     * {@code LocalWireNetwork(CompoundTag, GlobalWireNetwork)} constructor.
     *
     * It also catches the quieter failure of the same load: a wire whose type name
     * does not resolve comes back as COPPER rather than as nothing at all, which
     * would leave the player with a 120-block copper line they can neither have made
     * nor repair.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 400, batch = "strainSpanSurvivesSaveAndLoad")
    public static void strainSpanSurvivesSaveAndLoad(GameTestHelper helper)
    {
        BlockPos west = pos(0, WIRE_Y);
        BlockPos east = pos(STRAIN_SPAN, WIRE_Y);
        placeAnchor(helper, west, Direction.WEST);
        placeAnchor(helper, east, Direction.EAST);

        helper.runAfterDelay(2, () -> {
            runCoil(helper, coilStack(helper), west, east);
            helper.runAfterDelay(2, () -> {
                GlobalWireNetwork global = GlobalWireNetwork.getNetwork(helper.getLevel());
                ConnectionPoint cpWest = cp(helper, west);
                ConnectionPoint cpEast = cp(helper, east);
                LocalWireNetwork live = global.getNullableLocalNet(cpWest);
                helper.assertTrue(live!=null&&find(live, cpWest, cpEast)!=null,
                        "the strain span was never made, so its survival proves nothing");

                CompoundTag saved = live.writeToNBT();
                LocalWireNetwork reloaded = new LocalWireNetwork(saved, global);
                Connection after = find(reloaded, cpWest, cpEast);
                helper.assertTrue(after!=null,
                        "the "+STRAIN_SPAN+"-block strain span did not come back from NBT:"
                                +" it would disappear on the next server restart");
                helper.assertTrue(after.type==AFWireType.AF,
                        "the reloaded strain span is "+after.type.getUniqueName()
                                +", not AF wire: its type name did not resolve on load");
                helper.succeed();
            });
        });
    }

    /**
     * (h) The tag is what decides, and the coil now speaks. Three things at once,
     * because they are the same fact from three sides: an add-on's block is in
     * {@code alternatingflux:strain_anchors} and reports as an anchor, AF's own
     * relay is not and does not, and — the mirror of the bare install's silence —
     * the coil's tooltip now DOES state both reaches, because in this world there
     * is hardware that can earn the second one.
     */
    @GameTest(template = TEMPLATE, batch = "theAnchorTagIsWhatDecides")
    public static void theAnchorTagIsWhatDecides(GameTestHelper helper)
    {
        helper.assertTrue(StrainSpans.anchorsExist(),
                "the test datapack did not reach "+StrainSpans.STRAIN_ANCHORS.location()
                        +"; every other test in this file is vacuous");
        helper.assertTrue(AFStrainTest.STRAIN_ANCHOR.get().defaultBlockState().is(StrainSpans.STRAIN_ANCHORS),
                "the test anchor is not in "+StrainSpans.STRAIN_ANCHORS.location());
        helper.assertTrue(!AFBlocks.CONNECTOR_AF_RELAY.get().defaultBlockState().is(StrainSpans.STRAIN_ANCHORS),
                "AF's own wire relay is an anchor; it carries no tension and nothing AF ships may");

        StrainSpanCoilItem coil = coil(helper);
        List<Component> lines = new ArrayList<>();
        coil.appendHoverText(new ItemStack(coil), helper.getLevel(), lines, TooltipFlag.NORMAL);
        boolean stated = lines.stream().anyMatch(line -> line.getContents() instanceof TranslatableContents t
                &&StrainSpans.TOOLTIP_SPAN.equals(t.getKey()));
        helper.assertTrue(stated,
                "with strain hardware installed the coil's tooltip still says nothing about the"
                        +" second reach, so the rule is invisible to the player it applies to");
        helper.succeed();
    }

    // ---- world building --------------------------------------------------

    private static BlockPos pos(int offset, int y)
    {
        return new BlockPos(WEST_X+offset, y, WIRE_Z);
    }

    /** A stand-in dead-end: the same connector as AF's relay, but in the tag. */
    private static void placeAnchor(GameTestHelper helper, BlockPos pos, Direction facing)
    {
        place(helper, pos, facing, AFStrainTest.STRAIN_ANCHOR.get());
    }

    /** AF's own relay: carries the line past, and is not in the tag. */
    private static void placeRelay(GameTestHelper helper, BlockPos pos, Direction facing)
    {
        place(helper, pos, facing, AFBlocks.CONNECTOR_AF_RELAY.get());
    }

    /**
     * IE's connector facing points INTO the block it is mounted on, so the support
     * goes on that side. Placed with setBlock rather than through the block item
     * because the facing is the only thing placement would have decided.
     */
    private static void place(GameTestHelper helper, BlockPos pos, Direction facing, Block connector)
    {
        helper.setBlock(pos.relative(facing), Blocks.STONE.defaultBlockState());
        BlockState state = connector.defaultBlockState().setValue(IEProperties.FACING_ALL, facing);
        helper.setBlock(pos, state);
    }

    private static void assertBothEndsAreAnchors(GameTestHelper helper, BlockPos west, BlockPos east)
    {
        helper.assertTrue(StrainSpans.isAnchor(helper.getLevel(), helper.absolutePos(west))
                        &&StrainSpans.isAnchor(helper.getLevel(), helper.absolutePos(east)),
                "harness broken: one of the two ends is not a strain anchor, so the doubling was"
                        +" never on offer and a result either way means nothing");
    }

    // ---- driving the coil ------------------------------------------------

    private static StrainSpanCoilItem coil(GameTestHelper helper)
    {
        helper.assertTrue(AlternatingFlux.AF_WIRE_COIL.get() instanceof StrainSpanCoilItem,
                "wirecoil_af is not a StrainSpanCoilItem: the rule is not on the item at all");
        return (StrainSpanCoilItem)AlternatingFlux.AF_WIRE_COIL.get();
    }

    private static ItemStack coilStack(GameTestHelper helper)
    {
        return new ItemStack(coil(helper), 4);
    }

    /**
     * The two clicks that make a wire, in order, through the item's own useOn — the
     * only path a coil is ever reached by. One mock player carries the stack across
     * both clicks because the link from the first is stored ON the stack.
     *
     * Survival, not creative: IE only calls {@code coil.consumeWire} when the player
     * is not in creative, so a creative mock would skip a branch every real player
     * takes.
     */
    private static void runCoil(GameTestHelper helper, ItemStack coil, BlockPos from, BlockPos to)
    {
        Player player = helper.makeMockSurvivalPlayer();
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

    // ---- reading the result ----------------------------------------------

    private static ConnectionPoint cp(GameTestHelper helper, BlockPos rel)
    {
        return new ConnectionPoint(helper.absolutePos(rel), 0);
    }

    private static Connection find(LocalWireNetwork local, ConnectionPoint a, ConnectionPoint b)
    {
        for(Connection c : local.getConnections(a))
            if(!c.isInternal()&&c.getOtherEnd(a).equals(b))
                return c;
        return null;
    }

    /** The wire between two connectors as the world actually holds it, or null. */
    private static Connection connection(GameTestHelper helper, BlockPos a, BlockPos b)
    {
        GlobalWireNetwork global = GlobalWireNetwork.getNetwork(helper.getLevel());
        ConnectionPoint cpA = cp(helper, a);
        LocalWireNetwork local = global.getNullableLocalNet(cpA);
        if(local==null)
            return null;
        return find(local, cpA, cp(helper, b));
    }
}
