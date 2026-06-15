package antibluequirk.alternatingflux.block;

import blusunrize.immersiveengineering.api.IEProperties;
import blusunrize.immersiveengineering.common.blocks.generic.ConnectorBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;

/**
 * AF Transformer block. Mirrors IE's TransformerHVBlock: a 3-tall multiblock
 * (base + two slave blocks above), horizontal-facing, mirrorable, waterloggable.
 * ConnectorBlock handles the multiblock placement/break logic; we only declare
 * the same blockstate properties and the same placement footprint.
 *
 * In IE 1.16.5, ConnectorBlock(String name, RegistryObject&lt;TileEntityType&gt;) takes
 * a String name as first arg (used for block registration). No Properties arg.
 */
public class AFTransformerBlock extends ConnectorBlock<AFTransformerBlockEntity>
{
    public AFTransformerBlock(String name)
    {
        super(name, AFBlocks.TRANSFORMER_AF_BE);
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder);
        builder.add(IEProperties.FACING_HORIZONTAL, IEProperties.MULTIBLOCKSLAVE,
                IEProperties.MIRRORED, BlockStateProperties.WATERLOGGED);
    }

    @Override
    public boolean canIEBlockBePlaced(BlockState newState, BlockItemUseContext context)
    {
        // 3-tall: needs the clicked block plus the two above it free.
        return areAllReplaceable(context.getClickedPos(), context.getClickedPos().above(2), context);
    }
}
