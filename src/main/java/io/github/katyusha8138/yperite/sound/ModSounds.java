package io.github.katyusha8138.yperite.sound;

import io.github.katyusha8138.yperite.YperiteMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** The mod's own sounds (made by tools/build_sounds.py; sounds.json lists their files). */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, YperiteMod.MODID);

    /**
     * How far an officer's whistle carries, in blocks. Sent to every player this close and heard at its natural
     * loudness nearby, fading evenly to nothing at this distance (sounds.json gives the same attenuation distance).
     */
    public static final float WHISTLE_RANGE = 80f;

    @SuppressWarnings("removal")
    public static final RegistryObject<SoundEvent> OFFICER_WHISTLE = SOUNDS.register("item.officer_whistle",
            () -> SoundEvent.createFixedRangeEvent(new ResourceLocation(YperiteMod.MODID, "item.officer_whistle"), WHISTLE_RANGE));

    private ModSounds() {
    }

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
    }
}
