package io.github.katyusha8138.yperite.gas;

import io.github.katyusha8138.yperite.config.YperiteConfig;
import io.github.katyusha8138.yperite.gas.sim.GasLayer;
import io.github.katyusha8138.yperite.gas.sim.GasStepParams;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Entry point for everything that touches gas in a world: grenades, shells, cylinders and so on call
 * {@link #release}, and exposure code calls {@link #concentration} / {@link #residue}.
 * All methods must be called on the server thread.
 */
public final class GasManager {
    private GasManager() {
    }

    public static GasSavedData data(ServerLevel level) {
        return GasSavedData.get(level);
    }

    /**
     * Releases gas spread evenly over the open cells of a sphere.
     *
     * @param mass total amount; a single cell at concentration 1.0 holds a mass of 1
     * @return the mass actually placed (0 if the whole sphere is blocked or unloaded)
     */
    public static float release(ServerLevel level, GasType type, BlockPos center, float radius, float mass) {
        GasSavedData data = data(level);
        LevelTerrain terrain = new LevelTerrain(level, false, false);
        float placed = data.layer(type).release(terrain, center.getX(), center.getY(), center.getZ(), radius, mass);
        if (placed > 0f) {
            data.setDirty();
        }
        return placed;
    }

    /**
     * Lays residue of a gas on the ground in a disc around {@code center}: on each column, the first open cell
     * resting on a solid floor, within a few blocks above or below the centre. Returns how many cells got some.
     * For testing and for debug commands; in play residue comes from gas settling.
     */
    public static int contaminate(ServerLevel level, GasType type, BlockPos center, int radius, float amount) {
        GasLayer layer = data(level).layer(type);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        int cells = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }
                for (int dy = 3; dy >= -4; dy--) {
                    at.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockPos below = at.below();
                    if (!level.getBlockState(at).isCollisionShapeFullBlock(level, at)
                            && level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP)) {
                        layer.addResidue(at.getX(), at.getY(), at.getZ(), amount);
                        cells++;
                        break;
                    }
                }
            }
        }
        if (cells > 0) {
            data(level).setDirty();
        }
        return cells;
    }

    /** The wind in this dimension right now, as a vector with a length of 0..1 (see {@link WindModel.Wind}). */
    public static WindModel.Wind wind(ServerLevel level) {
        if (!YperiteConfig.WIND_ENABLED.get()) {
            return WindModel.Wind.CALM;
        }
        return WindModel.at(level, data(level));
    }

    public static float concentration(ServerLevel level, GasType type, BlockPos pos) {
        GasLayer layer = data(level).layerIfPresent(type);
        return layer == null ? 0f : layer.get(pos.getX(), pos.getY(), pos.getZ());
    }

    public static float residue(ServerLevel level, GasType type, BlockPos pos) {
        GasLayer layer = data(level).layerIfPresent(type);
        return layer == null ? 0f : layer.getResidue(pos.getX(), pos.getY(), pos.getZ());
    }

    /** Removes the gas and residue of one type, or of every type when {@code type} is null. */
    public static void clear(ServerLevel level, @Nullable GasType type) {
        GasSavedData data = data(level);
        if (type == null) {
            data.layers().values().forEach(GasLayer::clear);
        } else {
            GasLayer layer = data.layerIfPresent(type);
            if (layer != null) {
                layer.clear();
            }
        }
        data.setDirty();
    }

    /** Called every server tick for every dimension. */
    static void tick(ServerLevel level) {
        GasSavedData data = data(level);
        long time = level.getGameTime();
        // the haze in players' eyes, also when the last gas has just gone
        GasSight.update(level, data, time);
        if (data.isEmpty()) {
            return;
        }

        int interval = YperiteConfig.GAS_UPDATE_INTERVAL.get();
        if (time % interval == 0) {
            step(level, data, interval);
        }
        if (YperiteConfig.PARTICLES_ENABLED.get() && time % YperiteConfig.PARTICLE_INTERVAL.get() == 0) {
            GasParticles.spawn(level, data);
        }
    }

    /** One simulation step. Package-private so that in-world tests can drive and time it. */
    static void step(ServerLevel level, GasSavedData data, int intervalTicks) {
        double maxSpeed = YperiteConfig.WIND_MAX_SPEED.get();
        boolean windEnabled = YperiteConfig.WIND_ENABLED.get() && maxSpeed > 0.0;
        WindModel.Wind wind = windEnabled ? WindModel.at(level, data) : WindModel.Wind.CALM;
        float openAirDilution = YperiteConfig.OPEN_AIR_DILUTION.get().floatValue();
        GasStepParams params = new GasStepParams(
                intervalTicks / 20f,
                YperiteConfig.GAS_MIN_CONCENTRATION.get().floatValue(),
                YperiteConfig.GAS_MAX_CELLS.get(),
                (float) (wind.x() * maxSpeed),
                (float) (wind.z() * maxSpeed),
                YperiteConfig.WIND_SHELTER_EXPONENT.get().floatValue(),
                openAirDilution);
        // Sky light is needed for wind and for the thinner air outdoors.
        LevelTerrain terrain = new LevelTerrain(level, windEnabled || openAirDilution > 1f, true);

        for (Map.Entry<GasType, GasLayer> entry : data.layers().entrySet()) {
            GasLayer layer = entry.getValue();
            if (!layer.isEmpty()) {
                layer.step(terrain, entry.getKey().physics(), params);
            }
        }
        data.setDirty();
    }
}
