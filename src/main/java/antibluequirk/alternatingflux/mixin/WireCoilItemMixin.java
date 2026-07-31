package antibluequirk.alternatingflux.mixin;

import antibluequirk.alternatingflux.wire.StrainSpans;
import blusunrize.immersiveengineering.api.wires.IWireCoil;
import blusunrize.immersiveengineering.api.wires.WireType;
import blusunrize.immersiveengineering.api.wires.utils.WireLink;
import blusunrize.immersiveengineering.common.items.WireCoilItem;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * The strain-span rule, applied where Immersive Engineering measures a span.
 *
 * <h2>Why this is a mixin and not an item</h2>
 * The rule used to live on a coil subclass of ours, which decided in
 * {@code Item#useOn} — the one place with both ends of the span in scope — and
 * handed the answer down to {@code getMaxLength}. That worked, and it covered
 * exactly two coils in the world: the two we register. IE registers copper,
 * electrum and steel itself, so an ordinary IE coil got nothing, and a player
 * holding one between two dead-ends saw no difference at all.
 *
 * There is exactly ONE length check in all of IE, and every coil in the game goes
 * through it. {@code IEContent} binds {@code WirecoilUtils.COIL_USE} to
 * {@code WireCoilItem::doCoilUse}, and that static method is what
 * {@code WireCoilItem#useOn} calls; a third-party coil that implements
 * {@code IWireCoil} reaches the same method the same way. Inside it, on the second
 * click and server side only:
 * <pre>
 *   int distanceSq = Mth.ceil(storedLink.cp().position().distSqr(masterPos));
 *   int maxLengthSq = coil.getMaxLength(stack);
 *   maxLengthSq *= maxLengthSq;
 *   ...
 *   else if(distanceSq &gt; maxLengthSq) -&gt; "chat.immersiveengineering.warning.tooFar"
 * </pre>
 * At that {@code getMaxLength} call both ends are live locals — {@code masterPos}
 * for the end under the cursor, {@code storedLink} for the end on the stack — so
 * one hook there answers for every coil at once. That is the whole reason to
 * prefer a mixin to five more subclasses we do not own the registrations for.
 *
 * {@code WirecoilUtils.COIL_USE} cannot simply be re-bound from outside:
 * {@code SetRestrictedField#setValue} checks
 * {@code "immersiveengineering".equals(currentMod)}. There is no API door here.
 *
 * <h2>★ Failing soft, and the one thing that actually makes it soft</h2>
 * A mixin that cannot apply is normally FATAL AT LOAD, and this one is pinned to a
 * method body inside a mod we do not control. An IE refactor must cost this
 * feature, never the player's game.
 *
 * {@code require = 0} on every handler is what buys that, and it is not a
 * belt-and-braces duplicate of {@code "required": false} in the config — it is the
 * load-bearing one. This was measured, not reasoned about: with the target method
 * renamed and {@code require} left at 1, Mixin does exactly what its source says it
 * will and treats a non-required config as {@code WARN} rather than {@code ERROR}…
 * and the game still dies, because by then MixinExtras has already merged the
 * sugar-wrapped handler into {@code WireCoilItem}. The dropped mixin leaves behind
 * {@code modifyExpressionValue$…$alternatingflux$strainSpanReach(I)I}, whose body
 * reads locals the truncated signature does not have:
 * <pre>
 *   java.lang.VerifyError: Bad local variable type
 *     Location: …WireCoilItem.modifyExpressionValue$zzg000$alternatingflux$strainSpanReach(I)I @0
 *     Reason: Type top (current frame, locals[1]) is not assignable to reference type
 *   → Immersive Engineering (immersiveengineering) has failed to load correctly
 * </pre>
 * With {@code require = 0} no injector is created for a target that is not there,
 * so nothing is merged and nothing is corrupt: the server boots, the bare suite
 * passes in full, and only the doubling is gone. {@code "required": false} is kept
 * as the second line of defence for the failures that surface later than parsing.
 *
 * The cost of that choice, stated plainly: a {@code require = 0} failure is SILENT.
 * Mixin logs nothing, because from its point of view nothing went wrong. A future
 * IE that moves this method will take strain spans away without a word in the log,
 * and {@code StrainSpanGameTests} in the anchored run is the only thing that will
 * say so. That is the right trade — a quiet lost feature beats a loud dead game —
 * but it is a trade.
 *
 * The worst case on an IE update is that long spans stop reaching double.
 *
 * <h2>What an empty tag changes: nothing</h2>
 * Every handler asks {@link StrainSpans#anchorsExist()} first and returns its
 * argument untouched when the answer is no. With no add-on installed the tag is
 * empty, so IE gets back its own number and its own message, no block is read out
 * of the world, and no coil in the game says anything it did not say in 1.0.5.
 */
@Mixin(WireCoilItem.class)
public abstract class WireCoilItemMixin
{
	@Shadow
	public abstract WireType getWireType(ItemStack stack);

	/**
	 * A. THE RULE. Double the reach IE is about to square, and only when both ends
	 * of this span are strain anchors.
	 *
	 * Returns a plain block distance because IE squares it on the very next
	 * instruction — {@code maxLengthSq *= maxLengthSq} — so doubling here is
	 * quadrupling the area, which is the point.
	 *
	 * The two locals are IE's own: {@code masterPos} is the connector the wire
	 * would land on for the click being made (ordinal 1 of the {@code BlockPos}
	 * locals in scope, after the {@code pos} argument), and {@code storedLink} is
	 * the far end recorded by the first click (the only {@code WireLink} in scope).
	 * They are the very two values the {@code distSqr} on the line above measures
	 * between, so the rule and the measurement cannot disagree about which blocks
	 * the span runs between.
	 */
	@ModifyExpressionValue(
			method = "doCoilUse",
			at = @At(
					value = "INVOKE",
					target = "Lblusunrize/immersiveengineering/api/wires/IWireCoil;getMaxLength(Lnet/minecraft/world/item/ItemStack;)I"
			),
			require = 0
	)
	private static int alternatingflux$strainSpanReach(
			int original,
			@Local(argsOnly = true) Level level,
			@Local(ordinal = 1) BlockPos masterPos,
			@Local WireLink storedLink
	)
	{
		return StrainSpans.reachFor(level, masterPos, storedLink, original);
	}

	/**
	 * B. THE REFUSAL. "Too far" is true and teaches nothing to a player who just
	 * made a longer span with the same coil somewhere else. Where the only thing
	 * missing is an anchor, say so; everywhere else leave IE's message alone.
	 *
	 * Sliced from the {@code tooFar} string constant so it lands on that branch's
	 * {@code displayClientMessage} and not on one of the seven others in the same
	 * method. {@code distanceSq} is the first of the two ints in scope there, the
	 * second being the already-squared {@code maxLengthSq}; the ordinary reach is
	 * asked of the coil rather than square-rooted back out of it.
	 */
	@ModifyArg(
			method = "doCoilUse",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/player/Player;displayClientMessage(Lnet/minecraft/network/chat/Component;Z)V",
					ordinal = 0
			),
			slice = @Slice(
					from = @At(
							value = "CONSTANT",
							args = "stringValue=chat.immersiveengineering.warning.tooFar"
					)
			),
			index = 0,
			require = 0
	)
	private static Component alternatingflux$explainTooFar(
			Component original,
			@Local(argsOnly = true) Level level,
			@Local(ordinal = 1) BlockPos masterPos,
			@Local WireLink storedLink,
			@Local ItemStack stack,
			@Local(ordinal = 0) int distanceSq
	)
	{
		if(!(stack.getItem() instanceof IWireCoil coil))
			return original;
		Component reason = StrainSpans.tooFarReason(
				level, coil.getMaxLength(stack), masterPos, storedLink, distanceSq);
		return reason!=null?reason: original;
	}

	/**
	 * C. THE ITEM SAYS SO. The rule is invisible otherwise — a player holding a
	 * coil has no other way to learn that its reach changes with what is on the far
	 * end, and now that every coil in the game carries the rule, every coil has to
	 * be able to say it.
	 *
	 * At HEAD, so the line sits above IE's flavour text and above its dynamic
	 * "attached to X, Y, Z" line, which reads as current state rather than as part
	 * of the description. That is the order the AF coil printed before the rule
	 * moved here.
	 *
	 * Stated only where the rule is reachable. With no strain hardware installed
	 * the second number is not a longer reach a player could go and earn; it is a
	 * distance nothing in their game can produce, quoted beside the name of a block
	 * they cannot craft.
	 */
	@Inject(method = "appendHoverText", at = @At("HEAD"), require = 0)
	private void alternatingflux$strainSpanTooltip(
			ItemStack stack, Item.TooltipContext context, List<Component> list, TooltipFlag flag, CallbackInfo ci
	)
	{
		if(!StrainSpans.anchorsExist())
			return;
		int ordinary = getWireType(stack).getMaxLength();
		list.add(Component
				.translatable(StrainSpans.TOOLTIP_SPAN, ordinary, StrainSpans.strainSpanLength(ordinary))
				.withStyle(ChatFormatting.GRAY));
	}
}
