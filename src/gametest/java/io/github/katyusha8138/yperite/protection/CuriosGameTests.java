package io.github.katyusha8138.yperite.protection;

import io.github.katyusha8138.yperite.YperiteMod;
import io.github.katyusha8138.yperite.exposure.model.ExposureState;
import io.github.katyusha8138.yperite.gas.GasCategory;
import io.github.katyusha8138.yperite.gas.GasManager;
import io.github.katyusha8138.yperite.gas.GasTypes;
import io.github.katyusha8138.yperite.exposure.ExposureManager;
import io.github.katyusha8138.yperite.item.GasMaskItem;
import io.github.katyusha8138.yperite.item.ModItems;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

import static io.github.katyusha8138.yperite.gametest.TestRooms.pos;
import static io.github.katyusha8138.yperite.gametest.TestRooms.sealRoom;

/**
 * The Curios face slot. Curios is loaded in development runs only because it is an optional dependency; this
 * class refers to its API directly, so keep it apart from the other tests.
 */
@GameTestHolder(YperiteMod.MODID)
@PrefixGameTestTemplate(false)
public final class CuriosGameTests {
    private static final String ROOM = "empty_20x10x20";

    private CuriosGameTests() {
    }

    @BeforeBatch(batch = "curios")
    public static void resetBeforeBatch(ServerLevel level) {
        GasManager.clear(level, null);
    }

    @GameTest(template = ROOM, timeoutTicks = 200, batch = "curios")
    public static void aMaskInTheFaceSlotProtectsEvenWithAHelmetOn(GameTestHelper helper) {
        helper.assertTrue(ModList.get().isLoaded("curios"), "Curios must be loaded in development runs");
        ServerLevel level = helper.getLevel();
        sealRoom(helper);
        GasManager.release(level, GasTypes.CHLORINE.get(), helper.absolutePos(pos(10, 2, 10)), 3f, 2000f);

        FakePlayer bare = ProtectionGameTests.standAt(helper, "curios_bare", 10, 1, 10);
        FakePlayer worn = ProtectionGameTests.standAt(helper, "curios_worn", 10, 1, 10);
        ItemStack mask = new ItemStack(ModItems.GAS_MASK.get());
        GasMaskItem.load(mask, FilterKind.GENERAL);

        // A helmet takes the head slot; the mask goes into the Curios face slot.
        worn.setItemSlot(EquipmentSlot.HEAD, new ItemStack(net.minecraft.world.item.Items.IRON_HELMET));
        boolean placed = CuriosApi.getCuriosInventory(worn).resolve()
                .flatMap(handler -> handler.getStacksHandler("face"))
                .map(stacks -> {
                    stacks.getStacks().setStackInSlot(0, mask);
                    return true;
                }).orElse(false);
        helper.assertTrue(placed, "the player must have a Curios face slot");
        helper.assertTrue(EquipmentProtection.findMask(worn) == mask || EquipmentProtection.findMask(worn).getItem() instanceof GasMaskItem,
                "the mask in the face slot must be found");

        ProtectionGameTests.evaluate(helper, bare, 20);
        ProtectionGameTests.evaluate(helper, worn, 20);
        ExposureState b = ExposureManager.stateOf(bare);
        ExposureState w = ExposureManager.stateOf(worn);
        int k = GasCategory.CHOKING.ordinal();
        float bareDose = b.latent[k] + b.active[k];
        float wornDose = w.latent[k] + w.active[k];
        helper.assertTrue(bareDose > 5f && wornDose < bareDose * 0.1f,
                "the face-slot mask should protect like a helmet-slot one: bare=" + bareDose + " worn=" + wornDose);
        helper.assertTrue(GasMaskItem.charge(EquipmentProtection.findMask(worn)) < FilterKind.GENERAL.capacity() - 1f,
                "its filter should wear");
        helper.succeed();
    }
}
