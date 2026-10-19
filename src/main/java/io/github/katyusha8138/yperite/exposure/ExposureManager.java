package io.github.katyusha8138.yperite.exposure;

import io.github.katyusha8138.yperite.config.YperiteConfig;
import io.github.katyusha8138.yperite.exposure.model.ExposureModel;
import io.github.katyusha8138.yperite.exposure.model.ExposureState;
import io.github.katyusha8138.yperite.gas.GasCategory;
import io.github.katyusha8138.yperite.gas.GasSavedData;
import io.github.katyusha8138.yperite.gas.GasType;
import io.github.katyusha8138.yperite.gas.sim.GasLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;

/**
 * Turns the gas around a living thing into a dose, and the dose into symptoms.
 *
 * <p>Players follow the full model (see {@link ExposureModel}): the dose builds up, takes effect after a delay,
 * fades with time and can leave lasting damage. Other living things use the simple model in {@link MobExposure}.
 */
public final class ExposureManager {
    /** Concentration above this adds no more dose, so a cell packed with gas does not become absurdly deadly. */
    static final float CONCENTRATION_CAP = 3f;
    static final float RESIDUE_CAP = 20f;

    private static final float WASH_IN_WATER = 0.5f;
    private static final float WASH_IN_RAIN = 0.03f;
    private static final float WASH_DRY = 0.003f;

    /** Status effects are refreshed before they run out, so they never flicker. */
    static final int EFFECT_TICKS = 60;

    private ExposureManager() {
    }

    /** Called every server tick for every dimension. */
    public static void tick(ServerLevel level) {
        if (!YperiteConfig.EXPOSURE_ENABLED.get()) {
            return;
        }
        int interval = YperiteConfig.EXPOSURE_INTERVAL.get();
        long time = level.getGameTime();
        if (time % interval != 0) {
            return;
        }
        GasSavedData data = GasSavedData.get(level);
        float dt = interval / 20f;
        for (ServerPlayer player : level.players()) {
            evaluatePlayer(level, player, data, dt);
        }
        // Other living things are checked a quarter as often.
        if (!data.isEmpty() && YperiteConfig.MOBS_ENABLED.get() && time % (interval * 4L) == 0) {
            MobExposure.evaluate(level, data, interval * 4 / 20f);
        }
    }

    /** Advances one player by {@code dt} seconds. Public so that tests can drive it directly. */
    public static void evaluatePlayer(ServerLevel level, ServerPlayer player, GasSavedData data, float dt) {
        PlayerExposure exposure = PlayerExposure.get(player);
        if (exposure == null) {
            return;
        }
        ExposureState state = exposure.state();
        boolean gasPresent = !data.isEmpty();
        if (!gasPresent && state.isClean() && !exposure.hasAppliedModifiers()) {
            return;
        }
        if (player.isCreative() || player.isSpectator() || !player.isAlive()) {
            return;
        }

        Arrays.fill(exposure.inhaled, 0f);
        Arrays.fill(exposure.contact, 0f);
        if (gasPresent) {
            sample(player, data, exposure.inhaled, exposure.contact, dt);
        }
        ExposureModel.step(state, ExposureProfiles.all(), exposure.inhaled, exposure.contact,
                washRate(level, player), YperiteConfig.EXPOSURE_RECOVERY.get().floatValue(), dt);

        applySymptoms(level, player, exposure, dt);
        AftermathApplier.apply(player, exposure, dt);
    }

    /**
     * Adds the dose that the gas around a living thing delivers during {@code seconds}, per category, after protection.
     * Also tells the protective equipment how much reached it, so that it wears.
     * Breathing uses the air at the eyes (nothing under water); contact uses the air around the body and the
     * residue on the ground it stands on.
     */
    public static void sample(LivingEntity entity, GasSavedData data, float[] inhaled, float[] contact, float seconds) {
        BlockPos eye = BlockPos.containing(entity.getEyePosition());
        BlockPos feet = entity.blockPosition();
        BlockPos mid = BlockPos.containing(entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ());
        boolean breathing = !entity.isEyeInFluid(FluidTags.WATER);
        boolean grounded = entity.onGround();
        float scale = YperiteConfig.EXPOSURE_POTENCY.get().floatValue() * seconds;

        for (Map.Entry<GasType, GasLayer> entry : data.layers().entrySet()) {
            GasLayer layer = entry.getValue();
            if (layer.isEmpty()) {
                continue;
            }
            GasType type = entry.getKey();
            GasType.Toxicity toxicity = type.toxicity();
            GasCategory category = type.category();
            int k = category.ordinal();

            float atEyes = Math.min(layer.get(eye.getX(), eye.getY(), eye.getZ()), CONCENTRATION_CAP);
            float rawInhaled = 0f;
            if (breathing && toxicity.inhalation() > 0f && atEyes > 0f) {
                rawInhaled = toxicity.inhalation() * atEyes * scale;
            }

            float atBody = Math.max(atEyes, Math.min(Math.max(
                    layer.get(feet.getX(), feet.getY(), feet.getZ()),
                    layer.get(mid.getX(), mid.getY(), mid.getZ())), CONCENTRATION_CAP));
            float rawContact = 0f;
            if (toxicity.skin() > 0f && atBody > 0f) {
                rawContact += toxicity.skin() * atBody * scale;
            }
            if (grounded && toxicity.residue() > 0f) {
                float onGround = Math.min(layer.getResidue(feet.getX(), feet.getY(), feet.getZ()), RESIDUE_CAP);
                if (onGround > 0f) {
                    rawContact += toxicity.residue() * onGround * scale;
                }
            }

            if (rawInhaled > 0f || rawContact > 0f) {
                ProtectionRegistry.Protection protection = ProtectionRegistry.of(entity, category);
                inhaled[k] += rawInhaled * protection.inhale();
                contact[k] += rawContact * protection.skin();
                ProtectionRegistry.notifyDose(entity, category, rawInhaled, rawContact);
            }
        }
    }

