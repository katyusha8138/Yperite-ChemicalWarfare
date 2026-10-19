package io.github.katyusha8138.yperite;

import com.mojang.logging.LogUtils;
import io.github.katyusha8138.yperite.config.YperiteConfig;
import io.github.katyusha8138.yperite.gas.GasTypes;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
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
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, YperiteConfig.SPEC);
    }
}
