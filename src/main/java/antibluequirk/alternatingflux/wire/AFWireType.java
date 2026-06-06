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
 * The Alternating Flux wire tier.
 *
 * Port of AntiBlueQuirk's 1.12 AFWireType onto IE 10.x's wire API (1.20.1 Forge).
 * Identical in shape to the 1.21.1 version we already shipped — the IEnergyWire
 * and IShockingWire interfaces are unchanged between IE 10.x and 12.x. Loss
 * formula matches IE's own EnergyWire: lossRatio * length / maxLength.
 *
 * Tuned values (all config-exposed in {@link Config}):
 *   - transfer rate : 131072 IF/t  (4x modern HV's 32768)
 *   - max length    : 96 blocks
 *   - loss ratio    : 0.0005       (well below HV's 0.0008 over a longer span)
 *   - colour        : 0xf6866c     (original AF salmon)
 *
 * Note: the 1.12 original declared canCauseDamage()=true but a long-standing bug
 * (issue #12) meant AF wires never actually shocked anything. This port fixes
 * that rather than reproducing it: AF is an {@link IShockingWire}, so live AF
 * lines hurt. Damage continues IE's tier progression (LV/MV/HV base damage
 * 2/5/15 at radius 0.05/0.1/0.3 — AF defaults to 25 at 0.5, config-exposed)
 * and uses IE's own throughput-scaled formula, so an idle line is harmless.
 */
public class AFWireType extends WireType implements IShockingWire
{
    public static final String AF_CATEGORY = "AF";
    public static AFWireType AF;

    /**
     * Shock intensity vs IE's insulated gear, continuing IE's per-tier
     * progression (LV 0.5 / MV 1.0 / HV 1.5).
     */
    private static final ElectricSource ELECTRIC_SOURCE = new ElectricSource(2.0f);

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

    // --- IShockingWire -----------------------------------------------------

    @Override
    public double getDamageRadius()
    {
        return Config.SERVER.damageRadius.get();
    }

    @Override
    public ElectricSource getElectricSource()
    {
        // A radius or base of 0 (config-disabled) yields a harmless source so the
        // handler does nothing; otherwise the tier's fixed shock intensity.
        if (Config.SERVER.damageRadius.get() <= 0 || Config.SERVER.shockDamageBase.get() <= 0)
            return new ElectricSource(-1f);
        return ELECTRIC_SOURCE;
    }

    @Override
    public float getDamageAmount(Entity e, Connection c, int transferred)
    {
        // IE's own formula (IEWireTypes$ShockingWire): base * load fraction * 8.
        // 'transferred' is the loss-adjusted energy available from all sources,
        // capped by the handler at transferRate — so like IE's HV (15 -> 120),
        // a line at full available capacity deals base * 8.
        return (float)(Config.SERVER.shockDamageBase.get() * transferred / getTransferRate() * 8);
    }

    // --- ILocalHandlerProvider --------------------------------------------

    @Override
    public Collection<ResourceLocation> getRequestedHandlers()
    {
        // The EnergyTransferHandler is requested by the connector block entities,
        // not by the wire type itself (see EnergyConnectorBlockEntity). The
        // WireDamageHandler, however, attaches per wire type: requesting it here
        // is what makes entities brushing a live AF line take shock damage.
        return ImmutableList.of(WireDamageHandler.ID);
    }
}
