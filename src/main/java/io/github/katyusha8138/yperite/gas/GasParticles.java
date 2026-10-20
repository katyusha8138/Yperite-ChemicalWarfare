package io.github.katyusha8138.yperite.gas;

import io.github.katyusha8138.yperite.config.YperiteConfig;
import io.github.katyusha8138.yperite.gas.sim.Cells;
import io.github.katyusha8138.yperite.gas.sim.GasLayer;
import io.github.katyusha8138.yperite.particle.GasParticleOptions;
import io.github.katyusha8138.yperite.particle.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;

import java.util.Map;

/**
 * Picks where gas is drawn and sends each nearby player the puffs to draw (the client draws them:
 * {@code GasCloudParticle}). Puffs outlive the interval between bursts, so a cloud is many overlapping puffs that
 * are renewed a few at a time; dense cells get more of them and more opaque ones, thin edges fewer and fainter.
 * Each puff drifts with the wind of its cell (outdoors only, as the gas does) and rises or sinks with the gas.
 * Where gas has settled on the ground, a low flat mist lies over it, with now and then a wisp rising off it.
 * Gas is seen from far off (the particles ignore vanilla's 32-block limit); distant cells are drawn coarser.
 */
final class GasParticles {
    /** Concentration at which a cell is drawn at its fullest. */
    private static final float FULL_DRAW_CONCENTRATION = 1.5f;
    /** Residue at which the mist over a cell is at its thickest. */
    private static final float FULL_RESIDUE = 1.0f;
    /** Average life of a puff in ticks (see GasCloudParticle): with the burst interval it sets how often to renew. */
    private static final float CLOUD_LIFETIME = 75f;
    private static final float MIST_LIFETIME = 130f;
    /** Puffs that a dense cell keeps in the air at once. */
    private static final float PUFFS_PER_CELL = 1.6f;
    private static final float MIST_PER_CELL = 1.0f;
    /**
     * Within this distance every cell is drawn in full. Beyond it cells get fewer puffs, by the square of the
     * distance, and the puffs grow by the distance: far clouds look the same, at a fraction of the puffs.
     */
    private static final double NEAR = 20.0;
    /** How fast a gas of buoyancy 1 rises, in blocks per tick. */
    private static final float RISE_SPEED = 0.006f;

    private GasParticles() {
    }