    private static float washRate(ServerLevel level, ServerPlayer player) {
        if (player.isInWater()) {
            return WASH_IN_WATER;
        }
        if (level.isRaining() && level.isRainingAt(player.blockPosition())) {
            return WASH_IN_RAIN;
        }
        return WASH_DRY;
    }

    private static void applySymptoms(ServerLevel level, ServerPlayer player, PlayerExposure exposure, float dt) {
        ExposureState state = exposure.state();
        boolean messages = YperiteConfig.EXPOSURE_MESSAGES.get();
        float damagePerSecond = 0f;
        float airDrain = 0f;
        boolean stopSprint = false;

        for (int k = 0; k < ExposureProfiles.count(); k++) {
            GasCategory category = ExposureProfiles.category(k);
            int stage = ExposureModel.stage(state.active[k]);
            int shown = exposure.shownStage[k];
            if (messages && shown >= 0 && stage > shown) {
                player.displayClientMessage(Component.translatable("symptom.yperite." + category.id() + "." + stage), true);
            }
            exposure.shownStage[k] = stage;
            if (stage == 0) {
                continue;
            }
            SymptomTable.Symptom symptom = SymptomTable.get(category, stage);
            applyEffects(player, symptom);
            damagePerSecond += symptom.dps();
            airDrain = Math.max(airDrain, symptom.airDrain());
            stopSprint |= symptom.stopSprint();
        }

        if (airDrain > 0f && !player.isEyeInFluid(FluidTags.WATER)) {
            int target = (int) (player.getMaxAirSupply() * (1f - airDrain));
            if (player.getAirSupply() > target) {
                player.setAirSupply(target);
            }
        }
        if (stopSprint && player.isSprinting()) {
            player.setSprinting(false);
        }

        exposure.damageBuffer = flushDamage(level, player,
                exposure.damageBuffer + damagePerSecond * YperiteConfig.EXPOSURE_DAMAGE.get().floatValue() * dt);
    }

    static void applyEffects(LivingEntity entity, SymptomTable.Symptom symptom) {
        for (SymptomTable.Effect effect : symptom.effects()) {
            MobEffectInstance current = entity.getEffect(effect.effect());
            if (current != null && current.getAmplifier() >= effect.amplifier() && current.getDuration() > EFFECT_TICKS / 2) {
                continue;
            }
            entity.addEffect(new MobEffectInstance(effect.effect(), EFFECT_TICKS, effect.amplifier(), false, false, true));
        }
    }

    /**
     * Small amounts of damage are collected and dealt in lumps of at least one point, because vanilla ignores
     * most hits that follow another one too closely. Returns what is left in the buffer.
     */
    static float flushDamage(ServerLevel level, LivingEntity entity, float buffer) {
        if (buffer >= 1f) {
            entity.hurt(GasDamage.source(level), buffer);
            return 0f;
        }
        return buffer;
    }

    // ------------------------------------------------------------------ for commands and other code

    /** The dose record of a player, or null if the player has none. Treat it as read-only. */
    @Nullable
    public static ExposureState stateOf(Player player) {
        PlayerExposure exposure = PlayerExposure.get(player);
        return exposure == null ? null : exposure.state();
    }

    /** Puts dose into the body as if it had just been absorbed; it takes effect after the category's onset delay. */
    public static void addLatent(Player player, GasCategory category, float amount) {
        PlayerExposure exposure = PlayerExposure.get(player);
        if (exposure != null && amount > 0f) {
            exposure.state().latent[category.ordinal()] += amount;
        }
    }

    /**
     * Treats a category: the dose in the body (latent and active) is multiplied by the given factors, and the
     * contamination on the skin can be cleared. Lasting damage is not affected.
     */
    public static void treat(Player player, GasCategory category, float latentFactor, float activeFactor, boolean clearSkin) {
        PlayerExposure exposure = PlayerExposure.get(player);
        if (exposure == null) {
            return;
        }
        int k = category.ordinal();
        ExposureState state = exposure.state();
        state.latent[k] *= Math.max(0f, latentFactor);
        state.active[k] *= Math.max(0f, activeFactor);
        if (clearSkin) {
            state.skin[k] = 0f;
        }
    }

    /** Washes off the contamination on the skin, for every category. */
    public static void decontaminate(Player player) {
        PlayerExposure exposure = PlayerExposure.get(player);
        if (exposure != null) {
            Arrays.fill(exposure.state().skin, 0f);
        }
    }

    /** Removes all dose and all lasting damage, and the penalties that came with them. */
    public static void clear(ServerPlayer player) {
        PlayerExposure exposure = PlayerExposure.get(player);
        if (exposure == null) {
            return;
        }
        exposure.state().clear();
        exposure.damageBuffer = 0f;
        Arrays.fill(exposure.shownStage, -1);
        Arrays.fill(exposure.shownAftermath, -1f);
        AftermathApplier.apply(player, exposure, 0f);
    }
}
