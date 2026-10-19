package io.github.katyusha8138.yperite.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Shared helpers for the in-world tests. All coordinates are template-local: the stone floor is at y = 0. */
public final class TestRooms {
    private TestRooms() {
    }

    /**
     * Template-local coordinates to GameTest-relative ones. GameTest places the template one block above its own
     * origin, so the layer at relative y = 0 is below the template.
     */
    public static BlockPos pos(int x, int y, int z) {
        return new BlockPos(x, y + 1, z);
    }

    /** Seals the 20x10x20 template with stone walls and a stone ceiling; the inside is x, z 1..18 and y 1..8. */
    public static void sealRoom(GameTestHelper helper) {
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
}
