package io.github.katyusha8138.yperite.protection;

import java.util.Set;

/**
 * What protective clothing does against gas that reaches the skin. Each part covers a share of the body, and a
 * part that is worn keeps its effectiveness of that share out. The shares add up to 1, so a full outfit
 * (hood or mask on the head, jacket, trousers, boots) leaves only a few percent.
 */
public final class SuitProtection {
    public enum Part {
        /** The protective hood, worn in the helmet slot. */
        HOOD(0.10f, 0.95f),
        /** The face, covered by a gas mask. */
        MASK(0.10f, 0.90f),
        JACKET(0.40f, 0.95f),
        TROUSERS(0.30f, 0.95f),
        BOOTS(0.10f, 0.95f);

        private final float coverage;
        private final float effectiveness;

        Part(float coverage, float effectiveness) {
            this.coverage = coverage;
            this.effectiveness = effectiveness;
        }

        /** Share of the body this part covers. */
        public float coverage() {
            return coverage;
        }

        /** Share of the dose on that part of the body that it keeps out. */
        public float effectiveness() {
            return effectiveness;
        }
    }

    private SuitProtection() {
    }

    /** Share of the skin dose that gets through when the given parts are worn. */
    public static float skinPass(Set<Part> worn) {
        float kept = 0f;
        for (Part part : worn) {
            kept += part.coverage() * part.effectiveness();
        }
        return Math.max(0f, 1f - kept);
    }
}
