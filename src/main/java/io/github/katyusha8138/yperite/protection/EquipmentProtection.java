package io.github.katyusha8138.yperite.protection;

import io.github.katyusha8138.yperite.config.YperiteConfig;
import io.github.katyusha8138.yperite.exposure.ProtectionRegistry;
import io.github.katyusha8138.yperite.gas.GasCategory;
import io.github.katyusha8138.yperite.item.GasMaskItem;
import io.github.katyusha8138.yperite.item.ProtectiveArmorItem;
import io.github.katyusha8138.yperite.protection.compat.CuriosCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;

/**
 * Connects what a living thing wears to the dose model: the mask and its filter against breathing in gas, the
 * protective outfit against gas on the skin. Both wear out by the dose they take.
 */
public final class EquipmentProtection implements ProtectionRegistry.Provider {
    private static final String TAG_WEAR = "Wear";
    private static final EquipmentSlot[] SUIT_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    /** Durability points a suit part loses per exposure unit that reaches the whole body. */
    private static final float SUIT_WEAR_PER_UNIT = 4f;

    /** The mask being worn, in a Curios face slot or in the helmet slot; empty if there is none. */
    public static ItemStack findMask(LivingEntity entity) {
        if (CuriosCompat.isLoaded()) {
            ItemStack curio = CuriosCompat.findMask(entity);
            if (!curio.isEmpty()) {
                return curio;
            }
        }
        ItemStack head = entity.getItemBySlot(EquipmentSlot.HEAD);
        return head.getItem() instanceof GasMaskItem ? head : ItemStack.EMPTY;
    }

    /** The parts of the outfit that are worn right now. */
    public static EnumSet<SuitProtection.Part> wornParts(LivingEntity entity, ItemStack mask) {
        EnumSet<SuitProtection.Part> worn = EnumSet.noneOf(SuitProtection.Part.class);
        if (!mask.isEmpty()) {
            worn.add(SuitProtection.Part.MASK);
        }
        for (EquipmentSlot slot : SUIT_SLOTS) {
            if (entity.getItemBySlot(slot).getItem() instanceof ProtectiveArmorItem suit) {
                worn.add(suit.part());
            }
        }
        return worn;
    }

    @Override
    public ProtectionRegistry.Protection protection(LivingEntity entity, GasCategory category) {
        ItemStack mask = findMask(entity);
        float inhale = 1f;
        if (!mask.isEmpty()) {
            inhale = MaskProtection.inhalePass(GasMaskItem.filterKind(mask), GasMaskItem.charge(mask), category);
        }
        float skin = SuitProtection.skinPass(wornParts(entity, mask));
        return new ProtectionRegistry.Protection(inhale, skin);
    }

    @Override
    public void onDose(LivingEntity entity, GasCategory category, float rawInhaled, float rawContact) {
        if (rawInhaled > 0f) {
            ItemStack mask = findMask(entity);
            if (!mask.isEmpty() && GasMaskItem.filterKind(mask) != null) {
                float wear = rawInhaled * YperiteConfig.FILTER_WEAR.get().floatValue();
                GasMaskItem.setCharge(mask, MaskProtection.wear(GasMaskItem.charge(mask), wear));
            }
        }
        if (rawContact > 0f) {
            wearSuit(entity, rawContact * YperiteConfig.SUIT_WEAR.get().floatValue());
        }
    }

    /** Each worn part loses durability in proportion to the share of the body it covers. */
    private static void wearSuit(LivingEntity entity, float contact) {
        for (EquipmentSlot slot : SUIT_SLOTS) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!(stack.getItem() instanceof ProtectiveArmorItem suit)) {
                continue;
            }
            // Collect fractions in the item's data until they add up to a whole point.
            CompoundTag tag = stack.getOrCreateTag();
            float wear = tag.getFloat(TAG_WEAR) + contact * suit.part().coverage() * SUIT_WEAR_PER_UNIT;
            int whole = (int) wear;
            tag.putFloat(TAG_WEAR, wear - whole);
            if (whole > 0) {
                stack.hurtAndBreak(whole, entity, broken -> broken.broadcastBreakEvent(slot));
            }
        }
    }
}
