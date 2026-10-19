package io.github.katyusha8138.yperite.compat.superbwarfare;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * Superb Warfare's own equipment attribute, looked up by name so that nothing here needs that mod's classes.
 *
 * <p>{@code superbwarfare:bullet_resistance} (0..1) cuts the damage of bullets by 80 % of its value, of
 * armour-piercing rounds by 20 % and of explosions by 30 %. Superb Warfare's own helmets give 0.1 to 0.2 and its
 * vests 0.5, scaled down as the piece wears out; this mod does the same.
 */
@SuppressWarnings("removal") // ResourceLocation(String, String) exists on every Forge 47.x
public final class SbwAttributes {
    private static final ResourceLocation BULLET_RESISTANCE = new ResourceLocation(SbwCompat.MODID, "bullet_resistance");

    private SbwAttributes() {
    }

    /** The attribute, or null when Superb Warfare is not installed. */
    @Nullable
    public static Attribute bulletResistance() {
        return ForgeRegistries.ATTRIBUTES.containsKey(BULLET_RESISTANCE) ? ForgeRegistries.ATTRIBUTES.getValue(BULLET_RESISTANCE) : null;
    }
}
