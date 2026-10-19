package io.github.katyusha8138.yperite.exposure;

import io.github.katyusha8138.yperite.exposure.model.ExposureState;
import io.github.katyusha8138.yperite.gas.GasCategory;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * What a player has taken in. It is saved with the player, copied when changing dimension, and not copied on
 * death, so dying wipes the symptoms and the lasting damage alike.
 */
public final class PlayerExposure {
    public static final Capability<PlayerExposure> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
    });

    private static final String TAG_SKIN = "Skin";
    private static final String TAG_LATENT = "Latent";
    private static final String TAG_ACTIVE = "Active";
    private static final String TAG_AFTERMATH = "Aftermath";

    private final ExposureState state = new ExposureState(ExposureProfiles.count());

    // Everything below is rebuilt after loading and is not saved.
    final float[] inhaled = new float[ExposureProfiles.count()];
    final float[] contact = new float[ExposureProfiles.count()];
    /** Stage last reported to the player, -1 until the first evaluation. */
    final int[] shownStage = new int[ExposureProfiles.count()];
    /** Lasting damage last reported to the player, -1 until the first evaluation. */
    final float[] shownAftermath = new float[ExposureProfiles.count()];
    float damageBuffer;
    float appliedMaxHealthLoss;
    float appliedSpeedPenalty;

    public PlayerExposure() {
        Arrays.fill(shownStage, -1);
        Arrays.fill(shownAftermath, -1f);
    }

    public ExposureState state() {
        return state;
    }

    /** True while attribute changes made for lasting damage are still in place. */
    boolean hasAppliedModifiers() {
        return appliedMaxHealthLoss != 0f || appliedSpeedPenalty != 0f;
    }

    @Nullable
    public static PlayerExposure get(Player player) {
        return player.getCapability(CAPABILITY).resolve().orElse(null);
    }

    public CompoundTag save() {
        CompoundTag root = new CompoundTag();
        for (int k = 0; k < ExposureProfiles.count(); k++) {
            if (state.skin[k] == 0f && state.latent[k] == 0f && state.active[k] == 0f && state.aftermath[k] == 0f) {
                continue;
            }
            CompoundTag tag = new CompoundTag();
            tag.putFloat(TAG_SKIN, state.skin[k]);
            tag.putFloat(TAG_LATENT, state.latent[k]);
            tag.putFloat(TAG_ACTIVE, state.active[k]);
            tag.putFloat(TAG_AFTERMATH, state.aftermath[k]);
            root.put(ExposureProfiles.category(k).id(), tag);
        }
        return root;
    }

    public void load(CompoundTag root) {
        state.clear();
        for (int k = 0; k < ExposureProfiles.count(); k++) {
            GasCategory category = ExposureProfiles.category(k);
            if (root.contains(category.id(), Tag.TAG_COMPOUND)) {
                CompoundTag tag = root.getCompound(category.id());
                state.skin[k] = tag.getFloat(TAG_SKIN);
                state.latent[k] = tag.getFloat(TAG_LATENT);
                state.active[k] = tag.getFloat(TAG_ACTIVE);
                state.aftermath[k] = tag.getFloat(TAG_AFTERMATH);
            }
        }
    }

    /** Attaches a {@link PlayerExposure} to a player and saves it with them. */
    static final class Provider implements ICapabilitySerializable<CompoundTag> {
        private final PlayerExposure exposure = new PlayerExposure();
        private final LazyOptional<PlayerExposure> optional = LazyOptional.of(() -> exposure);

        @NotNull
        @Override
        public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return cap == CAPABILITY ? optional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            return exposure.save();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            exposure.load(nbt);
        }

        void invalidate() {
            optional.invalidate();
        }
    }
}
