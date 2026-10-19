package io.github.katyusha8138.yperite.client;

import io.github.katyusha8138.yperite.YperiteMod;
import io.github.katyusha8138.yperite.client.compat.CuriosClient;
import io.github.katyusha8138.yperite.protection.compat.CuriosCompat;
import io.github.katyusha8138.yperite.entity.ModEntities;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Client-only setup. Servers never load this class. */
@Mod.EventBusSubscriber(modid = YperiteMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.GAS_GRENADE.get(), ThrownItemRenderer::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            if (CuriosCompat.isLoaded()) {
                CuriosClient.register();
            }
        });
    }
}
