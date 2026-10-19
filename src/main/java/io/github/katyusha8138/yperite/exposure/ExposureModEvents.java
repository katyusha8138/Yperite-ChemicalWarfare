package io.github.katyusha8138.yperite.exposure;

import io.github.katyusha8138.yperite.YperiteMod;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Mod-bus registration for the exposure data. */
@Mod.EventBusSubscriber(modid = YperiteMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ExposureModEvents {
    private ExposureModEvents() {
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(PlayerExposure.class);
    }
}
