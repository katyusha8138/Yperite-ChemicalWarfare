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

    @GameTest(template = ROOM, timeoutTicks = 40, batch = "curios")
    public static void bodyArmourInTheBodySlotGoesOverATunic(GameTestHelper helper) {
        FakePlayer player = ProtectionGameTests.standAt(helper, "curios_armour", 5, 1, 5);
        ItemStack tunic = new ItemStack(ModItems.UNIFORMS.get(6).get());
        player.setItemSlot(EquipmentSlot.CHEST, tunic);
        ItemStack armour = new ItemStack(ModItems.SAPPENPANZER.get());
        var body = CuriosApi.getCuriosInventory(player).resolve().flatMap(handler -> handler.getStacksHandler("body"));
        helper.assertTrue(body.isPresent(), "the player must have a Curios body slot");
        body.get().getStacks().setStackInSlot(0, armour);
        helper.assertTrue(io.github.katyusha8138.yperite.protection.compat.CuriosCompat.findBodyArmour(player) == armour,
                "the armour in the body slot must be found");

        // In the body slot it gives what it gives in the chest slot: armour, bullet resistance and its weight.
        var context = new top.theillusivec4.curios.api.SlotContext("body", player, 0, false, true);
        java.util.UUID id = java.util.UUID.randomUUID();
        var curio = CuriosApi.getCurio(armour).resolve();
        helper.assertTrue(curio.isPresent(), "body armour must be a curio");
        var modifiers = curio.get().getAttributeModifiers(context, id);
        helper.assertTrue(modifiers.containsKey(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR), "it must give armour");
        helper.assertTrue(modifiers.get(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).stream()
                .anyMatch(m -> m.getAmount() < 0 && m.getId().equals(id)), "it must slow its wearer, under the slot's id");
        helper.assertTrue(curio.get().canEquip(context), "it goes over a tunic");

        // Over a tunic it stands off the chest by the tunic, and a haversack by both.
        float[] under = io.github.katyusha8138.yperite.item.armor.WornLayers.under(player, armour);
        helper.assertTrue(under[0] > 0f, "the armour must stand off the tunic");
        float[] bag = io.github.katyusha8138.yperite.item.armor.WornLayers.under(player, new ItemStack(ModItems.SMALL_BOX_RESPIRATOR.get()));
        helper.assertTrue(bag[0] > under[0], "a haversack must go over both");

        // It wears out by the blows it takes.
        int before = armour.getDamageValue();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.living.LivingHurtEvent(
                player, helper.getLevel().damageSources().explosion(null, null), 8f));
        helper.assertTrue(armour.getDamageValue() > before, "a blow must wear the armour in the body slot");

        // Not two sets at once.
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.DAYFIELD.get()));
        helper.assertTrue(!curio.get().canEquip(context), "no body armour over body armour");
        helper.succeed();
    }
}
