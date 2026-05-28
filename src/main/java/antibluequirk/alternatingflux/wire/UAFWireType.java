package antibluequirk.alternatingflux.wire;

import antibluequirk.alternatingflux.AlternatingFlux;
import antibluequirk.alternatingflux.Config;
import blusunrize.immersiveengineering.api.wires.Connection;
import blusunrize.immersiveengineering.api.wires.WireApi;
import blusunrize.immersiveengineering.api.wires.WireType;
import blusunrize.immersiveengineering.api.wires.localhandlers.EnergyTransferHandler.IEnergyWire;
import com.google.common.collect.ImmutableList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * Ultra High Alternating Flux (UAF) — the higher tier above AF.
 *
 * Structurally identical to {@link AFWireType}; it is its own registered wire
 * type with its own category ("UAF") so it forms an independent network and
 * connects only to UAF relays/transformers. The original AF tier is untouched.
 *
 * Tuned values (config-exposed in {@link Config}):
 *   - transfer rate : 524288 IF/t  (16x modern HV / 4x AF)
 *   - max length    : 96 blocks    (same span as AF)
 *   - loss ratio    : 0.0005       (same low-loss profile as AF)
 *   - colour        : 0x8b3fd6     (purple)
 *
 * Non-shocking, matching AF's observed behaviour.
 */
public class UAFWireType extends WireType implements IEnergyWire
{
    public static final String UAF_CATEGORY = "UAF";
    public static UAFWireType UAF;

    public static void init()
    {
        UAF = new UAFWireType();
    }

    public UAFWireType()
    {
        super();
        WireApi.registerWireType(this);
    }

    @Override
    public String getUniqueName()
    {
        return "UAF";
    }

    @Override
    public int getColour(Connection connection)
    {
        return Config.SERVER.uafWireColour.get();
    }

    @Override
    public double getSlack()
    {
        return 1.003;
    }

    @Override
    public int getMaxLength()
    {
        return Config.SERVER.uafMaxLength.get();
    }

    @Override
    public ItemStack getWireCoil(Connection con)
    {
        return new ItemStack(AlternatingFlux.UAF_WIRE_COIL.get());
    }

    @Override
    public double getRenderDiameter()
    {
        return 0.078125;
    }

    @Nonnull
    @Override
    public String getCategory()
    {
        return UAF_CATEGORY;
    }

    // --- IEnergyWire -------------------------------------------------------

    @Override
    public int getTransferRate()
    {
        return Config.SERVER.uafTransferRate.get();
    }

    @Override
    public double getBasicLossRate(Connection c)
    {
        return Config.SERVER.uafLossRatio.get() * c.getLength() / getMaxLength();
    }

    @Override
    public double getLossRate(Connection c, int transferred)
    {
        return 0;
    }

    // --- ILocalHandlerProvider --------------------------------------------

    @Override
    public Collection<ResourceLocation> getRequestedHandlers()
    {
        return ImmutableList.of();
    }
}
