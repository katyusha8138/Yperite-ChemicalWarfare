package io.github.katyusha8138.yperite.item;

import io.github.katyusha8138.yperite.item.armor.GeoArmorItem;
import io.github.katyusha8138.yperite.protection.SuitProtection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A piece of the protective outfit, drawn with its GeckoLib model (named like the item). It keeps gas off the skin
 * and loses durability as gas reaches it. The hood goes over a mask worn in the Curios face slot; body armour and a
 * respirator's haversack go over the jacket.
 */
public class ProtectiveArmorItem extends GeoArmorItem {
    private final SuitProtection.Part part;

    public ProtectiveArmorItem(Type type, SuitProtection.Part part, String model, Properties properties) {
        super(ModArmorMaterials.PROTECTIVE_SUIT, type, properties, model, 0.0, 0.0);
        this.part = part;
    }

    public SuitProtection.Part part() {
        return part;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.yperite.suit.covers", Math.round(part.coverage() * 100f)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.yperite.suit.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
