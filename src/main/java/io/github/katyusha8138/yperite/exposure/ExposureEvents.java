package io.github.katyusha8138.yperite.exposure;

import io.github.katyusha8138.yperite.YperiteMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Attaches the exposure data to players and carries it across dimension changes, but not across death. */
@Mod.EventBusSubscriber(modid = YperiteMod.MODID)
public final class ExposureEvents {
    @SuppressWarnings("removal")
    private static final ResourceLocation KEY = new ResourceLocation(YperiteMod.MODID, "exposure");

    private ExposureEvents() {
    }

    @SubscribeEvent
    public static void onAttach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            PlayerExposure.Provider provider = new PlayerExposure.Provider();
            event.addCapability(KEY, provider);
            event.addListener(provider::invalidate);
        }
    }

    /**
     * Copies the data to the new player after a dimension change. After a death nothing is copied, so the
     * symptoms and the lasting damage are gone.
     */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            return;
        }
        Player original = event.getOriginal();
        original.reviveCaps();
        try {
            PlayerExposure from = PlayerExposure.get(original);
            PlayerExposure to = PlayerExposure.get(event.getEntity());
            if (from != null && to != null) {
                to.state().copyFrom(from.state());
            }
        } finally {
            original.invalidateCaps();
        }
    }
}
