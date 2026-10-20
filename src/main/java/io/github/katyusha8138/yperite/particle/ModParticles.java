package io.github.katyusha8138.yperite.particle;

import com.mojang.serialization.Codec;
import io.github.katyusha8138.yperite.YperiteMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** The gas's own particles: rolling puffs of cloud, and a low mist over ground where gas has settled. */
public final class ModParticles {
    private static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, YperiteMod.MODID);

    public static final RegistryObject<ParticleType<GasParticleOptions>> GAS_CLOUD = PARTICLES.register("gas_cloud", ModParticles::type);
    public static final RegistryObject<ParticleType<GasParticleOptions>> GAS_MIST = PARTICLES.register("gas_mist", ModParticles::type);

    private ModParticles() {
    }

    @SuppressWarnings("deprecation")
    private static ParticleType<GasParticleOptions> type() {
        // drawn beyond vanilla's 32 blocks: gas is seen from afar (the client thins them by the particle setting)
        return new ParticleType<>(true, GasParticleOptions.DESERIALIZER) {
            @Override
            public Codec<GasParticleOptions> codec() {
                return GasParticleOptions.codec(this);
            }
        };
    }

    public static void register(IEventBus modBus) {
        PARTICLES.register(modBus);
    }
}
