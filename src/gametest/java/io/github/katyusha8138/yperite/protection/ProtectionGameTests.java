package io.github.katyusha8138.yperite.protection;

import com.mojang.authlib.GameProfile;
import io.github.katyusha8138.yperite.YperiteMod;
import io.github.katyusha8138.yperite.block.GasAlarmBlock;
import io.github.katyusha8138.yperite.block.ModBlocks;
import io.github.katyusha8138.yperite.exposure.ExposureManager;
import io.github.katyusha8138.yperite.exposure.model.ExposureState;
import io.github.katyusha8138.yperite.gas.GasCategory;
import io.github.katyusha8138.yperite.gas.GasManager;
import io.github.katyusha8138.yperite.gas.GasTypes;
import io.github.katyusha8138.yperite.gas.WindModel;
import io.github.katyusha8138.yperite.item.DeconPowderItem;
import io.github.katyusha8138.yperite.item.GasDetectorItem;
import io.github.katyusha8138.yperite.item.GasMaskItem;
import io.github.katyusha8138.yperite.item.MaskFilterRecipe;
import io.github.katyusha8138.yperite.item.MedicineItem;
import io.github.katyusha8138.yperite.item.ModItems;
import io.github.katyusha8138.yperite.item.WindVaneItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

import static io.github.katyusha8138.yperite.gametest.TestRooms.pos;
import static io.github.katyusha8138.yperite.gametest.TestRooms.sealRoom;

/**
 * In-world checks for the protective equipment, detectors and treatment.
 *
 * <p>Gas types are split between tests that share the batch so that they do not disturb each other: chlorine for
 * the mask tests, nerve gas for the filter comparison, tear gas for the detector scan, blue cross for the alarm and
 * residue of yperite for the suit.
 */
