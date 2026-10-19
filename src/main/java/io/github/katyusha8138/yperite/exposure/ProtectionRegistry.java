package io.github.katyusha8138.yperite.exposure;

import io.github.katyusha8138.yperite.gas.GasCategory;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Where protective equipment plugs in. Masks, suits and so on register a {@link Provider} that tells how much of
 * a gas category still gets through; the results of all providers are multiplied.
 */
public final class ProtectionRegistry {
    /**
     * Shares of the dose that still reach the body: 1 means no protection, 0 means fully protected.
     *
     * @param inhale share that gets through when breathing
     * @param skin   share that gets through the skin
     */
    public record Protection(float inhale, float skin) {
        public static final Protection NONE = new Protection(1f, 1f);

        public Protection {
            inhale = Math.max(0f, Math.min(1f, inhale));
            skin = Math.max(0f, Math.min(1f, skin));
        }
    }

    @FunctionalInterface
    public interface Provider {
        Protection protection(LivingEntity entity, GasCategory category);
    }

    private static final List<Provider> PROVIDERS = new CopyOnWriteArrayList<>();

    private ProtectionRegistry() {
    }

    public static void register(Provider provider) {
        PROVIDERS.add(provider);
    }

    public static void unregister(Provider provider) {
        PROVIDERS.remove(provider);
    }

    public static Protection of(LivingEntity entity, GasCategory category) {
        float inhale = 1f;
        float skin = 1f;
        for (Provider provider : PROVIDERS) {
            Protection p = provider.protection(entity, category);
            inhale *= p.inhale();
            skin *= p.skin();
        }
        return new Protection(inhale, skin);
    }
}
