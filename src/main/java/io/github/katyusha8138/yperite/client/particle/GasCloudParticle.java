package io.github.katyusha8138.yperite.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.katyusha8138.yperite.client.GasSightClient;
import io.github.katyusha8138.yperite.gas.GasSight;
import io.github.katyusha8138.yperite.particle.GasParticleOptions;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * One puff of gas. A cloud puff faces the camera, swells as it drifts with the wind and fades in and out; a mist puff lies flat just above the ground and spreads out where gas has settled. Puffs stop at walls.
 * Gas is not a light source: it takes the light of the place it is in.
 */
public class GasCloudParticle extends TextureSheetParticle {
    private final float maxAlpha;
    private final float size;
    private final boolean flat;

    protected GasCloudParticle(ClientLevel level, double x, double y, double z, GasParticleOptions options, SpriteSet sprites, boolean flat) {
        super(level, x, y, z);
        this.flat = flat;
        this.xd = options.vx();
        this.yd = options.vy();
        this.zd = options.vz();
        this.lifetime = flat ? 100 + random.nextInt(60) : 55 + random.nextInt(40);
        this.friction = 1.0f;
        this.gravity = 0f;
        this.hasPhysics = true;
        pickSprite(sprites);
        int rgb = options.color();
        float shade = 0.9f + random.nextFloat() * 0.18f;
        setColor(Math.min(1f, ((rgb >> 16) & 0xFF) / 255f * shade), Math.min(1f, ((rgb >> 8) & 0xFF) / 255f * shade),
                Math.min(1f, (rgb & 0xFF) / 255f * shade));
        this.maxAlpha = Mth.clamp(options.alpha(), 0f, 1f);
        this.alpha = 0f;
        this.size = options.size() / 2f;            // quadSize is half the width
        this.quadSize = size * 0.7f;
        // turned only by quarter turns: pixel art stays on the pixel grid, as vanilla particles do
        this.roll = random.nextInt(4) * Mth.HALF_PI;
        this.oRoll = roll;
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) {
            return;
        }
        // a little turbulence, so that puffs side by side do not move as one
        xd += (random.nextFloat() - 0.5f) * 0.002f;
        zd += (random.nextFloat() - 0.5f) * 0.002f;
        if (!flat) {
            yd += (random.nextFloat() - 0.5f) * 0.001f;
        }
        float t = age / (float) lifetime;
        float envelope = t < 0.2f ? t / 0.2f : (t > 0.6f ? (1f - t) / 0.4f : 1f);
        alpha = maxAlpha * Mth.clamp(envelope, 0f, 1f);
        quadSize = size * (0.7f + 0.45f * t);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        if (!flat) {
            // a puff right at the eyes would cover the screen: inside a cloud the haze (GasSightClient) shows it
            float full = alpha;
            double d = camera.getPosition().distanceTo(new Vec3(Mth.lerp(partialTicks, xo, x), Mth.lerp(partialTicks, yo, y),
                    Mth.lerp(partialTicks, zo, z)));
            alpha = full * (float) Mth.clamp((d - quadSize) / 2.0, 0.0, 1.0);
            // inside a cloud the haze already stands for the gas all round: the puffs thin out, or they would wall
            // in the view completely
            alpha *= 1f - 0.6f * GasSight.tint(GasSightClient.density());
            if (alpha > 0.003f) {
                super.render(buffer, camera, partialTicks);
            }
            alpha = full;
            return;
        }
        // a quad lying flat, turned about the vertical, seen from above and from below
        Vec3 eye = camera.getPosition();
        float px = (float) (Mth.lerp(partialTicks, xo, x) - eye.x());
        float py = (float) (Mth.lerp(partialTicks, yo, y) - eye.y());
        float pz = (float) (Mth.lerp(partialTicks, zo, z) - eye.z());
        float angle = Mth.lerp(partialTicks, oRoll, roll);
        float s = getQuadSize(partialTicks);
        float cos = Mth.cos(angle) * s;
        float sin = Mth.sin(angle) * s;
        Vector3f[] corners = {
                new Vector3f(px - cos + sin, py, pz - sin - cos), new Vector3f(px - cos - sin, py, pz - sin + cos),
                new Vector3f(px + cos - sin, py, pz + sin + cos), new Vector3f(px + cos + sin, py, pz + sin - cos)};
        float u0 = getU0(), u1 = getU1(), v0 = getV0(), v1 = getV1();
        int light = getLightColor(partialTicks);
        float[][] uv = {{u1, v1}, {u1, v0}, {u0, v0}, {u0, v1}};
        for (int k = 0; k < 4; k++) {
            vertex(buffer, corners[k], uv[k], light);
        }
        for (int k = 3; k >= 0; k--) {
            vertex(buffer, corners[k], uv[k], light);
        }
    }

    private void vertex(VertexConsumer buffer, Vector3f at, float[] uv, int light) {
        buffer.vertex(at.x(), at.y(), at.z()).uv(uv[0], uv[1]).color(rCol, gCol, bCol, alpha).uv2(light).endVertex();
    }

    /** Makes cloud or mist puffs from the options the server sent. */
    public record Provider(SpriteSet sprites, boolean flat) implements ParticleProvider<GasParticleOptions> {
        @Override
        public Particle createParticle(GasParticleOptions options, ClientLevel level, double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new GasCloudParticle(level, x, y, z, options, sprites, flat);
        }
    }
}
