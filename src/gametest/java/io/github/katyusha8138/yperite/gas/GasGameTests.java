package io.github.katyusha8138.yperite.gas;

import io.github.katyusha8138.yperite.YperiteMod;
import io.github.katyusha8138.yperite.gas.sim.GasLayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

    // ------------------------------------------------------------------ helpers

    /**
     * Template-local coordinates (floor at y = 0) to GameTest-relative ones. GameTest places the template one
     * block above its own origin, so the layer at relative y = 0 is below the template.
     */
    private static BlockPos pos(int x, int y, int z) {
        return new BlockPos(x, y + 1, z);
    }

    private static void sealRoom(GameTestHelper helper) {
        BlockState stone = Blocks.STONE.defaultBlockState();
        for (int x = 0; x < 20; x++) {
            for (int z = 0; z < 20; z++) {
                helper.setBlock(pos(x, 9, z), stone);
                for (int y = 1; y < 9; y++) {
                    if (x == 0 || x == 19 || z == 0 || z == 19) {
                        helper.setBlock(pos(x, y, z), stone);
                    }
                }
            }
        }
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
}
