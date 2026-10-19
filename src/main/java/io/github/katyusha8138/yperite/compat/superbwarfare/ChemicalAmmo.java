package io.github.katyusha8138.yperite.compat.superbwarfare;

import io.github.katyusha8138.yperite.gas.GasManager;
import io.github.katyusha8138.yperite.gas.GasType;
import io.github.katyusha8138.yperite.gas.GasTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/** Marking projectiles as chemical and releasing their gas. Free of Superb Warfare classes. */
public final class ChemicalAmmo {
    private ChemicalAmmo() {
    }

    /** Marks a projectile as carrying the gas, for a round of the given kind. */
    public static void mark(Entity projectile, ChemicalRound round, ResourceLocation gasId) {
        CompoundTag data = projectile.getPersistentData();
        data.putString(ChemicalRound.TAG_GAS, gasId.toString());
        data.putString(ChemicalRound.TAG_ROUND, round.dataName());
    }

    /** Whether the projectile was marked and has not released its gas yet. */
    public static boolean isArmed(Entity projectile) {
        CompoundTag data = projectile.getPersistentData();
        return data.contains(ChemicalRound.TAG_GAS, Tag.TAG_STRING)
                && !data.getBoolean(ChemicalRound.TAG_RELEASED);
    }

    /**
     * Releases the gas of a marked projectile at the given cell, once. The cell above is tried when this one is solid.
     *
     * @return the mass placed, 0 when nothing was released
     */
    public static float release(ServerLevel level, Entity projectile, BlockPos cell) {
        if (!isArmed(projectile)) {
            return 0f;
        }
        CompoundTag data = projectile.getPersistentData();
        GasType gas = gasOf(data);
        ChemicalRound round = ChemicalRound.byDataName(data.getString(ChemicalRound.TAG_ROUND));
        if (gas == null || round == null) {
            return 0f;
        }
        data.putBoolean(ChemicalRound.TAG_RELEASED, true);
        float placed = GasManager.release(level, gas, cell, round.gasRadius(), round.gasMass());
        if (placed <= 0f) {
            placed = GasManager.release(level, gas, cell.above(), round.gasRadius(), round.gasMass());
        }
        return placed;
    }

    @Nullable
    private static GasType gasOf(CompoundTag data) {
        ResourceLocation id = ResourceLocation.tryParse(data.getString(ChemicalRound.TAG_GAS));
        return id == null ? null : GasTypes.REGISTRY.get().getValue(id);
    }
}
