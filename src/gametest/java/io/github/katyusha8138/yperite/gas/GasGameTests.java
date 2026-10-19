package io.github.katyusha8138.yperite.gas;

import io.github.katyusha8138.yperite.YperiteMod;
import io.github.katyusha8138.yperite.gas.sim.GasLayer;
import io.github.katyusha8138.yperite.gas.sim.GasTerrain;
import io.github.katyusha8138.yperite.gametest.TestRooms;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * In-world checks that need real blocks, chunks and the command tree.
 *
 * <p>Every test uses a 20x10x20 template with a stone floor at y = 0. The room is sealed with stone and split
 * in two by a divider at x = 10, so gas can only cross the divider through whatever the test puts there.
 * Left room: x 1..9, right room: x 11..18, both z 1..18 and y 1..8.
 */
@GameTestHolder(YperiteMod.MODID)
@PrefixGameTestTemplate(false)
public final class GasGameTests {
    private static final String ROOM = "empty_20x10x20";
    private static final String BIG = "empty_32x16x32";

    private GasGameTests() {
    }

    // ------------------------------------------------------------------ clean slate

    /** Gas and the fixed wind are saved with the world, so every batch starts by wiping them. */
    private static void reset(ServerLevel level) {
        GasManager.clear(level, null);
        GasManager.data(level).setWindOverride(null);
    }

    @BeforeBatch(batch = "defaultBatch")
    public static void resetBeforeDefaultBatch(ServerLevel level) {
        reset(level);
    }

    @BeforeBatch(batch = "wind")
    public static void resetBeforeWindBatch(ServerLevel level) {
        reset(level);
    }

    /** The wind command test has a batch of its own: a test that fixes the wind must not run at the same time. */
    @BeforeBatch(batch = "windcommand")
    public static void resetBeforeWindCommandBatch(ServerLevel level) {
        reset(level);
    }

    @BeforeBatch(batch = "cost")
    public static void resetBeforeCostBatch(ServerLevel level) {
        reset(level);
    }

    // ------------------------------------------------------------------ helpers

    private static BlockPos pos(int x, int y, int z) {
        return TestRooms.pos(x, y, z);
    }

    private static void sealRoom(GameTestHelper helper) {
        TestRooms.sealRoom(helper);
    }

    /** Fills the divider at x = 10 with the given block, then lets the caller carve openings. */
    private static void divide(GameTestHelper helper, BlockState state) {
        for (int y = 1; y < 9; y++) {
            for (int z = 1; z < 19; z++) {
                helper.setBlock(pos(10, y, z), state);
            }
        }
    }

