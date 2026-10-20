package io.github.katyusha8138.yperite.protection;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.katyusha8138.yperite.YperiteMod;
import io.github.katyusha8138.yperite.item.ModItems;
import io.github.katyusha8138.yperite.item.armor.GeoArmorItem;
import io.github.katyusha8138.yperite.item.armor.WarArmorMaterials;
import io.github.katyusha8138.yperite.item.armor.WornGeoItem;
import io.github.katyusha8138.yperite.item.armor.WornLayers;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The First World War equipment: every worn piece has its model, texture and names; uniforms and body armour sit in
 * the right slots with the right materials; body armour slows its wearer; and pieces worn over others know how far
 * to stand off the chest.
 */
@GameTestHolder(YperiteMod.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private static final String ROOM = "empty_20x10x20";

    private EquipmentGameTests() {
    }

    private static List<Item> wornItems() {
        List<Item> items = new ArrayList<>();
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            if (item instanceof WornGeoItem && YperiteMod.MODID.equals(ForgeRegistries.ITEMS.getKey(item).getNamespace())) {
                items.add(item);
            }
        }
        return items;
    }

    private static JsonObject lang(String code) throws Exception {
        try (InputStream in = YperiteMod.class.getResourceAsStream("/assets/yperite/lang/" + code + ".json")) {
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @GameTest(template = ROOM, timeoutTicks = 20, batch = "equipment")
    public static void everyWornPieceHasItsModelTextureIconAndNames(GameTestHelper helper) throws Exception {
        JsonObject en = lang("en_us");
        JsonObject ja = lang("ja_jp");
        List<Item> worn = wornItems();
        helper.assertTrue(worn.size() >= 5 + 8 + 6 + 15 + 2, "expected helmets, caps, masks, uniforms and body armour, found " + worn.size());
        for (Item item : worn) {
            String id = ForgeRegistries.ITEMS.getKey(item).getPath();
            String model = ((WornGeoItem) item).model();
            for (String path : List.of("/assets/yperite/geo/armor/" + model + ".geo.json", "/assets/yperite/textures/armor/" + model + ".png",
                    "/assets/yperite/models/item/" + id + ".json", "/assets/yperite/textures/item/" + id + ".png")) {
                helper.assertTrue(YperiteMod.class.getResource(path) != null, id + " is missing " + path);
            }
            helper.assertTrue(en.has("item.yperite." + id) && ja.has("item.yperite." + id), id + " needs an English and a Japanese name");
        }
        helper.succeed();
    }

    @GameTest(template = ROOM, timeoutTicks = 20, batch = "equipment")
    public static void uniformsComeInThreePiecesPerArmyAndTunicsStandOffTheChest(GameTestHelper helper) {
        helper.assertTrue(ModItems.UNIFORMS.size() == 15, "five armies of three pieces");
        ArmorItem.Type[] order = {ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS};
        for (int k = 0; k < ModItems.UNIFORMS.size(); k++) {
            GeoArmorItem piece = (GeoArmorItem) ModItems.UNIFORMS.get(k).get();
            helper.assertTrue(piece.getType() == order[k % 3], piece + " is in the wrong slot");
            helper.assertTrue(piece.getMaterial() == WarArmorMaterials.WOOL_UNIFORM, piece + " should be wool");
            helper.assertTrue(piece.bulletResistance() == 0.0, "wool stops no bullets");
            boolean tunic = piece.getType() == ArmorItem.Type.CHESTPLATE;
            helper.assertTrue((piece.standOffFront() > 0f) == tunic, piece + ": only tunics have things worn over them");
        }
        helper.succeed();
    }

    @GameTest(template = ROOM, timeoutTicks = 20, batch = "equipment")
    public static void bodyArmourProtectsAndSlowsItsWearer(GameTestHelper helper) {
        FakePlayer player = ProtectionGameTests.standAt(helper, "armour", 5, 1, 5);
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance armour = player.getAttribute(Attributes.ARMOR);
        double baseSpeed = speed.getValue();
        double baseArmour = armour.getValue();
        for (RegistryObject<Item> entry : ModItems.BODY_ARMOUR) {
            GeoArmorItem item = (GeoArmorItem) entry.get();
            ItemStack stack = new ItemStack(item);
            helper.assertTrue(item.getType() == ArmorItem.Type.CHESTPLATE, item + " goes on the chest");
            helper.assertTrue(item.bulletResistance() > 0.1, item + " should resist bullets");
            var modifiers = stack.getAttributeModifiers(EquipmentSlot.CHEST);
            player.getAttributes().addTransientAttributeModifiers(modifiers);
            double slowed = speed.getValue();
            double armoured = armour.getValue();
            player.getAttributes().removeAttributeModifiers(modifiers);
            helper.assertTrue(Math.abs(slowed - baseSpeed * (1.0 - item.speedPenalty())) < 1e-6,
                    item + " should slow by " + item.speedPenalty() + ": " + baseSpeed + " -> " + slowed);
            helper.assertTrue(armoured >= baseArmour + 4, item + " should add armour: " + baseArmour + " -> " + armoured);
            helper.assertTrue(speed.getValue() == baseSpeed, "taking it off gives the speed back");
        }
        GeoArmorItem sappen = (GeoArmorItem) ModItems.SAPPENPANZER.get();
        GeoArmorItem dayfield = (GeoArmorItem) ModItems.DAYFIELD.get();
        helper.assertTrue(sappen.bulletResistance() > dayfield.bulletResistance() && sappen.speedPenalty() > dayfield.speedPenalty(),
                "the Sappenpanzer is the heavier and the stronger");
        helper.succeed();
    }

    @GameTest(template = ROOM, timeoutTicks = 20, batch = "equipment")
    public static void aHaversackStandsOffWhatIsWornOnTheChest(GameTestHelper helper) {
        FakePlayer player = ProtectionGameTests.standAt(helper, "layers", 5, 1, 5);
        ItemStack mask = new ItemStack(ModItems.SMALL_BOX_RESPIRATOR.get());
        float[] bare = WornLayers.under(player, mask);
        helper.assertTrue(bare[0] == 0f && bare[1] == 0f, "nothing on the chest: the haversack lies on the body");

        ItemStack tunic = new ItemStack(ModItems.UNIFORMS.get(0).get());
        player.setItemSlot(EquipmentSlot.CHEST, tunic);
        GeoArmorItem tunicItem = (GeoArmorItem) tunic.getItem();
        float[] overTunic = WornLayers.under(player, mask);
        helper.assertTrue(overTunic[0] == tunicItem.standOffFront() && overTunic[1] == tunicItem.standOffBack(), "the haversack goes over the tunic");
        float[] tunicItself = WornLayers.under(player, player.getItemBySlot(EquipmentSlot.CHEST));
        helper.assertTrue(tunicItself[0] == 0f, "the tunic itself lies on the body");

        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.SAPPENPANZER.get()));
        float[] overArmour = WornLayers.under(player, mask);
        helper.assertTrue(overArmour[0] == ((GeoArmorItem) ModItems.SAPPENPANZER.get()).standOffFront(), "the haversack goes over the armour");
        helper.succeed();
    }

    @GameTest(template = ROOM, timeoutTicks = 20, batch = "equipment")
    public static void capsAndOtherHeadgearGoOnTheHeadAndStopNoBullets(GameTestHelper helper) {
        helper.assertTrue(ModItems.HEADGEAR.size() == 8, "five caps, two Pickelhauben and a shako");
        for (RegistryObject<Item> entry : ModItems.HEADGEAR) {
            GeoArmorItem cap = (GeoArmorItem) entry.get();
            helper.assertTrue(cap.getType() == ArmorItem.Type.HELMET, cap + " goes on the head");
            helper.assertTrue(cap.bulletResistance() == 0.0, cap + " is not steel");
            helper.assertTrue(cap.getDefense() <= 1, cap + " gives a cloth or leather cap's protection, not a helmet's");
        }
        helper.succeed();
    }

    @GameTest(template = ROOM, timeoutTicks = 20, batch = "equipment")
    public static void theSabreCutsDeepButWearsOutSoon(GameTestHelper helper) {
        ItemStack sabre = new ItemStack(ModItems.SABRE.get());
        ItemStack iron = new ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
        ItemStack netherite = new ItemStack(net.minecraft.world.item.Items.NETHERITE_SWORD);
        double damage = 1.0 + attribute(sabre, Attributes.ATTACK_DAMAGE);
        double speed = 4.0 + attribute(sabre, Attributes.ATTACK_SPEED);
        helper.assertTrue(Math.abs(damage - 10.0) < 1e-6, "the sabre should deal 10, deals " + damage);
        helper.assertTrue(Math.abs(speed - (4.0 + attribute(iron, Attributes.ATTACK_SPEED))) < 1e-6, "the sabre should swing as fast as a sword: " + speed);
        helper.assertTrue(damage > 1.0 + attribute(netherite, Attributes.ATTACK_DAMAGE), "the sabre should cut deeper than a netherite sword");
        helper.assertTrue(sabre.getMaxDamage() == 100, "the sabre should last 100 uses, lasts " + sabre.getMaxDamage());
        helper.assertTrue(sabre.getMaxDamage() < new ItemStack(net.minecraft.world.item.Items.STONE_SWORD).getMaxDamage(),
                "the sabre should wear out sooner than a stone sword");
        helper.succeed();
    }

    private static double attribute(ItemStack stack, net.minecraft.world.entity.ai.attributes.Attribute attribute) {
        return stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(attribute).stream().mapToDouble(m -> m.getAmount()).sum();
    }

    @GameTest(template = ROOM, timeoutTicks = 20, batch = "equipment")
    public static void theWhistleIsBlownToTheMouthAndCarriesFar(GameTestHelper helper) throws Exception {
        var sound = io.github.katyusha8138.yperite.sound.ModSounds.OFFICER_WHISTLE.get();
        float range = io.github.katyusha8138.yperite.sound.ModSounds.WHISTLE_RANGE;
        helper.assertTrue(sound.getRange(1.0f) == range, "the whistle should be sent to players " + range + " blocks off");
        helper.assertTrue(range > 16f * 4, "the whistle should carry much further than an ordinary sound");
        // the client fades it over the same distance, at its natural loudness
        JsonObject sounds;
        try (InputStream in = YperiteMod.class.getResourceAsStream("/assets/yperite/sounds.json")) {
            sounds = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        var files = sounds.getAsJsonObject("item.officer_whistle").getAsJsonArray("sounds");
        helper.assertTrue(files.size() >= 1, "the whistle needs its sound files");
        for (var file : files) {
            var entry = file.getAsJsonObject();
            helper.assertTrue(entry.get("attenuation_distance").getAsFloat() == range, "the whistle should fade out at " + range + " blocks");
            helper.assertTrue(!entry.has("volume") || entry.get("volume").getAsFloat() <= 1f, "the whistle should not be made louder than it is");
            String path = "/assets/yperite/sounds/" + entry.get("name").getAsString().replace("yperite:", "") + ".ogg";
            helper.assertTrue(YperiteMod.class.getResource(path) != null, "missing " + path);
        }
        FakePlayer player = ProtectionGameTests.standAt(helper, "officer", 5, 1, 5);
        ItemStack whistle = new ItemStack(ModItems.OFFICER_WHISTLE.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, whistle);
        whistle.use(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(player.isUsingItem() && whistle.getUseAnimation() == net.minecraft.world.item.UseAnim.TOOT_HORN,
                "the whistle should be put to the mouth");
        helper.assertTrue(player.getCooldowns().isOnCooldown(whistle.getItem()), "the whistle should not be blown again at once");
        helper.succeed();
    }
}