@GameTestHolder(YperiteMod.MODID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("removal")
public final class ProtectionGameTests {
    private static final String ROOM = "empty_20x10x20";
    private static final float DT = 0.25f;

    private ProtectionGameTests() {
    }

    @BeforeBatch(batch = "protection")
    public static void resetBeforeBatch(ServerLevel level) {
        GasManager.clear(level, null);
        GasManager.data(level).setWindOverride(null);
    }

    // ------------------------------------------------------------------ helpers

    static FakePlayer standAt(GameTestHelper helper, String name, int x, int y, int z) {
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(pos(x, y, z))));
        player.setOnGround(true);
        return player;
    }

    static void evaluate(GameTestHelper helper, FakePlayer player, int steps) {
        ServerLevel level = helper.getLevel();
        for (int i = 0; i < steps; i++) {
            ExposureManager.evaluatePlayer(level, player, GasManager.data(level), DT);
        }
    }

    private static ExposureState state(Player player) {
        return ExposureManager.stateOf(player);
    }

    /** Everything the body has taken in for a category, wherever it currently is. */
    private static float dose(Player player, GasCategory category) {
        ExposureState s = state(player);
        int k = category.ordinal();
        return s.skin[k] + s.latent[k] + s.active[k];
    }

    private static ItemStack mask(io.github.katyusha8138.yperite.protection.FilterKind kind) {
        ItemStack stack = new ItemStack(ModItems.GAS_MASK.get());
        if (kind != null) {
            GasMaskItem.load(stack, kind);
        }
        return stack;
    }

    private static void fillRoomWith(GameTestHelper helper, io.github.katyusha8138.yperite.gas.GasType type) {
        sealRoom(helper);
        GasManager.release(helper.getLevel(), type, helper.absolutePos(pos(10, 2, 10)), 3f, 2000f);
    }

    // ------------------------------------------------------------------ the mask

    @GameTest(template = ROOM, timeoutTicks = 200, batch = "protection")
    public static void aMaskWithAFilterCutsTheDoseAndWearsTheFilter(GameTestHelper helper) {
        fillRoomWith(helper, GasTypes.CHLORINE.get());
        FakePlayer bare = standAt(helper, "mask_bare", 10, 1, 10);
        FakePlayer masked = standAt(helper, "mask_worn", 10, 1, 10);
        ItemStack mask = mask(FilterKind.GENERAL);
        masked.setItemSlot(EquipmentSlot.HEAD, mask);

        evaluate(helper, bare, 20);
        evaluate(helper, masked, 20);

        float bareDose = dose(bare, GasCategory.CHOKING);
        float maskedDose = dose(masked, GasCategory.CHOKING);
        helper.assertTrue(bareDose > 5f, "the bare player should take a lot, dose=" + bareDose);
        helper.assertTrue(maskedDose < bareDose * 0.1f, "a general filter should stop most choking gas: bare=" + bareDose + " masked=" + maskedDose);
        helper.assertTrue(GasMaskItem.charge(mask) < FilterKind.GENERAL.capacity() - 1f,
                "the filter should have worn, charge=" + GasMaskItem.charge(mask));
        helper.succeed();
    }

    @GameTest(template = ROOM, timeoutTicks = 200, batch = "protection")
    public static void theFrenchM2WorksOnItsSewnInPadAndWearsIt(GameTestHelper helper) {
        fillRoomWith(helper, GasTypes.CHLORINE.get());
        FakePlayer bare = standAt(helper, "m2_bare", 10, 1, 10);
        FakePlayer masked = standAt(helper, "m2_worn", 10, 1, 10);
        ItemStack m2 = new ItemStack(ModItems.M2_MASK.get());
        helper.assertTrue(GasMaskItem.filterKind(m2) == FilterKind.IMPREGNATED, "a new M2 carries its pad");
        helper.assertTrue(GasMaskItem.charge(m2) == FilterKind.IMPREGNATED.capacity(), "a new pad is full");
        masked.setItemSlot(EquipmentSlot.HEAD, m2);

        evaluate(helper, bare, 20);
        evaluate(helper, masked, 20);

        float bareDose = dose(bare, GasCategory.CHOKING);
        float maskedDose = dose(masked, GasCategory.CHOKING);
        helper.assertTrue(maskedDose < bareDose * 0.3f, "the pad should stop most chlorine: bare=" + bareDose + " masked=" + maskedDose);
        helper.assertTrue(GasMaskItem.charge(m2) < FilterKind.IMPREGNATED.capacity() - 1f, "the pad should have worn, charge=" + GasMaskItem.charge(m2));
        helper.assertTrue(GasMaskItem.filterKind(m2) == FilterKind.IMPREGNATED, "a worn pad is still the same pad");
        helper.succeed();
    }

    @GameTest(template = ROOM, timeoutTicks = 200, batch = "protection")
    public static void aFirstWorldWarMaskLetsArsenicalSmokeThroughWhereAModernOneDoesNot(GameTestHelper helper) {
        fillRoomWith(helper, GasTypes.BLUE_CROSS.get());
        FakePlayer modern = standAt(helper, "smoke_modern", 10, 1, 10);
        FakePlayer old = standAt(helper, "smoke_sbr", 10, 1, 10);
        modern.setItemSlot(EquipmentSlot.HEAD, mask(FilterKind.GENERAL));
        ItemStack sbr = new ItemStack(ModItems.SMALL_BOX_RESPIRATOR.get());
        GasMaskItem.load(sbr, FilterKind.GENERAL);
        old.setItemSlot(EquipmentSlot.HEAD, sbr);

        evaluate(helper, modern, 20);
        evaluate(helper, old, 20);

        float modernDose = dose(modern, GasCategory.INCAPACITANT);
        float oldDose = dose(old, GasCategory.INCAPACITANT);
        helper.assertTrue(oldDose > modernDose * 2f, "the old filter should let much more smoke through: modern=" + modernDose + " old=" + oldDose);
        helper.succeed();
    }

    @GameTest(template = ROOM, timeoutTicks = 200, batch = "protection")
    public static void theGeneralFilterBarelyHelpsAgainstNerveGasButTheSpecialOneDoes(GameTestHelper helper) {
        fillRoomWith(helper, GasTypes.NERVE_GAS.get());
        FakePlayer bare = standAt(helper, "nerve_bare", 10, 1, 10);
        FakePlayer general = standAt(helper, "nerve_general", 10, 1, 10);
        FakePlayer special = standAt(helper, "nerve_special", 10, 1, 10);
        general.setItemSlot(EquipmentSlot.HEAD, mask(FilterKind.GENERAL));
        special.setItemSlot(EquipmentSlot.HEAD, mask(FilterKind.SPECIAL));

        // A short stay: the skin route is the same for all three, so compare what was breathed in only.
        float[] inhaledBare = new float[io.github.katyusha8138.yperite.exposure.ExposureProfiles.count()];
        float[] inhaledGeneral = new float[io.github.katyusha8138.yperite.exposure.ExposureProfiles.count()];
        float[] inhaledSpecial = new float[io.github.katyusha8138.yperite.exposure.ExposureProfiles.count()];
        float[] contact = new float[io.github.katyusha8138.yperite.exposure.ExposureProfiles.count()];
        ExposureManager.sample(bare, GasManager.data(helper.getLevel()), inhaledBare, contact, 1f);
        ExposureManager.sample(general, GasManager.data(helper.getLevel()), inhaledGeneral, contact, 1f);
        ExposureManager.sample(special, GasManager.data(helper.getLevel()), inhaledSpecial, contact, 1f);

        int nerve = GasCategory.NERVE.ordinal();
        helper.assertTrue(inhaledBare[nerve] > 1f, "nerve gas should be breathed in, got " + inhaledBare[nerve]);
        helper.assertTrue(inhaledGeneral[nerve] > inhaledBare[nerve] * 0.5f,
                "a general filter should barely help: bare=" + inhaledBare[nerve] + " general=" + inhaledGeneral[nerve]);
        helper.assertTrue(inhaledSpecial[nerve] < inhaledBare[nerve] * 0.1f,
                "a special filter should stop nearly all of it: bare=" + inhaledBare[nerve] + " special=" + inhaledSpecial[nerve]);
        helper.succeed();
    }

    @GameTest(template = ROOM, timeoutTicks = 200, batch = "protection")
    public static void aSpentFilterNoLongerHelpsAndAMaskWithoutOneBarelyDoes(GameTestHelper helper) {
        fillRoomWith(helper, GasTypes.CHLORINE.get());
        FakePlayer bare = standAt(helper, "spent_bare", 10, 1, 10);
        FakePlayer spent = standAt(helper, "spent_mask", 10, 1, 10);
        FakePlayer empty = standAt(helper, "empty_mask", 10, 1, 10);
        ItemStack spentMask = mask(FilterKind.SPECIAL);
        GasMaskItem.setCharge(spentMask, 0f);
        spent.setItemSlot(EquipmentSlot.HEAD, spentMask);
        empty.setItemSlot(EquipmentSlot.HEAD, mask(null));

        evaluate(helper, bare, 20);
        evaluate(helper, spent, 20);
        evaluate(helper, empty, 20);

        float bareDose = dose(bare, GasCategory.CHOKING);
        helper.assertTrue(dose(spent, GasCategory.CHOKING) > bareDose * 0.7f, "a spent filter must not protect, spent=" + dose(spent, GasCategory.CHOKING) + " bare=" + bareDose);
        helper.assertTrue(dose(empty, GasCategory.CHOKING) > bareDose * 0.7f, "a mask without a filter must not protect, empty=" + dose(empty, GasCategory.CHOKING) + " bare=" + bareDose);
        helper.succeed();
    }

    @GameTest(template = ROOM, timeoutTicks = 200, batch = "protection")
    public static void theFilterRunsOutAfterEnoughGas(GameTestHelper helper) {
        fillRoomWith(helper, GasTypes.CHLORINE.get());
        FakePlayer player = standAt(helper, "wear_out", 10, 1, 10);
        ItemStack mask = mask(FilterKind.SPECIAL);
        player.setItemSlot(EquipmentSlot.HEAD, mask);

        evaluate(helper, player, 400); // 100 s in dense gas
        helper.assertTrue(GasMaskItem.charge(mask) == 0f, "the filter must be used up, charge=" + GasMaskItem.charge(mask));
        float before = dose(player, GasCategory.CHOKING);
        evaluate(helper, player, 20);
        helper.assertTrue(dose(player, GasCategory.CHOKING) > before + 3f, "the dose should climb quickly once the filter is spent");
        helper.succeed();
    }

    // ------------------------------------------------------------------ the outfit

    @GameTest(template = ROOM, timeoutTicks = 200, batch = "protection")
    public static void theOutfitKeepsMostOfTheContactDoseOffTheSkinAndWearsOut(GameTestHelper helper) {
        sealRoom(helper);
        FakePlayer bare = standAt(helper, "suit_bare", 6, 1, 6);
        FakePlayer dressed = standAt(helper, "suit_worn", 6, 1, 6);
        BlockPos feet = bare.blockPosition();
        GasManager.data(helper.getLevel()).layer(GasTypes.YPERITE.get()).addResidue(feet.getX(), feet.getY(), feet.getZ(), 10f);

        ItemStack jacket = new ItemStack(ModItems.PROTECTIVE_JACKET.get());
        dressed.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.PROTECTIVE_HOOD.get()));
        dressed.setItemSlot(EquipmentSlot.CHEST, jacket);
        dressed.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.PROTECTIVE_TROUSERS.get()));
        dressed.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.PROTECTIVE_BOOTS.get()));

        evaluate(helper, bare, 40);
        evaluate(helper, dressed, 40);

        float bareDose = dose(bare, GasCategory.BLISTER);
        float dressedDose = dose(dressed, GasCategory.BLISTER);
        helper.assertTrue(bareDose > 2f, "standing on residue must contaminate the bare player, dose=" + bareDose);
        helper.assertTrue(dressedDose < bareDose * 0.15f, "the outfit should keep nearly all off: bare=" + bareDose + " dressed=" + dressedDose);
        helper.assertTrue(jacket.getDamageValue() > 0, "the jacket should have lost durability, damage=" + jacket.getDamageValue());
        helper.succeed();
    }

    @GameTest(template = ROOM, timeoutTicks = 200, batch = "protection")
    public static void eachMissingPartLetsMoreThrough(GameTestHelper helper) {
        sealRoom(helper);
        FakePlayer full = standAt(helper, "part_full", 6, 1, 6);
        FakePlayer noJacket = standAt(helper, "part_no_jacket", 6, 1, 6);
        BlockPos feet = full.blockPosition();
        GasManager.data(helper.getLevel()).layer(GasTypes.YPERITE.get()).addResidue(feet.getX(), feet.getY(), feet.getZ(), 10f);
        for (FakePlayer p : new FakePlayer[]{full, noJacket}) {
            p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.PROTECTIVE_HOOD.get()));
            p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.PROTECTIVE_TROUSERS.get()));
            p.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.PROTECTIVE_BOOTS.get()));
        }
        full.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.PROTECTIVE_JACKET.get()));

        evaluate(helper, full, 40);
        evaluate(helper, noJacket, 40);

        helper.assertTrue(dose(noJacket, GasCategory.BLISTER) > dose(full, GasCategory.BLISTER) * 3f,
                "without the jacket far more gets through: full=" + dose(full, GasCategory.BLISTER) + " no jacket=" + dose(noJacket, GasCategory.BLISTER));
        helper.succeed();
    }

    // ------------------------------------------------------------------ the recipe

    @GameTest(template = ROOM, timeoutTicks = 100, batch = "protection")
    public static void theMaskFilterRecipeLoadsTheFilterAndRejectsOtherCombinations(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MaskFilterRecipe recipe = new MaskFilterRecipe(new ResourceLocation(YperiteMod.MODID, "test"), CraftingBookCategory.EQUIPMENT);

        TransientCraftingContainer grid = grid();
        grid.setItem(0, new ItemStack(ModItems.GAS_MASK.get()));
        grid.setItem(4, new ItemStack(ModItems.GAS_FILTER_SPECIAL.get()));
        helper.assertTrue(recipe.matches(grid, level), "a mask and a filter must match, wherever they lie");
        ItemStack result = recipe.assemble(grid, level.registryAccess());
        helper.assertTrue(GasMaskItem.filterKind(result) == FilterKind.SPECIAL, "the result must carry the special filter");
        helper.assertTrue(GasMaskItem.charge(result) == FilterKind.SPECIAL.capacity(), "the filter must be fresh");
        helper.assertTrue(result.getItem() == ModItems.GAS_MASK.get() && result.getCount() == 1, "the result is one mask");

        // The recipe manager finds the same recipe, so the serializer and the JSON file are in place.
        helper.assertTrue(level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).isPresent(), "the recipe manager must know the recipe");

        TransientCraftingContainer noFilter = grid();
        noFilter.setItem(0, new ItemStack(ModItems.GAS_MASK.get()));
        helper.assertTrue(!recipe.matches(noFilter, level), "a mask alone is not a recipe");

        TransientCraftingContainer twoMasks = grid();
        twoMasks.setItem(0, new ItemStack(ModItems.GAS_MASK.get()));
        twoMasks.setItem(1, new ItemStack(ModItems.GAS_MASK.get()));
        twoMasks.setItem(2, new ItemStack(ModItems.GAS_FILTER_GENERAL.get()));
        helper.assertTrue(!recipe.matches(twoMasks, level), "two masks are not a recipe");

        TransientCraftingContainer extra = grid();
        extra.setItem(0, new ItemStack(ModItems.GAS_MASK.get()));
        extra.setItem(1, new ItemStack(ModItems.GAS_FILTER_GENERAL.get()));
        extra.setItem(2, new ItemStack(net.minecraft.world.item.Items.STICK));
        helper.assertTrue(!recipe.matches(extra, level), "an unrelated item spoils the recipe");

        TransientCraftingContainer oldPattern = grid();
        oldPattern.setItem(0, new ItemStack(ModItems.LEDERSCHUTZMASKE.get()));
        oldPattern.setItem(1, new ItemStack(ModItems.GAS_FILTER_GENERAL.get()));
        helper.assertTrue(recipe.matches(oldPattern, level), "the German leather mask takes a screw-in filter");

        TransientCraftingContainer sewnIn = grid();
        sewnIn.setItem(0, new ItemStack(ModItems.M2_MASK.get()));
        sewnIn.setItem(1, new ItemStack(ModItems.GAS_FILTER_GENERAL.get()));
        helper.assertTrue(!recipe.matches(sewnIn, level), "the M2's pad is sewn in: no filter can be loaded");
        helper.succeed();
    }

    private static TransientCraftingContainer grid() {
        AbstractContainerMenu menu = new AbstractContainerMenu(null, 0) {
            @Override
            public ItemStack quickMoveStack(Player player, int index) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(Player player) {
                return true;
            }
        };
        return new TransientCraftingContainer(menu, 3, 3);
    }

    @GameTest(template = ROOM, timeoutTicks = 100, batch = "protection")
    public static void everyRecipeOfTheModLoads(GameTestHelper helper) {
        long count = helper.getLevel().getRecipeManager().getRecipes().stream()
                .filter(recipe -> recipe.getId().getNamespace().equals(YperiteMod.MODID)).count();
        helper.assertTrue(count == 17, "all 17 recipes should have loaded, found " + count);
        helper.succeed();
    }

    // ------------------------------------------------------------------ detection

    @GameTest(template = ROOM, timeoutTicks = 100, batch = "protection")
    public static void theScannerReportsTheCategoryAndTheLevel(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        sealRoom(helper);
        BlockPos middle = helper.absolutePos(pos(10, 2, 10));
        GasScanner.Reading clear = GasScanner.scan(level, middle, 1, middle);
        helper.assertTrue(clear.isClear(), "an empty room reads clear");

        GasManager.release(level, GasTypes.TEAR_GAS.get(), middle, 1f, 40f);
        GasScanner.Reading reading = GasScanner.scan(level, middle, 1, middle.below());
        helper.assertTrue(reading.level(GasCategory.RIOT_CONTROL) >= DetectorLevel.HIGH,
                "dense tear gas should read high or more, level=" + reading.level(GasCategory.RIOT_CONTROL));
        helper.assertTrue(reading.level(GasCategory.NERVE) == 0, "other categories stay at zero");
        helper.assertTrue(!GasDetectorItem.describe(reading).getString().isEmpty(), "the reading can be shown");

        GasScanner.Reading far = GasScanner.scan(level, helper.absolutePos(pos(17, 2, 17)), 1, null);
        helper.assertTrue(far.isClear(), "far from the gas the detector reads clear");
        helper.succeed();
    }

    @GameTest(template = ROOM, timeoutTicks = 100, batch = "protection")
    public static void theScannerNoticesResidueUnderfootEvenWithoutGasInTheAir(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        sealRoom(helper);
        BlockPos feet = helper.absolutePos(pos(10, 1, 10));
        GasManager.data(level).layer(GasTypes.YPERITE.get()).addResidue(feet.getX(), feet.getY(), feet.getZ(), 3f);
        GasScanner.Reading reading = GasScanner.scan(level, feet.above(), 1, feet);
        helper.assertTrue(reading.residue(), "residue underfoot must be reported");
        helper.assertTrue(!reading.isClear(), "residue means the reading is not clear");
        helper.succeed();
    }

    @GameTest(template = ROOM, timeoutTicks = 300, batch = "protection")
    public static void theAlarmGivesAScaledSignalWhileGasIsNearAndStopsWhenItIsGone(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        sealRoom(helper);
        BlockPos alarm = pos(10, 1, 10);
        helper.setBlock(alarm, ModBlocks.GAS_ALARM.get().defaultBlockState());
        BlockPos absolute = helper.absolutePos(alarm);
        GasManager.release(level, GasTypes.BLUE_CROSS.get(), absolute.offset(2, 0, 0), 1f, 40f);

        helper.runAfterDelay(60, () -> {
            int power = level.getBlockState(absolute).getValue(GasAlarmBlock.POWER);
            helper.assertTrue(power >= 7, "the alarm should give a strong signal near dense gas, power=" + power);
            helper.assertTrue(level.getBlockState(absolute).getLightEmission(level, absolute) > 0, "an alarm that sounds also glows");
            GasManager.clear(level, GasTypes.BLUE_CROSS.get());
            helper.runAfterDelay(60, () -> {
                int after = level.getBlockState(absolute).getValue(GasAlarmBlock.POWER);
                helper.assertTrue(after == 0, "the alarm should be silent again, power=" + after);
                helper.succeed();
            });
        });
    }

    @GameTest(template = ROOM, timeoutTicks = 100, batch = "protection")
    public static void theWindVaneShowsWhereTheWindBlowsAndWhereItComesFrom(GameTestHelper helper) {
        // Wind from the west blows east.
        WindModel.Wind west = WindModel.Wind.fromCompass(270f, 0.5f);
        helper.assertTrue(WindVaneItem.arrow(west).equals("→"), "wind from the west blows east, arrow=" + WindVaneItem.arrow(west));
        helper.assertTrue(WindVaneItem.compass(west).equals("west"), "it comes from the west, got " + WindVaneItem.compass(west));
        WindModel.Wind north = WindModel.Wind.fromCompass(0f, 0.5f);
        helper.assertTrue(WindVaneItem.arrow(north).equals("↓"), "wind from the north blows south, arrow=" + WindVaneItem.arrow(north));
        WindModel.Wind southEast = WindModel.Wind.fromCompass(135f, 0.5f);
        helper.assertTrue(WindVaneItem.arrow(southEast).equals("↖"), "wind from the south-east blows north-west, arrow=" + WindVaneItem.arrow(southEast));
        helper.assertTrue(WindVaneItem.compass(southEast).equals("south_east"), "got " + WindVaneItem.compass(southEast));

        ServerLevel level = helper.getLevel();
        GasManager.data(level).setWindOverride(new WindModel.Override(90f, 0.5f));
        try {
            helper.assertTrue(!WindVaneItem.reading(level).getString().isEmpty(), "the reading can be shown");
        } finally {
            GasManager.data(level).setWindOverride(null);
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------ decontamination and treatment

    @GameTest(template = ROOM, timeoutTicks = 100, batch = "protection")
    public static void theDeconSolutionWashesTheSkinButNotTheBody(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FakePlayer player = standAt(helper, "decon_self", 10, 1, 10);
        ExposureState s = state(player);
        s.skin[GasCategory.BLISTER.ordinal()] = 6f;
        s.skin[GasCategory.NERVE.ordinal()] = 2f;
        s.latent[GasCategory.BLISTER.ordinal()] = 3f;
        s.aftermath[GasCategory.BLISTER.ordinal()] = 0.5f;

        ItemStack solution = new ItemStack(ModItems.DECON_SOLUTION.get());
        solution.getItem().finishUsingItem(solution, level, player);

        helper.assertTrue(s.skin[GasCategory.BLISTER.ordinal()] == 0f && s.skin[GasCategory.NERVE.ordinal()] == 0f, "the skin must be clean");
        helper.assertTrue(s.latent[GasCategory.BLISTER.ordinal()] == 3f, "what is already in the body stays");
        helper.assertTrue(s.aftermath[GasCategory.BLISTER.ordinal()] == 0.5f, "lasting damage stays");
        helper.succeed();
    }

    @GameTest(template = ROOM, timeoutTicks = 100, batch = "protection")
    public static void theDeconPowderNeutralisesResidueAroundTheSpotOnly(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        sealRoom(helper);
        FakePlayer player = standAt(helper, "decon_ground", 10, 1, 10);
        var layer = GasManager.data(level).layer(GasTypes.YPERITE.get());
        BlockPos near = helper.absolutePos(pos(5, 1, 5));
        BlockPos nearby = helper.absolutePos(pos(6, 1, 5));
        BlockPos far = helper.absolutePos(pos(15, 1, 15));
        layer.addResidue(near.getX(), near.getY(), near.getZ(), 4f);
        layer.addResidue(nearby.getX(), nearby.getY(), nearby.getZ(), 4f);
        layer.addResidue(far.getX(), far.getY(), far.getZ(), 4f);

        ItemStack powder = new ItemStack(ModItems.DECON_POWDER.get(), 2);
        BlockPos floor = helper.absolutePos(pos(5, 0, 5));
        UseOnContext context = new UseOnContext(level, player, InteractionHand.MAIN_HAND, powder,
                new BlockHitResult(Vec3.atCenterOf(floor), Direction.UP, floor, false));
        ((DeconPowderItem) powder.getItem()).useOn(context);

        helper.assertTrue(layer.getResidue(near.getX(), near.getY(), near.getZ()) == 0f, "residue at the spot must be gone");
        helper.assertTrue(layer.getResidue(nearby.getX(), nearby.getY(), nearby.getZ()) == 0f, "residue next to it must be gone");
        helper.assertTrue(layer.getResidue(far.getX(), far.getY(), far.getZ()) == 4f, "residue far away must stay");
        helper.assertTrue(powder.getCount() == 1, "one powder is used up, count=" + powder.getCount());
        helper.succeed();
    }

    @GameTest(template = ROOM, timeoutTicks = 100, batch = "protection")
    public static void eachMedicineTreatsOnlyItsOwnCategories(GameTestHelper helper) {
        FakePlayer player = standAt(helper, "medicine", 10, 1, 10);
        ExposureState s = state(player);
        for (GasCategory category : GasCategory.values()) {
            s.latent[category.ordinal()] = 10f;
            s.active[category.ordinal()] = 10f;
            s.skin[category.ordinal()] = 10f;
            s.aftermath[category.ordinal()] = 0.5f;
        }

        MedicineItem.treat(player, MedicineItem.Kind.GENERAL);
        for (GasCategory treated : new GasCategory[]{GasCategory.RIOT_CONTROL, GasCategory.CHOKING, GasCategory.BLOOD, GasCategory.INCAPACITANT}) {
            helper.assertTrue(s.active[treated.ordinal()] < 5f && s.latent[treated.ordinal()] < 5f, "the general medicine must treat " + treated);
        }
        helper.assertTrue(s.active[GasCategory.NERVE.ordinal()] == 10f && s.active[GasCategory.BLISTER.ordinal()] == 10f,
                "the general medicine must not touch nerve or blister agents");

        MedicineItem.treat(player, MedicineItem.Kind.NERVE_ANTIDOTE);
        helper.assertTrue(s.active[GasCategory.NERVE.ordinal()] < 2f && s.latent[GasCategory.NERVE.ordinal()] < 2f, "the antidote must treat nerve agents strongly");
        helper.assertTrue(s.active[GasCategory.BLISTER.ordinal()] == 10f, "the antidote must not touch blister agents");

        MedicineItem.treat(player, MedicineItem.Kind.BLISTER_OINTMENT);
        helper.assertTrue(s.active[GasCategory.BLISTER.ordinal()] <= 5f && s.skin[GasCategory.BLISTER.ordinal()] == 0f, "the ointment must treat blister agents and clean the skin");
        helper.assertTrue(s.skin[GasCategory.NERVE.ordinal()] == 10f, "the ointment must leave the skin dose of other categories alone");
        for (GasCategory category : GasCategory.values()) {
            helper.assertTrue(s.aftermath[category.ordinal()] == 0.5f, "no medicine removes lasting damage");
        }
        helper.succeed();
    }
}
