package io.github.katyusha8138.yperite.gametest.client;

import io.github.katyusha8138.yperite.YperiteMod;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * A scripted walk through the things that can only be judged on a real client: it builds a platform, dresses the
 * player in turn in the mask (Curios slot and helmet slot), the full outfit and a hotbar of every item, looks at
 * the alarm block, and takes a screenshot of each scene. Then it quits.
 *
 * <p>Active only when the system property {@code yperite.smoke} is set (see {@code -PsmokeClient} in build.gradle),
 * so it never affects a normal run. Screenshots land in {@code run/screenshots}.
 */
@Mod.EventBusSubscriber(modid = YperiteMod.MODID, value = Dist.CLIENT)
public final class ClientSmoke {
    private static final boolean ACTIVE = System.getProperty("yperite.smoke") != null;
    /** Ticks to wait after changing a scene before taking its picture, so that a slow software renderer has drawn it. */
    private static final int SETTLE = 70;

    private record Scene(String name, List<String> commands, CameraType camera, float yaw, float pitch) {
        /** The three outfit scenes are shown on the player model of the inventory screen, which draws every render layer. */
        boolean inventory() {
            return name.startsWith("1_") || name.startsWith("2_") || name.startsWith("3_") || name.startsWith("show_inventory");
        }

        /** Scenes named "show_..." show the player large from four sides (see ShowcaseScreen). */
        boolean showcase() {
            return name.startsWith("show_") && !inventory() && !name.endsWith("_setup") && !name.startsWith("show_world");
        }
    }

    private static String hotbar(int slot, String item) {
        return "item replace entity @s hotbar." + slot + " with " + (item.equals("air") ? "minecraft:air" : "yperite:" + item);
    }

    /** With -Dyperite.smoke.only=show_ only the scenes whose names start with that are run (several: comma-separated). */
    private static final String ONLY = System.getProperty("yperite.smoke.only", "");

    private static final List<Scene> SCENES = allScenes().stream()
            .filter(s -> java.util.Arrays.stream(ONLY.split(",")).anyMatch(prefix -> s.name().startsWith(prefix))).toList();

    /** Five scenes: put on the given items, then show them from four sides. */
    private static List<Scene> showcase(String name, String... commands) {
        List<String> setup = new java.util.ArrayList<>(List.of(
                "item replace entity @s armor.head with minecraft:air",
                "item replace entity @s armor.chest with minecraft:air", "item replace entity @s armor.legs with minecraft:air",
                "item replace entity @s armor.feet with minecraft:air", "@curios clear",
                "item replace entity @s weapon.offhand with minecraft:air"));
        setup.addAll(List.of(commands));
        List<Scene> scenes = new java.util.ArrayList<>();
        scenes.add(new Scene("show_" + name + "_setup", setup, CameraType.FIRST_PERSON, 0f, 0f));
        for (int view = 0; view < 4; view++) {
            scenes.add(new Scene("show_" + name + "@" + view, List.of(), CameraType.FIRST_PERSON, 0f, 0f));
        }
        return scenes;
    }

    private static String[] kit(String helmet, String army, String mask, String armour) {
        String feet = army.equals("russian") ? "_boots" : "_puttees";
        java.util.List<String> c = new java.util.ArrayList<>(List.of(
                "item replace entity @s armor.head with yperite:" + helmet,
                "item replace entity @s armor.chest with yperite:" + army + "_tunic",
                "item replace entity @s armor.legs with yperite:" + army + "_trousers",
                "item replace entity @s armor.feet with yperite:" + army + feet,
                "@curios yperite:" + mask));
        if (armour != null) {
            c.add("@curios body yperite:" + armour);
        }
        return c.toArray(new String[0]);
    }

    private static final String FILTER = "{Filter:{Kind:\"general\",Charge:150f}}";

