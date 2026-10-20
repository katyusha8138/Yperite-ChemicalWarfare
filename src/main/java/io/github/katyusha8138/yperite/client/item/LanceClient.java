package io.github.katyusha8138.yperite.client.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.katyusha8138.yperite.item.LanceItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/**
 * How the lance is held. Carried, it rides up at an angle like any held weapon. Couched for the charge, the arm
 * holds it close against the side, the point lowered a little towards what lies ahead; in first person it is
 * levelled at the middle of the view.
 */
public final class LanceClient {
    /** The arm with a couched lance: drawn back a little, close to the body; the other hand forward on the reins. */
    public static final HumanoidModel.ArmPose COUCHED = HumanoidModel.ArmPose.create("YPERITE_LANCE_COUCHED", false,
            (model, entity, arm) -> {
                if (arm == HumanoidArm.RIGHT) {
                    model.rightArm.xRot = 0.3f;
                    model.rightArm.yRot = -0.12f;
                    model.leftArm.xRot = -0.55f;
                    model.leftArm.yRot = 0.1f;
                } else {
                    model.leftArm.xRot = 0.3f;
                    model.leftArm.yRot = 0.12f;
                    model.rightArm.xRot = -0.55f;
                    model.rightArm.yRot = -0.1f;
                }
            });

    private LanceClient() {
    }

    static boolean couched(LivingEntity entity, ItemStack stack) {
        return entity.isUsingItem() && entity.getUseItem() == stack && stack.getItem() instanceof LanceItem;
    }

    public static IClientItemExtensions extensions() {
        return new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = HeldGeoRenderers.<LanceItem>create("lance");
                }
                return renderer;
            }

            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                return couched(entity, stack) ? COUCHED : null;
            }

            @Override
            public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                                                   float partialTick, float equipProcess, float swingProcess) {
                if (!couched(player, stack)) {
                    return false;
                }
                // The lance held low at the right, its point levelled at the middle of the view about two blocks
                // ahead: the grip at the lower right, the shaft running in towards the crosshair.
                int side = arm == HumanoidArm.RIGHT ? 1 : -1;
                pose.translate(side * 0.34f, -0.41f + equipProcess * -0.6f, -0.81f);
                pose.mulPose(Axis.YP.rotationDegrees(side * 16.5f));
                pose.mulPose(Axis.XP.rotationDegrees(-72f));
                // undo the item's own first-person transform, which is applied after this (lance.json:
                // rotation [0, -90, -20] and translation [1.13, 3.2, 1.13], mirrored in x for the left hand)
                pose.mulPose(Axis.ZP.rotationDegrees(20f));
                pose.mulPose(Axis.YP.rotationDegrees(90f));
                pose.translate(-side * 1.13f / 16f, -3.2f / 16f, -1.13f / 16f);
                return true;
            }
        };
    }
}