    static void spawn(ServerLevel level, GasSavedData data) {
        int max = YperiteConfig.PARTICLE_MAX_PER_PLAYER.get();
        if (max <= 0) {
            return;
        }
        int interval = YperiteConfig.PARTICLE_INTERVAL.get();
        int range = YperiteConfig.PARTICLE_RANGE.get();
        double rangeSq = (double) range * range;
        int chunkRange = (range >> 4) + 1;
        RandomSource random = level.getRandom();
        double maxSpeed = YperiteConfig.WIND_MAX_SPEED.get();
        WindModel.Wind wind = YperiteConfig.WIND_ENABLED.get() && maxSpeed > 0.0 ? WindModel.at(level, data) : WindModel.Wind.CALM;
        float windX = (float) (wind.x() * maxSpeed / 20.0);
        float windZ = (float) (wind.z() * maxSpeed / 20.0);
        double shelter = YperiteConfig.WIND_SHELTER_EXPONENT.get();
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();

        for (ServerPlayer player : level.players()) {
            int pcx = player.blockPosition().getX() >> 4;
            int pcz = player.blockPosition().getZ() >> 4;
            double px = player.getX();
            double py = player.getY();
            double pz = player.getZ();

            for (Map.Entry<GasType, GasLayer> entry : data.layers().entrySet()) {
                GasType type = entry.getKey();
                GasLayer layer = entry.getValue();
                if (layer.isEmpty()) {
                    continue;
                }
                float opacity = type.opacity();
                // nearly invisible gases make fewer puffs as well as fainter ones
                float visible = 0.35f + 0.65f * opacity;
                float cloudRate = interval / CLOUD_LIFETIME * PUFFS_PER_CELL * visible;
                float mistRate = interval / MIST_LIFETIME * MIST_PER_CELL * visible;
                Sample clouds = new Sample(max);
                Sample mists = new Sample(Math.max(1, max / 3));

                for (int cx = pcx - chunkRange; cx <= pcx + chunkRange; cx++) {
                    for (int cz = pcz - chunkRange; cz <= pcz + chunkRange; cz++) {
                        long chunk = Cells.chunkKey(cx, cz);
                        layer.forEachGasCellInChunk(chunk, (cell, value) -> {
                            float scale = scale(cell, px, py, pz, rangeSq);
                            if (scale > 0f && random.nextFloat()
                                    < Mth.clamp(value / FULL_DRAW_CONCENTRATION, 0.12f, 1f) * cloudRate / (scale * scale)) {
                                clouds.offer(cell, value, scale, random);
                            }
                        });
                        layer.forEachResidueCellInChunk(chunk, (cell, value) -> {
                            float scale = scale(cell, px, py, pz, rangeSq);
                            if (scale > 0f && random.nextFloat()
                                    < Mth.clamp(value / FULL_RESIDUE, 0.2f, 1f) * mistRate / (scale * scale)) {
                                mists.offer(cell, value, scale, random);
                            }
                        });
                    }
                }

                int rgb = type.color();
                float rise = type.physics().buoyancy() * RISE_SPEED;
                for (int i = 0; i < clouds.count(); i++) {
                    long cell = clouds.cells[i];
                    float fill = Mth.clamp(clouds.values[i] / FULL_DRAW_CONCENTRATION, 0f, 1f);
                    float open = openness(level, at.set(Cells.x(cell), Cells.y(cell), Cells.z(cell)), shelter);
                    GasParticleOptions puff = new GasParticleOptions(ModParticles.GAS_CLOUD.get(), rgb,
                            opacity * 0.6f * (0.45f + 0.55f * fill),
                            (1.1f + 1.1f * fill + random.nextFloat() * 0.4f) * clouds.scales[i],
                            windX * open, rise, windZ * open);
                    level.sendParticles(player, puff, true, Cells.x(cell) + random.nextDouble(),
                            Cells.y(cell) + random.nextDouble(), Cells.z(cell) + random.nextDouble(), 1, 0.0, 0.0, 0.0, 0.0);
                }
                for (int i = 0; i < mists.count(); i++) {
                    long cell = mists.cells[i];
                    float fill = Mth.clamp(mists.values[i] / FULL_RESIDUE, 0.2f, 1f);
                    float open = openness(level, at.set(Cells.x(cell), Cells.y(cell), Cells.z(cell)), shelter);
                    double x = Cells.x(cell) + random.nextDouble();
                    double z = Cells.z(cell) + random.nextDouble();
                    GasParticleOptions mist = new GasParticleOptions(ModParticles.GAS_MIST.get(), rgb,
                            0.42f * fill * (0.5f + opacity), (1.5f + 0.8f * random.nextFloat()) * mists.scales[i],
                            windX * open * 0.25f, 0f, windZ * open * 0.25f);
                    level.sendParticles(player, mist, true, x, Cells.y(cell) + 0.06 + random.nextDouble() * 0.1, z,
                            1, 0.0, 0.0, 0.0, 0.0);
                    if (random.nextInt(4) == 0) {
                        // a wisp rising off the ground as the residue evaporates
                        GasParticleOptions wisp = new GasParticleOptions(ModParticles.GAS_CLOUD.get(), rgb,
                                0.22f * fill * (0.5f + opacity), 0.6f + 0.3f * random.nextFloat(),
                                windX * open * 0.5f, 0.004f, windZ * open * 0.5f);
                        level.sendParticles(player, wisp, true, x, Cells.y(cell) + 0.2, z, 1, 0.0, 0.0, 0.0, 0.0);
                    }
                }
            }
        }
    }

    /** 0 beyond the range; 1 near the player; distance / NEAR further out (see NEAR). */
    private static float scale(long cell, double px, double py, double pz, double rangeSq) {
        double dx = Cells.x(cell) + 0.5 - px;
        double dy = Cells.y(cell) + 0.5 - py;
        double dz = Cells.z(cell) + 0.5 - pz;
        double d2 = dx * dx + dy * dy + dz * dz;
        if (d2 > rangeSq) {
            return 0f;
        }
        return d2 <= NEAR * NEAR ? 1f : (float) (Math.sqrt(d2) / NEAR);
    }

    /** How open to the wind a cell is, as the gas field reckons it: (sky light / 15) ^ shelter exponent. */
    private static float openness(ServerLevel level, BlockPos pos, double shelter) {
        int light = level.getBrightness(LightLayer.SKY, pos);
        return (float) Math.pow(light / 15.0, shelter);
    }

    /** A random subset of at most {@code max} cells (reservoir sampling: unbiased when there are too many). */
    private static final class Sample {
        final long[] cells;
        final float[] values;
        final float[] scales;
        private int seen;

        Sample(int max) {
            cells = new long[max];
            values = new float[max];
            scales = new float[max];
        }

        void offer(long cell, float value, float scale, RandomSource random) {
            int n = seen++;
            int slot = n < cells.length ? n : random.nextInt(n + 1);
            if (slot < cells.length) {
                cells[slot] = cell;
                values[slot] = value;
                scales[slot] = scale;
            }
        }

        int count() {
            return Math.min(seen, cells.length);
        }
    }
}
