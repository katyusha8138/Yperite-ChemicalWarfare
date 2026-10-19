package io.github.katyusha8138.yperite.protection.compat;

import io.github.katyusha8138.yperite.item.GasMaskItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

/**
 * The only place that touches the Curios API. Never load this class unless {@link #isLoaded()} is true: then
 * the mod also works without Curios.
 */
public final class CuriosCompat {
    private CuriosCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded("curios");
    }

    /** The gas mask worn in a Curios slot, or an empty stack. */
    public static ItemStack findMask(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity).resolve()
                .flatMap(handler -> handler.findFirstCurio(stack -> stack.getItem() instanceof GasMaskItem))
                .map(SlotResult::stack)
                .orElse(ItemStack.EMPTY);
    }
}
