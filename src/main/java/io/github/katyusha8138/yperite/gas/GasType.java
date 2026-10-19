package io.github.katyusha8138.yperite.gas;

import io.github.katyusha8138.yperite.gas.sim.GasPhysics;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * One kind of gas. Instances are registered in the {@code yperite:gas_type} registry, so other mods can
 * add their own. Every number here is a game tuning value, not a model of real chemistry.
 */
public final class GasType {
    private final GasCategory category;
    private final int color;
    private final float opacity;
    private final GasPhysics physics;

    private GasType(Builder builder) {
        this.category = builder.category;
        this.color = builder.color;
        this.opacity = builder.opacity;
        this.physics = new GasPhysics(
                builder.buoyancy,
                builder.diffusion,
                builder.decayPerSecond,
                builder.settlePerSecond,
                builder.residueDecayPerSecond,
                builder.evaporationPerSecond,
                builder.rainWashoutPerSecond,
                builder.waterAbsorbPerSecond);
    }

    public static Builder builder(GasCategory category) {
        return new Builder(category);
    }

    public GasCategory category() {
        return category;
    }

    /** Particle colour as 0xRRGGBB. */
    public int color() {
        return color;
    }

    /** How visible the gas is, 0 (invisible) .. 1 (thick cloud). */
    public float opacity() {
        return opacity;
    }

    public GasPhysics physics() {
        return physics;
    }

    @Nullable
    public ResourceLocation id() {
        return GasTypes.REGISTRY.get().getKey(this);
    }

    @SuppressWarnings("removal")
    public Component displayName() {
        ResourceLocation id = id();
        return Component.translatable(Util.makeDescriptionId("gas", id == null ? new ResourceLocation("missingno") : id));
    }

    public static final class Builder {
        private final GasCategory category;
        private int color = 0xFFFFFF;
        private float opacity = 0.5f;
        private float buoyancy;
        private float diffusion = 0.08f;
        private float decayPerSecond;
        private float settlePerSecond;
        private float residueDecayPerSecond = 0.01f;
        private float evaporationPerSecond;
        private float rainWashoutPerSecond;
        private float waterAbsorbPerSecond;

        private Builder(GasCategory category) {
            this.category = category;
        }

        public Builder color(int rgb) {
            this.color = rgb & 0xFFFFFF;
            return this;
        }

        public Builder opacity(float opacity) {
            this.opacity = opacity;
            return this;
        }

        /** -1 sinks, +1 rises. */
        public Builder buoyancy(float buoyancy) {
            this.buoyancy = buoyancy;
            return this;
        }

        public Builder diffusion(float diffusion) {
            this.diffusion = diffusion;
            return this;
        }

        public Builder decayPerSecond(float rate) {
            this.decayPerSecond = rate;
            return this;
        }

        /** Gas at floor level is deposited as residue at this rate. */
        public Builder settlePerSecond(float rate) {
            this.settlePerSecond = rate;
            return this;
        }

        public Builder residueDecayPerSecond(float rate) {
            this.residueDecayPerSecond = rate;
            return this;
        }

        /** Residue that returns to the air as gas. */
        public Builder evaporationPerSecond(float rate) {
            this.evaporationPerSecond = rate;
            return this;
        }

        public Builder rainWashoutPerSecond(float rate) {
            this.rainWashoutPerSecond = rate;
            return this;
        }

        public Builder waterAbsorbPerSecond(float rate) {
            this.waterAbsorbPerSecond = rate;
            return this;
        }

        public GasType build() {
            return new GasType(this);
        }
    }
}