    private static List<Scene> allScenes() {
        List<Scene> all = new java.util.ArrayList<>();
        for (String helmet : List.of("brodie_mk1", "adrian_m15", "stahlhelm_m1916", "berndorfer", "adrian_m16_russian")) {
            all.addAll(showcase(helmet, "item replace entity @s armor.head with yperite:" + helmet));
        }
        for (String mask : List.of("gas_mask", "small_box_respirator", "lederschutzmaske", "austrian_gas_mask",
                "zelinsky_kummant_mask")) {
            all.addAll(showcase(mask, "item replace entity @s armor.head with yperite:" + mask + FILTER));
        }
        all.addAll(showcase("m2_mask", "item replace entity @s armor.head with yperite:m2_mask"));
        // A helmet over a mask worn in the Curios face slot.
        all.addAll(showcase("brodie_sbr", "item replace entity @s armor.head with yperite:brodie_mk1", "@curios yperite:small_box_respirator"));
        all.addAll(showcase("stahlhelm_leder", "item replace entity @s armor.head with yperite:stahlhelm_m1916", "@curios yperite:lederschutzmaske"));
        all.addAll(showcase("adrian_m2", "item replace entity @s armor.head with yperite:adrian_m15", "@curios yperite:m2_mask"));
        all.addAll(showcase("berndorfer_austrian", "item replace entity @s armor.head with yperite:berndorfer", "@curios yperite:austrian_gas_mask"));
        // Caps and the like, alone and over a mask in the face slot
        for (String cap : List.of("british_cap", "french_kepi", "german_cap", "austrian_cap", "russian_cap", "pickelhaube",
                "pickelhaube_covered", "austrian_shako")) {
            all.addAll(showcase(cap, "item replace entity @s armor.head with yperite:" + cap));
        }
        all.addAll(showcase("cap_sbr", "item replace entity @s armor.head with yperite:british_cap", "@curios yperite:small_box_respirator"));
        all.addAll(showcase("kepi_m2", "item replace entity @s armor.head with yperite:french_kepi", "@curios yperite:m2_mask"));
        all.addAll(showcase("pickelhaube_leder", "item replace entity @s armor.head with yperite:pickelhaube_covered", "@curios yperite:lederschutzmaske"));
        all.addAll(showcase("shako_austrian", "item replace entity @s armor.head with yperite:austrian_shako", "@curios yperite:austrian_gas_mask"));
        all.addAll(showcase("furazhka_zelinsky", "item replace entity @s armor.head with yperite:russian_cap", "@curios yperite:zelinsky_kummant_mask"));
        all.addAll(showcase("adrian_zelinsky", "item replace entity @s armor.head with yperite:adrian_m16_russian", "@curios yperite:zelinsky_kummant_mask"));
        // Whole outfits ("_kit" shows the whole figure): helmet, uniform, a mask in the face slot and body armour
        // over the tunic in the body slot.
        all.addAll(showcase("british_kit", kit("brodie_mk1", "british", "small_box_respirator", "dayfield")));
        all.addAll(showcase("german_kit", kit("stahlhelm_m1916", "german", "lederschutzmaske", "sappenpanzer")));
        all.addAll(showcase("french_kit", kit("adrian_m15", "french", "m2_mask", null)));
        all.addAll(showcase("austrian_kit", kit("berndorfer", "austrian", "austrian_gas_mask", null)));
        all.addAll(showcase("russian_kit", kit("adrian_m16_russian", "russian", "zelinsky_kummant_mask", null)));
        // an officer: cap, uniform, the sabre in hand and the whistle in the other
        List<String> officer = new java.util.ArrayList<>(List.of(kit("german_cap", "german", "lederschutzmaske", null)));
        officer.add("item replace entity @s weapon.mainhand with yperite:sabre");
        officer.add("item replace entity @s weapon.offhand with yperite:officer_whistle");
        all.addAll(showcase("officer_kit", officer.toArray(new String[0])));
        all.addAll(showcase("grenadier_kit", "item replace entity @s armor.head with yperite:pickelhaube_covered",
                "item replace entity @s weapon.mainhand with yperite:stick_grenade"));
        // The protective outfit, with a mask under the hood, and again with body armour and a haversack over it.
        all.addAll(showcase("suit_kit", "item replace entity @s armor.head with yperite:protective_hood",
                "item replace entity @s armor.chest with yperite:protective_jacket",
                "item replace entity @s armor.legs with yperite:protective_trousers",
                "item replace entity @s armor.feet with yperite:protective_boots", "@curios yperite:gas_mask"));
        all.addAll(showcase("suit_armour_kit", "item replace entity @s armor.head with yperite:protective_hood",
                "item replace entity @s armor.chest with yperite:protective_jacket",
                "item replace entity @s armor.legs with yperite:protective_trousers",
                "item replace entity @s armor.feet with yperite:protective_boots", "@curios yperite:small_box_respirator",
                "@curios body yperite:sappenpanzer"));
        // Body armour on its own in the chest slot, with the respirator's haversack over it.
        all.addAll(showcase("sappenpanzer_kit", "item replace entity @s armor.chest with yperite:sappenpanzer",
                "@curios yperite:small_box_respirator"));
        all.addAll(List.of(
            new Scene("show_world_front", List.of(), CameraType.THIRD_PERSON_FRONT, 0f, 0f),
            new Scene("show_world_angle", List.of(), CameraType.THIRD_PERSON_FRONT, 40f, 0f),
            new Scene("1_mask_in_curios_face_slot", List.of(
                    // Survival, because in creative the inventory screen is the item browser, with no player model.
                    "gamemode survival", "effect clear @s", "@curios yperite:gas_mask"), CameraType.THIRD_PERSON_FRONT, 0f, 0f),
            new Scene("2_mask_in_helmet_slot", List.of(
                    "@curios clear",
                    "item replace entity @s armor.head with yperite:gas_mask"), CameraType.THIRD_PERSON_FRONT, 0f, 0f),
            new Scene("3_full_outfit", List.of(
                    "item replace entity @s armor.head with yperite:protective_hood",
                    "@curios yperite:gas_mask",
                    "item replace entity @s armor.chest with yperite:protective_jacket",
                    "item replace entity @s armor.legs with yperite:protective_trousers",
                    "item replace entity @s armor.feet with yperite:protective_boots"), CameraType.THIRD_PERSON_FRONT, 0f, 0f),
            new Scene("4_hotbar_first_half", List.of(
                    "gamemode creative", "clear @s",
                    hotbar(0, "gas_mask"), hotbar(1, "gas_filter_general"),
                    hotbar(2, "gas_filter_special"), hotbar(3, "sealed_fabric"),
                    hotbar(4, "protective_hood"), hotbar(5, "protective_jacket"),
                    hotbar(6, "protective_trousers"), hotbar(7, "protective_boots"),
                    hotbar(8, "gas_detector")), CameraType.FIRST_PERSON, 0f, 60f),
            new Scene("5_hotbar_second_half", List.of(
                    hotbar(0, "wind_vane"), hotbar(1, "gas_alarm"),
                    hotbar(2, "decon_solution"), hotbar(3, "decon_powder"),
                    hotbar(4, "medicine_general"), hotbar(5, "antidote_nerve"),
                    hotbar(6, "ointment_blister"), hotbar(7, "air"),
                    hotbar(8, "air")), CameraType.FIRST_PERSON, 0f, 60f),
            new Scene("6_alarm_off_and_on", List.of(
                    "setblock -1 81 2 yperite:gas_alarm[power=0]",
                    "setblock 1 81 2 yperite:gas_alarm[power=15]"), CameraType.FIRST_PERSON, -20f, 15f),
            new Scene("7_delivery_hotbar", List.of(
                    hotbar(0, "tear_gas_grenade"), hotbar(1, "chlorine_grenade"), hotbar(2, "blood_gas_grenade"),
                    hotbar(3, "yperite_grenade"), hotbar(4, "nerve_gas_grenade"), hotbar(5, "blue_cross_grenade"),
                    hotbar(6, "gas_cylinder"),
                    "item replace entity @s hotbar.7 with yperite:gas_cylinder{BlockEntityTag:{Gas:\"yperite:chlorine\",Amount:2000f}}",
                    hotbar(8, "air")), CameraType.FIRST_PERSON, 0f, 60f),
            new Scene("8_cylinders_and_grenade", List.of(
                    "setblock -1 81 2 minecraft:air", "setblock 1 81 2 minecraft:air",
                    // the band takes the colour of the gas inside: chlorine, yperite (open), Blue Cross, empty
                    "setblock -2 80 4 yperite:gas_cylinder[open=false,powered=false]{Gas:\"yperite:chlorine\",Amount:2000f}",
                    "setblock 0 80 4 yperite:gas_cylinder[open=true,powered=false]{Gas:\"yperite:yperite\",Amount:2000f}",
                    "setblock 2 80 4 yperite:gas_cylinder[open=false,powered=false]{Gas:\"yperite:blue_cross\",Amount:2000f}",
                    "setblock 3 80 3 yperite:gas_cylinder[open=false,powered=false]",
                    "summon yperite:gas_grenade 0.5 80.3 2.5 {Item:{id:\"yperite:chlorine_grenade\",Count:1b},NoGravity:1b}"),
                    CameraType.FIRST_PERSON, 0f, 18f),
            // Only has an effect when Superb Warfare is loaded (-PwithSbw): the items do not exist otherwise.
            new Scene("10_chemical_rounds_for_superb_warfare", List.of(
                    hotbar(0, "grenade_40mm_chlorine"), hotbar(1, "grenade_40mm_nerve_gas"), hotbar(2, "grenade_40mm_yperite"),
                    hotbar(3, "mortar_shell_tear_gas"), hotbar(4, "mortar_shell_blood_gas"), hotbar(5, "mortar_shell_blue_cross"),
                    hotbar(6, "large_shell_chlorine"), hotbar(7, "large_shell_yperite"), hotbar(8, "large_shell_nerve_gas")),
                    CameraType.FIRST_PERSON, 0f, 60f),
            new Scene("9_cloud_from_an_open_cylinder", List.of(
                    "yperite gas burst yperite:chlorine 150 2 0 81 5"), CameraType.FIRST_PERSON, 0f, 12f),
            // every gas side by side, drifting in a light wind: tear gas, chlorine, blood agent, yperite, nerve, Blue Cross
            new Scene("11_clouds_of_every_gas", List.of(
                    "yperite gas clear", "yperite wind set 270 0.3",
                    // take away the cylinders of the scene before (filled first, so that the command never fails)
                    "fill -3 80 3 3 80 4 minecraft:glass", "fill -3 80 3 3 80 4 minecraft:air",
                    "yperite gas burst yperite:tear_gas 260 2 -6 82 5", "yperite gas burst yperite:chlorine 260 2 -3 82 5",
                    "yperite gas burst yperite:blood_gas 260 2 0 82 5", "yperite gas burst yperite:yperite 260 2 3 82 5",
                    "yperite gas burst yperite:nerve_gas 260 2 5 82 2", "yperite gas burst yperite:blue_cross 260 2 6 82 5"),
                    CameraType.FIRST_PERSON, 0f, 8f),
            // standing inside a thick chlorine cloud: the view closes in and turns yellow-green
            new Scene("12_inside_a_chlorine_cloud", List.of(
                    "yperite gas clear", "yperite wind set 0 0",
                    "yperite gas burst yperite:chlorine 400 3 0 81 1"), CameraType.FIRST_PERSON, 0f, 5f),
            // residue on the ground: a low mist lies over it
            new Scene("13_yperite_residue_mist", List.of(
                    "yperite gas clear", "yperite wind set 0 0",
                    "yperite gas contaminate yperite:yperite 1.5 3 0 80 4"), CameraType.FIRST_PERSON, 0f, 40f),
            // a cloud seen from afar (48 blocks): far cells are drawn with fewer, larger puffs
            new Scene("15_cloud_seen_from_afar", List.of(
                    "yperite gas clear", "yperite wind set 0 0",
                    "yperite gas burst yperite:chlorine 400 3 0 82 48"), CameraType.FIRST_PERSON, 0f, 3f),
            // the officer's things and the caps in the hotbar, the stick grenade (with Superb Warfare) held up
            new Scene("16_officer_items", List.of("yperite gas clear",
                    hotbar(0, "stick_grenade"), hotbar(1, "sabre"), hotbar(2, "officer_whistle"),
                    hotbar(3, "british_cap"), hotbar(4, "french_kepi"), hotbar(5, "german_cap"),
                    hotbar(6, "pickelhaube"), hotbar(7, "pickelhaube_covered"), hotbar(8, "austrian_shako")),
                    CameraType.FIRST_PERSON, 0f, 30f),
            // a thrown stick grenade lying on the ground in front (its fuse set long)
            new Scene("17_stick_grenade_on_the_ground", List.of("kill @e[type=yperite:stick_grenade]",
                    "summon yperite:stick_grenade ~0.6 ~0.2 ~2.2 {Life:400,Motion:[0.05d,0.0d,0.0d]}"),
                    CameraType.FIRST_PERSON, 0f, 55f),
            new Scene("14_clear_again", List.of("yperite gas clear", "yperite wind auto", "kill @e[type=yperite:stick_grenade]"), CameraType.FIRST_PERSON, 0f, 5f)));
        return all;
    }

