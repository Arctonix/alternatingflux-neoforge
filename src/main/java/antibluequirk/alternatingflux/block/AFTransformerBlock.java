package antibluequirk.alternatingflux.block;

import antibluequirk.alternatingflux.AlternatingFlux;
import blusunrize.immersiveengineering.api.IEProperties;
import blusunrize.immersiveengineering.common.blocks.generic.ConnectorBlock;
import blusunrize.immersiveengineering.common.blocks.metal.TransformerBlockItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.ResourceLocation;

/**
 * AF Transformer block. Mirrors IE's TransformerHVBlock: a 3-tall multiblock
 * (base + two slave blocks above), horizontal-facing, mirrorable, waterloggable.
 * ConnectorBlock handles the multiblock placement/break logic; we only declare
 * the same blockstate properties and the same placement footprint.
 *
 * Registration follows IE 1.16.5's self-registering model (see {@link AFBlocks}): the
 * super-constructor sets the registry name and self-adds to IEContent's lists, so we pass
 * a custom item factory to get a {@link TransformerBlockItem} (required for the multiblock
 * to place) in AF's creative tab, and override createRegistryName() to the alternatingflux:
 * namespace (IEBaseBlock hardcodes immersiveengineering:; it reads the public {@code name}
 * field, already set when the super-constructor calls createRegistryName()).
 */
public class AFTransformerBlock extends ConnectorBlock<AFTransformerBlockEntity>
{
    public AFTransformerBlock(String name)
    {
        super(name, AFBlocks.TRANSFORMER_AF_BE,
                (block, props) -> new TransformerBlockItem(block, new Item.Properties().tab(AlternatingFlux.TAB)));
    }

    @Override
    public ResourceLocation createRegistryName()
    {
        return AlternatingFlux.rl(this.name);
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
