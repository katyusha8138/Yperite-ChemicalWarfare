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
        if (data.isEmpty()) {
            return;
        }

        long time = level.getGameTime();
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
        boolean windEnabled = YperiteConfig.WIND_ENABLED.get() && YperiteConfig.WIND_GAIN.get() > 0.0;
        WindModel.Wind wind = windEnabled ? WindModel.at(level) : WindModel.Wind.CALM;
        GasStepParams params = new GasStepParams(
                intervalTicks / 20f,
                YperiteConfig.GAS_MIN_CONCENTRATION.get().floatValue(),
                YperiteConfig.GAS_MAX_CELLS.get(),
                wind.x(),
                wind.z(),
                YperiteConfig.WIND_GAIN.get().floatValue());
        LevelTerrain terrain = new LevelTerrain(level, windEnabled, true);

        for (Map.Entry<GasType, GasLayer> entry : data.layers().entrySet()) {
            GasLayer layer = entry.getValue();
            if (!layer.isEmpty()) {
                layer.step(terrain, entry.getKey().physics(), params);
            }
        }
        data.setDirty();
    }
}
