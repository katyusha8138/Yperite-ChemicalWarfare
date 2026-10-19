package io.github.katyusha8138.yperite.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Locale;

/**
 * What one puff of gas looks like: its colour, how opaque it is at most, how wide it grows (in blocks) and how it
 * drifts (blocks per tick: the wind where it is, and rising or sinking with the gas). The server picks these from
 * the concentration field; the client draws the puff ({@code GasCloudParticle}).
 */
public record GasParticleOptions(ParticleType<GasParticleOptions> type, int color, float alpha, float size,
                                 float vx, float vy, float vz) implements ParticleOptions {

    public static Codec<GasParticleOptions> codec(ParticleType<GasParticleOptions> type) {
        return RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("color").forGetter(GasParticleOptions::color),
                Codec.FLOAT.fieldOf("alpha").forGetter(GasParticleOptions::alpha),
                Codec.FLOAT.fieldOf("size").forGetter(GasParticleOptions::size),
                Codec.FLOAT.fieldOf("vx").forGetter(GasParticleOptions::vx),
                Codec.FLOAT.fieldOf("vy").forGetter(GasParticleOptions::vy),
                Codec.FLOAT.fieldOf("vz").forGetter(GasParticleOptions::vz)
        ).apply(i, (color, alpha, size, vx, vy, vz) -> new GasParticleOptions(type, color, alpha, size, vx, vy, vz)));
    }

    @SuppressWarnings("deprecation") // the deserializer is still how 1.20.1 reads particles from commands and packets
    public static final Deserializer<GasParticleOptions> DESERIALIZER = new Deserializer<>() {
        @Override
        public GasParticleOptions fromCommand(ParticleType<GasParticleOptions> type, StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            int color = reader.readInt();
            reader.expect(' ');
            float alpha = reader.readFloat();
            reader.expect(' ');
            float size = reader.readFloat();
            reader.expect(' ');
            float vx = reader.readFloat();
            reader.expect(' ');
            float vy = reader.readFloat();
            reader.expect(' ');
            float vz = reader.readFloat();
            return new GasParticleOptions(type, color, alpha, size, vx, vy, vz);
        }

        @Override
        public GasParticleOptions fromNetwork(ParticleType<GasParticleOptions> type, FriendlyByteBuf buf) {
            return new GasParticleOptions(type, buf.readInt(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                    buf.readFloat(), buf.readFloat());
        }
    };

    @Override
    public ParticleType<?> getType() {
        return type;
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeInt(color);
        buf.writeFloat(alpha);
        buf.writeFloat(size);
        buf.writeFloat(vx);
        buf.writeFloat(vy);
        buf.writeFloat(vz);
    }

    @Override
    public String writeToString() {
        return String.format(Locale.ROOT, "%s %d %.3f %.3f %.4f %.4f %.4f", BuiltInRegistries.PARTICLE_TYPE.getKey(type),
                color, alpha, size, vx, vy, vz);
    }
}
