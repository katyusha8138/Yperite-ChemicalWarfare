package io.github.katyusha8138.yperite.exposure;

import io.github.katyusha8138.yperite.gas.GasCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * What each stage of each gas category does. Deliberately abstract: a few familiar status effects, some damage
 * and, for choking agents, a loss of breath. Nothing here describes real symptoms in detail.
 */
public final class SymptomTable {
    public record Effect(MobEffect effect, int amplifier) {
    }

    /**
     * @param effects    status effects to keep applied
     * @param dps        damage per second (ignores armour)
     * @param airDrain   share of the air supply taken away, 0..1 (visible as lost bubbles)
     * @param stopSprint whether sprinting is cut short
     */
    public record Symptom(List<Effect> effects, float dps, float airDrain, boolean stopSprint) {
        public static final Symptom NONE = new Symptom(List.of(), 0f, 0f, false);
    }

    private static final Map<GasCategory, Symptom[]> TABLE = new EnumMap<>(GasCategory.class);

    static {
        // Index 0 is stage 1, index 3 is stage 4.
        TABLE.put(GasCategory.RIOT_CONTROL, new Symptom[]{
                symptom(0f, 0f, false, slow(0)),
                symptom(0f, 0f, false, slow(0), nausea(0)),
                symptom(0f, 0f, false, slow(1), nausea(0), blind(0)),
                symptom(0f, 0f, false, slow(1), nausea(0), blind(0), weak(0)),
        });
        TABLE.put(GasCategory.CHOKING, new Symptom[]{
                symptom(0f, 0.25f, false),
                symptom(0.1f, 0.5f, true, slow(0)),
                symptom(0.35f, 0.8f, true, slow(0), nausea(0)),
                symptom(0.9f, 1f, true, slow(1), nausea(0), weak(0)),
        });
        TABLE.put(GasCategory.BLOOD, new Symptom[]{
                symptom(0f, 0f, false, hunger(0)),
                symptom(0f, 0f, false, hunger(0), weak(0)),
                symptom(0.25f, 0f, false, hunger(1), weak(1), slow(0)),
                symptom(1.2f, 0f, false, hunger(2), weak(1), slow(1), fatigue(0)),
        });
        TABLE.put(GasCategory.BLISTER, new Symptom[]{
                symptom(0f, 0f, false, weak(0)),
                symptom(0.12f, 0f, false, weak(0), slow(0)),
                symptom(0.35f, 0f, false, weak(1), slow(1), nausea(0)),
                symptom(0.7f, 0f, false, weak(1), slow(1), nausea(0), blind(0)),
        });
        TABLE.put(GasCategory.NERVE, new Symptom[]{
                symptom(0f, 0f, false, slow(0)),
                symptom(0f, 0f, false, slow(1), fatigue(0), nausea(0)),
                symptom(0.3f, 0f, false, slow(2), fatigue(1), nausea(0), weak(0)),
                // Slowness 7 holds the body still: paralysis.
                symptom(1.5f, 0f, false, slow(6), fatigue(2), nausea(0), weak(1)),
        });
        TABLE.put(GasCategory.INCAPACITANT, new Symptom[]{
                symptom(0f, 0f, false, slow(0)),
                symptom(0f, 0f, false, slow(0), weak(0), nausea(0)),
                symptom(0f, 0f, false, slow(1), weak(1), fatigue(0), nausea(0)),
                symptom(0f, 0f, false, slow(2), weak(1), fatigue(1), nausea(0), blind(0)),
        });
    }

    private SymptomTable() {
    }

    /** Symptoms of a stage (1..4); {@link Symptom#NONE} for stage 0. */
    public static Symptom get(GasCategory category, int stage) {
        if (stage <= 0) {
            return Symptom.NONE;
        }
        Symptom[] stages = TABLE.get(category);
        return stages[Math.min(stage, stages.length) - 1];
    }

    private static Symptom symptom(float dps, float airDrain, boolean stopSprint, Effect... effects) {
        return new Symptom(List.of(effects), dps, airDrain, stopSprint);
    }

    private static Effect slow(int amplifier) {
        return new Effect(MobEffects.MOVEMENT_SLOWDOWN, amplifier);
    }

    private static Effect weak(int amplifier) {
        return new Effect(MobEffects.WEAKNESS, amplifier);
    }

    private static Effect nausea(int amplifier) {
        return new Effect(MobEffects.CONFUSION, amplifier);
    }

    private static Effect blind(int amplifier) {
        return new Effect(MobEffects.BLINDNESS, amplifier);
    }

    private static Effect hunger(int amplifier) {
        return new Effect(MobEffects.HUNGER, amplifier);
    }

    private static Effect fatigue(int amplifier) {
        return new Effect(MobEffects.DIG_SLOWDOWN, amplifier);
    }
}
