package io.github.katyusha8138.yperite.item.armor;

import software.bernie.geckolib.animatable.GeoItem;

/** Something worn that is drawn with a GeckoLib model: {@code geo/armor/<model>.geo.json} and its texture. */
public interface WornGeoItem extends GeoItem {
    String model();

    /**
     * Whether a bone named {@code filter} is shown for this stack (a mask hides its filter while none is loaded).
     * Bones of the body are drawn for head pieces too when the model has any (a respirator's haversack).
     */
    default boolean showsFilter(net.minecraft.world.item.ItemStack stack) {
        return true;
    }
}
