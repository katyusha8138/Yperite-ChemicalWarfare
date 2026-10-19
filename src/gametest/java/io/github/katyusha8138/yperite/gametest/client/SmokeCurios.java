package io.github.katyusha8138.yperite.gametest.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;

/** Puts items in the Curios face slot for the smoke scenes. Kept apart so that nothing loads Curios without it. */
final class SmokeCurios {
    private SmokeCurios() {
    }

    static void equip(ServerPlayer player, String itemOrClear) {
        if (!ModList.get().isLoaded("curios")) {
            return;
        }
        ItemStack stack = itemOrClear.equals("clear")
                ? ItemStack.EMPTY
                : new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(itemOrClear)));
        CuriosApi.getCuriosInventory(player).resolve()
                .flatMap(handler -> handler.getStacksHandler("face"))
                .ifPresent(slots -> slots.getStacks().setStackInSlot(0, stack));
    }
}
