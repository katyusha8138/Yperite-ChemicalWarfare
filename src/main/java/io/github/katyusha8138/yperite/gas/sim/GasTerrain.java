package io.github.katyusha8138.yperite.gas.sim;

/**
 * What the simulation needs to know about the world. The Minecraft implementation reads real blocks;
 * unit tests use small synthetic terrains.
 */
public interface GasTerrain {
    /** Bits 0..5: gas may leave through the face with that direction index (see {@link Cells}). */
    int EXIT_MASK = 0b111111;
    /** There is solid ground directly below the cell. */
    int FLOOR = 1 << 6;
    /** The cell is under water. */
    int SUBMERGED = 1 << 7;
    /** Rain falls on the cell. */
    int RAIN = 1 << 8;
    // Bit 9 is reserved.
    /** The cell itself lies inside a gas-blocking block. */
    int SOLID = 1 << 10;
    /** Bits 11..14: sky light of the cell, 0 (fully covered) .. 15 (open sky). Decides how open to the air it is. */
    int SKY_LIGHT_SHIFT = 11;
    int SKY_LIGHT_MASK = 0xF << SKY_LIGHT_SHIFT;

    static int skyLight(int flags) {
        return (flags & SKY_LIGHT_MASK) >>> SKY_LIGHT_SHIFT;
    }

    static int withSkyLight(int flags, int skyLight) {
        return (flags & ~SKY_LIGHT_MASK) | ((skyLight & 0xF) << SKY_LIGHT_SHIFT);
    }

    /** Whether the chunk is loaded. Gas in unloaded chunks is frozen and gas never flows into them. */
    boolean isChunkLoaded(int chunkX, int chunkZ);

    /**
     * Flags for a cell in a loaded chunk. Faces that lead into an unloaded chunk, outside the world, or
     * through a blocking surface must not be set in {@link #EXIT_MASK}.
     */
    int flags(int x, int y, int z);
}
