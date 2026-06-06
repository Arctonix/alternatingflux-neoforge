package antibluequirk.alternatingflux.block;

import blusunrize.immersiveengineering.api.wires.WireType;
import blusunrize.immersiveengineering.common.blocks.metal.TransformerBlockEntity;
import com.google.common.collect.ImmutableSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * AF Transformer block entity — the HV <-> AF step-down.
 *
 * IE 10.x's AbstractTransformerBlockEntity is properly abstract and designed
 * for extension by voltage-tier subclasses (see TransformerHVBlockEntity for
 * the canonical pattern). We extend TransformerBlockEntity (the concrete
 * MV/LV transformer) the same way TransformerHVBlockEntity does, but set:
 *   - higher wire category = "AF"
 *   - acceptable lower wires = { "HV" }
 *
 * canAttach() in the abstract parent enforces exactly one higher + one lower
 * wire on opposite sides, so this naturally accepts AF on one side and HV on
 * the other and rejects two of the same. Transfer rate is min(leftRate, rightRate)
 * automatically. AF converts to/from Flux at 1:1 — the transformer just moves
 * energy between the two connection points, it doesn't scale it.
 */
public class AFTransformerBlockEntity extends TransformerBlockEntity
{
    public AFTransformerBlockEntity(BlockPos pos, BlockState state)
    {
        super(AFBlocks.TRANSFORMER_AF_BE.get(), pos, state);
        // Low side accepts HV; high side is AF (below).
        acceptableLowerWires = ImmutableSet.of(WireType.HV_CATEGORY);
    }

    @Override
    public String getHigherWiretype()
    {
        return AFBlocks.AF_VOLTAGE; // "AF"
    }

    // Offsets: match HV transformer's geometry so wires attach at the right height.
    // TransformerBlockEntity's base getLowerOffset()=0.5 / getHigherOffset()=0.5625 are
    // the MV positions. IE's HV transformer anchors its higher (HV) wire at 0.75 and its
    // lower (MV) wire at the base higher offset 0.5625 (via super.getHigherOffset()).
    // We mirror that exactly: AF (high) at 0.75, HV (low) at 0.5625 — note the super.
    // call, a virtual getHigherOffset() here would wrongly return our 0.75.
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
