package antibluequirk.alternatingflux.wire;

import antibluequirk.alternatingflux.AlternatingFlux;
import antibluequirk.alternatingflux.Config;
import blusunrize.immersiveengineering.api.tool.IElectricEquipment.ElectricSource;
import blusunrize.immersiveengineering.api.wires.Connection;
import blusunrize.immersiveengineering.api.wires.WireApi;
import blusunrize.immersiveengineering.api.wires.WireType;
import blusunrize.immersiveengineering.api.wires.localhandlers.WireDamageHandler;
import blusunrize.immersiveengineering.api.wires.localhandlers.WireDamageHandler.IShockingWire;
import com.google.common.collect.ImmutableList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
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
 *   - loss ratio    : 0.0001       (one-fifth of AF's 0.0005)
 *   - colour        : 0x8b3fd6     (purple)
 *
 * Like AF, UAF is an {@link IShockingWire}: live UAF lines shock anything that
 * brushes them. Damage continues the tier progression above AF (base 40 at
 * radius 0.75, vs AF's 25 at 0.5), scaled by throughput via IE's own formula.
 */
public class UAFWireType extends WireType implements IShockingWire
{
    public static final String UAF_CATEGORY = "UAF";
    public static UAFWireType UAF;

    /**
     * Shock intensity vs IE's insulated gear, one step above AF's 2.0
     * (IE's own tiers run LV 0.5 / MV 1.0 / HV 1.5).
     */
    private static final ElectricSource ELECTRIC_SOURCE = new ElectricSource(2.5f);

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

    // --- IShockingWire -----------------------------------------------------

    @Override
    public double getDamageRadius()
    {
        return Config.SERVER.uafDamageRadius.get();
    }

    @Override
    public ElectricSource getElectricSource()
    {
        // A negative level disables shocking; honour a config that zeroes either knob.
        if (Config.SERVER.uafDamageRadius.get() <= 0 || Config.SERVER.uafShockDamageBase.get() <= 0)
            return new ElectricSource(-1f);
        return ELECTRIC_SOURCE;
    }

    @Override
    public float getDamageAmount(Entity e, Connection c, int transferred)
    {
        // IE's throughput-scaled formula: base * load fraction * 8 (see AFWireType).
        return (float)(Config.SERVER.uafShockDamageBase.get() * transferred / getTransferRate() * 8);
    }

    // --- ILocalHandlerProvider --------------------------------------------

    @Override
    public Collection<ResourceLocation> getRequestedHandlers()
    {
        // Requesting the WireDamageHandler per wire type is what makes a live UAF
        // line shock entities that touch it.
        return ImmutableList.of(WireDamageHandler.ID);
    }
}
