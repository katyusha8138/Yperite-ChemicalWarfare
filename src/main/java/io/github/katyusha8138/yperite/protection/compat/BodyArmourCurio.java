package io.github.katyusha8138.yperite.protection.compat;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.github.katyusha8138.yperite.item.ModItems;
import io.github.katyusha8138.yperite.item.armor.GeoArmorItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.UUID;

/**
 * Body armour in the Curios body slot: it gives the chest slot's attributes (armour, toughness, bullet
 * resistance, the weight), keyed to the Curios slot. Only loaded when Curios is installed (see {@link CuriosCompat}).
 */
final class BodyArmourCurio implements ICurioItem {
    static void registerAll() {
        ModItems.BODY_ARMOUR.forEach(armour -> CuriosApi.registerCurio(armour.get(), new BodyArmourCurio()));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> out = HashMultimap.create();
        if (stack.getItem() instanceof GeoArmorItem armour) {
            armour.getAttributeModifiers(EquipmentSlot.CHEST, stack).forEach((attribute, modifier) -> out.put(attribute,
                    new AttributeModifier(uuid, modifier.getName(), modifier.getAmount(), modifier.getOperation())));
        }
        return out;
    }

    /** One set of body armour at a time: not over armour already worn in the chest slot. */
    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        ItemStack chest = slotContext.entity().getItemBySlot(EquipmentSlot.CHEST);
        return ModItems.BODY_ARMOUR.stream().noneMatch(a -> a.get() == chest.getItem());
    }
}
