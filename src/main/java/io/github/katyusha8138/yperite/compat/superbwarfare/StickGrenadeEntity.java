package io.github.katyusha8138.yperite.compat.superbwarfare;

import com.atsuishio.superbwarfare.entity.projectile.HandGrenadeEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A thrown stick grenade: Superb Warfare's hand grenade (its flight, bounce, fuse and blast are inherited), drawn
 * as the stick grenade. On the client it keeps the angle it is drawn at: it turns end over end while it flies, and
 * settles lying on its side when it comes to rest.
 */
public class StickGrenadeEntity extends HandGrenadeEntity {
    /** Turn per tick, in degrees, per block per tick of speed. */
    private static final float SPIN_PER_SPEED = 32f;

    private float spin;
    private float spinO;
    private float heading;
    private float headingO;

    public StickGrenadeEntity(EntityType<? extends HandGrenadeEntity> type, Level level) {
        super(type, level);
    }

    public StickGrenadeEntity(EntityType<? extends HandGrenadeEntity> type, double x, double y, double z, Level level) {
        super(type, x, y, z, level);
    }

    /** A grenade leaving the hand of `thrower`, as Superb Warfare places its own. */
    public static StickGrenadeEntity thrownBy(LivingEntity thrower, Level level) {
        StickGrenadeEntity grenade = new StickGrenadeEntity(SbwCompat.STICK_GRENADE_ENTITY.get(),
                thrower.getX(), thrower.getEyeY() - 0.1, thrower.getZ(), level);
        grenade.setOwner(thrower);
        grenade.heading = grenade.headingO = -thrower.getYRot();
        return grenade;
    }

    @Override
    protected Item getDefaultItem() {
        return SbwCompat.STICK_GRENADE.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            turn();
        }
    }

    private void turn() {
        spinO = spin;
        headingO = heading;
        Vec3 motion = getDeltaMovement();
        double flat = motion.horizontalDistance();
        double speed = motion.length();
        if (flat > 0.02) {
            heading = (float) (Mth.atan2(motion.x, motion.z) * Mth.RAD_TO_DEG);
            headingO = heading - Mth.wrapDegrees(heading - headingO);
        }
        if (speed > 0.06) {
            spin += (float) speed * SPIN_PER_SPEED;
        } else {
            // at rest it lies on its side: ease to the nearest angle at which the stick is level
            float target = Math.round((spin - 90f) / 180f) * 180f + 90f;
            spin += (target - spin) * 0.35f;
        }
    }

    /** The angle of the stick from upright, about the level axis across its flight. */
    public float spin(float partialTick) {
        return Mth.lerp(partialTick, spinO, spin);
    }

    /** The direction of its flight, as a yaw in degrees (0 is +Z). */
    public float heading(float partialTick) {
        return Mth.lerp(partialTick, headingO, heading);
    }
}
