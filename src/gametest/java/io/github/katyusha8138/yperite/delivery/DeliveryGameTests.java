package io.github.katyusha8138.yperite.delivery;

import com.mojang.authlib.GameProfile;
import io.github.katyusha8138.yperite.YperiteMod;
import io.github.katyusha8138.yperite.block.GasCylinderBlock;
import io.github.katyusha8138.yperite.block.GasCylinderBlockEntity;
import io.github.katyusha8138.yperite.block.ModBlocks;
import io.github.katyusha8138.yperite.config.YperiteConfig;
import io.github.katyusha8138.yperite.entity.GasGrenadeEntity;
import io.github.katyusha8138.yperite.entity.ModEntities;
import io.github.katyusha8138.yperite.gas.GasManager;
import io.github.katyusha8138.yperite.gas.GasType;
import io.github.katyusha8138.yperite.gas.GasTypes;
import io.github.katyusha8138.yperite.gas.sim.GasLayer;
import io.github.katyusha8138.yperite.item.GasCylinderItem;
import io.github.katyusha8138.yperite.item.GasGrenadeItem;
import io.github.katyusha8138.yperite.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

import static io.github.katyusha8138.yperite.gametest.TestRooms.pos;
import static io.github.katyusha8138.yperite.gametest.TestRooms.sealRoom;

/**
 * In-world checks for the grenades and the gas cylinder.
 *
 * <p>Tests that share the batch use different gases, so that the total of one gas can be read without the others
 * adding to it: tear gas for the grenade fuse, chlorine for the saved grenade, nerve gas for the thrown one, blood
 * agent gas for the open cylinder, blue cross for the closed one and yperite for the walled-in one.
 */
