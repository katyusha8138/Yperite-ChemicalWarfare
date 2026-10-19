package io.github.katyusha8138.yperite.item.armor;

import io.github.katyusha8138.yperite.protection.compat.CuriosCompat;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * The order in which things are worn on the chest, from the skin outwards: the chest slot (a tunic, or body
 * armour), then the Curios body slot (body armour over a tunic), then anything else (a respirator's haversack).
 * Each piece's model is drawn for the bare body; the renderer moves its outer bones out by what lies beneath.
 */
public final class WornLayers {
    private WornLayers() {
    }

    /** How far the given stack must stand off the chest, {front, back}, for what is worn under it. */
    public static float[] under(LivingEntity entity, ItemStack stack) {
        float front = 0f;
        float back = 0f;
        for (ItemStack layer : chest(entity)) {
            if (layer == stack) {
                break;
            }
            if (layer.getItem() instanceof WornGeoItem worn) {
                front += worn.standOffFront();
                back += worn.standOffBack();
            }
        }
        return new float[]{front, back};
    }

    private static ItemStack[] chest(LivingEntity entity) {
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack body = CuriosCompat.isLoaded() ? CuriosCompat.findBodyArmour(entity) : ItemStack.EMPTY;
        return new ItemStack[]{chest, body};
    }
}
