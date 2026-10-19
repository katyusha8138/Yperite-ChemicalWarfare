package io.github.katyusha8138.yperite.compat.superbwarfare;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adds chemical ammunition to Superb Warfare's gun and vehicle definitions while they load, by editing their JSON.
 * Its own files are never touched, and a data pack that replaces one of them is patched the same way.
 *
 * <p>Superb Warfare lets a weapon list several kinds of ammunition, each with its own projectile and property
 * changes. A chemical entry names this mod's item as ammunition and gives the projectile a mark (see
 * {@link ChemicalRound}) that the hit listener turns into gas. Works on plain JSON text so it can be tested
 * without the game.
 */
public final class SbwDataPatcher {
    private static final String AMMO_40MM = "superbwarfare:grenade_40mm";
    private static final String LARGE_SHELL_PREFIX = "superbwarfare:large_shell_";
    private static final String DEFAULT_40MM_PROJECTILE = "superbwarfare:gun_grenade";
    private static final String DEFAULT_CANNON_PROJECTILE = "superbwarfare:cannon_shell";
    private static final String CANNON_ICON = "superbwarfare:textures/overlay/vehicle/weapon/icons/he_shell.png";
    private static final String CANNON_NAME_KEY = "weapon.yperite.cannon_chemical";
    private static final String OWN_NAMESPACE = "yperite:";

    /** The vehicle data files of the artillery pieces that get chemical shells. */
    public static final Set<String> ARTILLERY = Set.of(
            "superbwarfare:mk_42", "superbwarfare:mle_1934", "superbwarfare:fh_77bw", "superbwarfare:plz_05");

    /**
     * What one kind of chemical round does when it hits.
     *
     * @param itemPrefix      the item name before the gas, for example {@code grenade_40mm}
     * @param dataName        the value for the projectile's round mark
     * @param impactDamage    damage of the round hitting something directly
     * @param explosionDamage damage of the small explosion; 0 for none
     * @param explosionRadius radius of that explosion
     */
    public record Spec(String itemPrefix, String dataName, float impactDamage, float explosionDamage, float explosionRadius) {
    }

    private SbwDataPatcher() {
    }

    /**
     * @param id       the data id, for example {@code superbwarfare:m_79}
     * @param gasIds   ids of the gases to make ammunition for, for example {@code yperite:chlorine}
     * @return the patched JSON text, or null when this file needs no change
     */
    @Nullable
    public static String patch(String id, String json, List<String> gasIds, Spec grenade40mm, Spec largeShell) {
        JsonObject root;
        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (!parsed.isJsonObject()) {
                return null;
            }
            root = parsed.getAsJsonObject();
        } catch (RuntimeException e) {
            return null;
        }

