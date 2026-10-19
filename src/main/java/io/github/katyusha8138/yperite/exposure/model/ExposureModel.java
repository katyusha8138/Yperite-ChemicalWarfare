package io.github.katyusha8138.yperite.exposure.model;

/**
 * The dose model. Dose is counted in abstract "exposure units"; the numbers are game tuning values and
 * say nothing about real doses.
 *
 * <p>Two routes lead into the body. Breathing adds straight to {@code latent}. Contact adds to {@code skin},
 * from where it seeps into {@code latent} until it is washed off. {@code latent} turns into {@code active}
 * with the category's onset delay, and {@code active} fades with its recovery time. The symptom stage is
 * read from {@code active}.
 *
 * <p>All updates are exact exponential steps, so the result does not depend on the step length.
 */
public final class ExposureModel {
    /** Active dose at which stage 1 (mild), 2 (moderate), 3 (severe) and 4 (critical) begin. */
    public static final float[] STAGE_THRESHOLDS = {1f, 3f, 6f, 10f};
    public static final int MAX_STAGE = 4;

    private static final float TINY = 1e-4f;
    private static final float TINY_ACTIVE = 5e-3f;

    private ExposureModel() {
    }

    /** 0 (no symptoms) .. 4 (critical). */
    public static int stage(float active) {
        int stage = 0;
        for (float threshold : STAGE_THRESHOLDS) {
            if (active >= threshold) {
                stage++;
            }
        }
        return stage;
    }

    /**
     * Lasting damage that a given symptom level leaves: nothing below stage 2, growing to 1 at stage 4.
     */
    public static float aftermathFor(float active) {
        float low = STAGE_THRESHOLDS[1];
        float high = STAGE_THRESHOLDS[3];
        return Math.max(0f, Math.min(1f, (active - low) / (high - low)));
    }

    /**
     * Advances the state by {@code dt} seconds.
     *
     * @param inhaled       dose breathed in during the step, per category (already scaled by protection)
     * @param contact       dose that reached the skin during the step, per category (already scaled by protection)
     * @param washPerSecond how fast skin contamination is washed off; 0 when dry
     * @param recoveryScale makes symptoms fade faster (above 1) or slower (below 1)
     */
    public static void step(ExposureState state, CategoryProfile[] profiles, float[] inhaled, float[] contact,
                            float washPerSecond, float recoveryScale, float dt) {
        if (profiles.length != state.categories()) {
            throw new IllegalArgumentException("expected " + state.categories() + " profiles but got " + profiles.length);
        }
        float wash = (float) Math.exp(-Math.max(0f, washPerSecond) * dt);
        float recover = Math.max(1e-3f, recoveryScale);
        for (int k = 0; k < state.categories(); k++) {
            CategoryProfile profile = profiles[k];

            // Contact: what is on the skin seeps into the body, and the rest may be washed off.
            float skin = state.skin[k] + contact[k];
            float seeped = skin * (1f - (float) Math.exp(-dt / profile.absorbSeconds()));
            skin = (skin - seeped) * wash;

            // Dose in the body takes effect after the onset delay.
            float latent = state.latent[k] + inhaled[k] + seeped;
            float moved = latent * (1f - (float) Math.exp(-dt / profile.onsetSeconds()));
            latent -= moved;

            // Symptoms fade.
            float active = (state.active[k] + moved) * (float) Math.exp(-dt * recover / profile.recoverySeconds());

            state.skin[k] = skin < TINY ? 0f : skin;
            state.latent[k] = latent < TINY ? 0f : latent;
            state.active[k] = active < TINY_ACTIVE ? 0f : active;
            state.aftermath[k] = Math.max(state.aftermath[k], aftermathFor(state.active[k]));
        }
    }
}
