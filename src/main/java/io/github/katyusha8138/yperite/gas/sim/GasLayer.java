package io.github.katyusha8138.yperite.gas.sim;

import it.unimi.dsi.fastutil.longs.Long2FloatMap;
import it.unimi.dsi.fastutil.longs.Long2FloatMaps;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import java.util.Arrays;

/**
 * The concentration field of one kind of gas in one world, plus the residue it left on the ground.
 *
 * <p>Cells are stored sparsely and grouped by chunk so that unloaded chunks can be skipped cheaply.
 * This class does not depend on Minecraft; the world is reached through {@link GasTerrain}.
 *
 * <p>Concentration {@code 1.0} is the reference for "dense and harmful". The update only moves mass
 * between cells, so the total is conserved except for decay, rain, water, cell removal below the minimum
 * concentration and the cell cap.
 */
public final class GasLayer {
    /** Upper bound on the share of a cell that may leave in one step. Keeps the update stable. */
    private static final float MAX_OUTFLOW = 0.9f;
    /** How strongly buoyancy skews vertical movement. */
    private static final float BUOYANCY_SKEW = 0.95f;
    private static final float MIN_VERTICAL_FACTOR = 0.05f;
    private static final float MAX_VERTICAL_FACTOR = 2f;
    /**
     * Residue below {@code minConcentration * this} is dropped. It has to stay well below what a single step
     * deposits from thin gas, otherwise small deposits would be discarded before they can add up.
     */
    private static final float RESIDUE_MIN_RATIO = 0.01f;
    /** After trimming to the cap, leave this much headroom so that trimming does not run every step. */
    private static final float TRIM_TARGET_RATIO = 0.9f;
    private static final int MAX_RELEASE_RADIUS = 24;

    private Long2ObjectOpenHashMap<Long2FloatOpenHashMap> gas = new Long2ObjectOpenHashMap<>();
    private Long2ObjectOpenHashMap<Long2FloatOpenHashMap> residue = new Long2ObjectOpenHashMap<>();
    private int gasCells;
    private int residueCells;

    /** Receives one cell of a chunk. */
    @FunctionalInterface
    public interface CellVisitor {
        void visit(long cellKey, float value);
    }

    // ------------------------------------------------------------------ queries

    public float get(int x, int y, int z) {
        return get(Cells.pack(x, y, z));
    }

    public float get(long cellKey) {
        Long2FloatOpenHashMap cells = gas.get(Cells.chunkKeyOfCell(cellKey));
        return cells == null ? 0f : cells.get(cellKey);
    }

    public float getResidue(int x, int y, int z) {
        return getResidue(Cells.pack(x, y, z));
    }

    public float getResidue(long cellKey) {
        Long2FloatOpenHashMap cells = residue.get(Cells.chunkKeyOfCell(cellKey));
        return cells == null ? 0f : cells.get(cellKey);
    }

    public int cellCount() {
        return gasCells;
    }

    public int residueCellCount() {
        return residueCells;
    }

    public boolean isEmpty() {
        return gasCells == 0 && residueCells == 0;
    }

    public double totalMass() {
        return sum(gas);
    }

    public double totalResidue() {
        return sum(residue);
    }

    /** Adds the key of every chunk that holds gas or residue to {@code out}. */
    public void collectChunkKeys(it.unimi.dsi.fastutil.longs.LongSet out) {
        out.addAll(gas.keySet());
        out.addAll(residue.keySet());
    }

    /** Visits every gas cell stored for the chunk. Do not modify the layer from the visitor. */
    public void forEachGasCellInChunk(long chunkKey, CellVisitor visitor) {
        visit(gas.get(chunkKey), visitor);
    }

    /** Visits every residue cell stored for the chunk. Do not modify the layer from the visitor. */
    public void forEachResidueCellInChunk(long chunkKey, CellVisitor visitor) {
        visit(residue.get(chunkKey), visitor);
    }

    // ------------------------------------------------------------------ changes

    public void add(int x, int y, int z, float amount) {
        if (amount <= 0f) {
            return;
        }
        long key = Cells.pack(x, y, z);
        getOrCreate(gas, Cells.chunkKeyOfCell(key)).addTo(key, amount);
        gasCells = countCells(gas);
    }

