package io.github.katyusha8138.yperite.entity;

import io.github.katyusha8138.yperite.config.YperiteConfig;
import io.github.katyusha8138.yperite.gas.GasManager;
import io.github.katyusha8138.yperite.gas.GasType;
import io.github.katyusha8138.yperite.item.GasGrenadeItem;
import io.github.katyusha8138.yperite.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A thrown gas grenade. It bounces, and a fuse that starts at the first impact lets it go off a few seconds later,
 * releasing its gas where it lies. Which gas it carries is decided by its item.
 */
public class GasGrenadeEntity extends ThrowableItemProjectile {
    private static final String TAG_FUSE = "Fuse";
    private static final String TAG_FLIGHT = "Flight";
    /** A grenade that has not hit anything after this many ticks (it was thrown into the sky) goes off anyway. */
    private static final int MAX_FLIGHT_TICKS = 200;

    /** Ticks left on the fuse, or -1 while it has not hit anything yet. */
    private int fuse = -1;
    private int flightTicks;

    public GasGrenadeEntity(EntityType<? extends GasGrenadeEntity> type, Level level) {
        super(type, level);
    }

    public GasGrenadeEntity(Level level, LivingEntity thrower) {
        super(ModEntities.GAS_GRENADE.get(), thrower, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.firstGrenade();
    }

    @Nullable
    public GasType gas() {
        return getItem().getItem() instanceof GasGrenadeItem grenade ? grenade.gas() : null;
    }

    public int fuseTicks() {
        return fuse;
    }

    public boolean isFuseLit() {
        return fuse >= 0;
    }

    private void lightFuse() {
        if (fuse < 0) {
            fuse = YperiteConfig.GRENADE_FUSE_TICKS.get();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel serverLevel) || isRemoved()) {
            return;
        }
        if (fuse >= 0) {
            fuse--;
            if (fuse % 4 == 0) {
                serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.2, getZ(), 2, 0.05, 0.05, 0.05, 0.01);
            }
            if (fuse % 10 == 0) {
                serverLevel.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 0.4f, 1.6f);
            }
            if (fuse <= 0) {
                detonate(serverLevel);
            }
        } else if (++flightTicks > MAX_FLIGHT_TICKS) {
            lightFuse();
        }
        if (!isRemoved() && getY() < serverLevel.getMinBuildHeight() - 16) {
            discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        bounce(hit.getDirection(), hit.getLocation());
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        setDeltaMovement(getDeltaMovement().scale(-0.2));
        lightFuse();
    }

    /** Reflects the velocity off the face that was hit and puts the grenade just outside the block. */
    private void bounce(Direction face, Vec3 at) {
        Vec3 m = getDeltaMovement();
        Vec3 bounced = switch (face.getAxis()) {
            case X -> new Vec3(-m.x * 0.4, m.y * 0.8, m.z * 0.8);
            case Y -> new Vec3(m.x * 0.7, -m.y * 0.3, m.z * 0.7);
            case Z -> new Vec3(m.x * 0.8, m.y * 0.8, -m.z * 0.4);
        };
        if (face == Direction.UP && Math.abs(bounced.y) < 0.05) {
            bounced = new Vec3(bounced.x * 0.5, 0.0, bounced.z * 0.5);
        }
        if (m.lengthSqr() > 0.04) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.COPPER_HIT, SoundSource.NEUTRAL, 0.6f, 1.2f);
        }
        setDeltaMovement(bounced);
        // The entity position is the bottom of its box, which is 0.25 tall.
        setPos(at.x + face.getStepX() * 0.13, at.y + face.getStepY() * 0.13 - 0.125, at.z + face.getStepZ() * 0.13);
        lightFuse();
    }

    private void detonate(ServerLevel serverLevel) {
        GasType type = gas();
        if (type != null) {
            BlockPos center = BlockPos.containing(getX(), getY() + 0.125, getZ());
            float mass = YperiteConfig.GRENADE_MASS.get().floatValue();
            float radius = YperiteConfig.GRENADE_RADIUS.get().floatValue();
            if (GasManager.release(serverLevel, type, center, radius, mass) <= 0f) {
                GasManager.release(serverLevel, type, center.above(), radius, mass);
            }
        }
        serverLevel.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 1.5f, 0.5f);
        serverLevel.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.3, getZ(), 24, 0.3, 0.2, 0.3, 0.06);
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(TAG_FUSE, fuse);
        tag.putInt(TAG_FLIGHT, flightTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        fuse = tag.contains(TAG_FUSE) ? tag.getInt(TAG_FUSE) : -1;
        flightTicks = tag.getInt(TAG_FLIGHT);
    }
}
