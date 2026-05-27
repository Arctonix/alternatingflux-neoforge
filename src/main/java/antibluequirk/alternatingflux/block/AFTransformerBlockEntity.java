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
    // We mirror TransformerHVBlockEntity exactly: getLowerOffset returns higher,
    // and higher is .75F. This makes AF (high) and HV (low) sit like a stock HV unit.
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
