package io.github.katyusha8138.yperite.client.armor;

import io.github.katyusha8138.yperite.YperiteMod;
import io.github.katyusha8138.yperite.item.armor.WornGeoItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import software.bernie.geckolib.model.GeoModel;

/** Finds an item's model and texture by its model name (see {@link WornGeoItem#model()}). */
@SuppressWarnings("removal")
public class WornGeoModel<T extends Item & WornGeoItem> extends GeoModel<T> {
    private static final ResourceLocation NO_ANIMATIONS = new ResourceLocation(YperiteMod.MODID, "animations/armor/static.animation.json");

    public static ResourceLocation texture(WornGeoItem item) {
        return new ResourceLocation(YperiteMod.MODID, "textures/armor/" + item.model() + ".png");
    }

    @Override
    public ResourceLocation getModelResource(T item) {
        return new ResourceLocation(YperiteMod.MODID, "geo/armor/" + item.model() + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T item) {
        return texture(item);
    }

    @Override
    public ResourceLocation getAnimationResource(T item) {
        return NO_ANIMATIONS;
    }
}
