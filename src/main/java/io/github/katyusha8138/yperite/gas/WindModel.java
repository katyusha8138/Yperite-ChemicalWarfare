package io.github.katyusha8138.yperite.gas;

import net.minecraft.server.level.ServerLevel;

/**
 * Minecraft has no wind, so this derives a slowly changing one from the world time. Nothing needs to be
 * saved: the same world at the same time always gives the same wind.
 */
public final class WindModel {
    /** Wind vector. East is +x, south is +z; the length is the strength within 0..1. */
    public record Wind(float x, float z) {
        public static final Wind CALM = new Wind(0f, 0f);

        public float strength() {
            return (float) Math.sqrt(x * x + z * z);
        }
    }

    private WindModel() {
    }

    public static Wind at(ServerLevel level) {
        if (!level.dimensionType().hasSkyLight()) {
            return Wind.CALM;
        }
        double phase = (level.getSeed() & 0xFFFFL) / 65535.0 * Math.PI * 2.0;
        double t = level.getGameTime();
        double angle = phase + 1.1 * Math.sin(t / 6000.0 + phase) + 0.6 * Math.sin(t / 1700.0 + 2.0 * phase);
        double strength = 0.35 + 0.25 * Math.sin(t / 2900.0 + 3.0 * phase);
        if (level.isThundering()) {
            strength += 0.35;
        } else if (level.isRaining()) {
            strength += 0.15;
        }
        strength = Math.max(0.0, Math.min(1.0, strength));
        return new Wind((float) (Math.cos(angle) * strength), (float) (Math.sin(angle) * strength));
    }
}