@GameTestHolder(YperiteMod.MODID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("removal")
public final class DeliveryGameTests {
    private static final String ROOM = "empty_20x10x20";

    private DeliveryGameTests() {
    }

    @BeforeBatch(batch = "delivery")
    public static void resetBeforeBatch(ServerLevel level) {
        GasManager.clear(level, null);
        GasManager.data(level).setWindOverride(null);
    }

    // ------------------------------------------------------------------ helpers

    private static FakePlayer player(GameTestHelper helper, int x, int y, int z) {
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "thrower"));
        player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(pos(x, y, z))));
        player.setOnGround(true);
        return player;
    }

    private static double totalMass(GameTestHelper helper, GasType type) {
        GasLayer layer = GasManager.data(helper.getLevel()).layerIfPresent(type);
        return layer == null ? 0.0 : layer.totalMass();
    }

    private static GasGrenadeEntity spawnGrenade(GameTestHelper helper, String item, int x, int y, int z) {
        GasGrenadeEntity grenade = new GasGrenadeEntity(ModEntities.GAS_GRENADE.get(), helper.getLevel());
        grenade.setItem(new ItemStack(ModItems.GRENADES.get(item).get()));
        grenade.setPos(Vec3.atBottomCenterOf(helper.absolutePos(pos(x, y, z))));
        helper.getLevel().addFreshEntity(grenade);
        return grenade;
    }

    private static List<GasGrenadeEntity> grenadesNear(GameTestHelper helper, int x, int y, int z) {
        BlockPos at = helper.absolutePos(pos(x, y, z));
        return helper.getLevel().getEntitiesOfClass(GasGrenadeEntity.class, new AABB(at).inflate(12.0));
    }

    private static GasCylinderBlockEntity placeCylinder(GameTestHelper helper, int x, int y, int z, GasType type, float amount) {
        helper.setBlock(pos(x, y, z), ModBlocks.GAS_CYLINDER.get().defaultBlockState());
        GasCylinderBlockEntity cylinder = (GasCylinderBlockEntity) helper.getBlockEntity(pos(x, y, z));
        cylinder.setContents(type, amount);
        return cylinder;
    }

    private static boolean isOpen(GameTestHelper helper, int x, int y, int z) {
        return helper.getBlockState(pos(x, y, z)).getValue(GasCylinderBlock.OPEN);
    }

    private static InteractionResult useValve(GameTestHelper helper, FakePlayer player, int x, int y, int z, InteractionHand hand) {
        BlockPos at = helper.absolutePos(pos(x, y, z));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false);
        return helper.getLevel().getBlockState(at).use(helper.getLevel(), player, hand, hit);
    }

    // ------------------------------------------------------------------ grenades

    // The grenades wait for their landing instead of a fixed number of ticks: in the first batch after the server
    // starts, the test area may take a moment before its entities tick.

    @GameTest(template = ROOM, timeoutTicks = 400, batch = "delivery")
    public static void grenadeWaitsForItsFuseThenReleasesGas(GameTestHelper helper) {
        sealRoom(helper);
        GasGrenadeEntity grenade = spawnGrenade(helper, "tear_gas_grenade", 10, 3, 10);
        helper.startSequence()
                .thenWaitUntil(() -> {
                    if (!grenade.isFuseLit()) {
                        throw new GameTestAssertException("the fuse should be lit after the grenade has landed");
                    }
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    if (grenade.isRemoved() || totalMass(helper, GasTypes.TEAR_GAS.get()) > 0.0) {
                        throw new GameTestAssertException("the grenade went off before its fuse ran out");
                    }
                })
                .thenWaitUntil(() -> {
                    if (!grenade.isRemoved()) {
                        throw new GameTestAssertException("the grenade should be gone after it went off");
                    }
                })
                .thenExecute(() -> {
                    double mass = totalMass(helper, GasTypes.TEAR_GAS.get());
                    if (mass < 0.5 * YperiteConfig.GRENADE_MASS.get()) {
                        throw new GameTestAssertException("the grenade released too little gas: " + mass);
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = ROOM, timeoutTicks = 400, batch = "delivery")
    public static void grenadeKeepsItsFuseThroughSaveAndLoad(GameTestHelper helper) {
        sealRoom(helper);
        GasGrenadeEntity grenade = spawnGrenade(helper, "chlorine_grenade", 10, 3, 10);
        helper.startSequence()
                .thenWaitUntil(() -> {
                    if (!grenade.isFuseLit()) {
                        throw new GameTestAssertException("the fuse should be lit after the grenade has landed");
                    }
                })
                .thenIdle(5)
                .thenExecute(() -> {
                    CompoundTag tag = new CompoundTag();
                    if (!grenade.save(tag)) {
                        throw new GameTestAssertException("the grenade did not save");
                    }
                    tag.remove("UUID");
                    grenade.discard();
                    if (!(EntityType.loadEntityRecursive(tag, helper.getLevel(), entity -> entity) instanceof GasGrenadeEntity loaded)) {
                        throw new GameTestAssertException("the grenade did not load back");
                    }
                    if (!loaded.isFuseLit() || loaded.fuseTicks() <= 0 || loaded.gas() != GasTypes.CHLORINE.get()) {
                        throw new GameTestAssertException("the loaded grenade lost its fuse or gas: fuse " + loaded.fuseTicks() + ", gas " + loaded.gas());
                    }
                    helper.getLevel().addFreshEntity(loaded);
                })
                .thenWaitUntil(() -> {
                    double mass = totalMass(helper, GasTypes.CHLORINE.get());
                    if (mass < 0.5 * YperiteConfig.GRENADE_MASS.get()) {
                        throw new GameTestAssertException("the loaded grenade did not go off: " + mass);
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = ROOM, timeoutTicks = 60, batch = "delivery")
    public static void rightClickThrowsAGrenadeAndUsesItUp(GameTestHelper helper) {
        sealRoom(helper);
        FakePlayer survival = player(helper, 4, 1, 4);
        ItemStack stack = new ItemStack(ModItems.GRENADES.get("nerve_gas_grenade").get(), 3);
        survival.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.getItem().use(helper.getLevel(), survival, InteractionHand.MAIN_HAND);
        if (stack.getCount() != 2) {
            throw new GameTestAssertException("a thrown grenade should be used up, count is " + stack.getCount());
        }
        List<GasGrenadeEntity> thrown = grenadesNear(helper, 4, 1, 4);
        if (thrown.size() != 1 || thrown.get(0).gas() != GasTypes.NERVE_GAS.get()) {
            throw new GameTestAssertException("expected one nerve gas grenade in flight, found " + thrown.size());
        }
        thrown.forEach(GasGrenadeEntity::discard);

        FakePlayer creative = player(helper, 4, 1, 4);
        creative.getAbilities().instabuild = true;
        ItemStack spare = new ItemStack(ModItems.GRENADES.get("nerve_gas_grenade").get(), 1);
        creative.setItemInHand(InteractionHand.MAIN_HAND, spare);
        spare.getItem().use(helper.getLevel(), creative, InteractionHand.MAIN_HAND);
        if (spare.getCount() != 1) {
            throw new GameTestAssertException("creative mode should not use the grenade up");
        }
        grenadesNear(helper, 4, 1, 4).forEach(GasGrenadeEntity::discard);
        helper.succeed();
    }

    @GameTest(template = ROOM, timeoutTicks = 20, batch = "delivery")
    public static void everyGasHasItsOwnGrenade(GameTestHelper helper) {
        for (var entry : GasTypes.REGISTRY.get().getEntries()) {
            GasType type = entry.getValue();
            long grenades = ModItems.GRENADES.values().stream()
                    .filter(item -> ((GasGrenadeItem) item.get()).gas() == type).count();
            if (grenades != 1) {
                throw new GameTestAssertException(entry.getKey().location() + " has " + grenades + " grenades");
            }
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------ the cylinder

    /** The plume tests read concentrations, so they get a batch of their own that no other gas of theirs shares. */
    @BeforeBatch(batch = "plume")
    public static void resetBeforePlumeBatch(ServerLevel level) {
        GasManager.clear(level, null);
        GasManager.data(level).setWindOverride(null);
    }

    @GameTest(template = ROOM, timeoutTicks = 400, batch = "plume")
    public static void openCylinderFillsTheNeighbourhoodWithGas(GameTestHelper helper) {
        sealRoom(helper);
        GasType gas = GasTypes.CHLORINE.get();
        GasCylinderBlockEntity cylinder = placeCylinder(helper, 10, 1, 10, gas, YperiteConfig.CYLINDER_CAPACITY.get().floatValue());
        helper.setBlock(pos(10, 1, 10), helper.getBlockState(pos(10, 1, 10)).setValue(GasCylinderBlock.OPEN, true));
        helper.runAfterDelay(300, () -> {
            float[] along = new float[8];
            StringBuilder line = new StringBuilder();
            for (int d = 1; d < along.length; d++) {
                along[d] = GasManager.concentration(helper.getLevel(), gas, helper.absolutePos(pos(10 + d, 1, 10)));
                line.append(' ').append(d).append(':').append(along[d]);
            }
            YperiteMod.LOGGER.info("[delivery] plume after 15 s, used {}, by distance{}",
                    YperiteConfig.CYLINDER_CAPACITY.get() - cylinder.amount(), line);
            if (along[1] < 1.0f || along[4] < 0.2f || along[6] < 0.03f) {
                throw new GameTestAssertException("the plume is too thin: " + line);
            }
            helper.succeed();
        });
    }

    @GameTest(template = ROOM, timeoutTicks = 200, batch = "delivery")
    public static void openCylinderReleasesItsGasUntilItIsEmpty(GameTestHelper helper) {
        sealRoom(helper);
        GasType gas = GasTypes.BLOOD_GAS.get();
        GasCylinderBlockEntity cylinder = placeCylinder(helper, 10, 1, 10, gas, 60f);
        FakePlayer player = player(helper, 4, 1, 4);
        if (!useValve(helper, player, 10, 1, 10, InteractionHand.MAIN_HAND).consumesAction() || !isOpen(helper, 10, 1, 10)) {
            throw new GameTestAssertException("an empty hand should open the valve");
        }
        helper.runAfterDelay(30, () -> {
            if (cylinder.amount() >= 60f || cylinder.amount() <= 0f || totalMass(helper, gas) <= 0.0) {
                throw new GameTestAssertException("expected a partly used cylinder, amount " + cylinder.amount());
            }
        });
        helper.runAfterDelay(150, () -> {
            if (cylinder.amount() > 0.01f) {
                throw new GameTestAssertException("the cylinder should be empty by now, amount " + cylinder.amount());
            }
            if (cylinder.comparatorSignal() != 0) {
                throw new GameTestAssertException("an empty cylinder should read 0 on a comparator");
            }
            helper.succeed();
        });
    }

    @GameTest(template = ROOM, timeoutTicks = 100, batch = "delivery")
    public static void closedCylinderKeepsItsGas(GameTestHelper helper) {
        sealRoom(helper);
        GasType gas = GasTypes.BLUE_CROSS.get();
        GasCylinderBlockEntity cylinder = placeCylinder(helper, 10, 1, 10, gas, 500f);
        helper.runAfterDelay(60, () -> {
            if (cylinder.amount() != 500f || totalMass(helper, gas) > 0.0) {
                throw new GameTestAssertException("a closed cylinder should hold on to its gas, amount " + cylinder.amount());
            }
            helper.succeed();
        });
    }

    @GameTest(template = ROOM, timeoutTicks = 60, batch = "delivery")
    public static void redstoneOpensAndClosesTheValve(GameTestHelper helper) {
        sealRoom(helper);
        placeCylinder(helper, 10, 1, 10, GasTypes.BLUE_CROSS.get(), 0f);
        helper.setBlock(pos(11, 1, 10), Blocks.REDSTONE_BLOCK.defaultBlockState());
        helper.runAfterDelay(2, () -> {
            BlockState state = helper.getBlockState(pos(10, 1, 10));
            if (!state.getValue(GasCylinderBlock.OPEN) || !state.getValue(GasCylinderBlock.POWERED)) {
                throw new GameTestAssertException("a redstone signal should open the valve");
            }
            helper.setBlock(pos(11, 1, 10), Blocks.AIR.defaultBlockState());
        });
        helper.runAfterDelay(6, () -> {
            if (isOpen(helper, 10, 1, 10)) {
                throw new GameTestAssertException("the valve should close when the signal ends");
            }
            helper.succeed();
        });
    }

    @GameTest(template = ROOM, timeoutTicks = 60, batch = "delivery")
    public static void valveTurnsOnlyWithAnEmptyHand(GameTestHelper helper) {
        sealRoom(helper);
        placeCylinder(helper, 10, 1, 10, GasTypes.BLUE_CROSS.get(), 0f);
        FakePlayer player = player(helper, 4, 1, 4);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
        if (useValve(helper, player, 10, 1, 10, InteractionHand.MAIN_HAND) != InteractionResult.PASS || isOpen(helper, 10, 1, 10)) {
            throw new GameTestAssertException("with a block in hand the click should pass through, so that it can be placed");
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        useValve(helper, player, 10, 1, 10, InteractionHand.MAIN_HAND);
        if (!isOpen(helper, 10, 1, 10)) {
            throw new GameTestAssertException("an empty hand should open the valve");
        }
        useValve(helper, player, 10, 1, 10, InteractionHand.MAIN_HAND);
        if (isOpen(helper, 10, 1, 10)) {
            throw new GameTestAssertException("a second click should close the valve");
        }
        helper.succeed();
    }

    @GameTest(template = ROOM, timeoutTicks = 100, batch = "delivery")
    public static void cylinderWalledInKeepsItsGas(GameTestHelper helper) {
        sealRoom(helper);
        GasType gas = GasTypes.YPERITE.get();
        GasCylinderBlockEntity cylinder = placeCylinder(helper, 10, 1, 10, gas, 500f);
        for (Direction side : new Direction[]{Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            helper.setBlock(pos(10, 1, 10).relative(side), Blocks.STONE.defaultBlockState());
        }
        helper.setBlock(pos(10, 1, 10), helper.getBlockState(pos(10, 1, 10)).setValue(GasCylinderBlock.OPEN, true));
        helper.runAfterDelay(60, () -> {
            if (cylinder.amount() != 500f || totalMass(helper, gas) > 0.0) {
                throw new GameTestAssertException("a cylinder with no way out should not lose its gas, amount " + cylinder.amount());
            }
            helper.succeed();
        });
    }

    @GameTest(template = ROOM, timeoutTicks = 60, batch = "delivery")
    public static void cylinderContentsSurvivePlacingAndBreaking(GameTestHelper helper) {
        sealRoom(helper);
        GasType gas = GasTypes.NERVE_GAS.get();
        FakePlayer player = player(helper, 4, 1, 4);
        ItemStack stack = GasCylinderItem.filled(gas, 777f);
        BlockPos floor = helper.absolutePos(pos(10, 0, 10));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(floor).add(0.0, 0.5, 0.0), Direction.UP, floor, false);
        InteractionResult result = stack.useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit));
        if (!result.consumesAction()) {
            throw new GameTestAssertException("the cylinder could not be placed");
        }
        GasCylinderBlockEntity placed = (GasCylinderBlockEntity) helper.getBlockEntity(pos(10, 1, 10));
        if (placed.gas() != gas || placed.amount() != 777f) {
            throw new GameTestAssertException("a placed cylinder should keep its contents: " + placed.gas() + ", " + placed.amount());
        }
        placed.setContents(gas, 400f);
        List<ItemStack> drops = Block.getDrops(helper.getBlockState(pos(10, 1, 10)), helper.getLevel(),
                helper.absolutePos(pos(10, 1, 10)), placed);
        if (drops.size() != 1 || GasCylinderItem.contentsOf(drops.get(0)) != gas) {
            throw new GameTestAssertException("a broken cylinder should drop one cylinder holding its gas: " + drops);
        }
        float dropped = drops.get(0).getTagElement("BlockEntityTag").getFloat(GasCylinderBlockEntity.TAG_AMOUNT);
        if (dropped != 400f) {
            throw new GameTestAssertException("a broken cylinder should keep what is left, got " + dropped);
        }
        helper.succeed();
    }

    @GameTest(template = ROOM, timeoutTicks = 20, batch = "delivery")
    public static void comparatorReadsTheFillLevel(GameTestHelper helper) {
        GasType gas = GasTypes.NERVE_GAS.get();
        float capacity = YperiteConfig.CYLINDER_CAPACITY.get().floatValue();
        GasCylinderBlockEntity cylinder = placeCylinder(helper, 10, 1, 10, gas, capacity);
        int full = cylinder.comparatorSignal();
        cylinder.setContents(gas, capacity / 2f);
        int half = cylinder.comparatorSignal();
        cylinder.setContents(gas, 0.001f);
        int trace = cylinder.comparatorSignal();
        cylinder.setContents(gas, 0f);
        int empty = cylinder.comparatorSignal();
        if (full != 15 || half < 7 || half > 8 || trace != 1 || empty != 0) {
            throw new GameTestAssertException("comparator readings were " + full + ", " + half + ", " + trace + ", " + empty);
        }
        helper.succeed();
    }
}