        boolean changed = false;
        if (root.has("AmmoType") && usesAmmo(root.get("AmmoType"), AMMO_40MM)) {
            changed |= addEntries(root, "AmmoType", gasIds, grenade40mm, projectileType(root.get("Projectile"), DEFAULT_40MM_PROJECTILE),
                    projectileData(root.get("Projectile")), null);
        }
        if (ARTILLERY.contains(id) && root.has("Weapons") && root.get("Weapons").isJsonObject()) {
            for (Map.Entry<String, JsonElement> weapon : root.getAsJsonObject("Weapons").entrySet()) {
                if (weapon.getValue().isJsonObject() && weapon.getValue().getAsJsonObject().has("AmmoType")
                        && usesAmmo(weapon.getValue().getAsJsonObject().get("AmmoType"), LARGE_SHELL_PREFIX)) {
                    JsonObject definition = weapon.getValue().getAsJsonObject();
                    changed |= addEntries(definition, "AmmoType", gasIds, largeShell,
                            projectileType(definition.get("Projectile"), DEFAULT_CANNON_PROJECTILE),
                            projectileData(definition.get("Projectile")), CANNON_ICON);
                }
            }
        }
        return changed ? root.toString() : null;
    }

    /** Whether some ammunition entry starts with the given name. */
    private static boolean usesAmmo(JsonElement ammoType, String namePrefix) {
        for (JsonElement entry : asList(ammoType)) {
            if (ammoName(entry).startsWith(namePrefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean addEntries(JsonObject owner, String key, List<String> gasIds, Spec spec, String projectileType,
                                      @Nullable JsonObject baseData, @Nullable String cannonIcon) {
        JsonArray list = new JsonArray();
        for (JsonElement entry : asList(owner.get(key))) {
            if (ammoName(entry).startsWith(OWN_NAMESPACE + spec.itemPrefix() + "_")) {
                return false; // already patched
            }
            list.add(entry);
        }
        for (String gasId : gasIds) {
            list.add(entry(spec, gasId, projectileType, baseData, cannonIcon));
        }
        owner.add(key, list);
        return true;
    }

    private static JsonObject entry(Spec spec, String gasId, String projectileType, @Nullable JsonObject baseData, @Nullable String cannonIcon) {
        JsonObject data = baseData == null ? new JsonObject() : baseData.deepCopy();
        JsonObject mark = new JsonObject();
        mark.addProperty(ChemicalRound.TAG_GAS, gasId);
        mark.addProperty(ChemicalRound.TAG_ROUND, spec.dataName());
        data.add("ForgeData", mark);
        data.addProperty("ExplosionDestroy", false);

        JsonObject projectile = new JsonObject();
        projectile.addProperty("Type", projectileType);
        projectile.add("Data", data);

        JsonObject override = new JsonObject();
        override.addProperty("Damage", spec.impactDamage());
        override.addProperty("ExplosionDamage", spec.explosionDamage());
        override.addProperty("ExplosionRadius", spec.explosionRadius());
        if (cannonIcon != null) {
            override.addProperty("Icon", cannonIcon);
            override.addProperty("ShellType", "HE");
            override.addProperty("Name", CANNON_NAME_KEY);
        }

        JsonObject entry = new JsonObject();
        entry.addProperty("Ammo", OWN_NAMESPACE + spec.itemPrefix() + "_" + gasId.substring(gasId.indexOf(':') + 1));
        entry.addProperty("AmmoSlot", "Default");
        entry.add("Projectile", projectile);
        entry.add("Override", override);
        return entry;
    }

    /** {@code "AmmoType"} can be a single string or object, or a list of them. */
    private static JsonArray asList(@Nullable JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return new JsonArray();
        }
        if (element.isJsonArray()) {
            return element.getAsJsonArray();
        }
        JsonArray single = new JsonArray();
        single.add(element);
        return single;
    }

    /** The item an ammunition entry names, without the leading count ("1 mod:item"). */
    private static String ammoName(JsonElement entry) {
        String raw = null;
        if (entry.isJsonPrimitive()) {
            raw = entry.getAsString();
        } else if (entry.isJsonObject() && entry.getAsJsonObject().has("Ammo") && entry.getAsJsonObject().get("Ammo").isJsonPrimitive()) {
            raw = entry.getAsJsonObject().get("Ammo").getAsString();
        }
        if (raw == null) {
            return "";
        }
        int start = 0;
        while (start < raw.length() && (Character.isDigit(raw.charAt(start)) || Character.isWhitespace(raw.charAt(start)))) {
            start++;
        }
        return raw.substring(start).trim();
    }

    private static String projectileType(@Nullable JsonElement projectile, String fallback) {
        if (projectile != null && projectile.isJsonPrimitive()) {
            return projectile.getAsString();
        }
        if (projectile != null && projectile.isJsonObject() && projectile.getAsJsonObject().has("Type")) {
            return projectile.getAsJsonObject().get("Type").getAsString();
        }
        return fallback;
    }

    @Nullable
    private static JsonObject projectileData(@Nullable JsonElement projectile) {
        if (projectile != null && projectile.isJsonObject()) {
            JsonElement data = projectile.getAsJsonObject().get("Data");
            if (data != null && data.isJsonObject()) {
                return data.getAsJsonObject();
            }
        }
        return null;
    }
}
