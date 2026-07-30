package antibluequirk.alternatingflux.wire;

import blusunrize.immersiveengineering.api.IEApiDataComponents;
import blusunrize.immersiveengineering.api.TargetingInfo;
import blusunrize.immersiveengineering.api.wires.ConnectionPoint;
import blusunrize.immersiveengineering.api.wires.IImmersiveConnectable;
import blusunrize.immersiveengineering.api.wires.WireType;
import blusunrize.immersiveengineering.api.wires.utils.WireLink;
import blusunrize.immersiveengineering.common.items.WireCoilItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A wire coil that reaches twice as far when both ends of the span are strain
 * anchors — see {@link StrainSpans} for the rule and the tag behind it.
 *
 * <h2>How the length gets through to IE without a mixin</h2>
 * IE measures the span in one place, {@code WirecoilUtils#doCoilUse}:
 * <pre>
 *   int maxLengthSq = coil.getMaxLength(stack);   // asked of the COIL, stack only
 *   maxLengthSq *= maxLengthSq;
 *   ... else if(distanceSq &gt; maxLengthSq) -&gt; "tooFar"
 * </pre>
 * {@code getMaxLength} is handed the stack and nothing else, so an ordinary
 * override can never see the block under the cursor and can never answer the
 * question "are BOTH ends anchored?". But {@code Item#useOn} can: it has the
 * level, the clicked position and the stack, and it is the method that calls
 * doCoilUse — synchronously, on the same thread, immediately. So this class
 * decides in {@code useOn} and hands the answer down to {@code getMaxLength}
 * through a thread-scoped flag that exists only for the duration of that one
 * call, in a try/finally.
 *
 * It is deliberately NOT stored on the stack: a doubling written to the item
 * would outlive the click that justified it and would still be there when the
 * player next aimed at an ordinary relay.
 *
 * <h2>What is unchanged</h2>
 * Everything that is not a two-anchor span. With no stored link, with an
 * unanchored end, or with the tag empty, {@code getMaxLength} returns
 * {@code getWireType(stack).getMaxLength()} — the exact expression IWireCoil's
 * default returns — and {@code useOn} calls {@code super.useOn} with nothing
 * touched. There is no path by which an existing world's wires are measured
 * differently than they were before.
 */
public class StrainSpanCoilItem extends WireCoilItem
{
	/**
	 * Set for the duration of one {@code useOn} call and removed in a finally.
	 * Thread-scoped because a single-player client runs its own integrated server
	 * on a second thread and both of them push clicks through this item.
	 */
	private static final ThreadLocal<Boolean> STRAIN_SPAN_THIS_CLICK = ThreadLocal.withInitial(() -> Boolean.FALSE);

	public StrainSpanCoilItem(WireType type)
	{
		super(type);
	}

	/**
	 * The ordinary reach, doubled only while {@link #useOn} has established that
	 * this very click lands on an anchor at both ends. IE squares it itself, so
	 * this is a plain block distance.
	 */
	@Override
	public int getMaxLength(ItemStack stack)
	{
		// Not super.getMaxLength: writing the wire's own expression out makes it
		// obvious that the undoubled answer is byte-for-byte IWireCoil's default.
		int ordinary = getWireType(stack).getMaxLength();
		return STRAIN_SPAN_THIS_CLICK.get()?ordinary*StrainSpans.SPAN_MULTIPLIER: ordinary;
	}

	/** The doubled reach of this coil's wire, for tooltips and the HUD. */
	public int getStrainSpanLength(ItemStack stack)
	{
		return getWireType(stack).getMaxLength()*StrainSpans.SPAN_MULTIPLIER;
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx)
	{
		Level level = ctx.getLevel();
		ItemStack stack = ctx.getItemInHand();
		WireType wire = getWireType(stack);
		TargetingInfo target = StrainSpans.targeting(ctx);

		if(StrainSpans.bothEndsAnchored(level, stack, wire, ctx.getClickedPos(), target))
		{
			STRAIN_SPAN_THIS_CLICK.set(Boolean.TRUE);
			try
			{
				return super.useOn(ctx);
			}
			finally
			{
				// Finally, not a plain unset: doCoilUse can throw, and a flag left
				// standing would hand the doubled reach to the player's next click.
				STRAIN_SPAN_THIS_CLICK.remove();
			}
		}

		Component refusal = strainRefusal(ctx, wire, target);
		Player player = ctx.getPlayer();
		if(refusal==null||player==null)
			return super.useOn(ctx);

		if(!level.isClientSide)
		{
			player.displayClientMessage(refusal.copy().withStyle(ChatFormatting.RED), true);
			// IE drops the stored link on every second click, taken or refused
			// (the unconditional remove at the end of that branch). Match it, so a
			// refused click resets the coil like any other miss instead of leaving
			// a half-made link the player cannot see.
			stack.remove(IEApiDataComponents.WIRE_LINK);
		}
		// The same result IE returns down this path: the whole connectable branch
		// of doCoilUse ends in SUCCESS, refusal messages included.
		return InteractionResult.SUCCESS;
	}

	/** The rule's own refusal for this click, or null to let IE answer. */
	@Nullable
	public Component strainRefusal(UseOnContext ctx)
	{
		return strainRefusal(ctx, getWireType(ctx.getItemInHand()), StrainSpans.targeting(ctx));
	}

	/**
	 * IE's word for a span it will not take is "Too far" — true, and useless to a
	 * player who has just watched this same coil make a longer one somewhere else.
	 * Where the ONLY thing standing between the click and a wire is the missing
	 * anchor, say that instead.
	 *
	 * Deliberately narrow. It speaks only when the span is longer than the
	 * ordinary reach AND within the doubled one: shorter and IE connects the wire,
	 * longer and no anchor would have helped, so IE's own "Too far" is the honest
	 * answer and is left alone. It also replicates IE's earlier tests — not a
	 * connector, wrong cable, wrong dimension, same connection — because each of
	 * those has a better message than ours and IE reaches them first.
	 *
	 * Returns null to mean "IE should answer this click", never to mean "allow".
	 * A decision only — the message and the cleared link are {@link #useOn}'s job,
	 * which is what lets a gametest ask what a click would be told without having
	 * to read chat.
	 */
	@Nullable
	private Component strainRefusal(UseOnContext ctx, WireType wire, TargetingInfo target)
	{
		Player player = ctx.getPlayer();
		Level level = ctx.getLevel();
		ItemStack stack = ctx.getItemInHand();
		if(player==null)
			return null;

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
				||!canConnectCable(stack, level.getBlockEntity(masterPos)))
			return null; // IE: "wrongCable", which is the more useful thing to hear

		BlockPos farPos = stored.cp().position();
		if(farPos.equals(masterPos))
			return null; // IE: "sameConnection"

		int ordinary = wire.getMaxLength();
		int doubled = ordinary*StrainSpans.SPAN_MULTIPLIER;
		// IE's own measurement, to the integer: WirecoilUtils ceils the squared
		// distance and compares it against the squared reach.
		int distanceSq = (int)Math.ceil(farPos.distSqr(masterPos));
		if(distanceSq <= ordinary*ordinary||distanceSq > doubled*doubled)
			return null; // it fits, or nothing would have made it fit

		return Component.translatable(StrainSpans.CHAT_NEEDS_BOTH_ANCHORS, ordinary, doubled);
	}

	/**
	 * Both reaches, stated on the item, because the rule is invisible otherwise —
	 * a player holding this coil has no other way to learn that the number changes
	 * with what is on the far end.
	 */
	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag)
	{
		list.add(Component.translatable(
						StrainSpans.TOOLTIP_SPAN, getWireType(stack).getMaxLength(), getStrainSpanLength(stack))
				.withStyle(ChatFormatting.GRAY));
		// Last, so IE's dynamic "attached to X, Y, Z" line reads as current state
		// rather than as part of the description.
		super.appendHoverText(stack, context, list, flag);
	}
}
