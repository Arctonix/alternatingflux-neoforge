package antibluequirk.alternatingflux.wire;

import blusunrize.immersiveengineering.api.wires.IWireCoil;
import blusunrize.immersiveengineering.api.wires.WireType;
import blusunrize.immersiveengineering.api.wires.utils.WirecoilUtils;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;

/**
 * AF wire coil item. IE 1.16.5's WireCoilItem extends IEBaseItem which hard-codes
 * the registry name to immersiveengineering:&lt;name&gt; in its constructor, making it
 * impossible to use in our DeferredRegister under a different namespace.
 *
 * We create our own item that extends Item directly and implements IWireCoil,
 * which only requires getWireType(ItemStack). This lets DeferredRegister assign
 * the correct alternatingflux:wirecoil_af registry name.
 *
 * Because we do NOT extend IE's WireCoilItem, we must replicate its connection
 * interaction ourselves: override useOn to delegate to WirecoilUtils.doCoilUse (the
 * exact call IE's WireCoilItem.useOn makes). Without this, right-clicking the coil on a
 * connector does nothing and no wire can be placed — IWireCoil alone is just a marker
 * that supplies getWireType; it carries no interaction behaviour.
 */
public class AFWireCoilItem extends Item implements IWireCoil
{
    private final WireType wireType;

    public AFWireCoilItem(WireType wireType, Properties props)
    {
        super(props);
        this.wireType = wireType;
    }

    @Override
    public WireType getWireType(ItemStack stack)
    {
        return wireType;
    }

    /**
     * Start/finish a wire connection. Mirrors IE 1.16.5 WireCoilItem.useOn exactly:
     * delegate to WirecoilUtils.doCoilUse with the raw click-location coordinates.
     */
    @Override
    public ActionResultType useOn(ItemUseContext context)
    {
        return WirecoilUtils.doCoilUse(this,
                context.getPlayer(),
                context.getLevel(),
                context.getClickedPos(),
                context.getHand(),
                context.getClickedFace(),
                (float) context.getClickLocation().x,
                (float) context.getClickLocation().y,
                (float) context.getClickLocation().z);
    }
}
