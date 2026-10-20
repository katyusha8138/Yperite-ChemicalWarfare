package io.github.katyusha8138.yperite.protection;

import io.github.katyusha8138.yperite.gas.GasCategory;
import org.jetbrains.annotations.Nullable;

/**
 * The patterns of gas mask. They differ in how well they seal on the face (a share of the air that goes round the
 * filter), in whether a filter can be changed, and in how the filters of the First World War fare against the fine
 * arsenical smoke of incapacitating agents (Blue Cross), which got through them: that share is multiplied.
 * Numbers are game tuning values.
 */
public enum MaskType {
    //                     model                   leak   filters  built-in pad               incapacitant
    MODERN("gas_mask", 0.00f, true, null, 1.0f),
    SMALL_BOX_RESPIRATOR("small_box_respirator", 0.02f, true, null, 2.5f),
    M2("m2_mask", 0.06f, false, FilterKind.IMPREGNATED, 4.0f),
    LEDERSCHUTZMASKE("lederschutzmaske", 0.03f, true, null, 3.0f),
    AUSTRIAN("austrian_gas_mask", 0.04f, true, null, 3.0f),
    ZELINSKY("zelinsky_kummant_mask", 0.02f, true, null, 3.0f);

    private final String model;
    private final float leak;
    private final boolean acceptsFilters;
    @Nullable
    private final FilterKind builtIn;
    private final float incapacitantFactor;

    MaskType(String model, float leak, boolean acceptsFilters, @Nullable FilterKind builtIn, float incapacitantFactor) {
        this.model = model;
        this.leak = leak;
        this.acceptsFilters = acceptsFilters;
        this.builtIn = builtIn;
        this.incapacitantFactor = incapacitantFactor;
    }

    /** Name of the model and texture files, and of the item. */
    public String model() {
        return model;
    }

    /** Share of the air that goes past the filter through a poor seal. */
    public float leak() {
        return leak;
    }

    public boolean acceptsFilters() {
        return acceptsFilters;
    }

    /** The pad built into the mask, for masks without a changeable filter. */
    @Nullable
    public FilterKind builtIn() {
        return builtIn;
    }

    /** True for the patterns whose filters let arsenical smoke through. */
    public boolean weakAgainstArsenicalSmoke() {
        return incapacitantFactor > 1f;
    }

    /** Share of the dose that gets through this mask with a working filter of the given kind. */
    public float pass(FilterKind kind, GasCategory category) {
        float through = kind.pass(category);
        if (category == GasCategory.INCAPACITANT) {
            through *= incapacitantFactor;
        }
        through = Math.min(1f, through);
        return Math.min(1f, leak + (1f - leak) * through);
    }
}
