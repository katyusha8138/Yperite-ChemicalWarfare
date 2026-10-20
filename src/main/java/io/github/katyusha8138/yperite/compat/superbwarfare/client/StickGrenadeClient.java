package io.github.katyusha8138.yperite.compat.superbwarfare.client;

import io.github.katyusha8138.yperite.YperiteMod;
import io.github.katyusha8138.yperite.compat.superbwarfare.SbwCompat;
import io.github.katyusha8138.yperite.compat.superbwarfare.StickGrenadeItem;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/** Client side of the stick grenade: its 3D item model and the renderer of the thrown grenade. */
@SuppressWarnings("removal")
public final class StickGrenadeClient {
    private static final ResourceLocation MODEL = new ResourceLocation(YperiteMod.MODID, "geo/item/stick_grenade.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(YperiteMod.MODID, "textures/geo/stick_grenade.png");
    private static final ResourceLocation NO_ANIMATIONS = new ResourceLocation(YperiteMod.MODID, "animations/armor/static.animation.json");

    private StickGrenadeClient() {
    }

    public static BlockEntityWithoutLevelRenderer itemRenderer() {
        return new GeoItemRenderer<>(new GeoModel<StickGrenadeItem>() {
            @Override
            public ResourceLocation getModelResource(StickGrenadeItem item) {
                return MODEL;
            }

            @Override
            public ResourceLocation getTextureResource(StickGrenadeItem item) {
                return TEXTURE;
            }

            @Override
            public ResourceLocation getAnimationResource(StickGrenadeItem item) {
                return NO_ANIMATIONS;
            }
        });
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SbwCompat.STICK_GRENADE_ENTITY.get(), StickGrenadeRenderer::new);
    }
}
