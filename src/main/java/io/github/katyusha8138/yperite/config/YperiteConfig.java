package io.github.katyusha8138.yperite.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Common configuration. Everything here can be changed without touching code. */
public final class YperiteConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue GAS_UPDATE_INTERVAL;
    public static final ForgeConfigSpec.IntValue GAS_MAX_CELLS;
    public static final ForgeConfigSpec.DoubleValue GAS_MIN_CONCENTRATION;
    public static final ForgeConfigSpec.BooleanValue WIND_ENABLED;
    public static final ForgeConfigSpec.DoubleValue WIND_GAIN;
    public static final ForgeConfigSpec.BooleanValue PARTICLES_ENABLED;
    public static final ForgeConfigSpec.IntValue PARTICLE_INTERVAL;
    public static final ForgeConfigSpec.IntValue PARTICLE_RANGE;
    public static final ForgeConfigSpec.IntValue PARTICLE_MAX_PER_PLAYER;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Gas simulation").push("gas");
        GAS_UPDATE_INTERVAL = b
                .comment("Ticks between gas updates. Higher is lighter on the server but makes gas spread in bigger jumps.")
                .defineInRange("updateIntervalTicks", 4, 1, 20);
        GAS_MAX_CELLS = b
                .comment("Soft cap on gas cells per gas type per dimension. The thinnest cells are removed when it is exceeded.")
                .defineInRange("maxCellsPerLayer", 40000, 1000, 1000000);
        GAS_MIN_CONCENTRATION = b
                .comment("Gas thinner than this disappears. 1.0 is dense, harmful gas.")
                .defineInRange("minConcentration", 0.01, 0.0001, 0.5);
        b.push("wind");
        WIND_ENABLED = b
                .comment("Whether wind carries gas that is open to the sky.")
                .define("enabled", true);
        WIND_GAIN = b
                .comment("How strongly wind skews the spread. 0 disables the effect.")
                .defineInRange("gain", 2.5, 0.0, 10.0);
        b.pop();
        b.pop();

        b.comment("Gas particles (sent from the server to nearby players)").push("particles");
        PARTICLES_ENABLED = b
                .comment("Whether gas is drawn with particles.")
                .define("enabled", true);
        PARTICLE_INTERVAL = b
                .comment("Ticks between particle bursts.")
                .defineInRange("intervalTicks", 5, 1, 40);
        PARTICLE_RANGE = b
                .comment("Distance in blocks within which a player sees gas particles.")
                .defineInRange("range", 24, 4, 64);
        PARTICLE_MAX_PER_PLAYER = b
                .comment("Maximum particles sent to one player per burst.")
                .defineInRange("maxPerPlayer", 48, 0, 512);
        b.pop();

        SPEC = b.build();
    }

    private YperiteConfig() {
    }
}
