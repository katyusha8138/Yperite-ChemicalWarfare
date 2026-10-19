package io.github.katyusha8138.yperite.item;

import io.github.katyusha8138.yperite.protection.FilterKind;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The gas mask. It sits in the helmet slot, or in the Curios "face" slot when Curios is installed. A filter is
 * loaded by crafting; the filter's kind and what is left of its charge live in the item's data, and the bar shows
 * the charge.
 */
public class GasMaskItem extends ArmorItem {
    private static final String TAG_FILTER = "Filter";
    private static final String TAG_KIND = "Kind";
    private static final String TAG_CHARGE = "Charge";

    public GasMaskItem(Properties properties) {
        super(ModArmorMaterials.GAS_MASK, Type.HELMET, properties);
    }

    // ------------------------------------------------------------------ the loaded filter

    @Nullable
    public static FilterKind filterKind(ItemStack mask) {
        CompoundTag filter = mask.getTagElement(TAG_FILTER);
        return filter == null ? null : FilterKind.byId(filter.getString(TAG_KIND));
    }

    /** What is left of the filter's charge; 0 without a filter. */
    public static float charge(ItemStack mask) {
        CompoundTag filter = mask.getTagElement(TAG_FILTER);
        return filter == null ? 0f : filter.getFloat(TAG_CHARGE);
    }

    /** Loads a fresh filter, replacing whatever was in the mask. */
    public static void load(ItemStack mask, FilterKind kind) {
        CompoundTag filter = mask.getOrCreateTagElement(TAG_FILTER);
        filter.putString(TAG_KIND, kind.id());
        filter.putFloat(TAG_CHARGE, kind.capacity());
    }

    public static void setCharge(ItemStack mask, float charge) {
        CompoundTag filter = mask.getTagElement(TAG_FILTER);
        if (filter != null) {
            filter.putFloat(TAG_CHARGE, Math.max(0f, charge));
        }
    }

    /** A mask with a fresh filter loaded, for the creative tab. */
    public static ItemStack withFilter(ItemStack mask, FilterKind kind) {
        ItemStack copy = mask.copy();
        load(copy, kind);
        return copy;
    }

    // ------------------------------------------------------------------ filter bar and tooltip

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return filterKind(stack) != null || super.isBarVisible(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        FilterKind kind = filterKind(stack);
        if (kind == null) {
            return super.getBarWidth(stack);
        }
        return Math.round(13f * Math.min(1f, charge(stack) / kind.capacity()));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        FilterKind kind = filterKind(stack);
        if (kind == null) {
            return super.getBarColor(stack);
        }
        return charge(stack) / kind.capacity() > 0.2f ? 0x3FA7D6 : 0xD64040;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        FilterKind kind = filterKind(stack);
        if (kind == null) {
            tooltip.add(Component.translatable("tooltip.yperite.gas_mask.no_filter").withStyle(ChatFormatting.RED));
        } else {
            int percent = Math.round(100f * Math.min(1f, charge(stack) / kind.capacity()));
            tooltip.add(Component.translatable("tooltip.yperite.gas_mask.filter",
                    Component.translatable("item.yperite.gas_filter_" + kind.id()), percent).withStyle(ChatFormatting.GRAY));
            if (charge(stack) <= 0f) {
                tooltip.add(Component.translatable("tooltip.yperite.gas_mask.exhausted").withStyle(ChatFormatting.RED));
            }
        }
        tooltip.add(Component.translatable("tooltip.yperite.gas_mask.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
