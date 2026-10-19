package io.github.katyusha8138.yperite.compat.superbwarfare;

import com.atsuishio.superbwarfare.api.event.ProjectileHitEvent;
import com.atsuishio.superbwarfare.data.CustomData;
import com.atsuishio.superbwarfare.data.gun.AmmoConsumer;
import com.atsuishio.superbwarfare.data.gun.DefaultGunData;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.ProjectileInfo;
import com.atsuishio.superbwarfare.data.launchable.LaunchableEntityTool;
import com.atsuishio.superbwarfare.data.launchable.ShootData;
import com.atsuishio.superbwarfare.data.vehicle.DefaultVehicleData;
import com.atsuishio.superbwarfare.entity.projectile.CannonShellEntity;
import com.atsuishio.superbwarfare.entity.projectile.GunGrenadeEntity;
import com.atsuishio.superbwarfare.entity.projectile.MortarShellEntity;
import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.item.projectile.MortarShellItem;
import com.mojang.authlib.GameProfile;
import io.github.katyusha8138.yperite.YperiteMod;
import io.github.katyusha8138.yperite.gas.GasManager;
import io.github.katyusha8138.yperite.gas.GasType;
import io.github.katyusha8138.yperite.gas.GasTypes;
import io.github.katyusha8138.yperite.gas.sim.GasLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static io.github.katyusha8138.yperite.gametest.TestRooms.pos;
import static io.github.katyusha8138.yperite.gametest.TestRooms.sealRoom;

/**
 * The integration with the real Superb Warfare, which only runs with {@code -PwithSbw}: the chemical rounds exist,
 * the weapon data was extended while it loaded, the data reaches the projectile, and a marked projectile releases
 * its gas when it hits.
 *
 * <p>Gas types are split between tests so that a total can be read without other tests adding to it.
 */
