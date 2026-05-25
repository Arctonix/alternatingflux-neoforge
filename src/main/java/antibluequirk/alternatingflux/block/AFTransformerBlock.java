package antibluequirk.alternatingflux.block;

import blusunrize.immersiveengineering.api.IEProperties;
import blusunrize.immersiveengineering.common.blocks.generic.ConnectorBlock;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * AF Transformer block. Mirrors IE's TransformerHVBlock: a 3-tall multiblock
 * (base + two slave blocks above), horizontal-facing, mirrorable, waterloggable.
 * ConnectorBlock handles the multiblock placement/break logic; we only declare
 * the same blockstate properties and the same placement footprint.
 */
public class AFTransformerBlock extends ConnectorBlock<AFTransformerBlockEntity>
{
    public AFTransformerBlock(Properties props)
    {
        super(props, AFBlocks.TRANSFORMER_AF_BE);
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder);
        builder.add(IEProperties.FACING_HORIZONTAL, IEProperties.MULTIBLOCKSLAVE,
                IEProperties.MIRRORED, BlockStateProperties.WATERLOGGED);
    }

    @Override
    public boolean canIEBlockBePlaced(BlockState newState, BlockPlaceContext context)
    {
        // 3-tall: needs the clicked block plus the two above it free.
        return areAllReplaceable(context.getClickedPos(), context.getClickedPos().above(2), context);
    }
}
