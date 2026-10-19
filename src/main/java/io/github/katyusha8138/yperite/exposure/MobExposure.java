package io.github.katyusha8138.yperite.exposure;

import io.github.katyusha8138.yperite.YperiteMod;
import io.github.katyusha8138.yperite.config.YperiteConfig;
import io.github.katyusha8138.yperite.exposure.model.ExposureModel;
import io.github.katyusha8138.yperite.gas.GasSavedData;
import io.github.katyusha8138.yperite.gas.sim.Cells;
import io.github.katyusha8138.yperite.gas.sim.GasLayer;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * The simple model for everything that is not a player: animals, villagers, monsters. There is no build-up and
 * no delay; the symptoms follow the gas around the creature right now, as if it had been breathing it for
 * a few seconds, and fade as soon as it leaves.
 */
final class MobExposure {
    /** The gas around a creature counts as this many seconds of exposure. */
    private static final float WINDOW_SECONDS = 6f;

    /** Creatures that gas does not affect, for example golems and armour stands. */
    static final TagKey<EntityType<?>> IMMUNE = tag();
    private static final String TAG_DAMAGE = "yperite_gas_damage";

    private MobExposure() {
    }

    @SuppressWarnings("removal")
    private static TagKey<EntityType<?>> tag() {
        return TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(YperiteMod.MODID, "gas_immune"));
    }

    static void evaluate(ServerLevel level, GasSavedData data, float seconds) {
        LongOpenHashSet chunks = new LongOpenHashSet();
        for (GasLayer layer : data.layers().values()) {
            layer.collectChunkKeys(chunks);
        }
        IntOpenHashSet done = new IntOpenHashSet();
        float[] dose = new float[ExposureProfiles.count()];
        float[] unused = new float[ExposureProfiles.count()];

        LongIterator it = chunks.iterator();
        while (it.hasNext()) {
            long key = it.nextLong();
            int chunkX = Cells.chunkX(key);
            int chunkZ = Cells.chunkZ(key);
            if (!level.hasChunk(chunkX, chunkZ)) {
                continue;
            }
            AABB box = new AABB(chunkX << 4, level.getMinBuildHeight(), chunkZ << 4,
                    (chunkX << 4) + 16, level.getMaxBuildHeight(), (chunkZ << 4) + 16);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, MobExposure::eligible)) {
                if (done.add(entity.getId())) {
                    evaluateOne(level, data, entity, seconds, dose, unused);
                }
            }
        }
    }

    static boolean eligible(LivingEntity entity) {
        return !(entity instanceof Player) && entity.isAlive() && !isImmune(entity);
    }

    static boolean isImmune(LivingEntity entity) {
        if (entity.getType().is(IMMUNE)) {
            return true;
        }
        return YperiteConfig.MOBS_UNDEAD_IMMUNE.get() && entity.getMobType() == MobType.UNDEAD;
    }

    private static void evaluateOne(ServerLevel level, GasSavedData data, LivingEntity entity, float seconds,
                                    float[] dose, float[] contactScratch) {
        java.util.Arrays.fill(dose, 0f);
        java.util.Arrays.fill(contactScratch, 0f);
        ExposureManager.sample(entity, data, dose, contactScratch, WINDOW_SECONDS);

        float damagePerSecond = 0f;
        for (int k = 0; k < ExposureProfiles.count(); k++) {
            int stage = ExposureModel.stage(dose[k] + contactScratch[k]);
            if (stage == 0) {
                continue;
            }
            SymptomTable.Symptom symptom = SymptomTable.get(ExposureProfiles.category(k), stage);
            ExposureManager.applyEffects(entity, symptom);
            damagePerSecond += symptom.dps();
        }
        if (damagePerSecond <= 0f) {
            return;
        }
        CompoundTag persistent = entity.getPersistentData();
        float buffer = persistent.getFloat(TAG_DAMAGE)
                + damagePerSecond * YperiteConfig.EXPOSURE_DAMAGE.get().floatValue() * seconds;
        persistent.putFloat(TAG_DAMAGE, ExposureManager.flushDamage(level, entity, buffer));
    }
}
