package io.github.katyusha8138.yperite.item.armor;

import io.github.katyusha8138.yperite.YperiteMod;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;

import java.util.EnumMap;
import java.util.function.Supplier;

/**
 * Armour materials of the First World War equipment. Defence per slot (helmet, chest, legs, boots), durability per
 * slot, toughness and knockback resistance. Superb Warfare's bullet resistance is set per item, not here.
 */
public enum WarArmorMaterials implements ArmorMaterial {
    /** Pressed steel helmets: like iron against blows, and they last. */
    STEEL_HELMET("steel_helmet", slots(2, 0, 0, 0), slots(220, 0, 0, 0), 9, SoundEvents.ARMOR_EQUIP_IRON, 0.5f, 0f,
            () -> Ingredient.of(Items.IRON_INGOT)),
    /** Trench armour: thick plates over the chest and belly; heavy, and slows the wearer down (see the item). */
    TRENCH_ARMOR("trench_armor", slots(0, 6, 0, 0), slots(0, 320, 0, 0), 5, SoundEvents.ARMOR_EQUIP_IRON, 1.5f, 0.1f,
            () -> Ingredient.of(Items.IRON_INGOT)),
    /** A body shield: steel plates sewn into cloth; lighter than trench armour and less protective. */
    BODY_SHIELD("body_shield", slots(0, 4, 0, 0), slots(0, 240, 0, 0), 8, SoundEvents.ARMOR_EQUIP_CHAIN, 0.5f, 0.05f,
            () -> Ingredient.of(Items.IRON_INGOT)),
    /** Headgear of leather and felt (the Pickelhaube, the shako): a little more than a cloth cap, far less than steel. */
    LEATHER_HEADGEAR("leather_headgear", slots(1, 0, 0, 0), slots(110, 0, 0, 0), 15, SoundEvents.ARMOR_EQUIP_LEATHER, 0f, 0f,
            () -> Ingredient.of(Items.LEATHER)),
    /** Wool uniforms and cloth caps: little protection, like leather. */
    WOOL_UNIFORM("wool_uniform", slots(1, 3, 2, 1), slots(70, 100, 90, 80), 15, SoundEvents.ARMOR_EQUIP_LEATHER, 0f, 0f,
            () -> Ingredient.of(Blocks.WHITE_WOOL));

    private final String name;
    private final EnumMap<ArmorItem.Type, Integer> defense;
    private final EnumMap<ArmorItem.Type, Integer> durability;
    private final int enchantability;
    private final SoundEvent sound;
    private final float toughness;
    private final float knockbackResistance;
    private final Supplier<Ingredient> repair;

    WarArmorMaterials(String name, EnumMap<ArmorItem.Type, Integer> defense, EnumMap<ArmorItem.Type, Integer> durability,
                      int enchantability, SoundEvent sound, float toughness, float knockbackResistance, Supplier<Ingredient> repair) {
        this.name = name;
        this.defense = defense;
        this.durability = durability;
        this.enchantability = enchantability;
        this.sound = sound;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.repair = repair;
    }

    private static EnumMap<ArmorItem.Type, Integer> slots(int helmet, int chestplate, int leggings, int boots) {
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
        return defense.get(type);
    }

    @Override
    public int getEnchantmentValue() {
        return enchantability;
    }

    @Override
    public SoundEvent getEquipSound() {
        return sound;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return repair.get();
    }

    @Override
    public String getName() {
        return YperiteMod.MODID + ":" + name;
    }

    @Override
    public float getToughness() {
        return toughness;
    }

    @Override
    public float getKnockbackResistance() {
        return knockbackResistance;
    }
}
