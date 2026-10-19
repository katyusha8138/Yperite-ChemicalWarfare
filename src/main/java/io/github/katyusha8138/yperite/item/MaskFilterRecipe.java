package io.github.katyusha8138.yperite.item;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * A gas mask and a filter, anywhere in the grid, give the mask with the filter loaded. A filter that was in the
 * mask is lost, so swap filters only when the old one is spent. Masks with a built-in pad take no filter.
 */
public class MaskFilterRecipe extends CustomRecipe {
    public MaskFilterRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        int masks = 0;
        int filters = 0;
        for (ItemStack stack : container.getItems()) {
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof GasMaskItem item) {
                if (!item.type().acceptsFilters()) {
                    return false;
                }
                masks++;
            } else if (stack.getItem() instanceof GasFilterItem) {
                filters++;
            } else {
                return false;
            }
        }
        return masks == 1 && filters == 1;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registries) {
        ItemStack mask = ItemStack.EMPTY;
        GasFilterItem filter = null;
        for (ItemStack stack : container.getItems()) {
            if (stack.getItem() instanceof GasMaskItem) {
                mask = stack;
            } else if (stack.getItem() instanceof GasFilterItem found) {
                filter = found;
            }
        }
        if (mask.isEmpty() || filter == null) {
            return ItemStack.EMPTY;
        }
        ItemStack result = mask.copy();
        result.setCount(1);
        GasMaskItem.load(result, filter.kind());
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.MASK_FILTER.get();
    }
}
