package io.github.katyusha8138.yperite.gametest.client;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;

/**
 * Shows the player large, from the front, the front-left, the side and the back, on a plain background, so that worn
 * models can be judged in the real renderer. The upper row is the whole body, the lower row the head close up.
 */
public class ShowcaseScreen extends Screen {
    private static final float[] ANGLES = {0f, 40f, 90f, 180f};
    /** -1 draws all four views; otherwise only that one, large. */
    private final int only;
    /** Whether the single view shows the whole figure rather than head and chest. */
    private final boolean whole;

    public ShowcaseScreen() {
        this(-1, false);
    }

    public ShowcaseScreen(int only, boolean whole) {
        super(Component.literal("showcase"));
        this.only = only;
        this.whole = whole;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFF5A5F66);
        LivingEntity player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        if (only >= 0) {
            if (whole) {
                draw(graphics, player, width / 2, (int) (height * 0.93f), (int) (height * 0.40f), ANGLES[only]);
            } else {
                draw(graphics, player, width / 2, (int) (height * 1.42f), (int) (height * 0.55f), ANGLES[only]);
            }
            return;
        }
        int column = width / ANGLES.length;
        for (int k = 0; k < ANGLES.length; k++) {
            int x = column * k + column / 2;
            draw(graphics, player, x, height / 2 - 4, height / 6, ANGLES[k]);
            draw(graphics, player, x, height + height / 3 + 20, height / 3, ANGLES[k]);
        }
    }

    private static void draw(GuiGraphics graphics, LivingEntity entity, int x, int y, int scale, float yaw) {
        float bodyRot = entity.yBodyRot, yRot = entity.getYRot(), xRot = entity.getXRot(), headRotO = entity.yHeadRotO, headRot = entity.yHeadRot;
        entity.yBodyRot = 180f;
        entity.setYRot(180f);
        entity.setXRot(0f);
        entity.yHeadRot = entity.getYRot();
        entity.yHeadRotO = entity.getYRot();
        Quaternionf pose = Axis.ZP.rotationDegrees(180f).mul(Axis.YP.rotationDegrees(yaw));
        Quaternionf camera = Axis.XP.rotationDegrees(-8f);
        pose.mul(camera);
        InventoryScreen.renderEntityInInventory(graphics, x, y, scale, pose, camera, entity);
        entity.yBodyRot = bodyRot;
        entity.setYRot(yRot);
        entity.setXRot(xRot);
        entity.yHeadRotO = headRotO;
        entity.yHeadRot = headRot;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
