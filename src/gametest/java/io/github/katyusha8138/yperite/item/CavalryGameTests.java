package io.github.katyusha8138.yperite.item;

import com.mojang.authlib.GameProfile;
import io.github.katyusha8138.yperite.YperiteMod;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/**
 * The cavalry's things: the lance (its reach, its charge that strikes by itself only at speed, the spur to the
 * horse) and the cavalry horse armour (its protection and the faster horse, only while it is worn).
 */
@GameTestHolder(YperiteMod.MODID)
@PrefixGameTestTemplate(false)
public final class CavalryGameTests {
    private static final String ROOM = "empty_20x10x20";

    private CavalryGameTests() {
    }

    private static FakePlayer rider(GameTestHelper helper, String name, int x, int z) {
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(x, 2, z)));
        player.moveTo(at.x, at.y, at.z, 0f, 0f);
        return player;
    }

    /** A rider facing +Z at (x, z) with the pig `ahead` blocks in front, looking down at it; the lance couched. */
    private static FakePlayer couchedBefore(GameTestHelper helper, Pig pig, double ahead) {
        FakePlayer player = rider(helper, "lancer", 10, 4);
        pig.moveTo(player.getX(), player.getY(), player.getZ() + ahead, 0f, 0f);
        double drop = player.getEyeY() - pig.getY(0.5);
        player.setXRot((float) Math.toDegrees(Math.atan2(drop, ahead)));
        ItemStack lance = new ItemStack(ModItems.LANCE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, lance);
        lance.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        return player;
    }

    @GameTest(template = ROOM, timeoutTicks = 20, batch = "cavalry")
    public static void theLanceReachesFurtherAndDoesNotSweep(GameTestHelper helper) {
        ItemStack lance = new ItemStack(ModItems.LANCE.get());
        double reach = lance.getAttributeModifiers(EquipmentSlot.MAINHAND).get(ForgeMod.ENTITY_REACH.get()).stream()
                .mapToDouble(m -> m.getAmount()).sum();
        helper.assertTrue(reach == LanceItem.REACH_BONUS, "the lance should add " + LanceItem.REACH_BONUS + " blocks of reach, adds " + reach);
        helper.assertTrue(!lance.canPerformAction(net.minecraftforge.common.ToolActions.SWORD_SWEEP), "a lance thrusts, it does not sweep");
        helper.assertTrue(LanceItem.chargeDamage(0.6) > LanceItem.chargeDamage(0.3) && LanceItem.chargeDamage(0.3) > 6f,
                "a faster charge should strike harder");
        helper.assertTrue(LanceItem.chargeDamage(10.0) <= 26f, "even the fastest charge has a limit");
        helper.succeed();
    }

    @GameTest(template = ROOM, timeoutTicks = 20, batch = "cavalry")
    public static void aCouchedLanceAtTheGallopStrikesWhatIsAhead(GameTestHelper helper) {
        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(10, 2, 7));
        FakePlayer player = couchedBefore(helper, pig, 2.5);
        ItemStack lance = player.getMainHandItem();
        helper.assertTrue(player.isUsingItem(), "the lance should be couched");
        // half a block in one tick: a gallop
        player.setPos(player.getX(), player.getY(), player.getZ() + 0.5);
        lance.getItem().onUseTick(helper.getLevel(), player, lance, player.getUseItemRemainingTicks());
        helper.assertTrue(!pig.isAlive() || pig.getHealth() <= pig.getMaxHealth() - LanceItem.chargeDamage(0.5) + 0.01f,
                "the charge should have struck the pig: health " + pig.getHealth());
        helper.assertTrue(!player.isUsingItem(), "the strike should end the charge");
        helper.assertTrue(lance.getDamageValue() == 2, "the strike should wear the lance, worn " + lance.getDamageValue());
        helper.succeed();
    }

    @GameTest(template = ROOM, timeoutTicks = 20, batch = "cavalry")
    public static void aCouchedLanceAtAWalkStrikesNothing(GameTestHelper helper) {
        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(10, 2, 7));
        FakePlayer player = couchedBefore(helper, pig, 2.5);
        ItemStack lance = player.getMainHandItem();
        player.setPos(player.getX(), player.getY(), player.getZ() + 0.1);
        lance.getItem().onUseTick(helper.getLevel(), player, lance, player.getUseItemRemainingTicks());
        helper.assertTrue(pig.getHealth() == pig.getMaxHealth(), "walking into the pig should not strike it");
        helper.assertTrue(player.isUsingItem(), "the charge should go on");
        helper.succeed();
    }

    @GameTest(template = ROOM, timeoutTicks = 20, batch = "cavalry")
    public static void couchingTheLanceSpursTheHorse(GameTestHelper helper) {
        Horse horse = helper.spawn(EntityType.HORSE, new BlockPos(10, 2, 10));
        horse.setTamed(true);
        FakePlayer player = rider(helper, "uhlan", 10, 10);
        player.startRiding(horse, true);
        helper.assertTrue(player.getVehicle() == horse, "the rider should be on the horse");
        ItemStack lance = new ItemStack(ModItems.LANCE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, lance);
        lance.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        var spur = horse.getEffect(MobEffects.MOVEMENT_SPEED);
        helper.assertTrue(spur != null && spur.getAmplifier() == 1, "the horse should break into a gallop");
        helper.assertTrue(!player.hasEffect(MobEffects.MOVEMENT_SPEED), "the rider is carried, not spurred");
        helper.assertTrue(player.getCooldowns().isOnCooldown(lance.getItem()), "a charge cannot be started again at once");
        player.stopRiding();
        helper.succeed();
    }

    @GameTest(template = ROOM, timeoutTicks = 40, batch = "cavalry")
    public static void cavalryHorseArmourProtectsAndSpeedsTheHorseWhileWorn(GameTestHelper helper) {
        Horse horse = helper.spawn(EntityType.HORSE, new BlockPos(10, 2, 10));
        horse.setTamed(true);
        double base = horse.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
        horse.getSlot(401).set(new ItemStack(ModItems.CAVALRY_HORSE_ARMOR.get()));
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(horse.getAttributeValue(Attributes.ARMOR) == CavalryHorseArmorItem.PROTECTION,
                    "the armour should protect like " + CavalryHorseArmorItem.PROTECTION + ", protects " + horse.getAttributeValue(Attributes.ARMOR));
            double speed = horse.getAttributeValue(Attributes.MOVEMENT_SPEED);
            helper.assertTrue(Math.abs(speed - base * (1 + CavalryHorseArmorItem.SPEED_BONUS)) < 1e-6,
                    "the horse should be faster: " + base + " -> " + speed);
            horse.getSlot(401).set(ItemStack.EMPTY);
            helper.runAfterDelay(3, () -> {
                helper.assertTrue(Math.abs(horse.getAttributeValue(Attributes.MOVEMENT_SPEED) - base) < 1e-6,
                        "taking the armour off should give the horse its own speed back");
                helper.succeed();
            });
        });
    }
}
