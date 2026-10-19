package io.github.katyusha8138.yperite.exposure;

import io.github.katyusha8138.yperite.exposure.model.CategoryProfile;
import io.github.katyusha8138.yperite.gas.GasCategory;

/**
 * Time constants per gas category (seconds). With the default recovery speed:
 * <ul>
 *   <li>riot control and incapacitants fade within tens of seconds</li>
 *   <li>choking, blood and nerve agents take minutes to fade</li>
 *   <li>blister agents show up late (a minute or two) and last more than ten minutes</li>
 * </ul>
 * Game tuning values only. The recovery speed can be scaled in the config.
 */
public final class ExposureProfiles {
    private static final GasCategory[] CATEGORIES = GasCategory.values();
    private static final CategoryProfile[] PROFILES = new CategoryProfile[CATEGORIES.length];

    static {
        set(GasCategory.RIOT_CONTROL, 0.5f, 20f, 30f);
        set(GasCategory.CHOKING, 4f, 120f, 30f);
        set(GasCategory.BLOOD, 3f, 150f, 30f);
        set(GasCategory.BLISTER, 75f, 600f, 90f);
        set(GasCategory.NERVE, 2f, 240f, 20f);
        set(GasCategory.INCAPACITANT, 2f, 30f, 30f);
    }

    private ExposureProfiles() {
    }

    private static void set(GasCategory category, float onset, float recovery, float absorb) {
        PROFILES[category.ordinal()] = new CategoryProfile(onset, recovery, absorb);
    }

    public static int count() {
        return CATEGORIES.length;
    }

    public static GasCategory category(int index) {
        return CATEGORIES[index];
    }

    /** Indexed by {@link GasCategory#ordinal()}. Do not modify. */
    public static CategoryProfile[] all() {
        return PROFILES;
    }
}
