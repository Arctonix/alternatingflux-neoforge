package antibluequirk.alternatingflux.block;

import antibluequirk.alternatingflux.AlternatingFlux;
import blusunrize.immersiveengineering.common.blocks.metal.TransformerBlockItem;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * IE's 1.19.2 TransformerBlockItem pins itself to IE's creative tab in its only
 * constructor, so this subclass reroutes creative-tab display to the AF tab
 * (same trick as {@link antibluequirk.alternatingflux.wire.AFWireCoilItem}).
 * It must stay a TransformerBlockItem: its getPlacementState override is what
 * swaps in IE's post transformer when clicked on an attachable post.
 */
public class AFTransformerBlockItem extends TransformerBlockItem
{
    public AFTransformerBlockItem(Block b)
    {
        super(b);
    }

    @Override
    public void fillItemCategory(CreativeModeTab tab, NonNullList<ItemStack> items)
    {
        if(tab==AlternatingFlux.TAB||tab==CreativeModeTab.TAB_SEARCH)
            items.add(new ItemStack(this));
    }
}
