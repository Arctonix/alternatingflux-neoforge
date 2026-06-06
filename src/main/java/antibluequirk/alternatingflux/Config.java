package antibluequirk.alternatingflux;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

/**
 * Config for the AF wire tier. The 1.12 original exposed transfer rate, loss,
 * colour and max length; this keeps the same knobs on NeoForge's ModConfigSpec.
 *
 * These are server-side values (gameplay-affecting). Colour is included here for
 * simplicity; if you want per-client colour, move it to a CLIENT spec later.
 */
public final class Config
{
    public static final class Server
    {
        public final IntValue transferRate;
        public final IntValue maxLength;
        public final DoubleValue lossRatio;
        public final IntValue wireColour;
        public final DoubleValue damageRadius;
        public final DoubleValue shockDamageBase;

        Server(ModConfigSpec.Builder b)
        {
            b.push("alternating_flux");

            transferRate = b
                    .comment("AF wire transfer rate in IF/t. Default 131072 = 4x modern HV (32768).")
                    .defineInRange("transferRate", 131072, 0, Integer.MAX_VALUE);

            maxLength = b
                    .comment(
                            "Maximum length of a single AF wire, in blocks.",
                            "There is no hard cap in IE; longer wires simply cross more chunks, so keep",
                            "both endpoints chunk-loaded. Default 96.")
                    .defineInRange("maxLength", 96, 1, 1024);

            lossRatio = b
                    .comment(
                            "Power lost across a full-length AF run (loss = lossRatio * length / maxLength,",
                            "identical to IE's own energy-wire formula). Default 0.0005 — below HV's 0.0008",
                            "over a span twice as long, so one AF line beats chaining HV connectors.")
                    .defineInRange("lossRatio", 0.0005, 0.0, 1.0);

            wireColour = b
                    .comment("RGB colour of the AF wire. Default 0xf6866c (original salmon).")
                    .defineInRange("wireColour", 0xf6866c, 0, 0xffffff);

            damageRadius = b
                    .comment(
                            "How close an entity must get to a live AF wire to be shocked, in blocks",
                            "(entity hitbox is inflated by this much for the check). IE's own tiers:",
                            "LV 0.05 / MV 0.1 / HV 0.3. Default 0.5 — AF sits above HV.")
                    .defineInRange("damageRadius", 0.5, 0.0, 4.0);

            shockDamageBase = b
                    .comment(
                            "Base shock damage of a fully-loaded AF wire; actual damage scales with",
                            "current throughput (IE's formula). IE's tiers: LV 2 / MV 5 / HV 15.",
                            "Default 25. Set 0 to disable shock damage entirely.")
                    .defineInRange("shockDamageBase", 25.0, 0.0, 1024.0);

            b.pop();
        }
    }

    public static final ModConfigSpec SERVER_SPEC;
    public static final Server SERVER;

    static
    {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        SERVER = new Server(builder);
        SERVER_SPEC = builder.build();
    }

    private Config() {}
}
