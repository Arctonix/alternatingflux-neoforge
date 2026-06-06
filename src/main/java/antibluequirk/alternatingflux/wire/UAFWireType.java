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
 * Shocking like AF (since 1.0.4): continues IE's tier progression (LV/MV/HV/AF
 * base damage 2/5/15/25 at radius 0.05/0.1/0.3/0.5 — UAF defaults to 40 at 0.75,
 * config-exposed) with IE's throughput-scaled formula; an idle line is harmless.
 */
public class UAFWireType extends WireType implements IShockingWire
{
    public static final String UAF_CATEGORY = "UAF";
    public static UAFWireType UAF;

    /**
     * Shock intensity vs IE's insulated gear, continuing IE's per-tier
     * progression (LV 0.5 / MV 1.0 / HV 1.5 / AF 2.0).
     */
    private static final ElectricSource ELECTRIC_SOURCE = new ElectricSource(2.5f);

    /** Sentinel for "no shock", mirroring IE's ShockingWire when damage is disabled. */
    private static final ElectricSource NO_SOURCE = new ElectricSource(-1.0f);

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
        // Like IE's ShockingWire: a -1 source means "no shock" when damage is off.
        if(Config.SERVER.uafDamageRadius.get() <= 0 || Config.SERVER.uafShockDamageBase.get() <= 0)
            return NO_SOURCE;
        return ELECTRIC_SOURCE;
    }

    @Override
    public float getDamageAmount(Entity e, Connection c, int transferred)
    {
        // IE's own formula (IEWireTypes$ShockingWire): base * load fraction * 8.
        return (float)(Config.SERVER.uafShockDamageBase.get() * transferred / getTransferRate() * 8);
    }

    // --- ILocalHandlerProvider --------------------------------------------

    @Override
    public Collection<ResourceLocation> getRequestedHandlers()
    {
        // Requesting the WireDamageHandler is what makes entities brushing a
        // live UAF line take shock damage (see AFWireType for the full note).
        return ImmutableList.of(WireDamageHandler.ID);
    }
}