    private static int tick;
    private static int scene = -1;
    private static int sceneStart;
    private static boolean pictureTaken;

    private ClientSmoke() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!ACTIVE || event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        if (mc.player == null || mc.level == null || server == null) {
            return;
        }
        if (mc.player.isDeadOrDying()) {
            // The player of the saved test world may have been left dead by an earlier run.
            mc.player.respawn();
            return;
        }
        tick++;

        if (tick == 40) {
            // The software renderer's light map comes out black, so make everything bright by other means.
            mc.options.gamma().set(1.0);
            // no chat lines from the commands and no tutorial hints over the pictures
            mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            mc.options.chatVisibility().set(net.minecraft.world.entity.player.ChatVisiblity.HIDDEN);
            mc.getToasts().clear();
            run(server, "gamerule sendCommandFeedback false", "gamemode creative", "fill -8 79 -8 8 95 8 minecraft:air", "fill -8 79 -8 8 79 8 minecraft:stone",
                    "fill -8 80 -4 8 90 -4 minecraft:white_concrete", "fill -8 80 6 8 90 6 minecraft:white_concrete",
                    "tp @s 0.5 80 0.5 0 0",
                    "effect give @s minecraft:night_vision infinite 0 true", "time set day", "gamerule doDaylightCycle false", "weather clear");
            server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
            });
        }
        if (tick < 100) {
            return;
        }

        if (scene < 0 || (pictureTaken && tick - sceneStart > SETTLE + 10)) {
            scene++;
            if (scene >= SCENES.size()) {
                System.out.println("[smoke] all scenes done, quitting");
                mc.stop();
                return;
            }
            Scene next = SCENES.get(scene);
            sceneStart = tick;
            pictureTaken = false;
            run(server, next.commands().toArray(new String[0]));
            run(server, String.format(java.util.Locale.ROOT, "tp @s 0.5 80 0.5 %s %s", next.yaw(), next.pitch()));
            mc.options.setCameraType(next.camera());
            mc.setScreen(next.inventory() ? new InventoryScreen(mc.player) : next.showcase() ? showcase(next.name()) : null);
            System.out.println("[smoke] scene " + next.name() + " camera=" + mc.options.getCameraType());
            return;
        }
        if (!pictureTaken && tick - sceneStart >= SETTLE) {
            Scene current = SCENES.get(scene);
            Screenshot.grab(mc.gameDirectory, current.name() + ".png", mc.getMainRenderTarget(), message -> System.out.println("[smoke] " + message.getString()));
            pictureTaken = true;
        }
    }

    /** "show_x" draws four views; "show_x@2" only the third view, large. */
    private static ShowcaseScreen showcase(String name) {
        int at = name.lastIndexOf('@');
        return at < 0 ? new ShowcaseScreen() : new ShowcaseScreen(Integer.parseInt(name.substring(at + 1)), name.contains("_kit"));
    }

    private static void run(MinecraftServer server, String... commands) {
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            for (String command : commands) {
                if (command.startsWith("@curios ")) {
                    SmokeCurios.equip(player, command.substring("@curios ".length()));
                } else {
                    server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(4), command);
                }
            }
        });
    }
}
