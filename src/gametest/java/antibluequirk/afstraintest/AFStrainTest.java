package antibluequirk.afstraintest;

import antibluequirk.alternatingflux.block.AFBlocks;
import blusunrize.immersiveengineering.common.blocks.generic.ConnectorBlock;
import blusunrize.immersiveengineering.common.blocks.metal.BasicConnectorBlock;
import blusunrize.immersiveengineering.common.blocks.metal.EnergyConnectorBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * A TEST-ONLY stand-in for the add-ons that ship real strain hardware.
 *
 * <h2>Why this exists at all</h2>
 * Alternating Flux declares {@code alternatingflux:strain_anchors} and ships it
 * EMPTY, because the dead-ends belong to add-ons and AF is the base mod. That is
 * the right shape, and it leaves AF unable to prove its own headline rule: with
 * nothing in the tag, {@code bothEndsAnchored} is false everywhere and the
 * doubling is unreachable, so a suite that only installs AF can pin the
 * "nothing changed" guarantee and nothing else. AF is already shipped and already
 * has players on 1.20.1; shipping the feature untested was not an option.
 *
 * So this little mod does exactly what an add-on does — it registers one block
 * that can host an AF wire and puts it in the tag from its own datapack — and it
 * lives in the {@code gametest} source set, which {@code jar} never consumes.
 * Nothing here reaches a release, and AF's own roster and its own shipped tag are
 * untouched.
 *
 * <h2>Why a block and not just a tag entry</h2>
 * The rule needs TWO kinds of AF-capable connector in one world: one that is an
 * anchor and one that is not, or "only one end is anchored" cannot be set up at
 * all. AF ships exactly one wire relay, so tagging it would leave nothing to be
 * the ordinary end. This block is the anchored one; AF's own relay stays the
 * ordinary one, which is also the more honest reading — a relay carries the line
 * past, an anchor terminates it.
 *
 * It is the same {@code EnergyConnectorBlockEntity} on the same ("AF", relay)
 * spec as AF's relay, so it accepts exactly the same wire and hangs it at exactly
 * the same height. The only thing that distinguishes it is being in the tag.
 */
@Mod(AFStrainTest.MODID)
public class AFStrainTest
{
    public static final String MODID = "afstraintest";

    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);

    public static final RegistryObject<BlockEntityType<EnergyConnectorBlockEntity>> STRAIN_ANCHOR_BE =
            BLOCK_ENTITIES.register("strain_anchor", () ->
                    BlockEntityType.Builder.<EnergyConnectorBlockEntity>of(
                            (pos, state) -> new EnergyConnectorBlockEntity(AFStrainTest.STRAIN_ANCHOR_BE.get(), pos, state),
                            AFStrainTest.STRAIN_ANCHOR.get()
                    ).build(null));

    public static final RegistryObject<BasicConnectorBlock<EnergyConnectorBlockEntity>> STRAIN_ANCHOR =
            BLOCKS.register("strain_anchor", () ->
                    new BasicConnectorBlock<>(ConnectorBlock.PROPERTIES.get(), STRAIN_ANCHOR_BE));

    public AFStrainTest()
    {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(this::commonSetup);
    }

    /**
     * IE looks a connector's voltage up by its BLOCK ENTITY TYPE's registry name,
     * in a map it fills in for its own three tiers only, and dereferences the
     * result without a null check. AF injects its relay there; this injects ours,
     * onto the same ("AF", relay) spec, which is also what gives the wire the same
     * attachment height.
     */
    private void commonSetup(FMLCommonSetupEvent event)
    {
        event.enqueueWork(() -> EnergyConnectorBlockEntity.NAME_TO_SPEC.put(
                new ResourceLocation(MODID, "strain_anchor"), AFBlocks.AF_RELAY_SPEC));
    }
}
