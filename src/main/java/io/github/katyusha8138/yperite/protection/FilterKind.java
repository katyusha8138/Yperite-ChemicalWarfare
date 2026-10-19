package io.github.katyusha8138.yperite.protection;

import io.github.katyusha8138.yperite.gas.GasCategory;
import org.jetbrains.annotations.Nullable;

/**
 * The kinds of mask filter. A filter lets only a share of each category through ("pass"), and wears out by the
 * dose it takes in. Numbers are game tuning values.
 *
 * <ul>
 *   <li>general: stops riot control, choking and incapacitating agents well, blister agents mostly, but is
 *       poor against blood and nerve agents</li>
 *   <li>special: good against everything, but its charge is smaller</li>
 * </ul>
 */
public enum FilterKind {
    //                 id         charge   RIOT  CHOKE BLOOD BLIST NERVE INCAP
    GENERAL("general", 150f, 0.05f, 0.05f, 0.60f, 0.10f, 0.70f, 0.08f),
    SPECIAL("special", 100f, 0.03f, 0.03f, 0.08f, 0.05f, 0.04f, 0.03f);

    private final String id;
    private final float capacity;
    private final float[] pass = new float[GasCategory.values().length];

    FilterKind(String id, float capacity, float riot, float choking, float blood, float blister, float nerve, float incapacitant) {
        this.id = id;
        this.capacity = capacity;
        pass[GasCategory.RIOT_CONTROL.ordinal()] = riot;
        pass[GasCategory.CHOKING.ordinal()] = choking;
        pass[GasCategory.BLOOD.ordinal()] = blood;
        pass[GasCategory.BLISTER.ordinal()] = blister;
        pass[GasCategory.NERVE.ordinal()] = nerve;
        pass[GasCategory.INCAPACITANT.ordinal()] = incapacitant;
    }

    public String id() {
        return id;
    }

    /** Dose (in exposure units) a fresh filter can take in before it is used up. */
    public float capacity() {
        return capacity;
    }

    /** Share of the dose of this category that still gets through while the filter has charge left. */
    public float pass(GasCategory category) {
        return pass[category.ordinal()];
    }

    @Nullable
    public static FilterKind byId(String id) {
        for (FilterKind kind : values()) {
            if (kind.id.equals(id)) {
                return kind;
            }
        }
        return null;
    }
}
