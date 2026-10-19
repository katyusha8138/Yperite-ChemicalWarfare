package io.github.katyusha8138.yperite.gas;

import io.github.katyusha8138.yperite.gas.sim.Cells;
import io.github.katyusha8138.yperite.gas.sim.GasLayer;
import io.github.katyusha8138.yperite.gas.sim.GasPhysics;
import io.github.katyusha8138.yperite.gas.sim.GasStepParams;
import io.github.katyusha8138.yperite.gas.sim.GasTerrain;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GasLayerTest {
    private static final float DT = 0.2f;

    /** No decay, deposit or weather: only spreading. */
    private static GasPhysics neutral(float buoyancy, float diffusion) {
        return new GasPhysics(buoyancy, diffusion, 0f, 0f, 0f, 0f, 0f, 0f);
    }

    private static GasStepParams calm() {
        return new GasStepParams(DT, 0f, 10_000_000, 0f, 0f, 0f);
    }

    private static void run(GasLayer layer, GasTerrain terrain, GasPhysics physics, GasStepParams params, int steps) {
        for (int i = 0; i < steps; i++) {
            layer.step(terrain, physics, params);
        }
    }

    /** Mass-weighted mean of one coordinate over every gas cell. */
    private static double centroid(GasLayer layer, int axis, int sizeX, int sizeY, int sizeZ) {
        double sum = 0;
        double total = 0;
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    double c = layer.get(x, y, z);
                    sum += c * (axis == 0 ? x : axis == 1 ? y : z);
                    total += c;
                }
            }
        }
        return sum / total;
    }

    @Test
    void closedRoomConservesMassAndEvensOut() {
        BoxTerrain room = new BoxTerrain(8, 6, 8);
        GasLayer layer = new GasLayer();
        layer.add(4, 3, 4, 100f);

        run(layer, room, neutral(0f, 0.1f), calm(), 2500);

        assertEquals(100.0, layer.totalMass(), 0.05, "no mass may be lost in a closed room");
        float expected = 100f / (8 * 6 * 8);
        float min = Float.MAX_VALUE;
        float max = 0f;
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 6; y++) {
                for (int z = 0; z < 8; z++) {
                    float c = layer.get(x, y, z);
                    min = Math.min(min, c);
                    max = Math.max(max, c);
                }
            }
        }
        assertEquals(expected, min, expected * 0.05f, "gas should even out");
        assertEquals(expected, max, expected * 0.05f, "gas should even out");
    }

    @Test
    void heavyGasSinksAndLightGasRises() {
        BoxTerrain tower = new BoxTerrain(5, 12, 5);
        GasLayer heavy = new GasLayer();
        GasLayer light = new GasLayer();
        heavy.add(2, 6, 2, 50f);
        light.add(2, 6, 2, 50f);

        run(heavy, tower, neutral(-1f, 0.08f), calm(), 800);
        run(light, tower, neutral(1f, 0.08f), calm(), 800);

        double heavyY = centroid(heavy, 1, 5, 12, 5);
        double lightY = centroid(light, 1, 5, 12, 5);
        assertTrue(heavyY < 2.0, "heavy gas should collect near the floor, was " + heavyY);
        assertTrue(lightY > 9.0, "light gas should collect near the ceiling, was " + lightY);
        assertEquals(50.0, heavy.totalMass(), 0.05);
        assertEquals(50.0, light.totalMass(), 0.05);
    }

    @Test
    void heavyGasPoolsInAPitBelowTheFloorLevel() {
        // A 3x3 pit (y = 0..2) in the middle of a flat floor at y = 3. Heavy gas released beside the pit
        // should end up mostly inside it.
        BoxTerrain ground = new BoxTerrain(21, 8, 21)
                .solid((x, y, z) -> y < 3 && !(x >= 9 && x <= 11 && z >= 9 && z <= 11));
        GasLayer layer = new GasLayer();
        layer.add(7, 3, 10, 30f);

        run(layer, ground, neutral(-1f, 0.08f), calm(), 3000);

        double inPit = 0;
        for (int x = 9; x <= 11; x++) {
            for (int y = 0; y < 3; y++) {
                for (int z = 9; z <= 11; z++) {
                    inPit += layer.get(x, y, z);
                }
            }
        }
        assertTrue(inPit > 0.6 * layer.totalMass(),
                "most of the heavy gas should have drained into the pit, but only " + inPit + " of " + layer.totalMass());
    }

    @Test
    void solidWallBlocksGasButAGapLetsItThrough() {
        BoxTerrain walled = new BoxTerrain(11, 4, 5).solid((x, y, z) -> x == 5);
        GasLayer layer = new GasLayer();
        layer.add(1, 1, 2, 10f);
        run(layer, walled, neutral(0f, 0.1f), calm(), 1500);

        double farSide = 0;
        for (int x = 6; x < 11; x++) {
            for (int y = 0; y < 4; y++) {
                for (int z = 0; z < 5; z++) {
                    farSide += layer.get(x, y, z);
                }
            }
        }
        assertEquals(0.0, farSide, 0.0, "nothing may cross a closed wall");
        assertEquals(10.0, layer.totalMass(), 0.01);

        BoxTerrain withDoorway = new BoxTerrain(11, 4, 5).solid((x, y, z) -> x == 5 && !(z == 2 && y < 2));
        GasLayer leaky = new GasLayer();
        leaky.add(1, 1, 2, 10f);
        run(leaky, withDoorway, neutral(0f, 0.1f), calm(), 1500);

        double leaked = 0;
        for (int x = 6; x < 11; x++) {
            for (int y = 0; y < 4; y++) {
                for (int z = 0; z < 5; z++) {
                    leaked += leaky.get(x, y, z);
                }
            }
        }
        assertTrue(leaked > 3.0, "gas should leak through the doorway, got " + leaked);
        assertEquals(10.0, leaky.totalMass(), 0.01);
    }

    @Test
    void decayRemovesGasOverTime() {
        BoxTerrain room = new BoxTerrain(6, 4, 6);
        GasLayer layer = new GasLayer();
        layer.add(3, 2, 3, 100f);
        GasPhysics decaying = new GasPhysics(0f, 0.1f, 0.10f, 0f, 0f, 0f, 0f, 0f);

        run(layer, room, decaying, calm(), 100); // 20 seconds

        assertEquals(100.0 * Math.pow(0.9, 20), layer.totalMass(), 1.0);
    }

    @Test
    void rainAndWaterRemoveGasOnlyWhereTheyApply() {
        GasPhysics washable = new GasPhysics(0f, 0.1f, 0f, 0f, 0f, 0f, 0.2f, 0.5f);

        GasLayer dry = new GasLayer();
        dry.add(3, 2, 3, 100f);
        run(dry, new BoxTerrain(6, 4, 6), washable, calm(), 50);
        assertEquals(100.0, dry.totalMass(), 0.05);

        GasLayer rained = new GasLayer();
        rained.add(3, 2, 3, 100f);
        run(rained, new BoxTerrain(6, 4, 6).rain(true), washable, calm(), 50);
        assertEquals(100.0 * Math.pow(0.8, 10), rained.totalMass(), 1.0);

        GasLayer wet = new GasLayer();
        wet.add(3, 2, 3, 100f);
        run(wet, new BoxTerrain(6, 4, 6).submerged(true), washable, calm(), 50);
        assertEquals(100.0 * Math.pow(0.5, 10), wet.totalMass(), 0.5);
    }

    @Test
    void windCarriesGasDownwindOnlyWhereTheSkyIsOpen() {
        GasPhysics gas = neutral(0f, 0.05f);
        GasStepParams east = new GasStepParams(DT, 0f, 10_000_000, 0.8f, 0f, 2.5f);
        GasStepParams west = new GasStepParams(DT, 0f, 10_000_000, -0.8f, 0f, 2.5f);

        GasLayer still = new GasLayer();
        still.add(30, 1, 4, 20f);
        run(still, new BoxTerrain(61, 3, 9).sky(true), gas, calm(), 100);
        assertEquals(30.0, centroid(still, 0, 61, 3, 9), 0.05, "no wind, no drift");

        GasLayer blownEast = new GasLayer();
        blownEast.add(30, 1, 4, 20f);
        run(blownEast, new BoxTerrain(61, 3, 9).sky(true), gas, east, 100);
        assertTrue(centroid(blownEast, 0, 61, 3, 9) > 34.0, "wind from the west should push gas east");

        GasLayer blownWest = new GasLayer();
        blownWest.add(30, 1, 4, 20f);
        run(blownWest, new BoxTerrain(61, 3, 9).sky(true), gas, west, 100);
        assertTrue(centroid(blownWest, 0, 61, 3, 9) < 26.0, "wind from the east should push gas west");

        GasLayer indoors = new GasLayer();
        indoors.add(30, 1, 4, 20f);
        run(indoors, new BoxTerrain(61, 3, 9).sky(false), gas, east, 100);
        assertEquals(30.0, centroid(indoors, 0, 61, 3, 9), 0.05, "wind does not reach covered cells");
        assertEquals(20.0, blownEast.totalMass(), 0.05);
    }

    @Test
    void gasSettlesOntoTheFloorAsResidueAndTheTotalIsKept() {
        BoxTerrain room = new BoxTerrain(7, 5, 7);
        GasPhysics persistent = new GasPhysics(-0.5f, 0.08f, 0f, 0.5f, 0f, 0f, 0f, 0f);
        GasLayer layer = new GasLayer();
        layer.add(3, 3, 3, 40f);

        run(layer, room, persistent, calm(), 600);

        assertEquals(40.0, layer.totalMass() + layer.totalResidue(), 0.05, "gas plus residue is conserved");
        assertTrue(layer.totalResidue() > 30.0, "most gas should have settled, residue was " + layer.totalResidue());
        // Residue only exists in cells that have ground below them.
        LongArrayList keys = new LongArrayList();
        FloatArrayList values = new FloatArrayList();
        layer.exportResidue(keys, values);
        for (int i = 0; i < keys.size(); i++) {
            assertEquals(0, Cells.y(keys.getLong(i)), "residue must lie on the floor");
        }
    }

    @Test
    void residueEvaporatesBackIntoGas() {
        BoxTerrain room = new BoxTerrain(5, 4, 5);
        GasPhysics evaporating = new GasPhysics(0f, 0.08f, 0f, 0f, 0f, 0.5f, 0f, 0f);
        GasLayer layer = new GasLayer();
        layer.addResidue(2, 0, 2, 10f);

        run(layer, room, evaporating, calm(), 100);

        assertEquals(10.0, layer.totalMass() + layer.totalResidue(), 0.01);
        assertTrue(layer.totalMass() > 9.0, "nearly all residue should have returned to the air");
    }

    @Test
    void residueDecaysWithoutBecomingGas() {
        BoxTerrain room = new BoxTerrain(5, 4, 5);
        GasPhysics decaying = new GasPhysics(0f, 0.08f, 0f, 0f, 0.2f, 0f, 0f, 0f);
        GasLayer layer = new GasLayer();
        layer.addResidue(2, 0, 2, 10f);

        run(layer, room, decaying, calm(), 50);

        assertEquals(10.0 * Math.pow(0.8, 10), layer.totalResidue(), 0.1);
        assertEquals(0.0, layer.totalMass(), 0.0);
    }

    @Test
    void unloadedChunksAreFrozenAndNeverReceiveGas() {
        // Chunk 0 covers x 0..15, chunk 1 covers x 16..31.
        BoxTerrain world = new BoxTerrain(32, 3, 8).unloadChunk(1, 0);
        GasLayer layer = new GasLayer();
        layer.add(15, 1, 4, 10f);
        layer.add(20, 1, 4, 7f);

        run(layer, world, neutral(0f, 0.1f), calm(), 500);

        assertEquals(7.0, layer.get(20, 1, 4), 0.0, "frozen cell must keep its value exactly");
        double across = 0;
        for (int x = 16; x < 32; x++) {
            for (int y = 0; y < 3; y++) {
                for (int z = 0; z < 8; z++) {
                    if (x != 20 || y != 1 || z != 4) {
                        across += layer.get(x, y, z);
                    }
                }
            }
        }
        assertEquals(0.0, across, 0.0, "gas must not flow into an unloaded chunk");
        assertEquals(17.0, layer.totalMass(), 0.01);
    }

    @Test
    void gasBuriedInABlockIsPushedOutToOpenSides() {
        BoxTerrain room = new BoxTerrain(5, 5, 5).solid((x, y, z) -> x == 2 && y == 2 && z == 2);
        GasLayer layer = new GasLayer();
        layer.add(2, 2, 2, 8f);

        run(layer, room, neutral(0f, 0.05f), calm(), 1);

        assertEquals(0.0, layer.get(2, 2, 2), 0.0);
        assertEquals(8.0, layer.totalMass(), 0.001);
    }

    @Test
    void gasCompletelyEnclosedInSolidRockIsDropped() {
        BoxTerrain rock = new BoxTerrain(5, 5, 5).solid((x, y, z) -> true);
        GasLayer layer = new GasLayer();
        layer.add(2, 2, 2, 8f);

        run(layer, rock, neutral(0f, 0.05f), calm(), 1);

        assertTrue(layer.isEmpty());
    }

    @Test
    void releaseFillsOnlyOpenLoadedCellsAndReportsWhatItPlaced() {
        BoxTerrain room = new BoxTerrain(9, 9, 9).solid((x, y, z) -> x == 4 && y == 4 && z == 4);
        GasLayer layer = new GasLayer();

        float placed = layer.release(room, 4, 4, 4, 2f, 30f);

        assertEquals(30f, placed, 0.0f);
        assertEquals(30.0, layer.totalMass(), 0.001);
        assertEquals(0f, layer.get(4, 4, 4), 0f, "the solid centre cell gets nothing");
        assertTrue(layer.cellCount() > 20);

        BoxTerrain rock = new BoxTerrain(5, 5, 5).solid((x, y, z) -> true);
        GasLayer none = new GasLayer();
        assertEquals(0f, none.release(rock, 2, 2, 2, 1f, 10f), 0f);
        assertTrue(none.isEmpty());
    }

    @Test
    void cellCapKeepsTheDensestCells() {
        BoxTerrain open = new BoxTerrain(41, 41, 41);
        GasLayer layer = new GasLayer();
        layer.add(20, 20, 20, 5000f);
        GasStepParams capped = new GasStepParams(DT, 0f, 500, 0f, 0f, 0f);

        run(layer, open, neutral(0f, 0.1f), capped, 20);

        assertTrue(layer.cellCount() <= 500, "cell count " + layer.cellCount() + " exceeds the cap");
        assertTrue(layer.get(20, 20, 20) > 0f, "the densest cell must survive trimming");
    }

    @Test
    void diluteGasVanishesOnceEveryCellIsBelowTheMinimum() {
        BoxTerrain open = new BoxTerrain(61, 61, 61);
        GasLayer layer = new GasLayer();
        layer.add(30, 30, 30, 100f);
        GasStepParams params = new GasStepParams(DT, 0.01f, 1_000_000, 0f, 0f, 0f);

        run(layer, open, neutral(0f, 0.1f), params, 100);
        assertTrue(layer.cellCount() > 1, "the cloud should have spread by now");
        assertTrue(layer.totalMass() <= 100.0 + 1e-2);

        run(layer, open, neutral(0f, 0.1f), params, 900);
        assertTrue(layer.isEmpty(), "a cloud too thin to matter must disappear, but " + layer.cellCount() + " cells remain");
    }

    @Test
    void exportAndImportRoundTrip() {
        BoxTerrain room = new BoxTerrain(6, 4, 6);
        GasPhysics settling = new GasPhysics(-0.3f, 0.08f, 0.01f, 0.3f, 0.01f, 0.01f, 0f, 0f);
        GasLayer original = new GasLayer();
        original.add(3, 2, 3, 20f);
        run(original, room, settling, calm(), 40);

        LongArrayList gasKeys = new LongArrayList();
        FloatArrayList gasValues = new FloatArrayList();
        LongArrayList residueKeys = new LongArrayList();
        FloatArrayList residueValues = new FloatArrayList();
        original.exportGas(gasKeys, gasValues);
        original.exportResidue(residueKeys, residueValues);

        GasLayer copy = new GasLayer();
        copy.importGas(gasKeys.toLongArray(), gasValues.toFloatArray());
        copy.importResidue(residueKeys.toLongArray(), residueValues.toFloatArray());

        assertEquals(original.cellCount(), copy.cellCount());
        assertEquals(original.residueCellCount(), copy.residueCellCount());
        assertEquals(original.totalMass(), copy.totalMass(), 1e-6);
        assertEquals(original.totalResidue(), copy.totalResidue(), 1e-6);
        for (int i = 0; i < gasKeys.size(); i++) {
            assertEquals(gasValues.getFloat(i), copy.get(gasKeys.getLong(i)), 0f);
        }
    }

    @Test
    void largeFieldStaysWithinTheCapAndReportsTiming() {
        BoxTerrain open = new BoxTerrain(120, 40, 120).solid((x, y, z) -> y < 4);
        GasLayer layer = new GasLayer();
        layer.release(open, 60, 6, 60, 10f, 40000f);
        GasStepParams params = new GasStepParams(DT, 0.01f, 40000, 0.3f, 0.2f, 2.5f);
        GasPhysics gas = new GasPhysics(-0.5f, 0.08f, 0.02f, 0.05f, 0.002f, 0.002f, 0f, 0f);

        run(layer, open, gas, params, 5);
        long start = System.nanoTime();
        int steps = 20;
        run(layer, open, gas, params, steps);
        double millis = (System.nanoTime() - start) / 1e6 / steps;

        System.out.printf("large field: %d cells, %.2f ms per step (fake terrain)%n", layer.cellCount(), millis);
        assertTrue(layer.cellCount() <= 40000);
    }
}
