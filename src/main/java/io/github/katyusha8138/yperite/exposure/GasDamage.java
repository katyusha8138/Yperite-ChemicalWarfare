package io.github.katyusha8138.yperite.exposure;

import io.github.katyusha8138.yperite.YperiteMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

/** The damage type of gas. It is defined in {@code data/yperite/damage_type/gas.json} and ignores armour. */
@SuppressWarnings("removal")
public final class GasDamage {
    public static final ResourceKey<DamageType> TYPE =
            ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(YperiteMod.MODID, "gas"));

    private GasDamage() {
    }

    public static DamageSource source(Level level) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(TYPE));
    }
}
