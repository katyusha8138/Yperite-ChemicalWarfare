package io.github.katyusha8138.yperite.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.katyusha8138.yperite.client.armor.WornGeoModel;
import io.github.katyusha8138.yperite.item.armor.WornGeoItem;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/**
 * Draws an item worn in a Curios slot with the same GeckoLib model it has in an armour slot.
 *
 * <p>Curios draws a worn item only if a renderer is registered for it, and unlike the armour slots it has no
 * fallback: without this class a mask in the face slot or body armour in the body slot would be invisible. The
 * model takes its pose from the wearer's body model, exactly as in the armour slot ({@code slot} says which one).
 */
public class WornCurioRenderer implements ICurioRenderer {
    private final EquipmentSlot slot;

    public WornCurioRenderer(EquipmentSlot slot) {
        this.slot = slot;
    }

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(
            ItemStack stack, SlotContext slotContext, PoseStack poseStack, RenderLayerParent<T, M> renderLayerParent,
            MultiBufferSource buffer, int light, float limbSwing, float limbSwingAmount, float partialTicks,
            float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(renderLayerParent.getModel() instanceof HumanoidModel<?> body) || !(stack.getItem() instanceof WornGeoItem item)) {
            return;
        }
        HumanoidModel<?> model = IClientItemExtensions.of(stack).getHumanoidArmorModel(slotContext.entity(), stack, slot, body);
        // GeckoLib draws into its own buffer; the consumer passed here is only a placeholder of the right kind
        model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.armorCutoutNoCull(WornGeoModel.texture(item))), light,
                OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
    }
}
