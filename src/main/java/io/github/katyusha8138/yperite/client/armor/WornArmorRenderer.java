package io.github.katyusha8138.yperite.client.armor;

import io.github.katyusha8138.yperite.item.armor.GeoArmorItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/** Draws worn equipment with its GeckoLib model. Client only. */
public class WornArmorRenderer extends GeoArmorRenderer<GeoArmorItem> {
    public WornArmorRenderer() {
        super(new WornGeoModel());
    }

    public static HumanoidModel<?> create() {
        return new WornArmorRenderer();
    }

    public static void prepare(HumanoidModel<?> renderer, LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
        ((WornArmorRenderer) renderer).prepForRender(entity, stack, slot, original);
    }
}
