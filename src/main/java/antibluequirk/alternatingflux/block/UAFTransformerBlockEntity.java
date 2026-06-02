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

    @Override
    protected float getLowerOffset()
    {
        return getHigherOffset();
    }

    @Override
    protected float getHigherOffset()
    {
        return .75F;
    }
}
