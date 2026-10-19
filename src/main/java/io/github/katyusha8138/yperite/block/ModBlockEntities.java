package io.github.katyusha8138.yperite.block;

import io.github.katyusha8138.yperite.YperiteMod;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, YperiteMod.MODID);

    public static final RegistryObject<BlockEntityType<GasCylinderBlockEntity>> GAS_CYLINDER = TYPES.register("gas_cylinder",
            () -> BlockEntityType.Builder.of(GasCylinderBlockEntity::new, ModBlocks.GAS_CYLINDER.get()).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }
}
