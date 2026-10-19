package io.github.katyusha8138.yperite.gas;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GasSightTest {
    @Test
    void clearAirIsClear() {
        GasSight.Sight sight = GasSight.mix(new float[]{0f, 0f}, new int[]{0xFF0000, 0x00FF00});
        assertTrue(sight.clear());
        assertEquals(0f, GasSight.tint(0f), 0f);
    }

    @Test
    void theColourOfAMixIsWeightedByHowMuchOfEachHangsInTheAir() {
        GasSight.Sight sight = GasSight.mix(new float[]{3f, 1f}, new int[]{0xFF0000, 0x0000FF});
        assertEquals(4f, sight.density(), 1e-6f);
        assertEquals(0xBF, (sight.color() >> 16) & 0xFF);
        assertEquals(0x40, sight.color() & 0xFF);
        assertEquals(0, (sight.color() >> 8) & 0xFF);
    }

    @Test
    void thickerGasTintsMoreAndHidesMore() {
        float lastTint = 0f;
        float lastView = Float.MAX_VALUE;
        for (float d = 0.1f; d < 5f; d += 0.1f) {
            float tint = GasSight.tint(d);
            float view = GasSight.visibility(d);
            assertTrue(tint > lastTint && tint < 1f, "tint must grow below 1 at " + d);
            assertTrue(view < lastView && view >= 4f, "the view must shrink, but not below a few blocks, at " + d);
            lastTint = tint;
            lastView = view;
        }
        assertTrue(GasSight.visibility(0.05f) > 50f, "a trace of gas hardly shortens the view");
        assertTrue(GasSight.visibility(2f) < 10f, "thick gas closes the view to a few blocks");
    }
}
