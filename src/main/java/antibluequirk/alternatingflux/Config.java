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

        public final IntValue uafTransferRate;
        public final IntValue uafMaxLength;
        public final DoubleValue uafLossRatio;
        public final IntValue uafWireColour;
        public final DoubleValue uafDamageRadius;
        public final DoubleValue uafShockDamageBase;

        Server(ModConfigSpec.Builder b)
        {
            b.push("alternating_flux");

            transferRate = b
                    .comment(
                            "AF line carrying capacity (trunk/aggregate) in IF/t. Default 131072.",
                            "Each AF<->HV transformer moves at most the HV rate (32768), so the 4x",
                            "refers to one AF trunk aggregating multiple HV feeds, not single-link",
                            "delivery. Minimum 1 (0 would divide-by-zero the shock-damage formula).")
                    .defineInRange("transferRate", 131072, 1, Integer.MAX_VALUE);

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

            b.push("ultra_high_alternating_flux");

            uafTransferRate = b
                    .comment(
                            "UAF line carrying capacity (trunk/aggregate) in IF/t. Default 524288 =",
                            "16x modern HV (32768) / 4x AF. Like AF, transformers cap single-link",
                            "delivery at the lower side's rate; the 16x/4x refers to trunk aggregation.",
                            "Minimum 1 (0 would divide-by-zero the shock-damage formula).")
                    .defineInRange("transferRate", 524288, 1, Integer.MAX_VALUE);

            uafMaxLength = b
                    .comment(
                            "Maximum length of a single UAF wire, in blocks.",
                            "Same span as AF. Keep both endpoints chunk-loaded. Default 96.")
                    .defineInRange("maxLength", 96, 1, 1024);

            uafLossRatio = b
                    .comment(
                            "Power lost across a full-length UAF run (loss = lossRatio * length / maxLength,",
                            "identical to IE's own energy-wire formula). Default 0.0001 - one-fifth of AF's",
                            "0.0005, a ~5x per-block efficiency jump over AF, similar to AF's ~4.8x over HV",
                            "(HV is 0.0008 over 32 blocks; AF 0.0005 over 96; UAF 0.0001 over 96).")
                    .defineInRange("lossRatio", 0.0001, 0.0, 1.0);

            uafWireColour = b
                    .comment("RGB colour of the UAF wire. Default 0x8b3fd6 (purple).")
                    .defineInRange("wireColour", 0x8b3fd6, 0, 0xffffff);

            uafDamageRadius = b
                    .comment(
                            "How close an entity must get to a live UAF wire to be shocked, in blocks",
                            "(entity hitbox is inflated by this much for the check). IE's own tiers:",
                            "LV 0.05 / MV 0.1 / HV 0.3; AF 0.5. Default 0.75 — UAF tops the ladder.")
                    .defineInRange("damageRadius", 0.75, 0.0, 4.0);

            uafShockDamageBase = b
                    .comment(
                            "Base shock damage of a fully-loaded UAF wire; actual damage scales with",
                            "current throughput (IE's formula). IE's tiers: LV 2 / MV 5 / HV 15; AF 25.",
                            "Default 40. Set 0 to disable shock damage entirely.")
                    .defineInRange("shockDamageBase", 40.0, 0.0, 1024.0);

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
