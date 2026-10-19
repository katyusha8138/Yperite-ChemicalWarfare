package io.github.katyusha8138.yperite.protection;

import io.github.katyusha8138.yperite.gas.GasCategory;
import io.github.katyusha8138.yperite.gas.GasManager;
import io.github.katyusha8138.yperite.gas.GasSavedData;
import io.github.katyusha8138.yperite.gas.GasType;
import io.github.katyusha8138.yperite.gas.sim.GasLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/** What the detector and the alarm sense: the gas level per category around a point, and residue underfoot. */
public final class GasScanner {
    /** Residue below this is too little to report. */
    private static final float RESIDUE_REPORT = 0.05f;

    /**
     * @param levels  detector level (see {@link DetectorLevel}) per category, indexed by {@link GasCategory#ordinal()}
     * @param residue whether there is residue on the ground at the checked spot
     */
    public record Reading(int[] levels, boolean residue) {
        public int level(GasCategory category) {
            return levels[category.ordinal()];
        }

        public int maxLevel() {
            int max = 0;
            for (int level : levels) {
                max = Math.max(max, level);
            }
            return max;
        }

        public boolean isClear() {
            return maxLevel() == 0 && !residue;
        }
    }

    private GasScanner() {
    }

    /**
     * Looks at the cells within {@code radius} of {@code center} (a cube). The strongest concentration of each
     * category becomes its level.
     *
     * @param feet where to look for residue, or null to skip that
     */
    public static Reading scan(ServerLevel level, BlockPos center, int radius, @Nullable BlockPos feet) {
        int[] levels = new int[GasCategory.values().length];
        boolean residue = false;
        GasSavedData data = GasManager.data(level);
        if (!data.isEmpty()) {
            for (Map.Entry<GasType, GasLayer> entry : data.layers().entrySet()) {
                GasLayer layer = entry.getValue();
                if (layer.isEmpty()) {
                    continue;
                }
                float strongest = 0f;
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dy = -radius; dy <= radius; dy++) {
                        for (int dz = -radius; dz <= radius; dz++) {
                            strongest = Math.max(strongest, layer.get(center.getX() + dx, center.getY() + dy, center.getZ() + dz));
                        }
                    }
                }
                int k = entry.getKey().category().ordinal();
                levels[k] = Math.max(levels[k], DetectorLevel.of(strongest));
                if (feet != null && layer.getResidue(feet.getX(), feet.getY(), feet.getZ()) >= RESIDUE_REPORT) {
                    residue = true;
                }
            }
        }
        return new Reading(levels, residue);
    }
}
