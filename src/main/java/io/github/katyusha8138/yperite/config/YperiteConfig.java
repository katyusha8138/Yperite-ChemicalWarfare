package io.github.katyusha8138.yperite.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Common configuration. Everything here can be changed without touching code. */
public final class YperiteConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue GAS_UPDATE_INTERVAL;
    public static final ForgeConfigSpec.IntValue GAS_MAX_CELLS;
    public static final ForgeConfigSpec.DoubleValue GAS_MIN_CONCENTRATION;
    public static final ForgeConfigSpec.BooleanValue WIND_ENABLED;
    public static final ForgeConfigSpec.DoubleValue WIND_MAX_SPEED;
    public static final ForgeConfigSpec.DoubleValue WIND_SHELTER_EXPONENT;
    public static final ForgeConfigSpec.DoubleValue OPEN_AIR_DILUTION;
    public static final ForgeConfigSpec.BooleanValue EXPOSURE_ENABLED;
    public static final ForgeConfigSpec.IntValue EXPOSURE_INTERVAL;
    public static final ForgeConfigSpec.DoubleValue EXPOSURE_POTENCY;
    public static final ForgeConfigSpec.DoubleValue EXPOSURE_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue EXPOSURE_RECOVERY;
    public static final ForgeConfigSpec.BooleanValue EXPOSURE_MESSAGES;
    public static final ForgeConfigSpec.BooleanValue MOBS_ENABLED;
    public static final ForgeConfigSpec.BooleanValue MOBS_UNDEAD_IMMUNE;
    public static final ForgeConfigSpec.BooleanValue AFTEREFFECTS_ENABLED;
    public static final ForgeConfigSpec.DoubleValue AFTEREFFECTS_STRENGTH;
    public static final ForgeConfigSpec.DoubleValue AFTEREFFECTS_MAX_HEALTH_LOSS;
    public static final ForgeConfigSpec.DoubleValue FILTER_WEAR;
    public static final ForgeConfigSpec.DoubleValue SUIT_WEAR;
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
        OPEN_AIR_DILUTION = b
                .comment("In fully open air gas spreads and decays this many times faster than indoors.",
                        "Cells that are partly covered get a share of the bonus. 1 turns the effect off.")
                .defineInRange("openAirDilution", 1.5, 1.0, 4.0);
        b.push("wind");
        WIND_ENABLED = b
                .comment("Whether wind carries gas that is open to the air.")
                .define("enabled", true);
        WIND_MAX_SPEED = b
                .comment("Speed in blocks per second at the strongest wind (strength 1). A storm reaches it; calm days are about half.",
                        "With the default update interval of 4 ticks speeds up to 5 blocks per second are exact; faster",
                        "winds are capped at one block per update.")
                .defineInRange("maxSpeed", 4.0, 0.0, 20.0);
        WIND_SHELTER_EXPONENT = b
                .comment("How quickly wind fades towards the inside of buildings and caves. The wind at a cell is scaled by",
                        "(skyLight / 15) ^ this. 1 fades linearly; larger values give a sharper edge near openings.")
                .defineInRange("shelterExponent", 2.0, 0.25, 8.0);
        b.pop(); // wind
        b.pop(); // gas

        b.comment("Exposure: how gas affects living things").push("exposure");
        EXPOSURE_ENABLED = b
                .comment("Whether gas affects living things at all. Turn it off to keep only the gas itself.")
                .define("enabled", true);
        EXPOSURE_INTERVAL = b
                .comment("Ticks between exposure checks for players.")
                .defineInRange("intervalTicks", 5, 1, 20);
        EXPOSURE_POTENCY = b
                .comment("Scales how fast dose builds up. 2 means every gas is twice as potent.")
                .defineInRange("potencyScale", 1.0, 0.0, 10.0);
        EXPOSURE_DAMAGE = b
                .comment("Scales the damage that symptoms deal. 0 makes gas harmless to health (status effects stay).")
                .defineInRange("damageScale", 1.0, 0.0, 10.0);
        EXPOSURE_RECOVERY = b
                .comment("Scales how fast symptoms fade. 2 means twice as fast, 0.5 means twice as slow.")
                .defineInRange("recoverySpeed", 1.0, 0.1, 10.0);
        EXPOSURE_MESSAGES = b
                .comment("Whether players get short messages about their condition.")
                .define("messages", true);
        b.comment("Animals, villagers and monsters use a simple model: symptoms follow the gas around them right now.")
                .push("mobs");
        MOBS_ENABLED = b
                .comment("Whether gas affects creatures other than players.")
                .define("enabled", true);
        MOBS_UNDEAD_IMMUNE = b
                .comment("Whether undead are immune. (Golems and armour stands always are; see the yperite:gas_immune entity tag.)")
                .define("undeadImmune", true);
        b.pop();
        b.pop();

        b.comment("Lasting damage after severe exposure. Dying removes it.").push("aftereffects");
        AFTEREFFECTS_ENABLED = b
                .comment("Whether severe exposure leaves lasting damage. The record is kept when this is off.")
                .define("enabled", true);
        AFTEREFFECTS_STRENGTH = b
                .comment("Scales every lasting penalty. 0.5 halves them.")
                .defineInRange("strength", 1.0, 0.0, 3.0);
        AFTEREFFECTS_MAX_HEALTH_LOSS = b
                .comment("Health points a blister agent can take off the maximum health (2 is one heart).")
                .defineInRange("maxHealthLoss", 6.0, 0.0, 18.0);
        b.pop();

        b.comment("Protective equipment").push("protection");
        FILTER_WEAR = b
                .comment("How fast mask filters are used up. 2 means they last half as long, 0 means they never wear out.")
                .defineInRange("filterWear", 1.0, 0.0, 10.0);
        SUIT_WEAR = b
                .comment("How fast protective clothing loses durability when gas reaches it. 0 means it never wears out.")
                .defineInRange("suitWear", 1.0, 0.0, 10.0);
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