@GameTestHolder(YperiteMod.MODID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("removal")
public final class SbwGameTests {
    private static final String ROOM = "empty_20x10x20";

    /** Every mortar shell that joined the world, as seen after this mod had looked at it. */
    private static final List<ShellRecord> SHELLS = new CopyOnWriteArrayList<>();

    private record ShellRecord(Vec3 at, boolean marked, String gas) {
    }

    static {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, (EntityJoinLevelEvent event) -> {
            if (event.getEntity() instanceof MortarShellEntity shell) {
                CompoundTag data = shell.getPersistentData();
                SHELLS.add(new ShellRecord(shell.position(), data.contains(ChemicalRound.TAG_GAS), data.getString(ChemicalRound.TAG_GAS)));
            }
        });
    }

    private SbwGameTests() {
    }

    @BeforeBatch(batch = "sbw")
    public static void resetBeforeBatch(ServerLevel level) {
        GasManager.clear(level, null);
        GasManager.data(level).setWindOverride(null);
        SHELLS.clear();
    }

    // ------------------------------------------------------------------ helpers

    private static double totalMass(GameTestHelper helper, GasType type) {
        GasLayer layer = GasManager.data(helper.getLevel()).layerIfPresent(type);
        return layer == null ? 0.0 : layer.totalMass();
    }

    private static BlockHitResult hitOnTheFloor(GameTestHelper helper, int x, int z) {
        BlockPos floor = helper.absolutePos(pos(x, 0, z));
        return new BlockHitResult(Vec3.atCenterOf(floor).add(0.0, 0.5, 0.0), Direction.UP, floor, false);
    }

    private static List<AmmoConsumer> chemicalAmmo(List<AmmoConsumer> consumers, String prefix) {
        return consumers.stream().filter(c -> c.getAmmo() != null && c.getAmmo().startsWith(prefix)).toList();
    }

    private static void expect(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }

    private static AmmoConsumer chemicalConsumer(List<AmmoConsumer> consumers, String ammoId) {
        return consumers.stream().filter(c -> ammoId.equals(c.getAmmo())).findFirst()
                .orElseThrow(() -> new GameTestAssertException("no ammunition entry for " + ammoId));
    }

    private static Item chemicalItem(ChemicalRound round, GasType gas) {
        return ForgeRegistries.ITEMS.getValue(new ResourceLocation(YperiteMod.MODID, round.itemName(gas.id())));
    }

    // ------------------------------------------------------------------ items and data

    @GameTest(template = ROOM, timeoutTicks = 20, batch = "sbw")
    public static void everyChemicalRoundIsRegistered(GameTestHelper helper) {
        int count = 0;
        for (ChemicalRound round : ChemicalRound.values()) {
            for (var gas : GasTypes.BUILT_IN) {
                Item item = chemicalItem(round, gas.get());
                expect(item != Items.AIR, "missing item " + round.itemName(gas.getId()));
                expect((round == ChemicalRound.MORTAR_SHELL) == (item instanceof MortarShellItem),
                        "only mortar shells should be Superb Warfare mortar shells: " + round.itemName(gas.getId()));
                count++;
            }
        }
        expect(count == 18 && ChemicalRound.ITEMS.size() == 18, "expected 18 chemical rounds, found " + count + " and " + ChemicalRound.ITEMS.size());
        helper.succeed();
    }

    @GameTest(template = ROOM, timeoutTicks = 20, batch = "sbw")
    public static void launchersTakeChemicalGrenades(GameTestHelper helper) {
        for (String id : List.of("superbwarfare:m_79", "superbwarfare:secondary_cataclysm")) {
            DefaultGunData gun = CustomData.GUN_DATA.get(id);
            expect(gun != null, "Superb Warfare has no gun " + id);
            List<AmmoConsumer> all = gun.getProcessedAmmoConsumers();
            List<AmmoConsumer> chemical = chemicalAmmo(all, "yperite:grenade_40mm_");
            expect(chemical.size() == 6, id + " should take 6 chemical grenades, takes " + chemical.size());
            expect(all.size() > chemical.size(), id + " should keep its own ammunition as well: " + all.size() + " entries");
            for (AmmoConsumer consumer : chemical) {
                expect(consumer.getType() == AmmoConsumer.AmmoConsumeType.ITEM, id + ": " + consumer.getAmmo() + " is not a valid item ammunition");
            }
        }
        helper.succeed();
    }

    @GameTest(template = ROOM, timeoutTicks = 20, batch = "sbw")
    public static void artilleryTakesChemicalShellsAndTanksDoNot(GameTestHelper helper) {
        for (String id : List.of("superbwarfare:mk_42", "superbwarfare:mle_1934", "superbwarfare:fh_77bw", "superbwarfare:plz_05")) {
            DefaultVehicleData vehicle = CustomData.VEHICLE_DATA.get(id);
            expect(vehicle != null, "Superb Warfare has no vehicle " + id);
            List<AmmoConsumer> chemical = chemicalAmmo(vehicle.weapons().get("Main").getProcessedAmmoConsumers(), "yperite:large_shell_");
            expect(chemical.size() == 6, id + " should take 6 chemical shells, takes " + chemical.size());
            for (AmmoConsumer consumer : chemical) {
                expect(consumer.getType() == AmmoConsumer.AmmoConsumeType.ITEM, id + ": " + consumer.getAmmo() + " is not a valid item ammunition");
            }
        }
        DefaultVehicleData tank = CustomData.VEHICLE_DATA.get("superbwarfare:t_90a");
        expect(tank != null, "Superb Warfare has no vehicle t_90a");
        expect(!tank.weapons().isEmpty(), "the tank has no weapons");
        tank.weapons().forEach((name, weapon) ->
                expect(chemicalAmmo(weapon.getProcessedAmmoConsumers(), "yperite:").isEmpty(), "the tank weapon " + name + " must not get chemical shells"));
        helper.succeed();
    }

    // ------------------------------------------------------------------ the data reaches the projectile, and the hit releases gas

    private static CompoundTag shotData(List<AmmoConsumer> consumers, String ammoId) {
        ProjectileInfo info = chemicalConsumer(consumers, ammoId).getProjectile().value;
        CompoundTag tag = LaunchableEntityTool.getModifiedTag(info, new ShootData(UUID.randomUUID(), 20.0, 25.0, 2.0, 0.0));
        expect(tag != null, "the chemical entry has no projectile data");
        return tag;
    }

    @GameTest(template = ROOM, timeoutTicks = 60, batch = "sbw")
    public static void grenadeFromTheLauncherDataReleasesGasOnceWhenItHits(GameTestHelper helper) {
        sealRoom(helper);
        GasType chlorine = GasTypes.CHLORINE.get();
        List<AmmoConsumer> consumers = CustomData.GUN_DATA.get("superbwarfare:m_79").getProcessedAmmoConsumers();
        GunGrenadeEntity grenade = new GunGrenadeEntity(ModEntities.GUN_GRENADE.get(), helper.getLevel());
        grenade.load(shotData(consumers, "yperite:grenade_40mm_chlorine"));
        grenade.setPos(Vec3.atBottomCenterOf(helper.absolutePos(pos(10, 1, 10))));

        expect(ChemicalAmmo.isArmed(grenade) && "yperite:chlorine".equals(grenade.getPersistentData().getString(ChemicalRound.TAG_GAS)),
                "the projectile did not get its mark from the data");
        expect(!grenade.hasExplosionDestroy(), "the round must not break blocks");
        expect(totalMass(helper, chlorine) == 0.0, "no gas before the hit");

        grenade.onHit(hitOnTheFloor(helper, 10, 10));
        double released = totalMass(helper, chlorine);
        expect(released > 0.5 * ChemicalRound.GRENADE_40MM.gasMass(), "the hit released too little gas: " + released);
        grenade.onHit(hitOnTheFloor(helper, 10, 10));
        expect(totalMass(helper, chlorine) <= released + 0.001, "a second hit must not release gas again");
        helper.succeed();
    }

    /** The whole way: a loaded M79 set to the chemical ammunition is fired and the grenade releases gas where it lands. */
    @GameTest(template = ROOM, timeoutTicks = 200, batch = "sbw")
    public static void firedM79ReleasesGasWhereTheChemicalGrenadeLands(GameTestHelper helper) {
        sealRoom(helper);
        GasType blood = GasTypes.BLOOD_GAS.get();
        List<AmmoConsumer> consumers = CustomData.GUN_DATA.get("superbwarfare:m_79").getProcessedAmmoConsumers();
        int index = consumers.indexOf(chemicalConsumer(consumers, "yperite:grenade_40mm_blood_gas"));

        ItemStack stack = new ItemStack(ModItems.M_79.get());
        GunData data = GunData.from(stack);
        data.selectedAmmoType.set(index);
        data.ammo.set(1);
        FakePlayer shooter = gunner(helper);
        shooter.setPos(Vec3.atBottomCenterOf(helper.absolutePos(pos(10, 1, 4))));
        shooter.setYRot(0f);
        shooter.setXRot(0f);
        shooter.setItemInHand(InteractionHand.MAIN_HAND, stack);

        expect(data.canShoot(shooter), "the loaded M79 should be ready to fire");
        data.shoot(shooter, 0.0, false, (UUID) null);
        helper.runAfterDelay(80, () -> {
            double mass = totalMass(helper, blood);
            expect(mass > 0.3 * ChemicalRound.GRENADE_40MM.gasMass(), "the grenade released too little gas: " + mass);
            helper.succeed();
        });
    }

    @GameTest(template = ROOM, timeoutTicks = 60, batch = "sbw")
    public static void cannonShellFromTheArtilleryDataReleasesGas(GameTestHelper helper) {
        sealRoom(helper);
        GasType yperite = GasTypes.YPERITE.get();
        List<AmmoConsumer> consumers = CustomData.VEHICLE_DATA.get("superbwarfare:mk_42").weapons().get("Main").getProcessedAmmoConsumers();
        CannonShellEntity shell = new CannonShellEntity(ModEntities.CANNON_SHELL.get(), helper.getLevel());
        shell.load(shotData(consumers, "yperite:large_shell_yperite"));
        shell.setPos(Vec3.atBottomCenterOf(helper.absolutePos(pos(10, 1, 10))));
        expect(ChemicalAmmo.isArmed(shell), "the shell did not get its mark from the data");

        shell.onHit(hitOnTheFloor(helper, 10, 10));
        double released = totalMass(helper, yperite);
        expect(released > 0.5 * ChemicalRound.LARGE_SHELL.gasMass(), "the hit released too little gas: " + released);
        helper.succeed();
    }

    @GameTest(template = ROOM, timeoutTicks = 60, batch = "sbw")
    public static void projectileThatHitsAnEntityReleasesGasToo(GameTestHelper helper) {
        sealRoom(helper);
        GasType nerve = GasTypes.NERVE_GAS.get();
        Pig target = helper.spawn(EntityType.PIG, pos(10, 1, 10));
        GunGrenadeEntity grenade = new GunGrenadeEntity(ModEntities.GUN_GRENADE.get(), helper.getLevel());
        ChemicalAmmo.mark(grenade, ChemicalRound.GRENADE_40MM, nerve.id());
        boolean cancelled = MinecraftForge.EVENT_BUS.post(new ProjectileHitEvent.HitEntity(null, grenade, target, target.position()));
        expect(!cancelled, "the listener must not cancel the hit: the round should still do its damage");
        expect(totalMass(helper, nerve) > 0.5 * ChemicalRound.GRENADE_40MM.gasMass(), "no gas after hitting an entity");
        helper.succeed();
    }

    @GameTest(template = ROOM, timeoutTicks = 60, batch = "sbw")
    public static void aPlainProjectileReleasesNothing(GameTestHelper helper) {
        sealRoom(helper);
        GunGrenadeEntity grenade = new GunGrenadeEntity(ModEntities.GUN_GRENADE.get(), helper.getLevel());
        grenade.setPos(Vec3.atBottomCenterOf(helper.absolutePos(pos(10, 1, 10))));
        grenade.onHit(hitOnTheFloor(helper, 10, 10));
        for (var gas : GasTypes.BUILT_IN) {
            // Other tests release into the same world, but never blue cross gas.
            if (gas.get() == GasTypes.BLUE_CROSS.get()) {
                expect(totalMass(helper, gas.get()) == 0.0, "an unmarked projectile released gas");
            }
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------ the mortar

    private static FakePlayer gunner(GameTestHelper helper) {
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "gunner"));
        player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(pos(3, 1, 3))));
        return player;
    }

    /** The shells that joined the world next to this test's mortar (tests of one batch run side by side). */
    private static List<ShellRecord> shellsOfThisTest(GameTestHelper helper) {
        Vec3 mortar = Vec3.atBottomCenterOf(helper.absolutePos(pos(10, 1, 10)));
        return SHELLS.stream().filter(shell -> shell.at().distanceTo(mortar) < 4.0).toList();
    }

    private static MortarEntity loadedMortar(GameTestHelper helper, Item shell) {
        MortarEntity mortar = new MortarEntity(ModEntities.MORTAR.get(), helper.getLevel());
        mortar.setPos(Vec3.atBottomCenterOf(helper.absolutePos(pos(10, 1, 10))));
        helper.getLevel().addFreshEntity(mortar);
        mortar.getItems().set(0, new ItemStack(shell));
        mortar.vehicleShoot(gunner(helper), "Main", null);
        return mortar;
    }

    @GameTest(template = ROOM, timeoutTicks = 200, batch = "sbw")
    public static void chemicalMortarShellIsMarkedWhenFiredAndReleasesGasOnImpact(GameTestHelper helper) {
        sealRoom(helper);
        GasType tear = GasTypes.TEAR_GAS.get();
        loadedMortar(helper, chemicalItem(ChemicalRound.MORTAR_SHELL, tear));
        helper.runAfterDelay(120, () -> {
            List<ShellRecord> fired = shellsOfThisTest(helper);
            expect(fired.size() == 1, "the mortar should have fired one shell, fired " + fired.size());
            ShellRecord shell = fired.get(0);
            expect(shell.marked() && "yperite:tear_gas".equals(shell.gas()), "the shell was not marked as carrying tear gas: " + shell);
            double mass = totalMass(helper, tear);
            expect(mass > 0.3 * ChemicalRound.MORTAR_SHELL.gasMass(), "the shell released too little gas: " + mass);
            helper.succeed();
        });
    }

    @GameTest(template = ROOM, timeoutTicks = 200, batch = "sbw")
    public static void anOrdinaryMortarShellIsLeftAlone(GameTestHelper helper) {
        sealRoom(helper);
        loadedMortar(helper, ModItems.MORTAR_SHELL.get());
        helper.runAfterDelay(120, () -> {
            List<ShellRecord> fired = shellsOfThisTest(helper);
            expect(fired.size() == 1, "the mortar should have fired one shell, fired " + fired.size());
            expect(!fired.get(0).marked(), "an ordinary shell must not be marked");
            helper.succeed();
        });
    }
}
