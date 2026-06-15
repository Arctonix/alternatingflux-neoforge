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
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;

import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * The Alternating Flux wire tier.
 *
 * Port of AntiBlueQuirk's 1.12 AFWireType onto IE 5.x's wire API (1.16.5 Forge).
 * In IE 1.16.5, WireType declares getIcon(Connection) as abstract. The energy
 * methods (getTransferRate, getBasicLossRate, getLossRate) come via IShockingWire
 * which extends EnergyTransferHandler.IEnergyWire.
 *
 * In 1.16.5, Connection has no getLength() — compute wire length from endpoint
 * positions using the same formula IE uses internally (Euclidean distance).
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

    /**
     * IE 1.16.5 WireType requires getIcon(Connection) for client-side wire rendering.
     * Return the static default wire icon (set by IE after texture loading).
     * This is client-only; on dedicated server this method is never called.
     */
    @Override
    public TextureAtlasSprite getIcon(Connection connection)
    {
        return WireType.iconDefaultWire;
    }

    @Nonnull
    @Override
    public String getCategory()
    {
        return AF_CATEGORY;
    }

    // --- IEnergyWire (via IShockingWire) ------------------------------------------

    @Override
    public int getTransferRate()
    {
        return Config.SERVER.transferRate.get();
    }

    @Override
    public double getBasicLossRate(Connection c)
    {
        // IE 1.16.5's Connection has no getLength(). Compute Euclidean distance from
        // endpoints — identical to IE's own EnergyWire.getBasicLossRate formula.
        Vector3d posA = Vector3d.atLowerCornerOf(c.getEndA().getPosition());
        double dist = Math.sqrt(c.getEndB().getPosition().distSqr(posA, false));
        return Config.SERVER.lossRatio.get() * dist / getMaxLength();
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
        // IE's own formula: base * load fraction * 8.
        return (float)(Config.SERVER.shockDamageBase.get() * transferred / getTransferRate() * 8);
    }

    // --- ILocalHandlerProvider --------------------------------------------

    @Override
    public Collection<ResourceLocation> getRequestedHandlers()
    {
        // The WireDamageHandler attaches per wire type, making entities brushing
        // a live AF line take shock damage.
        return ImmutableList.of(WireDamageHandler.ID);
    }
}
