package io.github.katyusha8138.yperite.compat.superbwarfare.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.katyusha8138.yperite.compat.superbwarfare.StickGrenadeEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;

/** Draws a thrown stick grenade: its item model, turned to its heading and to the angle of its tumble. */
public class StickGrenadeRenderer extends EntityRenderer<StickGrenadeEntity> {
    private final ItemRenderer items;

    public StickGrenadeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.items = context.getItemRenderer();
        this.shadowRadius = 0.15f;
    }

    @Override
    public void render(StickGrenadeEntity grenade, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        // the middle of the stick a little above the ground: the head (the widest part) just touches it at rest
        pose.translate(0.0, 0.09, 0.0);
        pose.mulPose(Axis.YP.rotationDegrees(grenade.heading(partialTick)));
        pose.mulPose(Axis.XP.rotationDegrees(grenade.spin(partialTick)));
        items.renderStatic(grenade.getItem(), ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, pose, buffers,
                grenade.level(), grenade.getId());
        pose.popPose();
        super.render(grenade, yaw, partialTick, pose, buffers, light);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureLocation(StickGrenadeEntity grenade) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
