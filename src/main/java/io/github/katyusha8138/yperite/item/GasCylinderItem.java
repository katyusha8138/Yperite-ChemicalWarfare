package io.github.katyusha8138.yperite.item;

import io.github.katyusha8138.yperite.block.GasCylinderBlockEntity;
import io.github.katyusha8138.yperite.config.YperiteConfig;
import io.github.katyusha8138.yperite.gas.GasType;
import io.github.katyusha8138.yperite.gas.GasTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The cylinder as an item. What it holds travels in the usual {@code BlockEntityTag}, so a placed cylinder starts
 * with that gas and a broken one gives it back (see the block's loot table).
 */
public class GasCylinderItem extends BlockItem {
    public GasCylinderItem(Block block, Properties properties) {
        super(block, properties);
    }

    /** A cylinder holding {@code amount} of the gas. */
    public static ItemStack filled(GasType type, float amount) {
        ItemStack stack = new ItemStack(ModItems.GAS_CYLINDER.get());
        CompoundTag entity = new CompoundTag();
        entity.putString(GasCylinderBlockEntity.TAG_GAS, String.valueOf(type.id()));
        entity.putFloat(GasCylinderBlockEntity.TAG_AMOUNT, amount);
        stack.getOrCreateTag().put(BlockItem.BLOCK_ENTITY_TAG, entity);
        return stack;
    }

    /** A full cylinder, by the configured capacity. */
    public static ItemStack full(GasType type) {
        return filled(type, YperiteConfig.CYLINDER_CAPACITY.get().floatValue());
    }

    @Nullable
    public static GasType contentsOf(ItemStack stack) {
        CompoundTag entity = stack.getTagElement(BlockItem.BLOCK_ENTITY_TAG);
        if (entity == null || !entity.contains(GasCylinderBlockEntity.TAG_GAS)) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(entity.getString(GasCylinderBlockEntity.TAG_GAS));
        return id == null ? null : GasTypes.REGISTRY.get().getValue(id);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        GasType type = contentsOf(stack);
        CompoundTag entity = stack.getTagElement(BlockItem.BLOCK_ENTITY_TAG);
        float amount = entity == null ? 0f : entity.getFloat(GasCylinderBlockEntity.TAG_AMOUNT);
        if (type == null || amount <= 0f) {
            tooltip.add(Component.translatable("item.yperite.gas_cylinder.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        int percent = Math.round(100f * Math.min(1f, amount / YperiteConfig.CYLINDER_CAPACITY.get().floatValue()));
        tooltip.add(Component.translatable("item.yperite.gas_cylinder.contents", type.displayName(), percent).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.yperite.gas_cylinder.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
