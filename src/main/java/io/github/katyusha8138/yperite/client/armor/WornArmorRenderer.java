package io.github.katyusha8138.yperite.client.armor;

import io.github.katyusha8138.yperite.item.armor.WornGeoItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/**
 * Draws worn equipment with its GeckoLib model. Client only.
 *
 * <p>Two additions to GeckoLib's armour renderer: a bone named {@code filter} is hidden while the item says so
 * (a mask without a filter), and a head piece whose model has body parts (a respirator's haversack) also draws its
 * body bone.
 */
public class WornArmorRenderer<T extends Item & WornGeoItem> extends GeoArmorRenderer<T> {
    public WornArmorRenderer() {
        super(new WornGeoModel<>());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static HumanoidModel<?> create() {
        return new WornArmorRenderer();
    }

    public static void prepare(HumanoidModel<?> renderer, LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
        ((WornArmorRenderer<?>) renderer).prepForRender(entity, stack, slot, original);
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot slot) {
        super.applyBoneVisibilityBySlot(slot);
        if (slot == EquipmentSlot.HEAD && body != null && (!body.getCubes().isEmpty() || !body.getChildBones().isEmpty())) {
            setBoneVisible(body, true);
        }
        ItemStack stack = getCurrentStack();
        if (stack != null && stack.getItem() instanceof WornGeoItem item) {
            boolean show = item.showsFilter(stack);
            getGeoModel().getBone("filter").ifPresent(bone -> ((GeoBone) bone).setHidden(!show));
        }
    }
}
