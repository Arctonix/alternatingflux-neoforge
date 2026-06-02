package antibluequirk.alternatingflux;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

/**
 * Config for the AF and UAF wire tiers. The 1.12 original exposed transfer rate,
 * loss, colour and max length; this keeps the same knobs on Forge's ForgeConfigSpec.
 *
 * Identical schema to the 1.21.1 NeoForge port — only the spec class changes
 * (ModConfigSpec -> ForgeConfigSpec). Server-side (gameplay-affecting).
 */
public final class Config
{
    public static final class Server
    {
        public final IntValue transferRate;
        public final IntValue maxLength;
        public final DoubleValue lossRatio;
        public final IntValue wireColour;

        public final IntValue uafTransferRate;
        public final IntValue uafMaxLength;
        public final DoubleValue uafLossRatio;
        public final IntValue uafWireColour;

        Server(ForgeConfigSpec.Builder b)
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

            b.pop();

            b.push("ultra_high_alternating_flux");

            uafTransferRate = b
                    .comment("UAF wire transfer rate in IF/t. Default 524288 = 16x modern HV (32768) / 4x AF.")
                    .defineInRange("transferRate", 524288, 0, Integer.MAX_VALUE);

            uafMaxLength = b
                    .comment(
                            "Maximum length of a single UAF wire, in blocks.",
                            "Same span as AF. Keep both endpoints chunk-loaded. Default 96.")
                    .defineInRange("maxLength", 96, 1, 1024);

            uafLossRatio = b
                    .comment(
                            "Power lost across a full-length UAF run (loss = lossRatio * length / maxLength,",
                            "identical to IE's own energy-wire formula). Default 0.0001 - one-fifth of AF's",
                            "0.0005, giving UAF the same ~5x efficiency jump over AF that AF has over HV",
                            "(HV is 0.0008 over 32 blocks; AF 0.0005 over 96; UAF 0.0001 over 96).")
                    .defineInRange("lossRatio", 0.0001, 0.0, 1.0);

            uafWireColour = b
                    .comment("RGB colour of the UAF wire. Default 0x8b3fd6 (purple).")
                    .defineInRange("wireColour", 0x8b3fd6, 0, 0xffffff);

            b.pop();
        }
    }

    public static final ForgeConfigSpec SERVER_SPEC;
    public static final Server SERVER;

    static
    {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        SERVER = new Server(builder);
        SERVER_SPEC = builder.build();
    }

    private Config() {}
}
