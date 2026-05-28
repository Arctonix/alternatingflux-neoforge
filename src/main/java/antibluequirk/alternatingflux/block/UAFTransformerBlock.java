package antibluequirk.alternatingflux.block;

import blusunrize.immersiveengineering.api.IEProperties;
import blusunrize.immersiveengineering.common.blocks.generic.ConnectorBlock;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.function.Supplier;

/**
 * UAF Transformer block. Same structure as {@link AFTransformerBlock} — a 3-tall
 * horizontal-facing mirrorable multiblock — but the BE-type is supplied so
 * one class backs both the HV/UAF and AF/UAF transformers.
 */
public class UAFTransformerBlock extends ConnectorBlock<UAFTransformerBlockEntity>
{
    public UAFTransformerBlock(
            Properties props,
            Supplier<BlockEntityType<UAFTransformerBlockEntity>> beType)
    {
        super(props, beType);
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
        return areAllReplaceable(context.getClickedPos(), context.getClickedPos().above(2), context);
    }
}
