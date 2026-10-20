package io.github.katyusha8138.yperite;

import com.mojang.logging.LogUtils;
import io.github.katyusha8138.yperite.block.ModBlockEntities;
import io.github.katyusha8138.yperite.block.ModBlocks;
import io.github.katyusha8138.yperite.compat.superbwarfare.SbwCompat;
import io.github.katyusha8138.yperite.config.YperiteConfig;
import io.github.katyusha8138.yperite.entity.ModEntities;
import io.github.katyusha8138.yperite.exposure.ProtectionRegistry;
import io.github.katyusha8138.yperite.gas.GasTypes;
import io.github.katyusha8138.yperite.item.ModItems;
import io.github.katyusha8138.yperite.item.ModRecipes;
import io.github.katyusha8138.yperite.item.ModTabs;
import io.github.katyusha8138.yperite.network.YperiteNetwork;
import io.github.katyusha8138.yperite.particle.ModParticles;
import io.github.katyusha8138.yperite.sound.ModSounds;
import io.github.katyusha8138.yperite.protection.EquipmentProtection;
import io.github.katyusha8138.yperite.protection.compat.CuriosCompat;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(YperiteMod.MODID)
public final class YperiteMod {
    public static final String MODID = "yperite";
    public static final Logger LOGGER = LogUtils.getLogger();

    @SuppressWarnings("removal") // the replacement APIs do not exist on every Forge 47.x
    public YperiteMod() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        GasTypes.register(modBus);
        ModBlocks.register(modBus);
        ModBlockEntities.register(modBus);
        ModEntities.register(modBus);
        ModItems.register(modBus);
        ModTabs.register(modBus);
        ModRecipes.register(modBus);
        ModParticles.register(modBus);
        ModSounds.register(modBus);
        ProtectionRegistry.register(new EquipmentProtection());
        modBus.addListener((FMLCommonSetupEvent event) -> {
            event.enqueueWork(YperiteNetwork::register);
            if (CuriosCompat.isLoaded()) {
                event.enqueueWork(CuriosCompat::init);
            }
        });
        if (ModList.get().isLoaded(SbwCompat.MODID)) {
            SbwCompat.init(modBus);
        }
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, YperiteConfig.SPEC);
    }
}
