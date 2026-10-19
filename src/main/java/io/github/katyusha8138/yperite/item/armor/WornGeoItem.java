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

    /**
     * How far this piece stands off the chest at the front (in model pixels). Whatever is worn over it - body
     * armour over a tunic, a respirator's haversack over both - moves its bone {@code layer_front} out by the sum
     * of what lies beneath (see {@link WornLayers}). The numbers match those the models are built with
     * (tools/models: uniforms.STAND_OFF, body_armor.STAND_OFF).
     */
    default float standOffFront() {
        return 0f;
    }

    /** The same at the back, for the bone {@code layer_back}. */
    default float standOffBack() {
        return 0f;
    }
}
