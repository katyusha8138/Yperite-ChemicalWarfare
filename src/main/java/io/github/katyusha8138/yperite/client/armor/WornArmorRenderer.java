package io.github.katyusha8138.yperite.client.armor;

import io.github.katyusha8138.yperite.item.armor.WornGeoItem;
import io.github.katyusha8138.yperite.item.armor.WornLayers;
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
 * <p>Three additions to GeckoLib's armour renderer: a bone named {@code filter} is hidden while the item says so
 * (a mask without a filter); a piece for another slot whose model has body parts (a respirator's haversack,
 * the waistband of trousers) also draws its body bone; and the bones {@code layer_front} and {@code layer_back}
 * move out by what is worn under the piece (see {@link WornLayers}).
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
        if (slot != EquipmentSlot.CHEST && body != null && (!body.getCubes().isEmpty() || !body.getChildBones().isEmpty())) {
            setBoneVisible(body, true);
        }
        ItemStack stack = getCurrentStack();
        if (stack == null || !(stack.getItem() instanceof WornGeoItem item)) {
            return;
        }
        boolean show = item.showsFilter(stack);
        getGeoModel().getBone("filter").ifPresent(bone -> ((GeoBone) bone).setHidden(!show));
        // Called while GeckoLib prepares the model, before it settles the bones: a moved bone keeps its place.
        float[] under = getCurrentEntity() instanceof LivingEntity living ? WornLayers.under(living, stack) : new float[2];
        getGeoModel().getBone("layer_front").ifPresent(bone -> bone.updatePosition(0f, 0f, -under[0]));
        getGeoModel().getBone("layer_back").ifPresent(bone -> bone.updatePosition(0f, 0f, under[1]));
    }
}
