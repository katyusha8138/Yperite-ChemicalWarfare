package io.github.katyusha8138.yperite.client.armor;

import io.github.katyusha8138.yperite.YperiteMod;
import io.github.katyusha8138.yperite.item.armor.GeoArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Finds an item's model and texture by its model name (see {@link GeoArmorItem#model()}). */
@SuppressWarnings("removal")
public class WornGeoModel extends GeoModel<GeoArmorItem> {
    private static final ResourceLocation NO_ANIMATIONS = new ResourceLocation(YperiteMod.MODID, "animations/armor/static.animation.json");

    @Override
    public ResourceLocation getModelResource(GeoArmorItem item) {
        return new ResourceLocation(YperiteMod.MODID, "geo/armor/" + item.model() + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GeoArmorItem item) {
        return new ResourceLocation(YperiteMod.MODID, "textures/armor/" + item.model() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(GeoArmorItem item) {
        return NO_ANIMATIONS;
    }
}
