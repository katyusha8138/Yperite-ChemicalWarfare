package io.github.katyusha8138.yperite.item;

import io.github.katyusha8138.yperite.YperiteMod;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.function.Supplier;

/**
 * Armour materials of the mod. They give almost no protection against blows: their job is gas, which is handled in
 * the {@code protection} package. The durability of the suit is what its contamination wears down.
 */
public enum ModArmorMaterials implements ArmorMaterial {
    GAS_MASK("gas_mask", 1, durability(0, 0, 0, 90), 5, () -> Ingredient.of(Items.LEATHER)),
    PROTECTIVE_SUIT("protective_suit", 1, durability(80, 160, 140, 120), 5, () -> Ingredient.of(Items.LEATHER));

    private final String name;
    private final int defense;
    private final EnumMap<ArmorItem.Type, Integer> durability;
    private final int enchantability;
    private final Supplier<Ingredient> repair;

    ModArmorMaterials(String name, int defense, EnumMap<ArmorItem.Type, Integer> durability, int enchantability, Supplier<Ingredient> repair) {
        this.name = name;
        this.defense = defense;
        this.durability = durability;
        this.enchantability = enchantability;
        this.repair = repair;
    }

    private static EnumMap<ArmorItem.Type, Integer> durability(int helmet, int chestplate, int leggings, int boots) {
        EnumMap<ArmorItem.Type, Integer> map = new EnumMap<>(ArmorItem.Type.class);
        map.put(ArmorItem.Type.HELMET, helmet);
        map.put(ArmorItem.Type.CHESTPLATE, chestplate);
        map.put(ArmorItem.Type.LEGGINGS, leggings);
        map.put(ArmorItem.Type.BOOTS, boots);
        return map;
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return durability.get(type);
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return defense;
    }

    @Override
    public int getEnchantmentValue() {
        return enchantability;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_LEATHER;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return repair.get();
    }

    /** With the namespace, so that the texture is looked up in this mod's assets. */
    @Override
    public String getName() {
        return YperiteMod.MODID + ":" + name;
    }

    @Override
    public float getToughness() {
        return 0f;
    }

    @Override
    public float getKnockbackResistance() {
        return 0f;
    }
}
