package antibluequirk.alternatingflux.block;

import antibluequirk.alternatingflux.AlternatingFlux;
import blusunrize.immersiveengineering.api.IEProperties;
import blusunrize.immersiveengineering.common.blocks.BlockItemIE;
import blusunrize.immersiveengineering.common.blocks.generic.ConnectorBlock;
import blusunrize.immersiveengineering.common.blocks.metal.EnergyConnectorTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.RegistryObject;

/**
 * AF Wire Relay block — an IE energy connector for the AF wire tier.
 *
 * Mirrors IE's BasicConnectorBlock (FACING_ALL + WATERLOGGED blockstate) but extends
 * ConnectorBlock directly so we can pass a custom BlockItem factory: IE 1.16.5
 * auto-creates the connector's BlockItem inside the IEBaseBlock constructor, and we want
 * it as a BlockItemIE in AF's creative tab — BasicConnectorBlock's only ctor would create
 * the item in IE's tab. (BasicConnectorBlock adds no behaviour beyond this blockstate.)
 *
 * createRegistryName() is overridden to the alternatingflux: namespace. IEBaseBlock
 * hardcodes immersiveengineering: there, but it reads the public {@code name} field — which
 * is already set when the super-constructor invokes createRegistryName() — so the override
 * re-namespaces both the block and its auto-created BlockItem to match our assets/data/lang.
 * IE then registers them from IEContent's lists during its RegistryEvents; see {@link AFBlocks}.
 */
public class AFRelayBlock extends ConnectorBlock<EnergyConnectorTileEntity>
{
    public AFRelayBlock(String name, RegistryObject<TileEntityType<EnergyConnectorTileEntity>> teType)
    {
        super(name, teType,
                (block, props) -> new BlockItemIE(block, new Item.Properties().tab(AlternatingFlux.TAB)));
    }

    @Override
    public ResourceLocation createRegistryName()
    {
        return AlternatingFlux.rl(this.name);
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder)
    {
        builder.add(IEProperties.FACING_ALL, BlockStateProperties.WATERLOGGED);
    }
}
