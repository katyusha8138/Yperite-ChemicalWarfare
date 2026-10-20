package io.github.katyusha8138.yperite.compat.superbwarfare.client;

import io.github.katyusha8138.yperite.client.item.HeldGeoRenderers;
import io.github.katyusha8138.yperite.compat.superbwarfare.SbwCompat;
import io.github.katyusha8138.yperite.compat.superbwarfare.StickGrenadeItem;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;

/** Client side of the stick grenade: its 3D item model and the renderer of the thrown grenade. */
public final class StickGrenadeClient {
    private StickGrenadeClient() {
    }

    public static BlockEntityWithoutLevelRenderer itemRenderer() {
        return HeldGeoRenderers.<StickGrenadeItem>create("stick_grenade");
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SbwCompat.STICK_GRENADE_ENTITY.get(), StickGrenadeRenderer::new);
    }
}
