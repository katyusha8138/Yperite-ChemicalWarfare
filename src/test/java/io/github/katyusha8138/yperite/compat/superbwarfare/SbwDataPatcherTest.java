package io.github.katyusha8138.yperite.compat.superbwarfare;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.katyusha8138.yperite.compat.superbwarfare.SbwDataPatcher.Spec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The files below are small stand-ins that only have the fields the patcher reads; they are not copies of any real data. */
class SbwDataPatcherTest {
    private static final List<String> GASES = List.of("yperite:tear_gas", "yperite:chlorine");
    private static final Spec GRENADE = new Spec("grenade_40mm", "GRENADE_40MM", 20f, 25f, 2f);
    private static final Spec LARGE = new Spec("large_shell", "LARGE_SHELL", 40f, 60f, 5f);

    private static String patch(String id, String json) {
        return SbwDataPatcher.patch(id, json, GASES, GRENADE, LARGE);
    }

    private static JsonObject parse(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    private static JsonObject mortarlessGun() {
        return parse("""
                {"Damage": 100, "Projectile": "superbwarfare:gun_grenade", "AmmoType": "superbwarfare:grenade_40mm", "Magazine": 1}
                """);
    }

    @Test
    void addsOneEntryPerGasToA40mmLauncherAndKeepsTheOriginalAmmunition() {
        String patched = patch("superbwarfare:m_79", mortarlessGun().toString());
        assertNotNull(patched);
        JsonObject gun = parse(patched);

        JsonArray ammo = gun.getAsJsonArray("AmmoType");
        assertEquals(3, ammo.size());
        assertEquals("superbwarfare:grenade_40mm", ammo.get(0).getAsString());
        JsonObject chlorine = ammo.get(2).getAsJsonObject();
        assertEquals("yperite:grenade_40mm_chlorine", chlorine.get("Ammo").getAsString());
        assertEquals("Default", chlorine.get("AmmoSlot").getAsString());

        JsonObject projectile = chlorine.getAsJsonObject("Projectile");
        assertEquals("superbwarfare:gun_grenade", projectile.get("Type").getAsString());
        JsonObject data = projectile.getAsJsonObject("Data");
        assertEquals("yperite:chlorine", data.getAsJsonObject("ForgeData").get("yperite_gas").getAsString());
        assertEquals("GRENADE_40MM", data.getAsJsonObject("ForgeData").get("yperite_round").getAsString());
        assertFalse(data.get("ExplosionDestroy").getAsBoolean(), "a chemical round must not break blocks");

        JsonObject override = chlorine.getAsJsonObject("Override");
        assertEquals(20f, override.get("Damage").getAsFloat());
        assertEquals(25f, override.get("ExplosionDamage").getAsFloat());
        assertEquals(2f, override.get("ExplosionRadius").getAsFloat());

        assertEquals(100, gun.get("Damage").getAsInt(), "the rest of the gun must be left alone");
    }

    @Test
    void patchingTwiceAddsNothingMore() {
        String once = patch("superbwarfare:m_79", mortarlessGun().toString());
        assertNull(patch("superbwarfare:m_79", once));
    }

    @Test
    void recognisesA40mmLauncherByItsAmmunitionNotByName() {
        assertNotNull(patch("somemod:grenade_launcher", mortarlessGun().toString()));
    }

    @Test
    void handlesASingleObjectAndACountInFrontOfTheAmmunitionName() {
        JsonObject gun = parse("""
                {"Projectile": {"Type": "superbwarfare:gun_grenade", "Data": {"owner": "@sbw:owner"}},
                 "AmmoType": {"Ammo": "1 superbwarfare:grenade_40mm", "AmmoSlot": "Default"}}
                """);
        JsonObject patched = parse(patch("superbwarfare:secondary", gun.toString()));
        JsonArray ammo = patched.getAsJsonArray("AmmoType");
        assertEquals(3, ammo.size());
        assertEquals("1 superbwarfare:grenade_40mm", ammo.get(0).getAsJsonObject().get("Ammo").getAsString());
        JsonObject data = ammo.get(1).getAsJsonObject().getAsJsonObject("Projectile").getAsJsonObject("Data");
        assertEquals("@sbw:owner", data.get("owner").getAsString(), "the weapon's own projectile data must be kept");
        assertTrue(data.has("ForgeData"));
        assertFalse(patched.getAsJsonObject("Projectile").getAsJsonObject("Data").has("ForgeData"), "the original must not be changed");
    }

    @Test
    void leavesOtherWeaponsAlone() {
        assertNull(patch("superbwarfare:m_4", "{\"AmmoType\": \"@RifleAmmo\", \"Projectile\": \"superbwarfare:projectile\"}"));
        assertNull(patch("superbwarfare:no_ammo", "{\"Damage\": 5}"));
    }

    private static String artillery() {
        return """
                {"Weapons": {
                  "Main": {"Projectile": "superbwarfare:cannon_shell",
                           "AmmoType": ["superbwarfare:large_shell_ap",
                                        {"Ammo": "superbwarfare:large_shell_he", "Override": {"ShellType": "HE"}}]},
                  "MachineGun": {"AmmoType": "@HeavyAmmo"}
                }}
                """;
    }

    @Test
    void addsChemicalShellsToTheMainGunOfAnArtilleryPiece() {
        JsonObject vehicle = parse(patch("superbwarfare:mk_42", artillery()));
        JsonObject weapons = vehicle.getAsJsonObject("Weapons");

        JsonArray ammo = weapons.getAsJsonObject("Main").getAsJsonArray("AmmoType");
        assertEquals(4, ammo.size());
        JsonObject entry = ammo.get(3).getAsJsonObject();
        assertEquals("yperite:large_shell_chlorine", entry.get("Ammo").getAsString());
        assertEquals("superbwarfare:cannon_shell", entry.getAsJsonObject("Projectile").get("Type").getAsString());
        JsonObject override = entry.getAsJsonObject("Override");
        assertEquals("HE", override.get("ShellType").getAsString());
        assertEquals(60f, override.get("ExplosionDamage").getAsFloat());
        assertEquals(5f, override.get("ExplosionRadius").getAsFloat());
        assertEquals("weapon.yperite.cannon_chemical", override.get("Name").getAsString());

        assertEquals("@HeavyAmmo", weapons.getAsJsonObject("MachineGun").get("AmmoType").getAsString(), "weapons without large shells are left alone");
    }

    @Test
    void everyArtilleryPieceIsPatchedAndNothingElse() {
        for (String id : SbwDataPatcher.ARTILLERY) {
            assertNotNull(patch(id, artillery()), id);
        }
        assertEquals(4, SbwDataPatcher.ARTILLERY.size());
        assertNull(patch("superbwarfare:t_90a", artillery()), "tanks do not get chemical shells");
        assertNull(patch("superbwarfare:mk_42", patch("superbwarfare:mk_42", artillery())), "patching twice adds nothing");
    }

    @Test
    void asksForNoExplosionWhenTheExplosionIsScaledToZero() {
        Spec noBang = new Spec("grenade_40mm", "GRENADE_40MM", 20f, 0f, 0f);
        JsonObject gun = parse(SbwDataPatcher.patch("superbwarfare:m_79", mortarlessGun().toString(), GASES, noBang, LARGE));
        JsonObject override = gun.getAsJsonArray("AmmoType").get(1).getAsJsonObject().getAsJsonObject("Override");
        assertEquals(0f, override.get("ExplosionDamage").getAsFloat());
    }

    @Test
    void ignoresTextThatIsNotAnObject() {
        assertNull(patch("superbwarfare:m_79", "not json at all {"));
        assertNull(patch("superbwarfare:m_79", "[1, 2, 3]"));
        JsonElement empty = JsonParser.parseString("{}");
        assertNull(patch("superbwarfare:m_79", empty.toString()));
    }
}
