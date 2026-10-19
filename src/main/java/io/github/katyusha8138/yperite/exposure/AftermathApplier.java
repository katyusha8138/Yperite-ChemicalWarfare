package io.github.katyusha8138.yperite.exposure;

import io.github.katyusha8138.yperite.YperiteMod;
import io.github.katyusha8138.yperite.config.YperiteConfig;
import io.github.katyusha8138.yperite.exposure.model.ExposureState;
import io.github.katyusha8138.yperite.gas.GasCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * Lasting damage, a different one for each gas category (see the design notes):
 * <ul>
 *   <li>blister agents lower the maximum health</li>
 *   <li>nerve agents slow the player down</li>
 *   <li>choking agents make sprinting tire the player out</li>
 *   <li>blood agents make the player hungrier</li>
 *   <li>riot control and incapacitating agents leave nothing</li>
 * </ul>
 * The size comes from the worst symptoms the player ever had and is scaled by the config. Turning the effect off
 * in the config removes the penalties but keeps the record, so turning it on again brings them back.
 */
final class AftermathApplier {
    private static final UUID MAX_HEALTH_ID = UUID.fromString("5c4a2f6e-3b0d-4f55-9a63-7d2f6a1e9b01");
    private static final UUID SPEED_ID = UUID.fromString("0e8b7c42-6a39-4d0c-8f1a-b3c5d2e47a02");

    /** Share of movement speed lost at full lasting damage. */
    private static final float MAX_SPEED_LOSS = 0.15f;
    /** Extra exhaustion per second while sprinting, at full lasting damage. */
    private static final float SPRINT_EXHAUSTION = 0.24f;
    /** Extra exhaustion per second at all times, at full lasting damage. */
    private static final float HUNGER_EXHAUSTION = 0.08f;

    private AftermathApplier() {
    }

    static void apply(ServerPlayer player, PlayerExposure exposure, float dt) {
        ExposureState state = exposure.state();
        boolean enabled = YperiteConfig.AFTEREFFECTS_ENABLED.get();
        float strength = enabled ? YperiteConfig.AFTEREFFECTS_STRENGTH.get().floatValue() : 0f;

        notifyNewDamage(player, exposure, state);

        float healthLoss = level(state, GasCategory.BLISTER) * strength * YperiteConfig.AFTEREFFECTS_MAX_HEALTH_LOSS.get().floatValue();
        setModifier(player, Attributes.MAX_HEALTH, MAX_HEALTH_ID, "yperite.blister_aftermath", -healthLoss,
                AttributeModifier.Operation.ADDITION);
        exposure.appliedMaxHealthLoss = healthLoss;
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }

        float speedLoss = level(state, GasCategory.NERVE) * strength * MAX_SPEED_LOSS;
        setModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID, "yperite.nerve_aftermath", -speedLoss,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
        exposure.appliedSpeedPenalty = speedLoss;

        float breathless = level(state, GasCategory.CHOKING) * strength;
        if (breathless > 0f && player.isSprinting()) {
            player.causeFoodExhaustion(breathless * SPRINT_EXHAUSTION * dt);
        }
        float hungry = level(state, GasCategory.BLOOD) * strength;
        if (hungry > 0f) {
            player.causeFoodExhaustion(hungry * HUNGER_EXHAUSTION * dt);
        }
    }

    private static float level(ExposureState state, GasCategory category) {
        return Math.min(1f, state.aftermath[category.ordinal()]);
    }

    /** Tells the player once when a category first leaves lasting damage. Nothing is said right after loading. */
    private static void notifyNewDamage(ServerPlayer player, PlayerExposure exposure, ExposureState state) {
        boolean messages = YperiteConfig.EXPOSURE_MESSAGES.get() && YperiteConfig.AFTEREFFECTS_ENABLED.get();
        for (int k = 0; k < ExposureProfiles.count(); k++) {
            float now = state.aftermath[k];
            float before = exposure.shownAftermath[k];
            if (messages && before == 0f && now > 0f) {
                GasCategory category = ExposureProfiles.category(k);
                if (hasAftermath(category)) {
                    player.sendSystemMessage(Component.translatable("aftermath.yperite." + category.id()));
                }
            }
            exposure.shownAftermath[k] = now;
        }
    }

    static boolean hasAftermath(GasCategory category) {
        return switch (category) {
            case BLISTER, NERVE, CHOKING, BLOOD -> true;
            case RIOT_CONTROL, INCAPACITANT -> false;
        };
    }

    private static void setModifier(ServerPlayer player, Attribute attribute, UUID id, String name, double amount,
                                    AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            YperiteMod.LOGGER.warn("Player has no attribute {}", attribute.getDescriptionId());
            return;
        }
        AttributeModifier existing = instance.getModifier(id);
        if (existing != null && Math.abs(existing.getAmount() - amount) < 1e-3) {
            return;
        }
        if (existing != null) {
            instance.removeModifier(id);
        }
        if (Math.abs(amount) > 1e-4) {
            instance.addTransientModifier(new AttributeModifier(id, name, amount, operation));
        }
    }
}
