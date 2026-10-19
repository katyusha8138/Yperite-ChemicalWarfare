package io.github.katyusha8138.yperite.protection;

import io.github.katyusha8138.yperite.gas.GasCategory;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtectionMathTest {
    @Test
    void everyFilterPassesAShareBetweenZeroAndOneForEveryCategory() {
        for (FilterKind kind : FilterKind.values()) {
            for (GasCategory category : GasCategory.values()) {
                float pass = kind.pass(category);
                assertTrue(pass > 0f && pass < 1f, kind + " / " + category + " pass=" + pass);
            }
            assertTrue(kind.capacity() > 0f);
        }
    }

    @Test
    void theSpecialFilterIsAtLeastAsGoodAsTheGeneralOneEverywhereAndMuchBetterAgainstBloodAndNerveAgents() {
        for (GasCategory category : GasCategory.values()) {
            assertTrue(FilterKind.SPECIAL.pass(category) <= FilterKind.GENERAL.pass(category),
                    "special must not be worse against " + category);
        }
        assertTrue(FilterKind.GENERAL.pass(GasCategory.NERVE) > 0.5f, "a general filter barely helps against nerve agents");
        assertTrue(FilterKind.GENERAL.pass(GasCategory.BLOOD) > 0.5f, "a general filter barely helps against blood agents");
        assertTrue(FilterKind.SPECIAL.pass(GasCategory.NERVE) < 0.1f);
        assertTrue(FilterKind.SPECIAL.pass(GasCategory.BLOOD) < 0.1f);
        assertTrue(FilterKind.GENERAL.pass(GasCategory.CHOKING) < 0.1f, "a general filter handles choking agents");
    }

    @Test
    void filtersAreFoundByTheirId() {
        for (FilterKind kind : FilterKind.values()) {
            assertEquals(kind, FilterKind.byId(kind.id()));
        }
        assertNull(FilterKind.byId("nonsense"));
    }

    @Test
    void aMaskWithoutAWorkingFilterBarelyHelps() {
        assertEquals(MaskProtection.WITHOUT_FILTER, MaskProtection.inhalePass(null, 100f, GasCategory.CHOKING), 0f);
        assertEquals(MaskProtection.WITHOUT_FILTER, MaskProtection.inhalePass(FilterKind.SPECIAL, 0f, GasCategory.NERVE), 0f);
        assertEquals(FilterKind.SPECIAL.pass(GasCategory.NERVE), MaskProtection.inhalePass(FilterKind.SPECIAL, 1f, GasCategory.NERVE), 0f);
        assertTrue(MaskProtection.WITHOUT_FILTER > 0.5f);
    }

    @Test
    void aFilterWearsByTheDoseItTakesAndNeverGoesBelowZero() {
        assertEquals(90f, MaskProtection.wear(100f, 10f), 0f);
        assertEquals(0f, MaskProtection.wear(5f, 10f), 0f);
        assertEquals(100f, MaskProtection.wear(100f, -3f), 0f);
    }

    @Test
    void theCoverageOfAllPartsAddsUpToTheWholeBody() {
        float total = 0f;
        for (SuitProtection.Part part : SuitProtection.Part.values()) {
            total += part.coverage();
        }
        assertEquals(1f, total, 1e-6f);
    }

    @Test
    void aFullOutfitLeavesOnlyAFewPercentAndEachMissingPartLetsMoreThrough() {
        EnumSet<SuitProtection.Part> all = EnumSet.allOf(SuitProtection.Part.class);
        float full = SuitProtection.skinPass(all);
        assertTrue(full < 0.08f, "a full outfit should keep out nearly everything, passed " + full);

        assertEquals(1f, SuitProtection.skinPass(EnumSet.noneOf(SuitProtection.Part.class)), 0f);
        for (SuitProtection.Part missing : SuitProtection.Part.values()) {
            EnumSet<SuitProtection.Part> partial = EnumSet.copyOf(all);
            partial.remove(missing);
            assertTrue(SuitProtection.skinPass(partial) > full, "missing " + missing + " must let more through");
        }
        // The jacket covers the most, the boots and the hood the least.
        EnumSet<SuitProtection.Part> noJacket = EnumSet.copyOf(all);
        noJacket.remove(SuitProtection.Part.JACKET);
        EnumSet<SuitProtection.Part> noBoots = EnumSet.copyOf(all);
        noBoots.remove(SuitProtection.Part.BOOTS);
        assertTrue(SuitProtection.skinPass(noJacket) > SuitProtection.skinPass(noBoots));
    }

    @Test
    void detectorLevelsFollowTheConcentration() {
        assertEquals(DetectorLevel.NONE, DetectorLevel.of(0f));
        assertEquals(DetectorLevel.NONE, DetectorLevel.of(0.019f));
        assertEquals(DetectorLevel.LOW, DetectorLevel.of(0.02f));
        assertEquals(DetectorLevel.MEDIUM, DetectorLevel.of(0.1f));
        assertEquals(DetectorLevel.HIGH, DetectorLevel.of(0.5f));
        assertEquals(DetectorLevel.EXTREME, DetectorLevel.of(1.5f));
        assertEquals(DetectorLevel.EXTREME, DetectorLevel.of(500f));
    }

    @Test
    void theAlarmSignalGrowsWithTheLevel() {
        assertEquals(0, DetectorLevel.redstone(DetectorLevel.NONE));
        assertEquals(3, DetectorLevel.redstone(DetectorLevel.LOW));
        assertEquals(7, DetectorLevel.redstone(DetectorLevel.MEDIUM));
        assertEquals(11, DetectorLevel.redstone(DetectorLevel.HIGH));
        assertEquals(15, DetectorLevel.redstone(DetectorLevel.EXTREME));
    }
}
