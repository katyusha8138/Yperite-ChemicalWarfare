package io.github.katyusha8138.yperite.compat.superbwarfare;

import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.HandGrenade;
import com.atsuishio.superbwarfare.tools.CustomExplosion;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.AbstractProjectileDispenseBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

/**
 * German stick grenade (Stielhandgranate). It is Superb Warfare's hand grenade in every way that matters: it is a
 * {@link HandGrenade}, and the thrown grenade is Superb Warfare's {@code HandGrenadeEntity}. Hold use to pull the
 * cord and cook it (the fuse runs 5 seconds from the pull), let go to throw it, harder the longer it was held, up to
 * 1.5. Holding it for the whole fuse sets it off in the hand. The blast is Superb Warfare's, with its damage and
 * radius (the M67's settings), and it bounces and rolls as Superb Warfare's grenade does. Only the look is its own:
 * a 3D model in the hand and in the inventory, and a grenade that turns end over end in flight.
 */
public class StickGrenadeItem extends HandGrenade implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        // not Superb Warfare's renderer (that one draws the M67): the model of the stick grenade
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = io.github.katyusha8138.yperite.compat.superbwarfare.client.StickGrenadeClient.itemRenderer();
                }
                return renderer;
            }
        });
    }

    /** As Superb Warfare's grenade, but the grenade thrown is the stick grenade. */
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (level.isClientSide || !(living instanceof Player player)) {
            return;
        }
        int usingTime = getUseDuration(stack) - timeLeft;
        if (usingTime <= 3) {
            return;
        }
        player.getCooldowns().addCooldown(this, 25);
        float power = Math.min(usingTime / 10.0f, 1.5f);
        StickGrenadeEntity grenade = StickGrenadeEntity.thrownBy(player, level);
        grenade.setLife(100 - usingTime);
        grenade.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, power, 0.0f);
        level.addFreshEntity(grenade);
        level.playSound(null, player.getOnPos(), ModSounds.GRENADE_THROW.get(), SoundSource.PLAYERS, 1f, 1f);
        if (!player.isCreative()) {
            stack.shrink(1);
        }
    }

    /** Held for the whole fuse: it goes off in the hand, as Superb Warfare's does. */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity living) {
        if (!level.isClientSide) {
            new CustomExplosion.Builder(StickGrenadeEntity.thrownBy(living, level))
                    .attacker(living)
                    .damage(ExplosionConfig.M67_GRENADE_EXPLOSION_DAMAGE.get().floatValue())
                    .radius(ExplosionConfig.M67_GRENADE_EXPLOSION_RADIUS.get().floatValue())
                    .explode();
            if (living instanceof Player player) {
                player.getCooldowns().addCooldown(this, 25);
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
            }
        }
        return stack;
    }

    /** A dispenser throws it, lit, as it throws Superb Warfare's grenade. */
    @Override
    public DispenseItemBehavior getLaunchBehavior() {
        return new AbstractProjectileDispenseBehavior() {
            @Override
            protected Projectile getProjectile(Level level, Position position, ItemStack stack) {
                return new StickGrenadeEntity(SbwCompat.STICK_GRENADE_ENTITY.get(), position.x(), position.y(), position.z(), level);
            }

            @Override
            protected void playSound(BlockSource source) {
                source.getLevel().playSound(null, source.getPos(), ModSounds.GRENADE_THROW.get(), SoundSource.BLOCKS, 1f, 1f);
            }
        };
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
