package io.github.katyusha8138.yperite.exposure;

import com.mojang.authlib.GameProfile;
import io.github.katyusha8138.yperite.YperiteMod;
import io.github.katyusha8138.yperite.exposure.model.ExposureModel;
import io.github.katyusha8138.yperite.exposure.model.ExposureState;
import io.github.katyusha8138.yperite.gas.GasCategory;
import io.github.katyusha8138.yperite.gas.GasManager;
import io.github.katyusha8138.yperite.gas.GasSavedData;
import io.github.katyusha8138.yperite.gas.GasTypes;
import io.github.katyusha8138.yperite.gametest.TestRooms;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

import static io.github.katyusha8138.yperite.gametest.TestRooms.pos;
import static io.github.katyusha8138.yperite.gametest.TestRooms.sealRoom;

/**
 * In-world checks for exposure and symptoms. Players are Forge fake players: they are real server players for the
 * dose model, effects and attributes, but vanilla makes them invulnerable, so damage is checked on mobs.
 */
@GameTestHolder(YperiteMod.MODID)
@PrefixGameTestTemplate(false)
public final class ExposureGameTests {
    private static final String ROOM = "empty_20x10x20";
    private static final float DT = 0.25f;

    private ExposureGameTests() {
    }

    @BeforeBatch(batch = "exposure")
    public static void resetBeforeBatch(ServerLevel level) {
        GasManager.clear(level, null);
        GasManager.data(level).setWindOverride(null);
    }

    // ------------------------------------------------------------------ helpers

    private static FakePlayer fakePlayer(ServerLevel level, String name) {
        return new FakePlayer(level, new GameProfile(UUID.randomUUID(), name));
    }

    private static FakePlayer standAt(GameTestHelper helper, String name, int x, int y, int z) {
        FakePlayer player = fakePlayer(helper.getLevel(), name);
        Vec3 where = Vec3.atBottomCenterOf(helper.absolutePos(pos(x, y, z)));
        player.setPos(where);
        player.setOnGround(true);
        return player;
    }

    private static void evaluate(GameTestHelper helper, FakePlayer player, int steps) {
        ServerLevel level = helper.getLevel();
        GasSavedData data = GasManager.data(level);
        for (int i = 0; i < steps; i++) {
            ExposureManager.evaluatePlayer(level, player, data, DT);
        }
    }

    private static ExposureState state(FakePlayer player) {
        ExposureState state = ExposureManager.stateOf(player);
        if (state == null) {
            throw new IllegalStateException("the player has no exposure data attached");
        }
        return state;
    }

    // ------------------------------------------------------------------ players

    @GameTest(template = ROOM, timeoutTicks = 200, batch = "exposure")
    public static void denseChokingGasDevelopsSymptomsInStages(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        sealRoom(helper);
        GasManager.release(level, GasTypes.CHLORINE.get(), helper.absolutePos(pos(10, 2, 10)), 3f, 2000f);
        FakePlayer player = standAt(helper, "gas_stage_test", 10, 1, 10);
        int choking = GasCategory.CHOKING.ordinal();

        evaluate(helper, player, 4); // one second
        helper.assertTrue(state(player).latent[choking] > 0.5f, "breathing the gas must add dose, latent=" + state(player).latent[choking]);
        helper.assertTrue(ExposureModel.stage(state(player).active[choking]) < 2, "symptoms must not be severe after one second");

        evaluate(helper, player, 60); // 16 seconds in all
        int stage = ExposureModel.stage(state(player).active[choking]);
        helper.assertTrue(stage >= 3, "a long stay in dense gas must reach a severe stage, stage=" + stage
                + " active=" + state(player).active[choking]);
        helper.assertTrue(player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "severe choking should slow the player");
        helper.assertTrue(player.hasEffect(MobEffects.CONFUSION), "severe choking should cause nausea");
        helper.assertTrue(player.getAirSupply() < player.getMaxAirSupply(), "choking should take the player's breath");
        helper.succeed();
    }

    @GameTest(template = ROOM, timeoutTicks = 200, batch = "exposure")
    public static void symptomsFadeOnceThePlayerLeavesTheGas(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        sealRoom(helper);
        FakePlayer player = standAt(helper, "gas_fade_test", 10, 1, 10);
        int incapacitant = GasCategory.INCAPACITANT.ordinal();
        ExposureManager.addLatent(player, GasCategory.INCAPACITANT, 5f);

        evaluate(helper, player, 20); // 5 s: takes effect
        helper.assertTrue(state(player).active[incapacitant] > 2f, "dose should have taken effect, active=" + state(player).active[incapacitant]);

        evaluate(helper, player, 400); // 100 s with no gas around
        helper.assertTrue(state(player).active[incapacitant] < 0.5f, "incapacitating symptoms should have faded, active=" + state(player).active[incapacitant]);
        helper.succeed();
    }

