package io.github.katyusha8138.yperite.gas;

import io.github.katyusha8138.yperite.config.YperiteConfig;
import io.github.katyusha8138.yperite.gas.sim.Cells;
import io.github.katyusha8138.yperite.gas.sim.GasLayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

import java.util.Map;

/**
 * Draws gas as coloured dust sent from the server to nearby players. This needs nothing on the client,
 * so it also works on a dedicated server; custom particles can replace it later.
 */
final class GasParticles {
    /** Concentration at which a cell is guaranteed to be drawn when the gas is fully opaque. */
    private static final float FULL_DRAW_CONCENTRATION = 1.5f;

    private GasParticles() {
    }

    static void spawn(ServerLevel level, GasSavedData data) {
        int max = YperiteConfig.PARTICLE_MAX_PER_PLAYER.get();
        if (max <= 0) {
            return;
        }
        int range = YperiteConfig.PARTICLE_RANGE.get();
        double rangeSq = (double) range * range;
        int chunkRange = (range >> 4) + 1;
        RandomSource random = level.getRandom();

        for (ServerPlayer player : level.players()) {
            int pcx = player.blockPosition().getX() >> 4;
            int pcz = player.blockPosition().getZ() >> 4;
            double px = player.getX();
            double py = player.getY();
            double pz = player.getZ();

            for (Map.Entry<GasType, GasLayer> entry : data.layers().entrySet()) {
                GasType type = entry.getKey();
                GasLayer layer = entry.getValue();
                if (layer.cellCount() == 0) {
                    continue;
                }
                long[] picked = new long[max];
                int[] seen = {0};
                float opacity = type.opacity();

                for (int cx = pcx - chunkRange; cx <= pcx + chunkRange; cx++) {
                    for (int cz = pcz - chunkRange; cz <= pcz + chunkRange; cz++) {
                        layer.forEachGasCellInChunk(Cells.chunkKey(cx, cz), (cell, value) -> {
                            double dx = Cells.x(cell) + 0.5 - px;
                            double dy = Cells.y(cell) + 0.5 - py;
                            double dz = Cells.z(cell) + 0.5 - pz;
                            if (dx * dx + dy * dy + dz * dz > rangeSq) {
                                return;
                            }
                            float chance = Math.min(1f, value / FULL_DRAW_CONCENTRATION) * opacity;
                            if (random.nextFloat() >= chance) {
                                return;
                            }
                            // Reservoir sampling keeps an unbiased random subset when there are too many.
                            int n = seen[0]++;
                            if (n < max) {
                                picked[n] = cell;
                            } else {
                                int j = random.nextInt(n + 1);
                                if (j < max) {
                                    picked[j] = cell;
                                }
                            }
                        });
                    }
                }

                int count = Math.min(seen[0], max);
                if (count == 0) {
                    continue;
                }
                int rgb = type.color();
                DustParticleOptions dust = new DustParticleOptions(
                        new Vector3f(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f),
                        1.6f);
                for (int i = 0; i < count; i++) {
                    long cell = picked[i];
                    level.sendParticles(player, dust, false,
                            Cells.x(cell) + random.nextDouble(),
                            Cells.y(cell) + random.nextDouble(),
                            Cells.z(cell) + random.nextDouble(),
                            1, 0.0, 0.0, 0.0, 0.0);
                }
            }
        }
    }
}
