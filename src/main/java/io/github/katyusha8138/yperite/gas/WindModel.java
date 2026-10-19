package io.github.katyusha8138.yperite.gas;

import net.minecraft.server.level.ServerLevel;

/**
 * Minecraft has no wind, so this derives one per dimension: either a slowly changing wind computed from the world
 * time, seed and weather, or a fixed wind that an operator set with {@code /yperite wind set}. Nothing about the
 * natural wind needs saving: the same world at the same time always gives the same wind.
 *
 * <p>Directions follow the weather-report convention: the angle is where the wind comes <em>from</em>,
 * clockwise from north (0 = from the north, 90 = from the east).
 */
public final class WindModel {
    /** Wind vector. East is +x, south is +z; the length is the strength within 0..1. */
    public record Wind(float x, float z) {
        public static final Wind CALM = new Wind(0f, 0f);

        public float strength() {
            return (float) Math.sqrt(x * x + z * z);
        }

        /** Where the wind comes from, in degrees clockwise from north, 0 (inclusive) to 360 (exclusive). */
        public float fromDegrees() {
            if (x == 0f && z == 0f) {
                return 0f;
            }
            double degrees = Math.toDegrees(Math.atan2(-x, z));
            return (float) ((degrees % 360.0 + 360.0) % 360.0);
        }

        public static Wind fromCompass(float fromDegrees, float strength) {
            double radians = Math.toRadians(fromDegrees);
            return new Wind((float) (-Math.sin(radians) * strength), (float) (Math.cos(radians) * strength));
        }
    }

    /** A wind that an operator fixed for one dimension. */
    public record Override(float fromDegrees, float strength) {
        public Wind toWind() {
            return Wind.fromCompass(fromDegrees, strength);
        }
    }

    private WindModel() {
    }

    /** The wind in this dimension right now. Dimensions without sky light (Nether, End) are always calm. */
    public static Wind at(ServerLevel level, GasSavedData data) {
        if (!level.dimensionType().hasSkyLight()) {
            return Wind.CALM;
        }
        Override fixed = data.windOverride();
        return fixed != null ? fixed.toWind() : natural(level);
    }

    private static Wind natural(ServerLevel level) {
        double phase = (level.getSeed() & 0xFFFFL) / 65535.0 * Math.PI * 2.0;
        double t = level.getGameTime();
        double angle = phase + 1.1 * Math.sin(t / 6000.0 + phase) + 0.6 * Math.sin(t / 1700.0 + 2.0 * phase);
        double strength = 0.5 + 0.25 * Math.sin(t / 2900.0 + 3.0 * phase);
        if (level.isThundering()) {
            strength += 0.35;
        } else if (level.isRaining()) {
            strength += 0.15;
        }
        strength = Math.max(0.0, Math.min(1.0, strength));
        return new Wind((float) (Math.cos(angle) * strength), (float) (Math.sin(angle) * strength));
    }
}
