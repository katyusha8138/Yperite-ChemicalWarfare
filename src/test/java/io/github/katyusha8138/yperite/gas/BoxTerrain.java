package io.github.katyusha8138.yperite.gas;

import io.github.katyusha8138.yperite.gas.sim.Cells;
import io.github.katyusha8138.yperite.gas.sim.GasTerrain;

import java.util.HashSet;
import java.util.Set;

/**
 * A synthetic world for tests: an open box whose outside is solid, with optional walls and
 * unloaded chunks.
 */
final class BoxTerrain implements GasTerrain {
    interface Solid {
        boolean at(int x, int y, int z);
    }

    final int sizeX;
    final int sizeY;
    final int sizeZ;
    private Solid solid = (x, y, z) -> false;
    private final Set<Long> unloaded = new HashSet<>();
    private boolean sky;
    private boolean rain;
    private boolean submerged;

    BoxTerrain(int sizeX, int sizeY, int sizeZ) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
    }

    BoxTerrain solid(Solid solid) {
        this.solid = solid;
        return this;
    }

    BoxTerrain unloadChunk(int chunkX, int chunkZ) {
        unloaded.add(Cells.chunkKey(chunkX, chunkZ));
        return this;
    }

    BoxTerrain sky(boolean sky) {
        this.sky = sky;
        return this;
    }

    BoxTerrain rain(boolean rain) {
        this.rain = rain;
        return this;
    }

    BoxTerrain submerged(boolean submerged) {
        this.submerged = submerged;
        return this;
    }

    private boolean inside(int x, int y, int z) {
        return x >= 0 && x < sizeX && y >= 0 && y < sizeY && z >= 0 && z < sizeZ;
    }

    private boolean blocked(int x, int y, int z) {
        return !inside(x, y, z) || solid.at(x, y, z);
    }

    @Override
    public boolean isChunkLoaded(int chunkX, int chunkZ) {
        return !unloaded.contains(Cells.chunkKey(chunkX, chunkZ));
    }

    @Override
    public int flags(int x, int y, int z) {
        int flags = 0;
        boolean selfSolid = inside(x, y, z) && solid.at(x, y, z);
        if (selfSolid) {
            flags |= SOLID;
        }
        for (int d = 0; d < 6; d++) {
            int nx = x + Cells.DX[d];
            int ny = y + Cells.DY[d];
            int nz = z + Cells.DZ[d];
            boolean closed = blocked(nx, ny, nz);
            if (!closed && !isChunkLoaded(nx >> 4, nz >> 4)) {
                closed = true;
            }
            if (!closed) {
                flags |= 1 << d;
            } else if (d == Cells.DOWN && blocked(nx, ny, nz) && !selfSolid) {
                flags |= FLOOR;
            }
        }
        if (sky) {
            flags |= SKY;
        }
        if (rain) {
            flags |= RAIN;
        }
        if (submerged) {
            flags |= SUBMERGED;
        }
        return flags;
    }
}
