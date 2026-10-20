package io.github.katyusha8138.yperite.client.item;

import io.github.katyusha8138.yperite.YperiteMod;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * Renderers of the items drawn as 3D models in the hand (made by tools/build_models.py): the model is
 * {@code geo/item/<name>.geo.json}, its texture {@code textures/geo/<name>.png}. They have no animations.
 */
@SuppressWarnings("removal")
public final class HeldGeoRenderers {
    private static final ResourceLocation NO_ANIMATIONS = new ResourceLocation(YperiteMod.MODID, "animations/armor/static.animation.json");

    private HeldGeoRenderers() {
    }

    public static <T extends Item & GeoAnimatable> BlockEntityWithoutLevelRenderer create(String name) {
        ResourceLocation model = new ResourceLocation(YperiteMod.MODID, "geo/item/" + name + ".geo.json");
        ResourceLocation texture = new ResourceLocation(YperiteMod.MODID, "textures/geo/" + name + ".png");
        return new GeoItemRenderer<T>(new GeoModel<T>() {
            @Override
            public ResourceLocation getModelResource(T item) {
                return model;
            }

            @Override
            public ResourceLocation getTextureResource(T item) {
                return texture;
            }

            @Override
            public ResourceLocation getAnimationResource(T item) {
                return NO_ANIMATIONS;
            }
        });
    }
}
