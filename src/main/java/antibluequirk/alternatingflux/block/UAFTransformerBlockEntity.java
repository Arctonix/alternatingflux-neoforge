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

    // Offsets: mirror IE's HV transformer geometry, the same way AFTransformerBlockEntity
    // does. TransformerBlockEntity's base getLowerOffset()=0.5 / getHigherOffset()=0.5625
    // are the MV positions; IE's HV unit anchors its higher wire at 0.75 and its lower wire
    // at the base higher offset 0.5625 (via super.getHigherOffset()). Both UAF transformer
    // .obj cups are the same height as the AF transformer's, so we use the same anchors for
    // both the HV<->UAF and AF<->UAF blocks: UAF (high) at 0.75, lower wire at 0.5625 — note
    // the super. call, a virtual getHigherOffset() here would wrongly return our 0.75.
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
