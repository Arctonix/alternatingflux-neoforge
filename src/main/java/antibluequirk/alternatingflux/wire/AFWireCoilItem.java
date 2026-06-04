package antibluequirk.alternatingflux.wire;

import antibluequirk.alternatingflux.AlternatingFlux;
import blusunrize.immersiveengineering.api.wires.WireType;
import blusunrize.immersiveengineering.common.items.WireCoilItem;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * IE's 1.19.2 WireCoilItem pins itself to IE's creative tab in its only
 * constructor (pre-1.19.3, the tab is baked into Item.Properties), so this
 * subclass reroutes creative-tab display to the AF tab by overriding
 * fillItemCategory — the canonical pre-1.19.3 hook for tab contents.
 */
public class AFWireCoilItem extends WireCoilItem
{
    public AFWireCoilItem(WireType type)
    {
        super(type);
    }

    @Override
    public void fillItemCategory(CreativeModeTab tab, NonNullList<ItemStack> items)
    {
        if(tab==AlternatingFlux.TAB||tab==CreativeModeTab.TAB_SEARCH)
            items.add(new ItemStack(this));
    }
}
