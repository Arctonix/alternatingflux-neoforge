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
 * The Alternating Flux wire tier.
 *
 * Port of AntiBlueQuirk's 1.12 AFWireType onto IE 10.x's wire API (1.20.1 Forge).
 * Identical in shape to the 1.21.1 version we already shipped — the IEnergyWire
 * interface is unchanged between IE 10.x and 12.x. Loss formula matches IE's own
 * EnergyWire: lossRatio * length / maxLength.
 *
 * Tuned values (all config-exposed in {@link Config}):
 *   - transfer rate : 131072 IF/t  (4x modern HV's 32768)
 *   - max length    : 96 blocks
 *   - loss ratio    : 0.0005       (well below HV's 0.0008 over a longer span)
 *   - colour        : 0xf6866c     (original AF salmon)
 *
 * Non-shocking, matching the 1.12 original's observed behaviour (issue #12 in
 * the original repo — declared shocking but never actually shocked).
 */
public class AFWireType extends WireType implements IEnergyWire
{
    public static final String AF_CATEGORY = "AF";
    public static AFWireType AF;

    public static void init()
    {
        AF = new AFWireType();
    }

    public AFWireType()
    {
        super();
        WireApi.registerWireType(this);
    }

    @Override
    public String getUniqueName()
    {
        return "AF";
    }

    @Override
    public int getColour(Connection connection)
    {
        return Config.SERVER.wireColour.get();
    }

    @Override
    public double getSlack()
    {
        // Matches the 1.12 original.
        return 1.003;
    }

    @Override
    public int getMaxLength()
    {
        return Config.SERVER.maxLength.get();
    }

    @Override
    public ItemStack getWireCoil(Connection con)
    {
        return new ItemStack(AlternatingFlux.AF_WIRE_COIL.get());
    }

    @Override
    public double getRenderDiameter()
    {
        // Matches the 1.12 original (thicker than HV's 0.0625).
        return 0.078125;
    }

    @Nonnull
    @Override
    public String getCategory()
    {
        return AF_CATEGORY;
    }

    // --- IEnergyWire -------------------------------------------------------

    @Override
    public int getTransferRate()
    {
        return Config.SERVER.transferRate.get();
    }

    @Override
    public double getBasicLossRate(Connection c)
    {
        // Identical to IE's EnergyWire formula.
        return Config.SERVER.lossRatio.get() * c.getLength() / getMaxLength();
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
        // The EnergyTransferHandler is requested by the connector block entities,
        // not by the wire type itself (see EnergyConnectorBlockEntity). AF is
        // non-shocking, so no WireDamageHandler here.
        return ImmutableList.of();
    }
}
