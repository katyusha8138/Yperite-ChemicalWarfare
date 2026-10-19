package io.github.katyusha8138.yperite.gas;

import io.github.katyusha8138.yperite.gas.sim.Cells;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CellsTest {
    private static final int[][] SAMPLES = {
            {0, 0, 0}, {1, 2, 3}, {-1, -2, -3}, {15, 64, 15}, {16, -64, -16},
            {29_999_999, 319, -29_999_999}, {-29_999_999, -64, 29_999_999}, {-1, 0, 1}, {123456, 200, -654321},
    };

    @Test
    void packingRoundTrips() {
        for (int[] s : SAMPLES) {
            long key = Cells.pack(s[0], s[1], s[2]);
            assertEquals(s[0], Cells.x(key));
            assertEquals(s[1], Cells.y(key));
            assertEquals(s[2], Cells.z(key));
        }
    }

    @Test
    void layoutMatchesBlockPos() {
        for (int[] s : SAMPLES) {
            assertEquals(BlockPos.asLong(s[0], s[1], s[2]), Cells.pack(s[0], s[1], s[2]));
        }
    }

    @Test
    void chunkKeysMatchChunkPos() {
        for (int[] s : SAMPLES) {
            int cx = s[0] >> 4;
            int cz = s[2] >> 4;
            assertEquals(ChunkPos.asLong(cx, cz), Cells.chunkKey(cx, cz));
            assertEquals(cx, Cells.chunkX(Cells.chunkKey(cx, cz)));
            assertEquals(cz, Cells.chunkZ(Cells.chunkKey(cx, cz)));
            assertEquals(Cells.chunkKey(cx, cz), Cells.chunkKeyOfCell(Cells.pack(s[0], s[1], s[2])));
        }
    }
}