    private static void placeDoor(GameTestHelper helper, int x, int z, boolean open) {
        BlockState lower = Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.EAST)
                .setValue(DoorBlock.OPEN, open);
        helper.setBlock(pos(x, 1, z), lower.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        helper.setBlock(pos(x, 2, z), lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    private static double mass(GameTestHelper helper, GasType type, int xMin, int xMax) {
        ServerLevel level = helper.getLevel();
        double total = 0;
        for (int x = xMin; x <= xMax; x++) {
            for (int y = 1; y < 9; y++) {
                for (int z = 1; z < 19; z++) {
                    total += GasManager.concentration(level, type, helper.absolutePos(pos(x, y, z)));
                }
            }
        }
        return total;
    }

    private static void releaseInLeftRoom(GameTestHelper helper, GasType type, float mass) {
        float placed = GasManager.release(helper.getLevel(), type, helper.absolutePos(pos(5, 2, 10)), 1f, mass);
        helper.assertTrue(placed > 0f, "the release itself must succeed");
    }

    private static void dividerCase(GameTestHelper helper, BlockState divider, boolean expectLeak) {
        GasType gas = GasTypes.TEAR_GAS.get();
        sealRoom(helper);
        divide(helper, divider);
        releaseInLeftRoom(helper, gas, 1500f);
        helper.runAfterDelay(200, () -> {
            double left = mass(helper, gas, 1, 9);
            double right = mass(helper, gas, 11, 18);
            helper.assertTrue(left > 1.0, "gas must still be in the room it was released in, left=" + left);
            if (expectLeak) {
                helper.assertTrue(right > 0.5, "gas should have crossed the divider, right=" + right);
            } else {
                helper.assertTrue(right == 0.0, "gas crossed a divider that should block it, right=" + right);
            }
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ blocking

    @GameTest(template = ROOM, timeoutTicks = 400)
    public static void stoneDividerBlocksGas(GameTestHelper helper) {
        dividerCase(helper, Blocks.STONE.defaultBlockState(), false);
    }

    @GameTest(template = ROOM, timeoutTicks = 400)
    public static void glassPaneDividerBlocksGas(GameTestHelper helper) {
        dividerCase(helper, Blocks.GLASS_PANE.defaultBlockState(), false);
    }

    @GameTest(template = ROOM, timeoutTicks = 400)
    public static void ironBarsLetGasThrough(GameTestHelper helper) {
        dividerCase(helper, Blocks.IRON_BARS.defaultBlockState(), true);
    }

    @GameTest(template = ROOM, timeoutTicks = 400)
    public static void leavesLetGasThrough(GameTestHelper helper) {
        dividerCase(helper, Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true), true);
    }

    @GameTest(template = ROOM, timeoutTicks = 400)
    public static void closedDoorBlocksGas(GameTestHelper helper) {
        GasType gas = GasTypes.TEAR_GAS.get();
        sealRoom(helper);
        divide(helper, Blocks.STONE.defaultBlockState());
        placeDoor(helper, 10, 10, false);
        releaseInLeftRoom(helper, gas, 1500f);
        helper.runAfterDelay(200, () -> {
            double right = mass(helper, gas, 11, 18);
            helper.assertTrue(right == 0.0, "gas went through a closed door, right=" + right);
            helper.succeed();
        });
    }

    @GameTest(template = ROOM, timeoutTicks = 400)
    public static void openDoorLetsGasThrough(GameTestHelper helper) {
        GasType gas = GasTypes.TEAR_GAS.get();
        sealRoom(helper);
        divide(helper, Blocks.STONE.defaultBlockState());
        placeDoor(helper, 10, 10, true);
        releaseInLeftRoom(helper, gas, 1500f);
        helper.runAfterDelay(200, () -> {
            double right = mass(helper, gas, 11, 18);
            helper.assertTrue(right > 0.05, "gas should pass an open door, right=" + right);
            helper.succeed();
        });
    }

    @GameTest(template = ROOM, timeoutTicks = 400)
    public static void blockPlacedIntoGasPushesItOut(GameTestHelper helper) {
        GasType gas = GasTypes.TEAR_GAS.get();
        sealRoom(helper);
        BlockPos cell = pos(5, 2, 10);
        GasManager.release(helper.getLevel(), gas, helper.absolutePos(cell), 0f, 50f);
        helper.setBlock(cell, Blocks.STONE.defaultBlockState());
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(GasManager.concentration(helper.getLevel(), gas, helper.absolutePos(cell)) == 0f,
                    "a cell inside a solid block must not keep gas");
            helper.assertTrue(mass(helper, gas, 1, 18) > 30.0, "the gas must be pushed out, not destroyed, mass=" + mass(helper, gas, 1, 18));
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ behaviour in a real room

    @GameTest(template = ROOM, timeoutTicks = 600)
    public static void heavyGasEndsUpLowerThanLightGas(GameTestHelper helper) {
        GasType heavy = GasTypes.CHLORINE.get();
        GasType light = GasTypes.TEAR_GAS.get();
        sealRoom(helper);
        BlockPos middle = helper.absolutePos(pos(9, 5, 9));
        GasManager.release(helper.getLevel(), heavy, middle, 1f, 1500f);
        GasManager.release(helper.getLevel(), light, middle, 1f, 1500f);
        helper.runAfterDelay(200, () -> {
            double heavyY = meanHeight(helper, heavy);
            double lightY = meanHeight(helper, light);
            helper.assertTrue(mass(helper, heavy, 1, 18) > 1.0 && mass(helper, light, 1, 18) > 1.0, "both gases must still exist");
            helper.assertTrue(heavyY < lightY, "heavy gas (" + heavyY + ") should sit below light gas (" + lightY + ")");
            helper.succeed();
        });
    }

    private static double meanHeight(GameTestHelper helper, GasType type) {
        ServerLevel level = helper.getLevel();
        double weighted = 0;
        double total = 0;
        for (int x = 1; x < 19; x++) {
            for (int y = 1; y < 9; y++) {
                for (int z = 1; z < 19; z++) {
                    double c = GasManager.concentration(level, type, helper.absolutePos(pos(x, y, z)));
                    weighted += c * y;
                    total += c;
                }
            }
        }
        return total == 0 ? 0 : weighted / total;
    }

    @GameTest(template = ROOM, timeoutTicks = 600)
    public static void vesicantLeavesResidueOnTheFloor(GameTestHelper helper) {
        GasType yperite = GasTypes.YPERITE.get();
        ServerLevel level = helper.getLevel();
        sealRoom(helper);
        BlockPos start = helper.absolutePos(pos(9, 2, 9));
        GasManager.release(level, yperite, start, 1f, 600f);
        helper.runAfterDelay(300, () -> {
            double residue = 0;
            for (int x = 1; x < 19; x++) {
                for (int z = 1; z < 19; z++) {
                    residue += GasManager.residue(level, yperite, helper.absolutePos(pos(x, 1, z)));
                }
            }
            GasLayer layer = GasManager.data(level).layerIfPresent(yperite);
            int floorFlags = new LevelTerrain(level, false, false).flags(start.getX(), helper.absolutePos(pos(0, 1, 0)).getY(), start.getZ());
            helper.assertTrue(residue > 1.0, "gas should have settled on the floor: residue=" + residue
                    + ", gasMass=" + (layer == null ? -1 : layer.totalMass())
                    + ", cells=" + (layer == null ? -1 : layer.cellCount())
                    + ", totalResidue=" + (layer == null ? -1 : layer.totalResidue())
                    + ", flagsAtFloorCell=0b" + Integer.toBinaryString(floorFlags));
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ persistence and commands

    @GameTest(template = ROOM, timeoutTicks = 200)
    public static void savedDataSurvivesASaveAndLoad(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        sealRoom(helper);
        GasManager.release(level, GasTypes.CHLORINE.get(), helper.absolutePos(pos(5, 3, 5)), 2f, 40f);
        GasManager.release(level, GasTypes.YPERITE.get(), helper.absolutePos(pos(12, 2, 12)), 1f, 20f);
        helper.runAfterDelay(60, () -> {
            GasSavedData original = GasManager.data(level);
            CompoundTag saved = original.save(new CompoundTag());
            GasSavedData copy = GasSavedData.load(saved);

            for (GasType type : new GasType[]{GasTypes.CHLORINE.get(), GasTypes.YPERITE.get()}) {
                GasLayer a = original.layerIfPresent(type);
                GasLayer b = copy.layerIfPresent(type);
                helper.assertTrue(a != null && b != null, "layer missing after reload for " + type.id());
                helper.assertTrue(a.cellCount() == b.cellCount(), "gas cell count changed for " + type.id());
                helper.assertTrue(a.residueCellCount() == b.residueCellCount(), "residue cell count changed for " + type.id());
                helper.assertTrue(Math.abs(a.totalMass() - b.totalMass()) < 1e-3, "gas mass changed for " + type.id());
                helper.assertTrue(Math.abs(a.totalResidue() - b.totalResidue()) < 1e-3, "residue mass changed for " + type.id());
            }
            helper.succeed();
        });
    }

    @GameTest(template = ROOM, timeoutTicks = 200)
    public static void debugCommandsWork(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        sealRoom(helper);
        BlockPos pos = helper.absolutePos(pos(5, 2, 5));
        CommandSourceStack source = level.getServer().createCommandSourceStack().withLevel(level)
                .withPosition(net.minecraft.world.phys.Vec3.atCenterOf(pos));

        run(helper, source, "yperite gas burst yperite:nerve_gas 25 1 " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
        helper.assertTrue(GasManager.concentration(level, GasTypes.NERVE_GAS.get(), pos) > 0f, "burst command released nothing");

        run(helper, source, "yperite gas info");
        run(helper, source, "yperite gas probe");

        run(helper, source, "yperite gas clear yperite:nerve_gas");
        helper.assertTrue(GasManager.concentration(level, GasTypes.NERVE_GAS.get(), pos) == 0f, "clear command left gas behind");

        helper.succeed();
    }

    private static void run(GameTestHelper helper, CommandSourceStack source, String command) {
        int result = source.getServer().getCommands().performPrefixedCommand(source, command);
        helper.assertTrue(result > 0, "command failed: /" + command);
    }

    // ------------------------------------------------------------------ openness and wind
    // These change the dimension-wide wind, so they run in their own batch, one after another.

    /**
     * The terrain must report exactly the level's sky light. (The GameTest world does not light the space near its
     * floor, so this checks the wiring at several heights rather than any particular brightness.)
     */
    @GameTest(template = ROOM, timeoutTicks = 100)
    public static void terrainReportsTheLevelsSkyLight(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        sealRoom(helper);
        LevelTerrain terrain = new LevelTerrain(level, true, false);
        BlockPos base = helper.absolutePos(pos(5, 3, 5));
        for (int dy : new int[]{0, 6, 40, 100, 150, 200}) {
            BlockPos at = base.above(dy);
            int expected = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, at);
            int actual = GasTerrain.skyLight(terrain.flags(at.getX(), at.getY(), at.getZ()));
            helper.assertTrue(expected == actual, "sky light at +" + dy + " should be " + expected + " but the terrain reported " + actual);
        }
        // Without the sky option the terrain reports no sky light at all.
        LevelTerrain blind = new LevelTerrain(level, false, false);
        BlockPos high = base.above(200);
        helper.assertTrue(GasTerrain.skyLight(blind.flags(high.getX(), high.getY(), high.getZ())) == 0, "sky light must be omitted when not requested");
        helper.succeed();
    }

    @GameTest(template = ROOM, timeoutTicks = 200, batch = "windcommand")
    public static void windCommandsSetShowAndResetTheWind(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        CommandSourceStack source = level.getServer().createCommandSourceStack().withLevel(level);
        GasSavedData data = GasManager.data(level);

        run(helper, source, "yperite wind");
        helper.assertTrue(data.windOverride() == null, "the wind starts out automatic");

        run(helper, source, "yperite wind set 270 0.5");
        WindModel.Override fixed = data.windOverride();
        helper.assertTrue(fixed != null && fixed.fromDegrees() == 270f && fixed.strength() == 0.5f, "wind set did not store the wind");
        WindModel.Wind wind = GasManager.wind(level);
        helper.assertTrue(wind.x() > 0.49f && Math.abs(wind.z()) < 0.01f,
                "wind from the west must blow east (+x), got x=" + wind.x() + " z=" + wind.z());
        helper.assertTrue(Math.abs(wind.fromDegrees() - 270f) < 0.5f, "fromDegrees round trip: " + wind.fromDegrees());
        run(helper, source, "yperite wind");

        GasSavedData reloaded = GasSavedData.load(data.save(new CompoundTag()));
        helper.assertTrue(reloaded.windOverride() != null && reloaded.windOverride().strength() == 0.5f,
                "the fixed wind must survive a save and load");

        run(helper, source, "yperite wind auto");
        helper.assertTrue(data.windOverride() == null, "wind auto must clear the fixed wind");
        helper.succeed();
    }

    /**
     * End to end on a real server: fixed wind, real sky light, real chunks. The cloud is released high above the arena
     * because the GameTest world only has sky light up there. A light gas is used so that it does not sink back into
     * the unlit space below.
     */
    @GameTest(template = BIG, timeoutTicks = 300, batch = "wind")
    public static void fixedWindCarriesOpenAirGasDownwind(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        GasType gas = GasTypes.BLUE_CROSS.get();
        GasSavedData data = GasManager.data(level);
        // From the west at half strength: 2 blocks per second towards +x with the default maximum speed.
        data.setWindOverride(new WindModel.Override(270f, 0.5f));

        // Find a height where the sky light is 15 at the release point and along the way the cloud will travel.
        BlockPos start = null;
        for (int up = 60; up <= 260 && start == null; up += 8) {
            BlockPos candidate = helper.absolutePos(pos(16, 3, 16)).above(up);
            if (level.isOutsideBuildHeight(candidate.above(12))) {
                break;
            }
            boolean lit = true;
            for (int dx = -4; dx <= 24 && lit; dx += 4) {
                lit = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, candidate.offset(dx, 0, 0)) == 15
                        && level.getBrightness(net.minecraft.world.level.LightLayer.SKY, candidate.offset(dx, 6, 0)) == 15;
            }
            if (lit) {
                start = candidate.above(4);
            }
        }
        if (start == null) {
            data.setWindOverride(null);
            helper.fail("could not find open sky above the arena in this world");
            return;
        }
        final BlockPos release = start;
        GasManager.release(level, gas, release, 1f, 3000f);
        LevelTerrain terrain = new LevelTerrain(level, true, false);
        int light = GasTerrain.skyLight(terrain.flags(release.getX(), release.getY(), release.getZ()));
        if (light != 15) {
            data.setWindOverride(null);
            GasManager.clear(level, gas);
            helper.fail("the release point should be under open sky, sky light was " + light);
            return;
        }

        helper.runAfterDelay(100, () -> {
            double weighted = 0;
            double total = 0;
            for (int x = -16; x < 48; x++) {
                for (int y = -12; y <= 12; y++) {
                    for (int z = 0; z < 32; z++) {
                        BlockPos at = release.offset(x, y, z - 16);
                        double c = GasManager.concentration(level, gas, at);
                        weighted += c * at.getX();
                        total += c;
                    }
                }
            }
            data.setWindOverride(null);
            GasManager.clear(level, gas);
            helper.assertTrue(total > 100.0, "most of the gas should still be in the sampled box, total=" + total);
            double shift = weighted / total - release.getX();
            helper.assertTrue(shift > 5.0, "5 seconds of a 2 blocks per second wind should move the cloud east by about 10 blocks, shift=" + shift);
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ cost

    /** Not a pass/fail comparison of speed; it prints the real cost per step and guards against disasters. */
    @GameTest(template = BIG, timeoutTicks = 200, batch = "cost")
    public static void stepCostInRealTerrain(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        GasType gas = GasTypes.CHLORINE.get();
        GasManager.release(level, gas, helper.absolutePos(pos(16, 3, 16)), 9f, 30000f);
        GasSavedData data = GasManager.data(level);

        for (int i = 0; i < 5; i++) {
            GasManager.step(level, data, 4);
        }
        long start = System.nanoTime();
        int steps = 20;
        for (int i = 0; i < steps; i++) {
            GasManager.step(level, data, 4);
        }
        double millis = (System.nanoTime() - start) / 1e6 / steps;
        int cells = data.layer(gas).cellCount();
        YperiteMod.LOGGER.info("[gametest] real terrain: {} cells, {} ms per step", cells, String.format("%.2f", millis));
        GasManager.clear(level, gas);
        helper.assertTrue(millis < 400.0, "a single gas step took " + millis + " ms");
        helper.succeed();
    }

    @GameTest(template = ROOM, timeoutTicks = 40)
    public static void aPlayerInsideACloudSeesHazeInItsColour(GameTestHelper helper) {
        sealRoom(helper);
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        net.minecraft.core.BlockPos centre = helper.absolutePos(pos(4, 2, 4));
        GasManager.release(level, GasTypes.CHLORINE.get(), centre, 2f, 200f);
        GasSavedData data = GasManager.data(level);
        GasSight.Sight inside = GasSight.at(level, data, net.minecraft.world.phys.Vec3.atCenterOf(centre));
        GasSight.Sight away = GasSight.at(level, data, net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(pos(16, 2, 16))));
        helper.assertTrue(inside.density() > 0.5f, "thick chlorine should haze the view, density " + inside.density());
        helper.assertTrue(inside.color() == GasTypes.CHLORINE.get().color(), "the haze takes the colour of the gas");
        helper.assertTrue(away.clear(), "clear air away from the cloud, density " + away.density());
        helper.succeed();
    }

    @GameTest(template = ROOM, timeoutTicks = 40)
    public static void contaminatingLaysResidueOnTheFloorOnly(GameTestHelper helper) {
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        net.minecraft.core.BlockPos centre = helper.absolutePos(pos(10, 2, 10));
        int cells = GasManager.contaminate(level, GasTypes.YPERITE.get(), centre, 2, 1.5f);
        helper.assertTrue(cells == 13, "a disc of radius 2 covers 13 columns, got " + cells);
        net.minecraft.core.BlockPos floor = helper.absolutePos(pos(10, 1, 10));
        helper.assertTrue(GasManager.residue(level, GasTypes.YPERITE.get(), floor) == 1.5f, "the residue lies on the floor");
        helper.assertTrue(GasManager.residue(level, GasTypes.YPERITE.get(), centre) == 0f, "and not in the air above it");
        GasManager.clear(level, GasTypes.YPERITE.get());
        helper.succeed();
    }
}
