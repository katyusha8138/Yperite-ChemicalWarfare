package io.github.katyusha8138.yperite.item;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

/** Tiers of the First World War's edged weapons. */
public enum WarTiers implements Tier {
    /**
     * An officer's sabre: a light, finely ground blade that cuts deep but does not last; it wears out sooner than
     * any vanilla sword but a wooden or golden one. Mines like iron.
     */
    SABRE(100, 6.0f, 0f, 2, 14),
    /** A cavalry lance: an ash shaft and a steel head, as tough as an iron sword. */
    LANCE(250, 2.0f, 0f, 1, 14);

    private final int uses;
    private final float speed;
    private final float damage;
    private final int level;
    private final int enchantability;

    WarTiers(int uses, float speed, float damage, int level, int enchantability) {
        this.uses = uses;
        this.speed = speed;
        this.damage = damage;
        this.level = level;
        this.enchantability = enchantability;
    }

    @Override
    public int getUses() {
        return uses;
    }

    @Override
    public float getSpeed() {
        return speed;
    }

    @Override
    public float getAttackDamageBonus() {
        return damage;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getLevel() {
        return level;
    }

    @Override
    public int getEnchantmentValue() {
        return enchantability;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(Items.IRON_INGOT);
    }
}