    public void addResidue(int x, int y, int z, float amount) {
        if (amount <= 0f) {
            return;
        }
        long key = Cells.pack(x, y, z);
        getOrCreate(residue, Cells.chunkKeyOfCell(key)).addTo(key, amount);
        residueCells = countCells(residue);
    }

    /**
     * Spreads {@code mass} evenly over the open cells inside a sphere. Cells in unloaded chunks and cells
     * inside blocking blocks are skipped.
     *
     * @return the mass that was actually placed (0 when no cell was open)
     */
    public float release(GasTerrain terrain, int centerX, int centerY, int centerZ, float radius, float mass) {
        if (mass <= 0f) {
            return 0f;
        }
        float r = Math.max(0f, Math.min(radius, MAX_RELEASE_RADIUS));
        int range = (int) Math.ceil(r);
        float r2 = r * r + 0.25f;
        LongArrayList targets = new LongArrayList();
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    if (dx * dx + dy * dy + dz * dz > r2) {
                        continue;
                    }
                    int x = centerX + dx;
                    int y = centerY + dy;
                    int z = centerZ + dz;
                    if (!terrain.isChunkLoaded(x >> 4, z >> 4)) {
                        continue;
                    }
                    if ((terrain.flags(x, y, z) & GasTerrain.SOLID) != 0) {
                        continue;
                    }
                    targets.add(Cells.pack(x, y, z));
                }
            }
        }
        if (targets.isEmpty()) {
            return 0f;
        }
        float each = mass / targets.size();
        for (int i = 0; i < targets.size(); i++) {
            long key = targets.getLong(i);
            getOrCreate(gas, Cells.chunkKeyOfCell(key)).addTo(key, each);
        }
        gasCells = countCells(gas);
        return mass;
    }

    public void clear() {
        gas.clear();
        residue.clear();
        gasCells = 0;
        residueCells = 0;
    }

    // ------------------------------------------------------------------ update

    /** Advances the layer by one step. */
    public void step(GasTerrain terrain, GasPhysics physics, GasStepParams params) {
        final float dt = params.dtSeconds();
        final float min = params.minConcentration();
        final float airKeep = keepFactor(physics.decayPerSecond(), dt);
        final float rainKeep = keepFactor(physics.rainWashoutPerSecond(), dt);
        final float waterKeep = keepFactor(physics.waterAbsorbPerSecond(), dt);
        final float settle = 1f - keepFactor(physics.settlePerSecond(), dt);
        final float residueKeep = keepFactor(physics.residueDecayPerSecond(), dt);
        final float evaporate = 1f - keepFactor(physics.evaporationPerSecond(), dt);
        final float residueMin = min * RESIDUE_MIN_RATIO;

        final float diffusion = physics.diffusion();
        final float buoyancy = physics.buoyancy();
        final float upWeight = diffusion * clamp(1f + buoyancy * BUOYANCY_SKEW, MIN_VERTICAL_FACTOR, MAX_VERTICAL_FACTOR);
        final float downWeight = diffusion * clamp(1f - buoyancy * BUOYANCY_SKEW, MIN_VERTICAL_FACTOR, MAX_VERTICAL_FACTOR);
        final boolean windy = params.hasWind();
        final float windX = params.windX();
        final float windZ = params.windZ();

        // Tables indexed by sky light (0 = fully covered, 15 = open sky).
        final float[] windScale = new float[16];     // cells moved per step for a wind of 1 block per second
        final float[] dilution = new float[16];      // factor on spreading and decay
        final float[] airKeepByLight = new float[16];
        for (int light = 0; light < 16; light++) {
            float openness = (float) Math.pow(light / 15.0, params.shelterExponent());
            windScale[light] = openness * dt;
            dilution[light] = 1f + (params.openAirDilution() - 1f) * openness;
            airKeepByLight[light] = (float) Math.pow(airKeep, dilution[light]);
        }

        Long2ObjectOpenHashMap<Long2FloatOpenHashMap> nextGas = new Long2ObjectOpenHashMap<>(Math.max(16, gas.size() * 2));
        Long2ObjectOpenHashMap<Long2FloatOpenHashMap> nextResidue = new Long2ObjectOpenHashMap<>(Math.max(16, residue.size() * 2));

        // Residue: decays slowly and partly returns to the air.
        for (Long2ObjectMap.Entry<Long2FloatOpenHashMap> chunk : residue.long2ObjectEntrySet()) {
            long chunkKey = chunk.getLongKey();
            Long2FloatOpenHashMap cells = chunk.getValue();
            if (!terrain.isChunkLoaded(Cells.chunkX(chunkKey), Cells.chunkZ(chunkKey))) {
                nextResidue.put(chunkKey, cells);
                continue;
            }
            Long2FloatOpenHashMap out = null;
            ObjectIterator<Long2FloatMap.Entry> it = Long2FloatMaps.fastIterator(cells);
            while (it.hasNext()) {
                Long2FloatMap.Entry entry = it.next();
                float amount = entry.getFloatValue() * residueKeep;
                float returned = amount * evaporate;
                amount -= returned;
                if (returned > 0f) {
                    getOrCreate(nextGas, chunkKey).addTo(entry.getLongKey(), returned);
                }
                if (amount > 0f) {
                    if (out == null) {
                        out = getOrCreate(nextResidue, chunkKey);
                    }
                    out.addTo(entry.getLongKey(), amount);
                }
            }
        }

        // Gas: decay, deposit, then spread to the neighbours.
        final float[] weights = new float[6];
        final float[] advect = new float[6];
        for (Long2ObjectMap.Entry<Long2FloatOpenHashMap> chunk : gas.long2ObjectEntrySet()) {
            long chunkKey = chunk.getLongKey();
            Long2FloatOpenHashMap cells = chunk.getValue();
            if (!terrain.isChunkLoaded(Cells.chunkX(chunkKey), Cells.chunkZ(chunkKey))) {
                mergeInto(nextGas, chunkKey, cells);
                continue;
            }
            Long2FloatOpenHashMap ownOut = getOrCreate(nextGas, chunkKey);
            Long2FloatOpenHashMap ownResidue = null;
            Long2FloatOpenHashMap cachedOut = ownOut;
            long cachedChunk = chunkKey;

            ObjectIterator<Long2FloatMap.Entry> it = Long2FloatMaps.fastIterator(cells);
            while (it.hasNext()) {
                Long2FloatMap.Entry entry = it.next();
                long key = entry.getLongKey();
                int x = Cells.x(key);
                int y = Cells.y(key);
                int z = Cells.z(key);
                int flags = terrain.flags(x, y, z);
                int light = GasTerrain.skyLight(flags);
                float c = entry.getFloatValue() * airKeepByLight[light];

                if ((flags & GasTerrain.RAIN) != 0) {
                    c *= rainKeep;
                }
                if ((flags & GasTerrain.SUBMERGED) != 0) {
                    c *= waterKeep;
                }
                if (c < min) {
                    continue;
                }

                if ((flags & GasTerrain.FLOOR) != 0 && settle > 0f) {
                    float deposited = c * settle;
                    c -= deposited;
                    if (ownResidue == null) {
                        ownResidue = getOrCreate(nextResidue, chunkKey);
                    }
                    ownResidue.addTo(key, deposited);
                }

                float sum = 0f;
                java.util.Arrays.fill(advect, 0f);
                if ((flags & GasTerrain.SOLID) != 0) {
                    // Buried in a block: push everything out to whichever sides are open.
                    int open = Integer.bitCount(flags & GasTerrain.EXIT_MASK);
                    if (open == 0) {
                        continue;
                    }
                    float share = 1f / open;
                    for (int d = 0; d < 6; d++) {
                        weights[d] = (flags & (1 << d)) != 0 ? share : 0f;
                    }
                    sum = 1f;
                } else {
                    float spread = dilution[light];
                    for (int d = 0; d < 6; d++) {
                        if ((flags & (1 << d)) == 0) {
                            weights[d] = 0f;
                            continue;
                        }
                        float w = (d == Cells.DOWN ? downWeight : d == Cells.UP ? upWeight : diffusion) * spread;
                        weights[d] = w;
                        sum += w;
                    }
                    if (sum > MAX_OUTFLOW) {
                        float scale = MAX_OUTFLOW / sum;
                        for (int d = 0; d < 6; d++) {
                            weights[d] *= scale;
                        }
                        sum = MAX_OUTFLOW;
                    }
                    if (windy && light > 0) {
                        // Wind carries a share of the gas one cell downwind. Through a closed face it carries
                        // nothing, so gas banks up against walls instead of passing through them.
                        float ax = windX * windScale[light];
                        float az = windZ * windScale[light];
                        float total = Math.abs(ax) + Math.abs(az);
                        if (total > 1f) {
                            ax /= total;
                            az /= total;
                        }
                        int dirX = ax > 0f ? Cells.EAST : Cells.WEST;
                        int dirZ = az > 0f ? Cells.SOUTH : Cells.NORTH;
                        if ((flags & (1 << dirX)) != 0) {
                            advect[dirX] = Math.abs(ax);
                        }
                        if ((flags & (1 << dirZ)) != 0) {
                            advect[dirZ] = Math.abs(az);
                        }
                    }
                }

                // What the wind carries leaves first; the rest spreads by diffusion.
                float carried = advect[Cells.NORTH] + advect[Cells.SOUTH] + advect[Cells.WEST] + advect[Cells.EAST];
                float remaining = 1f - carried;
                float stay = c * remaining * (1f - sum);
                if (stay > 0f) {
                    ownOut.addTo(key, stay);
                }
                for (int d = 0; d < 6; d++) {
                    float w = advect[d] + remaining * weights[d];
                    if (w <= 0f) {
                        continue;
                    }
                    int nx = x + Cells.DX[d];
                    int ny = y + Cells.DY[d];
                    int nz = z + Cells.DZ[d];
                    long neighborChunk = Cells.chunkKey(nx >> 4, nz >> 4);
                    if (neighborChunk != cachedChunk) {
                        cachedOut = getOrCreate(nextGas, neighborChunk);
                        cachedChunk = neighborChunk;
                    }
                    cachedOut.addTo(Cells.pack(nx, ny, nz), c * w);
                }
                // The cache may point at a neighbour chunk now; reset it for the next cell.
                cachedOut = ownOut;
                cachedChunk = chunkKey;
            }
        }

        // Faint residue is dropped only now, after this step's deposits were added. Dropping it earlier would
        // throw away every small deposit before the next one could add to it.
        for (Long2FloatOpenHashMap cells : nextResidue.values()) {
            cells.values().removeIf(v -> v < residueMin);
        }
        gas = pruneEmpty(nextGas);
        residue = pruneEmpty(nextResidue);
        gasCells = countCells(gas);
        residueCells = countCells(residue);

        if (gasCells > params.maxCells()) {
            trim(gas, gasCells, params.maxCells());
            gasCells = countCells(gas);
        }
        if (residueCells > params.maxCells()) {
            trim(residue, residueCells, params.maxCells());
            residueCells = countCells(residue);
        }
    }

    // ------------------------------------------------------------------ persistence

    /** Copies all gas cells into the two lists (same index = same cell). */
    public void exportGas(LongArrayList keys, it.unimi.dsi.fastutil.floats.FloatArrayList values) {
        export(gas, keys, values);
    }

    /** Copies all residue cells into the two lists (same index = same cell). */
    public void exportResidue(LongArrayList keys, it.unimi.dsi.fastutil.floats.FloatArrayList values) {
        export(residue, keys, values);
    }

    public void importGas(long[] keys, float[] values) {
        gas = importCells(keys, values);
        gasCells = countCells(gas);
    }

    public void importResidue(long[] keys, float[] values) {
        residue = importCells(keys, values);
        residueCells = countCells(residue);
    }

    // ------------------------------------------------------------------ internals

    private static float keepFactor(float ratePerSecond, float dtSeconds) {
        if (ratePerSecond <= 0f) {
            return 1f;
        }
        if (ratePerSecond >= 1f) {
            return 0f;
        }
        return (float) Math.pow(1.0 - ratePerSecond, dtSeconds);
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : Math.min(v, hi);
    }

    private static Long2FloatOpenHashMap getOrCreate(Long2ObjectOpenHashMap<Long2FloatOpenHashMap> map, long chunkKey) {
        Long2FloatOpenHashMap cells = map.get(chunkKey);
        if (cells == null) {
            cells = new Long2FloatOpenHashMap();
            map.put(chunkKey, cells);
        }
        return cells;
    }

    /** Moves frozen cells into the next map, merging when something already flowed into that chunk. */
    private static void mergeInto(Long2ObjectOpenHashMap<Long2FloatOpenHashMap> target, long chunkKey, Long2FloatOpenHashMap cells) {
        Long2FloatOpenHashMap existing = target.get(chunkKey);
        if (existing == null) {
            target.put(chunkKey, cells);
            return;
        }
        ObjectIterator<Long2FloatMap.Entry> it = Long2FloatMaps.fastIterator(cells);
        while (it.hasNext()) {
            Long2FloatMap.Entry entry = it.next();
            existing.addTo(entry.getLongKey(), entry.getFloatValue());
        }
    }

    private static Long2ObjectOpenHashMap<Long2FloatOpenHashMap> pruneEmpty(Long2ObjectOpenHashMap<Long2FloatOpenHashMap> map) {
        map.values().removeIf(Long2FloatOpenHashMap::isEmpty);
        return map;
    }

    private static int countCells(Long2ObjectOpenHashMap<Long2FloatOpenHashMap> map) {
        int total = 0;
        for (Long2FloatOpenHashMap cells : map.values()) {
            total += cells.size();
        }
        return total;
    }

    private static double sum(Long2ObjectOpenHashMap<Long2FloatOpenHashMap> map) {
        double total = 0;
        for (Long2FloatOpenHashMap cells : map.values()) {
            ObjectIterator<Long2FloatMap.Entry> it = Long2FloatMaps.fastIterator(cells);
            while (it.hasNext()) {
                total += it.next().getFloatValue();
            }
        }
        return total;
    }

    private static void visit(Long2FloatOpenHashMap cells, CellVisitor visitor) {
        if (cells == null) {
            return;
        }
        ObjectIterator<Long2FloatMap.Entry> it = Long2FloatMaps.fastIterator(cells);
        while (it.hasNext()) {
            Long2FloatMap.Entry entry = it.next();
            visitor.visit(entry.getLongKey(), entry.getFloatValue());
        }
    }

    /** Drops the thinnest cells so that about {@code TRIM_TARGET_RATIO * max} remain. */
    private static void trim(Long2ObjectOpenHashMap<Long2FloatOpenHashMap> map, int count, int max) {
        int target = Math.max(1, (int) (max * TRIM_TARGET_RATIO));
        if (count <= target) {
            return;
        }
        float[] values = new float[count];
        int i = 0;
        for (Long2FloatOpenHashMap cells : map.values()) {
            ObjectIterator<Long2FloatMap.Entry> it = Long2FloatMaps.fastIterator(cells);
            while (it.hasNext()) {
                values[i++] = it.next().getFloatValue();
            }
        }
        Arrays.sort(values);
        float cutoff = values[count - target];
        for (Long2FloatOpenHashMap cells : map.values()) {
            cells.values().removeIf(v -> v < cutoff);
        }
        pruneEmpty(map);
    }

    private static void export(Long2ObjectOpenHashMap<Long2FloatOpenHashMap> map, LongArrayList keys,
                               it.unimi.dsi.fastutil.floats.FloatArrayList values) {
        for (Long2FloatOpenHashMap cells : map.values()) {
            ObjectIterator<Long2FloatMap.Entry> it = Long2FloatMaps.fastIterator(cells);
            while (it.hasNext()) {
                Long2FloatMap.Entry entry = it.next();
                keys.add(entry.getLongKey());
                values.add(entry.getFloatValue());
            }
        }
    }

    private static Long2ObjectOpenHashMap<Long2FloatOpenHashMap> importCells(long[] keys, float[] values) {
        if (keys.length != values.length) {
            throw new IllegalArgumentException("keys and values differ in length: " + keys.length + " vs " + values.length);
        }
        Long2ObjectOpenHashMap<Long2FloatOpenHashMap> map = new Long2ObjectOpenHashMap<>();
        for (int i = 0; i < keys.length; i++) {
            if (values[i] > 0f) {
                getOrCreate(map, Cells.chunkKeyOfCell(keys[i])).addTo(keys[i], values[i]);
            }
        }
        return map;
    }
}
