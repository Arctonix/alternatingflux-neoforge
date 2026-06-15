package antibluequirk.alternatingflux.block;

import blusunrize.immersiveengineering.api.wires.WireType;
import blusunrize.immersiveengineering.common.blocks.metal.TransformerTileEntity;
import com.google.common.collect.ImmutableSet;

/**
 * AF Transformer tile entity — the HV &lt;-&gt; AF step-down.
 *
 * In IE 1.16.5, the class is TransformerTileEntity (not TransformerBlockEntity)
 * and the constructor takes only the TileEntityType — no BlockPos/BlockState
 * (those come from the type chain in the 1.16.5 tile entity system).
 *
 * canAttach() in the abstract parent enforces exactly one higher + one lower
 * wire on opposite sides, so this naturally accepts AF on one side and HV on
 * the other and rejects two of the same. Transfer rate is min(leftRate, rightRate)
 * automatically. AF converts to/from Flux at 1:1 — the transformer just moves
 * energy between the two connection points, it doesn't scale it.
 */
public class AFTransformerBlockEntity extends TransformerTileEntity
{
    public AFTransformerBlockEntity()
    {
        super(AFBlocks.TRANSFORMER_AF_BE.get());
        // Low side accepts HV; high side is AF (via getHigherWiretype()).
        acceptableLowerWires = ImmutableSet.of(WireType.HV_CATEGORY);
    }

    @Override
    public String getHigherWiretype()
    {
        return AFBlocks.AF_VOLTAGE; // "AF"
    }

    // Offsets: match HV transformer's geometry so wires attach at the right height.
    // TransformerTileEntity's base getLowerOffset()=0.5 / getHigherOffset()=0.5625 are
    // the MV positions. IE's HV transformer anchors its higher (HV) wire at 0.75 and its
    // lower (MV) wire at the base higher offset 0.5625 (via super.getHigherOffset()).
    // We mirror that exactly: AF (high) at 0.75, HV (low) at 0.5625.
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