    @GameTest(template = ROOM, timeoutTicks = 200, batch = "exposure")
    public static void protectionRegistryScalesTheDose(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        sealRoom(helper);
        GasManager.release(level, GasTypes.CHLORINE.get(), helper.absolutePos(pos(10, 2, 10)), 3f, 2000f);
        FakePlayer bare = standAt(helper, "gas_bare", 10, 1, 10);
        FakePlayer masked = standAt(helper, "gas_masked", 10, 1, 10);
        ProtectionRegistry.Provider mask = (entity, category) -> entity == masked
                ? new ProtectionRegistry.Protection(0.1f, 1f) : ProtectionRegistry.Protection.NONE;
        ProtectionRegistry.register(mask);
        try {
            evaluate(helper, bare, 20);
            evaluate(helper, masked, 20);
        } finally {
            ProtectionRegistry.unregister(mask);
        }
        int choking = GasCategory.CHOKING.ordinal();
        float bareDose = state(bare).latent[choking] + state(bare).active[choking];
        float maskedDose = state(masked).latent[choking] + state(masked).active[choking];
        helper.assertTrue(bareDose > 5f, "the unprotected player should have taken a lot, dose=" + bareDose);
        helper.assertTrue(maskedDose < bareDose * 0.15f, "the protected player should take about a tenth, bare=" + bareDose + " masked=" + maskedDose);
        helper.succeed();
    }

    @GameTest(template = ROOM, timeoutTicks = 200, batch = "exposure")
    public static void standingOnResidueContaminatesTheSkin(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        sealRoom(helper);
        FakePlayer player = standAt(helper, "gas_skin_test", 10, 1, 10);
        BlockPos feet = player.blockPosition();
        GasManager.data(level).layer(GasTypes.YPERITE.get()).addResidue(feet.getX(), feet.getY(), feet.getZ(), 10f);
        int blister = GasCategory.BLISTER.ordinal();

        evaluate(helper, player, 40); // 10 s
        ExposureState s = state(player);
        helper.assertTrue(s.skin[blister] + s.latent[blister] + s.active[blister] > 1f,
                "residue underfoot must contaminate the skin, skin=" + s.skin[blister] + " latent=" + s.latent[blister]);
        helper.assertTrue(ExposureModel.stage(s.active[blister]) == 0, "a blister agent must not show symptoms right away, active=" + s.active[blister]);
        helper.assertTrue(s.skin[blister] > 0.5f, "contamination should still be on the skin after ten seconds, skin=" + s.skin[blister]);

        // Someone who is not touching the ground is not touching the residue.
        FakePlayer floating = standAt(helper, "gas_floating_test", 10, 1, 10);
        floating.setOnGround(false);
        evaluate(helper, floating, 40);
        helper.assertTrue(state(floating).skin[blister] == 0f, "residue must not reach someone who is in the air");
        helper.succeed();
    }

    @GameTest(template = ROOM, timeoutTicks = 200, batch = "exposure")
    public static void breathingIsBlockedUnderWaterButTheSkinStillTakesGas(GameTestHelper helper) {
        // The player cannot be put under water in a test, so this checks the sampling rule directly: a
        // gas with only an inhalation value must add no dose to a player whose eyes are in water.
        ServerLevel level = helper.getLevel();
        sealRoom(helper);
        GasManager.release(level, GasTypes.CHLORINE.get(), helper.absolutePos(pos(10, 2, 10)), 3f, 2000f);
        FakePlayer player = standAt(helper, "gas_air_test", 10, 1, 10);
        float[] inhaled = new float[ExposureProfiles.count()];
        float[] contact = new float[ExposureProfiles.count()];
        ExposureManager.sample(player, GasManager.data(level), inhaled, contact, 1f);
        helper.assertTrue(inhaled[GasCategory.CHOKING.ordinal()] > 0.5f, "breathing in the gas must give dose, got " + inhaled[GasCategory.CHOKING.ordinal()]);
        helper.assertTrue(contact[GasCategory.CHOKING.ordinal()] == 0f, "chlorine has no skin route, got " + contact[GasCategory.CHOKING.ordinal()]);
        helper.succeed();
    }

    // ------------------------------------------------------------------ lasting damage, saving, death

