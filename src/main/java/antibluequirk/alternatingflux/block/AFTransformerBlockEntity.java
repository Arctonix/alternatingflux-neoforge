package antibluequirk.alternatingflux.block;

import blusunrize.immersiveengineering.api.wires.WireType;
import blusunrize.immersiveengineering.common.blocks.metal.TransformerBlockEntity;
import com.google.common.collect.ImmutableSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * AF Transformer block entity — the HV <-> AF step-down.
 *
 * IE 9.x's AbstractTransformerBlockEntity is properly abstract and designed
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
    // We mirror TransformerHVBlockEntity exactly: the low side sits at the base
    // transformer's higher offset (super.getHigherOffset() == .5625F), and the high
    // side is .75F. Calling super (not the virtual getHigherOffset()) is the whole
    // point — our own getHigherOffset is .75F, so a virtual self-call would pull the
    // low-side wire up to the AF height instead of the HV-relay height it must match.
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
