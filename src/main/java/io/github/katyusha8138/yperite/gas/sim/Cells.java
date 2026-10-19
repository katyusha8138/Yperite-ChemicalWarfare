package io.github.katyusha8138.yperite.gas.sim;

/**
 * Helpers for packing block coordinates into a {@code long} and for the six face directions.
 *
 * <p>The bit layout of {@link #pack(int, int, int)} is identical to
 * {@code net.minecraft.core.BlockPos#asLong}, so keys can be exchanged with {@code BlockPos.of(long)}.
 * Direction indices follow {@code Direction#get3DDataValue}: DOWN, UP, NORTH, SOUTH, WEST, EAST.
 */
public final class Cells {
    public static final int DOWN = 0;
    public static final int UP = 1;
    public static final int NORTH = 2;
    public static final int SOUTH = 3;
    public static final int WEST = 4;
    public static final int EAST = 5;

    public static final int[] DX = {0, 0, 0, 0, -1, 1};
    public static final int[] DY = {-1, 1, 0, 0, 0, 0};
    public static final int[] DZ = {0, 0, -1, 1, 0, 0};

    private static final int PACKED_X_LENGTH = 26;
    private static final int PACKED_Z_LENGTH = 26;
    private static final int PACKED_Y_LENGTH = 12;
    private static final long PACKED_X_MASK = (1L << PACKED_X_LENGTH) - 1L;
    private static final long PACKED_Y_MASK = (1L << PACKED_Y_LENGTH) - 1L;
    private static final long PACKED_Z_MASK = (1L << PACKED_Z_LENGTH) - 1L;
    private static final int Z_OFFSET = PACKED_Y_LENGTH;
    private static final int X_OFFSET = PACKED_Y_LENGTH + PACKED_Z_LENGTH;

    private Cells() {
    }

    public static long pack(int x, int y, int z) {
        return (x & PACKED_X_MASK) << X_OFFSET | (y & PACKED_Y_MASK) | (z & PACKED_Z_MASK) << Z_OFFSET;
    }

    public static int x(long packed) {
        return (int) (packed << 64 - X_OFFSET - PACKED_X_LENGTH >> 64 - PACKED_X_LENGTH);
    }

    public static int y(long packed) {
        return (int) (packed << 64 - PACKED_Y_LENGTH >> 64 - PACKED_Y_LENGTH);
    }

    public static int z(long packed) {
        return (int) (packed << 64 - Z_OFFSET - PACKED_Z_LENGTH >> 64 - PACKED_Z_LENGTH);
    }

    /** Same layout as {@code net.minecraft.world.level.ChunkPos#asLong}. */
    public static long chunkKey(int chunkX, int chunkZ) {
        return (chunkX & 0xFFFFFFFFL) | ((chunkZ & 0xFFFFFFFFL) << 32);
    }

    public static int chunkX(long chunkKey) {
        return (int) chunkKey;
    }

    public static int chunkZ(long chunkKey) {
        return (int) (chunkKey >>> 32);
    }

    /** Chunk key of the chunk that contains the given cell key. */
    public static long chunkKeyOfCell(long cellKey) {
        return chunkKey(x(cellKey) >> 4, z(cellKey) >> 4);
    }
}
