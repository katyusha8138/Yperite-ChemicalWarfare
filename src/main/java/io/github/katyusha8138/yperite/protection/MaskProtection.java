package io.github.katyusha8138.yperite.protection;

import io.github.katyusha8138.yperite.gas.GasCategory;
import org.jetbrains.annotations.Nullable;

/** What a gas mask does against breathing in gas. */
public final class MaskProtection {
    /** A mask without a working filter is only cloth against the face: it hardly helps. */
    public static final float WITHOUT_FILTER = 0.85f;

    private MaskProtection() {
    }

    /**
     * Share of the inhaled dose that gets through the mask.
     *
     * @param kind   the loaded filter, or null for none
     * @param charge what is left of the filter's charge
     */
    public static float inhalePass(@Nullable FilterKind kind, float charge, GasCategory category) {
        if (kind == null || charge <= 0f) {
            return WITHOUT_FILTER;
        }
        return kind.pass(category);
    }

    /** The charge left after the filter took in {@code dose}; never below zero. */
    public static float wear(float charge, float dose) {
        return Math.max(0f, charge - Math.max(0f, dose));
    }
}
