package io.github.katyusha8138.yperite.block;

import io.github.katyusha8138.yperite.YperiteMod;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, YperiteMod.MODID);

    public static final RegistryObject<Block> GAS_ALARM = BLOCKS.register("gas_alarm", GasAlarmBlock::new);
    public static final RegistryObject<Block> GAS_CYLINDER = BLOCKS.register("gas_cylinder", GasCylinderBlock::new);

    private ModBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
