package io.github.katyusha8138.yperite.exposure;

import io.github.katyusha8138.yperite.exposure.model.CategoryProfile;
import io.github.katyusha8138.yperite.exposure.model.ExposureModel;
import io.github.katyusha8138.yperite.exposure.model.ExposureState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExposureModelTest {
    /** Category 0 reacts at once and fades fast; category 1 is delayed and lingers; category 2 is in between. */
    private static final CategoryProfile[] PROFILES = {
            new CategoryProfile(0.5f, 20f, 30f),
            new CategoryProfile(75f, 600f, 90f),
            new CategoryProfile(4f, 120f, 30f),
    };

    private static ExposureState state() {
        return new ExposureState(PROFILES.length);
    }

    private static float[] none() {
        return new float[PROFILES.length];
    }

    private static float[] only(int category, float amount) {
        float[] values = new float[PROFILES.length];
        values[category] = amount;
        return values;
    }

    private static void run(ExposureState s, float seconds, float dt, float[] inhaled, float[] contact, float wash, float recovery) {
        int steps = Math.round(seconds / dt);
        for (int i = 0; i < steps; i++) {
            ExposureModel.step(s, PROFILES, inhaled, contact, wash, recovery, dt);
        }
    }

    private static void idle(ExposureState s, float seconds) {
        run(s, seconds, 0.25f, none(), none(), 0f, 1f);
    }

    @Test
    void stagesFollowTheThresholds() {
        assertEquals(0, ExposureModel.stage(0f));
        assertEquals(0, ExposureModel.stage(0.99f));
        assertEquals(1, ExposureModel.stage(1f));
        assertEquals(1, ExposureModel.stage(2.99f));
        assertEquals(2, ExposureModel.stage(3f));
        assertEquals(3, ExposureModel.stage(6f));
        assertEquals(4, ExposureModel.stage(10f));
        assertEquals(4, ExposureModel.stage(1000f));
    }

    @Test
    void onsetIsDelayedForSlowCategoriesAndImmediateForFastOnes() {
        ExposureState slow = state();
        slow.latent[1] = 6f;
        idle(slow, 5f);
        assertTrue(slow.active[1] < 0.5f, "a delayed category must not show symptoms right away, active=" + slow.active[1]);

        idle(slow, 70f);
        assertTrue(slow.active[1] > 3f, "symptoms should be well under way after the onset time, active=" + slow.active[1]);

        ExposureState fast = state();
        fast.latent[0] = 6f;
        idle(fast, 2f);
        assertTrue(fast.active[0] > 4.5f, "a fast category shows symptoms within seconds, active=" + fast.active[0]);
    }

    @Test
    void symptomsFadeWithTheRecoveryTimeAndRecoverySpeedScalesIt() {
        ExposureState normal = state();
        normal.active[2] = 8f;
        idle(normal, 120f);
        assertEquals(8f / Math.E, normal.active[2], 0.1f, "one time constant leaves 1/e");

        ExposureState quick = state();
        quick.active[2] = 8f;
        run(quick, 60f, 0.25f, none(), none(), 0f, 2f);
        assertEquals(8f / Math.E, quick.active[2], 0.1f, "twice the recovery speed halves the time");
    }

    @Test
    void theResultDoesNotDependOnTheStepLength() {
        ExposureState coarse = state();
        ExposureState fine = state();
        coarse.latent[2] = 5f;
        fine.latent[2] = 5f;
        run(coarse, 20f, 1f, none(), none(), 0f, 1f);
        run(fine, 20f, 0.05f, none(), none(), 0f, 1f);
        assertEquals(fine.active[2], coarse.active[2], 0.02f);
        assertEquals(fine.latent[2], coarse.latent[2], 0.02f);
    }

    @Test
    void breathingInAddsToTheBodyAndCategoriesStayApart() {
        ExposureState s = state();
        run(s, 10f, 0.25f, only(2, 0.5f), none(), 0f, 1f);
        assertTrue(s.active[2] > 1f, "ten seconds of a steady dose should cause symptoms, active=" + s.active[2]);
        assertEquals(0f, s.active[0], 0f);
        assertEquals(0f, s.active[1], 0f);
        assertEquals(0f, s.latent[0] + s.latent[1], 0f);
    }

    @Test
    void contaminationKeepsSeepingInAfterLeavingTheGasUntilItIsWashedOff() {
        ExposureState dirty = state();
        dirty.skin[1] = 10f;
        idle(dirty, 120f);
        float takenIn = dirty.latent[1] + dirty.active[1];
        assertTrue(takenIn > 3f, "dirty skin should keep feeding the body, took in " + takenIn);

        ExposureState washed = state();
        washed.skin[1] = 10f;
        run(washed, 120f, 0.25f, none(), none(), 0.5f, 1f); // standing in water
        float takenInWashed = washed.latent[1] + washed.active[1];
        assertTrue(takenInWashed < takenIn * 0.2f,
                "washing should stop most of it: " + takenInWashed + " vs " + takenIn);
        assertEquals(0f, washed.skin[1], 0.01f);
    }

    @Test
    void doseThatReachesTheSkinIsNeverLost() {
        // No recovery to speak of and no washing: everything that arrives must be somewhere in the state.
        ExposureState s = state();
        float arrived = 0f;
        for (int i = 0; i < 200; i++) {
            ExposureModel.step(s, PROFILES, only(2, 0.2f), only(1, 0.1f), 0f, 1e-3f * 1e-3f, 0.25f);
            arrived += 0.2f + 0.1f;
        }
        float stored = 0f;
        for (int k = 0; k < PROFILES.length; k++) {
            stored += s.skin[k] + s.latent[k] + s.active[k];
        }
        assertEquals(arrived, stored, arrived * 0.01f);
    }

    @Test
    void lastingDamageStartsAtStageTwoGrowsWithTheWorstSymptomsAndNeverShrinks() {
        assertEquals(0f, ExposureModel.aftermathFor(2.9f), 0f);
        assertEquals(0f, ExposureModel.aftermathFor(3f), 1e-6f);
        assertEquals(1f, ExposureModel.aftermathFor(10f), 1e-6f);
        assertEquals(1f, ExposureModel.aftermathFor(40f), 0f);

        ExposureState s = state();
        s.latent[2] = 12f;
        idle(s, 12f);
        float worst = s.aftermath[2];
        assertTrue(worst > 0.3f, "a heavy exposure should leave something behind, got " + worst);

        idle(s, 1200f);
        assertEquals(0f, s.active[2], 0f, "the symptoms are gone");
        assertEquals(worst, s.aftermath[2], 0f, "but the lasting damage stays");

        s.latent[2] = 4f;
        idle(s, 30f);
        assertEquals(worst, s.aftermath[2], 0f, "a lighter exposure afterwards does not undo it");
    }

    @Test
    void aMildExposureLeavesNothingBehind() {
        ExposureState s = state();
        s.latent[2] = 2f;
        idle(s, 60f);
        assertEquals(0f, s.aftermath[2], 0f);
    }

    @Test
    void aCleanBodyStaysCleanAndClearRemovesEverything() {
        ExposureState s = state();
        idle(s, 60f);
        assertTrue(s.isClean());

        s.latent[0] = 3f;
        s.aftermath[1] = 0.5f;
        assertFalse(s.isClean());
        s.clear();
        assertTrue(s.isClean());
    }

    @Test
    void fadingSymptomsReachExactlyZeroSoACleanBodyIsRecognised() {
        ExposureState s = state();
        s.latent[0] = 2f; // below the lasting-damage threshold
        idle(s, 600f);
        assertEquals(0f, s.active[0], 0f);
        assertTrue(s.isClean());
    }

    @Test
    void copyFromDuplicatesTheState() {
        ExposureState a = state();
        a.skin[0] = 1f;
        a.latent[1] = 2f;
        a.active[2] = 3f;
        a.aftermath[1] = 0.4f;
        ExposureState b = state();
        b.copyFrom(a);
        assertEquals(1f, b.skin[0], 0f);
        assertEquals(2f, b.latent[1], 0f);
        assertEquals(3f, b.active[2], 0f);
        assertEquals(0.4f, b.aftermath[1], 0f);
    }
}
