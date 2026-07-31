package antibluequirk.alternatingflux.gametest;

import antibluequirk.alternatingflux.AlternatingFlux;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The add-on Alternating Flux does not have.
 *
 * <h2>Why this exists</h2>
 * The strain-span rule can only be exercised where a strain anchor exists, and AF
 * declares the tag but ships it EMPTY on purpose: the dead-ends live in add-ons and
 * AF is the base mod. Until now that meant AF could only prove the HALF of the rule
 * that is "with nothing in the tag, nothing changes", and the doubling itself was
 * proved a repository away, in AF: Interchange.
 *
 * That is no longer good enough, because the rule now applies to IE's own coils —
 * copper, electrum, steel — and those are AF's responsibility, not an add-on's. So
 * AF carries a test-only datapack that puts two connectors in the tag and stands in
 * for an add-on: one AF relay, so the AF coil can be driven between two anchors, and
 * one IE HV relay, so IE's steel coil can be.
 *
 * <h2>Why it cannot be on by default, and cannot be a second tag file</h2>
 * A second {@code data/alternatingflux/tags/block/strain_anchors.json} in this mod's
 * own resources would merge into the shipped tag and be there for every player. So
 * the fixture is a separate datapack, inside this jar but NOT under {@code data/},
 * mounted only by the pack finder below and only when {@link #PROPERTY} is set. That
 * property is set by one Gradle run and nothing else:
 * {@code runGameTestServerAnchored}. In an installed game the finder returns before
 * it looks at anything, the pack does not exist, and the tag is empty.
 *
 * <h2>Why two runs rather than one</h2>
 * A datapack is chosen once, when the server starts. "Bare AF" and "AF with an
 * add-on's dead-ends" are therefore two different installs, not two states one run
 * can visit — and both have to be tested, because the promise to the five live MC
 * lines is about the first and the feature is about the second. So the suite runs
 * twice, and every test declares which install it is about; {@link #installed()} is
 * how it asks, and it reads the run's declared intent rather than the tag, so a
 * fixture that silently failed to load cannot quietly turn the doubling tests into
 * no-ops. {@code StrainSpanGameTests#runModeMatchesFixture} pins the two against
 * each other.
 */
@EventBusSubscriber(modid = AlternatingFlux.MODID, bus = Bus.MOD)
public final class StrainSpanFixture
{
	private static final Logger LOGGER = LogUtils.getLogger();

	/** Set by the anchored gametest run, and by nothing a player ever launches. */
	public static final String PROPERTY = "alternatingflux.gametest.anchors";

	/**
	 * The run directory the anchored run is given, and the ONLY evidence outside
	 * {@link #PROPERTY} that this JVM is the anchored one.
	 *
	 * It is here because a system property that never arrives is invisible: the
	 * anchored run would mount no fixture, every test about the doubling would hand
	 * itself in as "not my install", and the whole suite would go green having
	 * proved nothing. Two things the build must set, checked against each other, is
	 * what makes that impossible — see {@code StrainSpanGameTests#runModeMatchesFixture}.
	 * Both are set in build.gradle, side by side, on the same run.
	 */
	public static final String ANCHORED_RUN_DIRECTORY = "run-gametest-anchored";

	/** Path of the fixture pack inside this mod's file — deliberately not under data/. */
	private static final String PACK_PATH = "gametest_anchors";

	/** Is this the run that mounts the fixture, i.e. the "add-on installed" install? */
	public static boolean installed()
	{
		return Boolean.getBoolean(PROPERTY);
	}

	@SubscribeEvent
	static void addFixturePack(AddPackFindersEvent event)
	{
		if(!installed()||event.getPackType()!=PackType.SERVER_DATA)
			return;
		// The pack lives in the source tree but is excluded from the published jar
		// (see build.gradle), so on a release artifact there is nothing to mount even
		// if someone sets the property by hand. Say so and carry on rather than
		// handing the pack repository a null.
		Path pack = ModList.get().getModFileById(AlternatingFlux.MODID).getFile().findResource(PACK_PATH);
		if(!Files.exists(pack))
		{
			LOGGER.error("{} is set but the gametest fixture datapack is not in this build ({});"
					+" no strain anchors will be registered", PROPERTY, pack);
			return;
		}
		event.addPackFinders(
				AlternatingFlux.rl(PACK_PATH),
				PackType.SERVER_DATA,
				Component.literal("Alternating Flux gametest strain anchors"),
				PackSource.BUILT_IN,
				// Forced on: a gametest run has nobody to tick a checkbox, and a test
				// that silently ran without its fixture would be worse than no test.
				true,
				Pack.Position.TOP
		);
	}

	private StrainSpanFixture() {}
}
