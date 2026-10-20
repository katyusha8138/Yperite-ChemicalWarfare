package io.github.katyusha8138.yperite.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/**
 * A cavalry lance, for the charge. It works like Superb Warfare's lunge mine, with a lance's point instead of a
 * charge of explosive:
 * <ul>
 * <li>it reaches further than a sword ({@link #REACH_BONUS} more blocks);</li>
 * <li>hold use to couch it and charge: the horse under the rider (or the rider on foot) gets a burst of speed,
 * and the lance can be couched again only after {@link #COOLDOWN_TICKS};</li>
 * <li>while it is couched and its bearer moves fast enough, the first creature in front within its reach is struck
 * at once, harder the faster the charge ({@link #chargeDamage}); that ends the charge.</li>
 * </ul>
 * Otherwise it is a slow, long weapon to strike with. It does not sweep.
 */
public class LanceItem extends SwordItem implements GeoItem {
    /** Blocks of reach added to the hand's, as Superb Warfare's lunge mine adds. */
    public static final double REACH_BONUS = 1.5;
    /** How long a charge lasts at most, and the burst of speed with it, in ticks. */
    public static final int CHARGE_TICKS = 100;
    public static final int COOLDOWN_TICKS = 200;
    /** Below this speed (blocks per tick, about a sprint) a couched lance does not strike by itself. */
    public static final double MIN_CHARGE_SPEED = 0.25;
    private static final float BASE_DAMAGE = 6f;
    private static final float DAMAGE_PER_SPEED = 24f;
    private static final float MAX_DAMAGE = 26f;
    private static final UUID REACH_ID = UUID.fromString("5b0a0f0e-2c7e-4c35-9d0b-6b1e2f6a9c41");

    /** Where each charging bearer (or the horse under it) was at the last tick: its speed is measured from it. */
    private static final Map<LivingEntity, Vec3> LAST = new WeakHashMap<>();

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public LanceItem(Properties properties) {
        super(WarTiers.LANCE, 5, -3.0f, properties);
    }

    /** Damage of a charge at this speed (blocks per tick). */
    public static float chargeDamage(double speed) {
        return Math.min(MAX_DAMAGE, BASE_DAMAGE + DAMAGE_PER_SPEED * (float) speed);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> map = super.getAttributeModifiers(slot, stack);
        if (slot != EquipmentSlot.MAINHAND) {
            return map;
        }
        Multimap<Attribute, AttributeModifier> withReach = HashMultimap.create(map);
        withReach.put(ForgeMod.ENTITY_REACH.get(), new AttributeModifier(REACH_ID, "Lance reach", REACH_BONUS, AttributeModifier.Operation.ADDITION));
        return withReach;
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction action) {
        return action != ToolActions.SWORD_SWEEP && super.canPerformAction(stack, action);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(stack);
        }
        player.startUsingItem(hand);
        if (!level.isClientSide) {
            Entity vehicle = player.getVehicle();
            if (vehicle instanceof LivingEntity mount) {
                // the horse is spurred to the charge
                mount.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, CHARGE_TICKS, 1));
                level.playSound(null, mount, SoundEvents.HORSE_ANGRY, SoundSource.NEUTRAL, 1.2f, 1f);
            } else {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, CHARGE_TICKS, 0));
            }
            LAST.put(player, player.getRootVehicle().position());
        }
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity bearer, ItemStack stack, int remaining) {
        if (level.isClientSide) {
            return;
        }
        Entity mover = bearer.getRootVehicle();
        Vec3 now = mover.position();
        Vec3 before = LAST.put(bearer, now);
        if (before == null) {
            return;
        }
        Vec3 travel = now.subtract(before);
        double speed = travel.horizontalDistance();
        if (speed < MIN_CHARGE_SPEED) {
            return;
        }
        LivingEntity target = target(bearer, mover);
        if (target != null) {
            strike(level, bearer, stack, target, speed, travel);
        }
    }

    /** The first living thing in front of the bearer, within the lance's reach. */
    @Nullable
    public static LivingEntity target(LivingEntity bearer, Entity mover) {
        double reach = bearer instanceof Player player ? player.getEntityReach() : 3.0 + REACH_BONUS;
        Vec3 from = bearer.getEyePosition();
        Vec3 look = bearer.getLookAngle();
        Vec3 to = from.add(look.scale(reach));
        AABB around = bearer.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(bearer.level(), bearer, from, to, around,
                e -> e instanceof LivingEntity && e.isAlive() && e.isPickable() && e != mover && !e.isPassengerOfSameVehicle(bearer));
        return hit == null ? null : (LivingEntity) hit.getEntity();
    }

    /** The lance strikes home: damage by the speed of the charge, thrown back along it; the charge is over. */
    public static void strike(Level level, LivingEntity bearer, ItemStack stack, LivingEntity target, double speed, Vec3 travel) {
        DamageSource source = bearer instanceof Player player ? level.damageSources().playerAttack(player) : level.damageSources().mobAttack(bearer);
        float damage = chargeDamage(speed);
        if (target.hurt(source, damage)) {
            Vec3 push = travel.horizontalDistance() > 1e-6 ? travel.normalize() : bearer.getLookAngle();
            target.knockback(0.5 + speed * 2.0, -push.x, -push.z);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(), 12, 0.3, 0.3, 0.3, 0.2);
            }
        }
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.2f, 0.8f);
        stack.hurtAndBreak(2, bearer, b -> b.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        if (bearer instanceof Player player) {
            player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
            player.resetAttackStrengthTicker();
        }
        LAST.remove(bearer);
        bearer.stopUsingItem();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, java.util.List<net.minecraft.network.chat.Component> lines,
                                net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);
        lines.add(net.minecraft.network.chat.Component.translatable("tooltip.yperite.lance").withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity bearer, int remaining) {
        LAST.remove(bearer);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity bearer) {
        LAST.remove(bearer);
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return CHARGE_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(io.github.katyusha8138.yperite.client.item.LanceClient.extensions());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
