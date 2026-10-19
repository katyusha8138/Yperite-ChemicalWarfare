package io.github.katyusha8138.yperite.entity;

import io.github.katyusha8138.yperite.YperiteMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, YperiteMod.MODID);

    public static final RegistryObject<EntityType<GasGrenadeEntity>> GAS_GRENADE = ENTITIES.register("gas_grenade",
            () -> EntityType.Builder.<GasGrenadeEntity>of(GasGrenadeEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("gas_grenade"));

    private ModEntities() {
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
    }
}
