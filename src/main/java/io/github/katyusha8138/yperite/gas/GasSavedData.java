package io.github.katyusha8138.yperite.gas;

import io.github.katyusha8138.yperite.YperiteMod;
import io.github.katyusha8138.yperite.gas.sim.GasLayer;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/** Gas and residue of one dimension, saved with the world. */
public final class GasSavedData extends SavedData {
    private static final String ID = "yperite_gas";
    private static final String TAG_LAYERS = "Layers";
    private static final String TAG_TYPE = "Type";
    private static final String TAG_GAS_CELLS = "GasCells";
    private static final String TAG_GAS_VALUES = "GasValues";
    private static final String TAG_RESIDUE_CELLS = "ResidueCells";
    private static final String TAG_RESIDUE_VALUES = "ResidueValues";
    private static final String TAG_WIND = "Wind";
    private static final String TAG_WIND_FROM = "FromDegrees";
    private static final String TAG_WIND_STRENGTH = "Strength";

    private final Map<GasType, GasLayer> layers = new LinkedHashMap<>();
    @Nullable
    private WindModel.Override windOverride;

    public static GasSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(GasSavedData::load, GasSavedData::new, ID);
    }

    /** The layer for the gas, created on first use. */
    public GasLayer layer(GasType type) {
        return layers.computeIfAbsent(type, t -> new GasLayer());
    }

    /** The layer for the gas, or null if none exists yet. */
    public GasLayer layerIfPresent(GasType type) {
        return layers.get(type);
    }

    public Map<GasType, GasLayer> layers() {
        return layers;
    }

    /** The wind an operator fixed for this dimension, or null when the wind follows time and weather. */
    @Nullable
    public WindModel.Override windOverride() {
        return windOverride;
    }

    public void setWindOverride(@Nullable WindModel.Override windOverride) {
        this.windOverride = windOverride;
        setDirty();
    }

    public boolean isEmpty() {
        for (GasLayer layer : layers.values()) {
            if (!layer.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        ListTag list = new ListTag();
        for (Map.Entry<GasType, GasLayer> entry : layers.entrySet()) {
            ResourceLocation id = entry.getKey().id();
            GasLayer layer = entry.getValue();
            if (id == null || layer.isEmpty()) {
                continue;
            }
            CompoundTag tag = new CompoundTag();
            tag.putString(TAG_TYPE, id.toString());

            LongArrayList keys = new LongArrayList();
            FloatArrayList values = new FloatArrayList();
            layer.exportGas(keys, values);
            tag.putLongArray(TAG_GAS_CELLS, keys.toLongArray());
            tag.put(TAG_GAS_VALUES, toIntArray(values));

            keys.clear();
            values.clear();
            layer.exportResidue(keys, values);
            tag.putLongArray(TAG_RESIDUE_CELLS, keys.toLongArray());
            tag.put(TAG_RESIDUE_VALUES, toIntArray(values));

            list.add(tag);
        }
        root.put(TAG_LAYERS, list);
        if (windOverride != null) {
            CompoundTag wind = new CompoundTag();
            wind.putFloat(TAG_WIND_FROM, windOverride.fromDegrees());
            wind.putFloat(TAG_WIND_STRENGTH, windOverride.strength());
            root.put(TAG_WIND, wind);
        }
        return root;
    }

    public static GasSavedData load(CompoundTag root) {
        GasSavedData data = new GasSavedData();
        if (root.contains(TAG_WIND, Tag.TAG_COMPOUND)) {
            CompoundTag wind = root.getCompound(TAG_WIND);
            data.windOverride = new WindModel.Override(wind.getFloat(TAG_WIND_FROM), wind.getFloat(TAG_WIND_STRENGTH));
        }
        ListTag list = root.getList(TAG_LAYERS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(tag.getString(TAG_TYPE));
            GasType type = id == null ? null : GasTypes.REGISTRY.get().getValue(id);
            if (type == null) {
                YperiteMod.LOGGER.warn("Dropping saved gas of unknown type '{}'", tag.getString(TAG_TYPE));
                continue;
            }
            GasLayer layer = data.layer(type);
            layer.importGas(tag.getLongArray(TAG_GAS_CELLS), toFloatArray(tag.getIntArray(TAG_GAS_VALUES)));
            layer.importResidue(tag.getLongArray(TAG_RESIDUE_CELLS), toFloatArray(tag.getIntArray(TAG_RESIDUE_VALUES)));
        }
        return data;
    }

    private static IntArrayTag toIntArray(FloatArrayList values) {
        int[] bits = new int[values.size()];
        for (int i = 0; i < bits.length; i++) {
            bits[i] = Float.floatToRawIntBits(values.getFloat(i));
        }
        return new IntArrayTag(bits);
    }

    private static float[] toFloatArray(int[] bits) {
        float[] values = new float[bits.length];
        for (int i = 0; i < bits.length; i++) {
            values[i] = Float.intBitsToFloat(bits[i]);
        }
        return values;
    }
}
