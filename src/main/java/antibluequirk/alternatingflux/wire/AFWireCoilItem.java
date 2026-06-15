package antibluequirk.alternatingflux.wire;

import blusunrize.immersiveengineering.api.wires.IWireCoil;
import blusunrize.immersiveengineering.api.wires.WireType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * AF wire coil item. IE 1.16.5's WireCoilItem extends IEBaseItem which hard-codes
 * the registry name to immersiveengineering:&lt;name&gt; in its constructor, making it
 * impossible to use in our DeferredRegister under a different namespace.
 *
 * We create our own item that extends Item directly and implements IWireCoil,
 * which only requires getWireType(ItemStack). This lets DeferredRegister assign
 * the correct alternatingflux:wirecoil_af registry name.
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
}
