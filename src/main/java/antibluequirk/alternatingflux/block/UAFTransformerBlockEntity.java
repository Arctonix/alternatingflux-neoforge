package antibluequirk.alternatingflux.block;

import blusunrize.immersiveengineering.common.blocks.metal.TransformerBlockEntity;
import com.google.common.collect.ImmutableSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * UAF Transformer block entity. High side is always UAF; the low side is supplied
 * at construction so the same class serves both transformers:
 *   - HV/UAF : low = "HV"
 *   - AF/UAF : low = "AF"
 *
 * canAttach() in TransformerBlockEntity enforces exactly one higher (UAF) + one
 * lower wire, so each block accepts UAF on one post and its configured lower
 * voltage on the other. Conversion is 1:1; transfer rate is min(left, right).
 */
public class UAFTransformerBlockEntity extends TransformerBlockEntity
{
    public UAFTransformerBlockEntity(BlockEntityType<? extends TransformerBlockEntity> type,
                                     String lowerWire, BlockPos pos, BlockState state)
    {
        super(type, pos, state);
        acceptableLowerWires = ImmutableSet.of(lowerWire);
    }

    @Override
    public String getHigherWiretype()
    {
        return AFBlocks.UAF_VOLTAGE; // "UAF"
    }

    // Mirror IE's TransformerHVBlockEntity: the low side (HV or AF) sits at the base
    // transformer's higher offset (super.getHigherOffset() == .5625F), matching the
    // lower-tier relay geometry, while the high side (UAF) is .75F. super (not the
    // virtual getHigherOffset()) is deliberate — our getHigherOffset is .75F, so a
    // virtual self-call would raise the low-side wire to the UAF height by mistake.
    @Override
    protected float getLowerOffset()
    {
        return super.getHigherOffset();
    }

    @Override
    protected float getHigherOffset()
    {
        return .75F;
    }
}
