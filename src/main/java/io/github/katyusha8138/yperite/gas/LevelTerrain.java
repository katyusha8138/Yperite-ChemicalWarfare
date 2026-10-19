package io.github.katyusha8138.yperite.gas;

import io.github.katyusha8138.yperite.gas.sim.Cells;
import io.github.katyusha8138.yperite.gas.sim.GasTerrain;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Reads the real world for the gas simulation. Create one per update step: it caches chunk lookups
 * and must not be kept across ticks.
 *
 * <p>A face is closed when the collision shape of either neighbouring block covers that whole face.
 * Fences, bars, open doors and so on therefore let gas through, while stone, glass and closed doors do not.
 * The tags in {@link GasBlockTags} override this.
 */
final class LevelTerrain implements GasTerrain {
    private static final LevelChunk NOT_LOADED = null;

    private final ServerLevel level;
    private final ServerChunkCache chunkSource;
    private final boolean wantSky;
    private final boolean wantRain;
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
    private final Long2ObjectOpenHashMap<LevelChunk> chunks = new Long2ObjectOpenHashMap<>();
    private final boolean raining;

    /**
     * @param wantSky  compute {@link GasTerrain#SKY} (needed for wind)
     * @param wantRain compute {@link GasTerrain#RAIN}
     */
    LevelTerrain(ServerLevel level, boolean wantSky, boolean wantRain) {
        this.level = level;
        this.chunkSource = level.getChunkSource();
        this.wantSky = wantSky;
        this.wantRain = wantRain;
        this.raining = level.isRaining();
    }

    @Override
    public boolean isChunkLoaded(int chunkX, int chunkZ) {
        return chunk(chunkX, chunkZ) != NOT_LOADED;
    }

    @Override
    public int flags(int x, int y, int z) {
        BlockState self = state(x, y, z);
        if (self == null) {
            return 0;
        }
        cursor.set(x, y, z);
        int flags = 0;

        boolean selfFull = isFullyBlocking(self, cursor);
        if (selfFull) {
            flags |= SOLID;
        }

        for (int d = 0; d < 6; d++) {
            int nx = x + Cells.DX[d];
            int ny = y + Cells.DY[d];
            int nz = z + Cells.DZ[d];
            if (level.isOutsideBuildHeight(ny)) {
                continue;
            }
            BlockState neighbor = state(nx, ny, nz);
            if (neighbor == null) {
                continue;
            }
            Direction dir = Direction.from3DDataValue(d);
            probe.set(nx, ny, nz);
            boolean blockedBySelf = !selfFull && blocksFace(self, cursor, dir);
            boolean blockedByNeighbor = blocksFace(neighbor, probe, dir.getOpposite());
            if (!blockedBySelf && !blockedByNeighbor) {
                flags |= 1 << d;
            } else if (d == Cells.DOWN && !selfFull) {
                flags |= FLOOR;
            }
        }

        if (self.getFluidState().is(FluidTags.WATER)) {
            flags |= SUBMERGED;
        }
        if (wantSky && level.canSeeSky(cursor)) {
            flags |= SKY;
            if (wantRain && raining && level.isRainingAt(cursor)) {
                flags |= RAIN;
            }
        }
        return flags;
    }

    @Nullable
    private BlockState state(int x, int y, int z) {
        LevelChunk chunk = chunk(x >> 4, z >> 4);
        if (chunk == NOT_LOADED) {
            return null;
        }
        probe.set(x, y, z);
        return chunk.getBlockState(probe);
    }

    @Nullable
    private LevelChunk chunk(int chunkX, int chunkZ) {
        long key = Cells.chunkKey(chunkX, chunkZ);
        LevelChunk cached = chunks.get(key);
        if (cached != null) {
            return cached;
        }
        if (chunks.containsKey(key)) {
            return NOT_LOADED;
        }
        LevelChunk loaded = chunkSource.getChunkNow(chunkX, chunkZ);
        chunks.put(key, loaded);
        return loaded;
    }

    /** Whether the block stops gas from passing through the given face of its own cell. */
    private boolean blocksFace(BlockState state, BlockPos pos, Direction face) {
        if (state.isAir() || state.is(GasBlockTags.GAS_PERMEABLE)) {
            return false;
        }
        if (state.is(GasBlockTags.GAS_BLOCKING)) {
            return true;
        }
        VoxelShape shape = state.getCollisionShape(level, pos);
        return !shape.isEmpty() && Block.isFaceFull(shape, face);
    }

    /** Whether the block fills its whole cell so that gas cannot stay inside it. */
    private boolean isFullyBlocking(BlockState state, BlockPos pos) {
        if (state.isAir() || state.is(GasBlockTags.GAS_PERMEABLE)) {
            return false;
        }
        if (state.is(GasBlockTags.GAS_BLOCKING)) {
            return true;
        }
        VoxelShape shape = state.getCollisionShape(level, pos);
        return !shape.isEmpty() && Block.isShapeFullBlock(shape);
    }
}
