package io.github.katyusha8138.yperite.item;

import io.github.katyusha8138.yperite.protection.FilterKind;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** A filter for the gas mask. It does nothing on its own: combine it with a mask in a crafting grid. */
public class GasFilterItem extends Item {
    private final FilterKind kind;

    public GasFilterItem(FilterKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public FilterKind kind() {
        return kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.yperite.gas_filter." + kind.id()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.yperite.gas_filter.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
