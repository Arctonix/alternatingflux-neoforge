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
import blusunrize.immersiveengineering.api.wires.utils.WireLink;
import blusunrize.immersiveengineering.api.wires.utils.WirecoilUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * What Alternating Flux alone can be held to.
 *
 * AF declares the strain-anchor tag and applies the rule, but ships no hardware
 * that goes in the tag — the dead-ends live in add-ons, and AF is the base mod and
 * must not depend on them. So a bare AF install is the EMPTY-TAG case, and that is
 * exactly what this file pins: with nothing registered as an anchor, the AF coil
 * measures, refuses and connects precisely as it did before the rule existed.
 * That is the guarantee a shipped mod owes worlds that already exist, and this
 * line already has players on it.
 *
 * The doubling itself is proved in {@code StrainAnchorGameTests}, which runs in a
 * second, ANCHOR-POPULATED world — {@code ./gradlew runGameTestServer
 * -PstrainAnchors}. It cannot share this one: every test below depends on the tag
 * being empty, which is the point of them.
 */
@GameTestHolder(AlternatingFlux.MODID)
public class StrainSpanGameTests
{
    /**
     * An empty 104-long corridor: long enough to stand an AF span of 100 blocks in,
     * past AF's 96-block reach, with a support block outside each end. Forge
     * prefixes the template with the simple class name, so the file on disk is
     * data/alternatingflux/structures/strainspangametests.span_104x8x5.nbt.
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

    /**
     * (g) ★ AND IT SAYS NOTHING. A bare AF install must be unchanged in what the
     * player is TOLD, not only in which wires go up — this is a mod that has already
     * shipped, and a player with AF and IE and nothing else has no dead-end block in
     * their game at all.
     *
     * Both halves of (e) are checked here, on the same 100-block span (e) refuses:
     * the coil must hand that click back to IE so the player reads IE's own
     * "Too far", and the tooltip must not carry the two-reaches line. Either one
     * would have a bare install quoting a 192-block span it can never take and
     * naming hardware from a mod that is not installed.
     *
     * (e) could not see this: it asserted that no wire appeared and never asked what
     * the player was told.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void bareAfSaysNothingAboutStrain(GameTestHelper helper)
    {
        int span = 100;
        BlockPos west = relayPair(helper, span);
        BlockPos east = relayPos(span);

        helper.runAfterDelay(2, () -> {
            StrainSpanCoilItem coil = coil(helper);
            ItemStack stack = new ItemStack(coil, 4);

            // The register really is empty here, so the two assertions below are about
            // the empty-tag case and not about some other reason for silence.
            helper.assertTrue(!StrainSpans.anchorsExist(),
                    "a bare AF install reports strain hardware in "+StrainSpans.STRAIN_ANCHORS.location()
                            +"; this test no longer covers the case it is named for");

            // (1) The message. 100 blocks is inside the band the rule speaks in --
            // past AF's 96, within the doubled 192 -- so this is precisely the click
            // that used to be answered with "build a dead-end at both ends".
            helper.assertTrue(span > AFWireType.AF.getMaxLength()
                            &&span <= AFWireType.AF.getMaxLength()*StrainSpans.SPAN_MULTIPLIER,
                    "test is vacuous: "+span+" blocks is outside the band the rule speaks in");
            Player player = helper.makeMockSurvivalPlayer();
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            click(helper, player, west);
            helper.assertTrue(WirecoilUtils.hasWireLink(stack),
                    "harness broken: the first click stored no link, so there is no span to refuse");
            helper.assertTrue(coil.strainRefusal(context(helper, player, east))==null,
                    "with no strain hardware installed the AF coil still blames a missing dead-end;"
                            +" the player is sent looking for a block their game does not contain,"
                            +" in place of IE's own \"too far\"");

            // (2) The tooltip. Same claim, on the item rather than in chat.
            List<Component> lines = new ArrayList<>();
            coil.appendHoverText(new ItemStack(coil), helper.getLevel(), lines, TooltipFlag.NORMAL);
            for(Component line : lines)
                helper.assertTrue(!(line.getContents() instanceof TranslatableContents t
                                &&StrainSpans.TOOLTIP_SPAN.equals(t.getKey())),
                        "the AF coil's tooltip advertises a "
                                +coil.getStrainSpanLength(new ItemStack(coil))
                                +"-block span between dead-ends in a game with no dead-ends in it");
            helper.succeed();
        });
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
     * not an anchor. It is the loading that is under test.
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

        ItemStack stack = new ItemStack(coil(helper));
        WireLink.create(
                new ConnectionPoint(far, 0), level, BlockPos.ZERO,
                StrainSpans.targeting(Direction.UP, far, Vec3.atCenterOf(far))).writeToItem(stack);

        boolean anchored = StrainSpans.bothEndsAnchored(
                level, stack, AFWireType.AF, near,
                StrainSpans.targeting(Direction.UP, near, Vec3.atCenterOf(near)));
        helper.assertTrue(!anchored,
                "empty world a kilometre away reported as a strain anchor");
        helper.assertTrue(level.isLoaded(far),
                "the rule refused to look at a far end whose chunk was not loaded."
                        +" On a default server that is every span past 160 blocks -- the last"
                        +" stretch of the doubled reach -- refused with a message telling the"
                        +" player to build the dead-end they are standing at");
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
