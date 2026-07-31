package antibluequirk.alternatingflux.wire;

import blusunrize.immersiveengineering.api.wires.WireType;
import blusunrize.immersiveengineering.common.items.WireCoilItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

import javax.annotation.Nullable;

/**
 * A plain IE wire coil. The strain-span rule is no longer here.
 *
 * <h2>What happened to it</h2>
 * This class used to BE the rule: it overrode {@code useOn} to decide, with both
 * ends of the span in scope, whether the coil should reach twice as far, and
 * {@code getMaxLength} to hand that answer to IE. It worked — and it reached
 * exactly the two coils Alternating Flux and AF: Interchange register, because a
 * subclass can only ever apply to registrations you own. IE registers copper,
 * electrum and steel itself, so a player holding an ordinary IE coil between two
 * dead-ends got nothing, which is precisely the bug that was reported.
 *
 * The rule now lives in {@code WireCoilItemMixin}, on the single length check every
 * coil in the game funnels through. That covers this one too, so keeping an
 * override here as well would not be belt and braces — it would be a bug: the
 * subclass would double the reach and the mixin would double it again.
 *
 * <h2>Why the class is still here</h2>
 * Not for behaviour. It carries none, and {@code wirecoil_af} would be identical if
 * it were registered as a bare {@link WireCoilItem}. It is kept because Alternating
 * Flux is a shipped library and AF: Interchange already registers {@code
 * wirecoil_uaf} through this constructor; deleting a published class to gain
 * nothing is not a trade worth making. Register new coils as {@link WireCoilItem}.
 *
 * The two methods below are queries, not mechanism — the same answers the rule
 * gives, asked from the outside — and both delegate to {@link StrainSpans}, which
 * is the one place the rule is written down.
 *
 * @deprecated no longer carries the strain-span rule; use {@link WireCoilItem}.
 */
@Deprecated
public class StrainSpanCoilItem extends WireCoilItem
{
	public StrainSpanCoilItem(WireType type)
	{
		super(type);
	}

	/** The doubled reach of this coil's wire, for tooltips and the HUD. */
	public int getStrainSpanLength(ItemStack stack)
	{
		return StrainSpans.strainSpanLength(getWireType(stack).getMaxLength());
	}

	/**
	 * What this click would be told instead of IE's bare "Too far", or null when
	 * IE's own message is the honest one.
	 *
	 * A decision only, and one that answers for any coil — see
	 * {@link StrainSpans#refusalFor}.
	 */
	@Nullable
	public Component strainRefusal(UseOnContext ctx)
	{
		return StrainSpans.refusalFor(ctx);
	}
}
