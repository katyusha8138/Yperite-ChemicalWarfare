package io.github.katyusha8138.yperite.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.katyusha8138.yperite.YperiteMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/**
 * Draws the gas mask on the wearer's face when it sits in a Curios slot.
 *
 * <p>Curios draws a worn item only if a renderer is registered for it, and unlike the helmet slot it has no
 * fallback for armour. Without this class a mask in the face slot is simply invisible. It reuses the
 * mask's armour texture and the vanilla armour model, and draws nothing but the head part.
 */
@SuppressWarnings("removal")
public class GasMaskCurioRenderer implements ICurioRenderer {
    private static final ResourceLocation TEXTURE = new ResourceLocation(YperiteMod.MODID, "textures/models/armor/gas_mask_layer_1.png");

    /** Built on first use: the entity models are not guaranteed to exist when this object is created. */
    private HumanoidModel<LivingEntity> model;

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(
            ItemStack stack, SlotContext slotContext, PoseStack poseStack, RenderLayerParent<T, M> renderLayerParent,
            MultiBufferSource buffer, int light, float limbSwing, float limbSwingAmount, float partialTicks,
            float ageInTicks, float netHeadYaw, float headPitch) {
        if (model == null) {
            model = new HumanoidModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
        }
        model.setAllVisible(false);
        model.head.visible = true;
        // Copy the head's position and turn, so that the mask follows the player's head and ducks with them.
        ICurioRenderer.followHeadRotations(slotContext.entity(), model.head);

        VertexConsumer consumer = ItemRenderer.getArmorFoilBuffer(buffer, RenderType.armorCutoutNoCull(TEXTURE), false, stack.hasFoil());
        model.head.render(poseStack, consumer, light, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
    }
}