    @GameTest(template = ROOM, timeoutTicks = 200, batch = "exposure")
    public static void lastingDamageChangesTheAttributesAndClearRemovesIt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer player = standAt(helper, "gas_aftermath_test", 10, 1, 10);
        double baseHealth = player.getMaxHealth();
        double baseSpeed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);

        ExposureState s = state(player);
        s.aftermath[GasCategory.BLISTER.ordinal()] = 1f;
        s.aftermath[GasCategory.NERVE.ordinal()] = 1f;
        evaluate(helper, player, 1);

        helper.assertTrue(player.getMaxHealth() < baseHealth - 5.5, "blister damage should lower the maximum health from " + baseHealth + " to " + player.getMaxHealth());
        helper.assertTrue(player.getAttributeValue(Attributes.MOVEMENT_SPEED) < baseSpeed * 0.9, "nerve damage should slow the player");

        ExposureManager.clear(player);
        helper.assertTrue(state(player).isClean(), "clear must remove the record");
        helper.assertTrue(Math.abs(player.getMaxHealth() - baseHealth) < 1e-6, "clear must restore the maximum health");
        helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED) - baseSpeed) < 1e-6, "clear must restore the speed");
        helper.succeed();
    }

    @GameTest(template = ROOM, timeoutTicks = 200, batch = "exposure")
    public static void theRecordSurvivesSavingAndALoadedPlayerKeepsIt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer original = standAt(helper, "gas_save_a", 10, 1, 10);
        ExposureState s = state(original);
        s.skin[GasCategory.BLISTER.ordinal()] = 1.5f;
        s.latent[GasCategory.NERVE.ordinal()] = 2.5f;
        s.active[GasCategory.CHOKING.ordinal()] = 3.5f;
        s.aftermath[GasCategory.BLOOD.ordinal()] = 0.5f;

        FakePlayer loaded = standAt(helper, "gas_save_b", 10, 1, 10);
        PlayerExposure.get(loaded).load(PlayerExposure.get(original).save());

        ExposureState t = state(loaded);
        helper.assertTrue(t.skin[GasCategory.BLISTER.ordinal()] == 1.5f && t.latent[GasCategory.NERVE.ordinal()] == 2.5f
                && t.active[GasCategory.CHOKING.ordinal()] == 3.5f && t.aftermath[GasCategory.BLOOD.ordinal()] == 0.5f,
                "the saved record must come back unchanged");
        helper.assertTrue(PlayerExposure.get(fakePlayer(level, "gas_save_c")).state().isClean(), "a new player starts clean");
        helper.succeed();
    }

    @GameTest(template = ROOM, timeoutTicks = 200, batch = "exposure")
    public static void dyingWipesTheRecordButChangingDimensionKeepsIt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer old = standAt(helper, "gas_clone_old", 10, 1, 10);
        ExposureState s = state(old);
        ExposureState oldRecord = s; // the clone event retires the old player's capabilities, as in a real respawn
        s.active[GasCategory.NERVE.ordinal()] = 7f;
        s.aftermath[GasCategory.NERVE.ordinal()] = 0.8f;

        FakePlayer afterTravel = fakePlayer(level, "gas_clone_travel");
        MinecraftForge.EVENT_BUS.post(new PlayerEvent.Clone(afterTravel, old, false));
        helper.assertTrue(state(afterTravel).active[GasCategory.NERVE.ordinal()] == 7f
                && state(afterTravel).aftermath[GasCategory.NERVE.ordinal()] == 0.8f, "changing dimension must keep the record");

        FakePlayer afterDeath = fakePlayer(level, "gas_clone_death");
        MinecraftForge.EVENT_BUS.post(new PlayerEvent.Clone(afterDeath, old, true));
        helper.assertTrue(state(afterDeath).isClean(), "dying must wipe the symptoms and the lasting damage");
        helper.assertTrue(!oldRecord.isClean(), "the old player's record is left alone");
        helper.succeed();
    }

    @GameTest(template = ROOM, timeoutTicks = 200, batch = "exposure")
    public static void exposureCommandsShowAddAndClear(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer player = standAt(helper, "gas_command_test", 10, 1, 10);
        CommandSourceStack source = level.getServer().createCommandSourceStack().withLevel(level).withEntity(player);
        var commands = level.getServer().getCommands();

        helper.assertTrue(commands.performPrefixedCommand(source, "yperite exposure") == 0, "a clean player shows no lines");
        helper.assertTrue(commands.performPrefixedCommand(source, "yperite exposure add choking 5") == 1, "add must succeed");
        helper.assertTrue(state(player).latent[GasCategory.CHOKING.ordinal()] == 5f, "add must put the dose into the latent pool");
        helper.assertTrue(commands.performPrefixedCommand(source, "yperite exposure") == 1, "info must list the one category");
        helper.assertTrue(commands.performPrefixedCommand(source, "yperite exposure add nonsense 5") == 0, "an unknown category must be refused");
        helper.assertTrue(commands.performPrefixedCommand(source, "yperite exposure clear") == 1, "clear must succeed");
        helper.assertTrue(state(player).isClean(), "clear must empty the record");
        helper.succeed();
    }

    // ------------------------------------------------------------------ mobs (real ticks)

    @GameTest(template = ROOM, timeoutTicks = 300, batch = "exposure")
    public static void gasHurtsAnimalsButNotTheUndead(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        sealRoom(helper);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, pos(10, 1, 10));
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, pos(11, 1, 10));
        GasManager.release(level, GasTypes.NERVE_GAS.get(), helper.absolutePos(pos(10, 2, 10)), 3f, 2000f);

        helper.runAfterDelay(160, () -> {
            helper.assertTrue(pig.isRemoved() || pig.getHealth() < pig.getMaxHealth(),
                    "the pig should have been hurt by nerve gas, health=" + pig.getHealth());
            helper.assertTrue(zombie.getHealth() == zombie.getMaxHealth(), "the zombie should be immune, health=" + zombie.getHealth());
            helper.assertTrue(zombie.getActiveEffects().stream().noneMatch(e -> e.getEffect() == MobEffects.MOVEMENT_SLOWDOWN),
                    "the zombie should get no symptoms");
            helper.succeed();
        });
    }
}
