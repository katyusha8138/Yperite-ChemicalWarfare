package io.github.katyusha8138.yperite.client;

import io.github.katyusha8138.yperite.YperiteMod;
import io.github.katyusha8138.yperite.gas.GasSight;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import com.mojang.blaze3d.shaders.FogShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Hazes the view inside a gas cloud: the fog closes in and takes on the colour of the gas, the more the thicker
 * the gas is at the player's eyes (the server sends it: GasSight). The change is eased in over a moment, so that
 * walking into and out of a cloud does not flicker. Under water or in lava the vanilla fog is left alone.
 */
@Mod.EventBusSubscriber(modid = YperiteMod.MODID, value = Dist.CLIENT)
public final class GasSightClient {
    private static int colour = 0xFFFFFF;
    private static float target;
    private static float current;

    private GasSightClient() {
    }

    public static void receive(int rgb, float density) {
        if (density > 0.001f) {
            colour = rgb;
        }
        target = Math.max(0f, density);
    }

    /** The density as the view shows it now (eased towards what the server sent). */
    public static float density() {
        return current;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (Minecraft.getInstance().level == null) {
            target = 0f;
            current = 0f;
            return;
        }
        current += (target - current) * 0.12f;
        if (Math.abs(target - current) < 0.0005f) {
            current = target;
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        target = 0f;
        current = 0f;
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (current <= 0.001f || event.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        float far = Math.min(event.getFarPlaneDistance(), GasSight.visibility(current, event.getFarPlaneDistance()));
        if (far >= event.getFarPlaneDistance() - 0.01f) {
            return;
        }
        // the fog starts a little way off, so that what is right in front of one stays readable
        event.setNearPlaneDistance(Math.min(event.getNearPlaneDistance(), far * (0.15f + 0.35f * (1f - GasSight.tint(current)))));
        event.setFarPlaneDistance(far);
        event.setFogShape(FogShape.SPHERE);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFogColour(ViewportEvent.ComputeFogColor event) {
        if (current <= 0.001f || event.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        float tint = GasSight.tint(current) * 0.78f;
        // the gas is lit like everything else: darker at night and underground
        float luminance = 0.3f * event.getRed() + 0.59f * event.getGreen() + 0.11f * event.getBlue();
        float light = Mth.clamp(luminance * 1.4f, 0.12f, 1f);
        float r = ((colour >> 16) & 0xFF) / 255f * light;
        float g = ((colour >> 8) & 0xFF) / 255f * light;
        float b = (colour & 0xFF) / 255f * light;
        event.setRed(Mth.lerp(tint, event.getRed(), r));
        event.setGreen(Mth.lerp(tint, event.getGreen(), g));
        event.setBlue(Mth.lerp(tint, event.getBlue(), b));
    }
}
