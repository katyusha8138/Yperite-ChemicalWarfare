package io.github.katyusha8138.yperite.protection;

/** How the detector and the alarm sort a concentration into a level the player can read. */
public final class DetectorLevel {
    public static final int NONE = 0;
    public static final int LOW = 1;
    public static final int MEDIUM = 2;
    public static final int HIGH = 3;
    public static final int EXTREME = 4;

    /** Concentrations at which the levels begin (1 is dense, harmful gas). */
    private static final float[] THRESHOLDS = {0.02f, 0.1f, 0.5f, 1.5f};

    private DetectorLevel() {
    }

    /** 0 (nothing) .. 4 (extreme). */
    public static int of(float concentration) {
        int level = 0;
        for (float threshold : THRESHOLDS) {
            if (concentration >= threshold) {
                level++;
            }
        }
        return level;
    }

    /** Redstone signal strength for an alarm: 0 when nothing is detected, otherwise 3, 7, 11 or 15. */
    public static int redstone(int level) {
        return level <= 0 ? 0 : Math.min(15, level * 4 - 1);
    }
}
