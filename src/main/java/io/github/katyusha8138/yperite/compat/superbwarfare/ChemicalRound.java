package io.github.katyusha8138.yperite.compat.superbwarfare;

import io.github.katyusha8138.yperite.config.YperiteConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The kinds of Superb Warfare ammunition that this mod fills with gas. Nothing here touches Superb Warfare itself, so
 * the class can be loaded whether or not that mod is installed.
 *
 * <p>A chemical round is an ordinary projectile of the host mod that carries a small mark in its persistent data
 * ({@link #TAG_GAS}, {@link #TAG_ROUND}). The mark is what the hit listener looks for.
 */
public enum ChemicalRound {
    /** The 40mm grenade of the M79 and of every gun that uses the same ammunition. */
    GRENADE_40MM("grenade_40mm", 16, 20f, 25f, 2f),
    /** A mortar shell, fired from Superb Warfare's mortar. */
    MORTAR_SHELL("mortar_shell", 8, 20f, 40f, 4f),
    /** A large cannon shell, for the artillery pieces. */
    LARGE_SHELL("large_shell", 8, 40f, 60f, 5f);

    /** Persistent-data key: the id of the gas the projectile carries. */
    public static final String TAG_GAS = "yperite_gas";
    /** Persistent-data key: the name of the {@link ChemicalRound} it is. */
    public static final String TAG_ROUND = "yperite_round";
    /** Persistent-data key: set once the gas has been released, so that a projectile never releases twice. */
    public static final String TAG_RELEASED = "yperite_released";

    /** Every chemical round item, once they are registered (empty without Superb Warfare). */
    public static final List<Supplier<Item>> ITEMS = new ArrayList<>();

    private final String prefix;
    private final int stackSize;
    private final float impactDamage;
    private final float explosionDamage;
    private final float explosionRadius;

    ChemicalRound(String prefix, int stackSize, float impactDamage, float explosionDamage, float explosionRadius) {
        this.prefix = prefix;
        this.stackSize = stackSize;
        this.impactDamage = impactDamage;
        this.explosionDamage = explosionDamage;
        this.explosionRadius = explosionRadius;
    }

    /** The item name before the gas, for example {@code grenade_40mm}. */
    public String itemPrefix() {
        return prefix;
    }

    /** The registry name of the item for this round and gas, for example {@code grenade_40mm_chlorine}. */
    public String itemName(ResourceLocation gasId) {
        return prefix + "_" + gasId.getPath();
    }

    public int stackSize() {
        return stackSize;
    }

    /** Damage of the round hitting something directly. Small: the point of the round is its gas. */
    public float impactDamage() {
        return impactDamage;
    }

    /** Damage of the small explosion, 0 when the explosion is turned off. */
    public float explosionDamage() {
        return explosionDamage * YperiteConfig.SBW_EXPLOSION_SCALE.get().floatValue();
    }

    public float explosionRadius() {
        return explosionDamage() > 0f ? explosionRadius : 0f;
    }

    public float gasMass() {
        return switch (this) {
            case GRENADE_40MM -> YperiteConfig.SBW_GRENADE_MASS.get().floatValue();
            case MORTAR_SHELL -> YperiteConfig.SBW_MORTAR_MASS.get().floatValue();
            case LARGE_SHELL -> YperiteConfig.SBW_LARGE_MASS.get().floatValue();
        };
    }

    public float gasRadius() {
        return switch (this) {
            case GRENADE_40MM -> YperiteConfig.SBW_GRENADE_RADIUS.get().floatValue();
            case MORTAR_SHELL -> YperiteConfig.SBW_MORTAR_RADIUS.get().floatValue();
            case LARGE_SHELL -> YperiteConfig.SBW_LARGE_RADIUS.get().floatValue();
        };
    }

    /** The name written into projectile data. */
    public String dataName() {
        return name();
    }

    @Nullable
    public static ChemicalRound byDataName(String name) {
        for (ChemicalRound round : values()) {
            if (round.name().equals(name)) {
                return round;
            }
        }
        return null;
    }
}
