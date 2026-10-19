package io.github.katyusha8138.yperite.gametest.client;

import io.github.katyusha8138.yperite.item.GasMaskItem;
import io.github.katyusha8138.yperite.protection.FilterKind;
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

    /** "clear" empties the face and body slots; "body <item>" fills the body slot, "<item>" the face slot. */
    static void equip(ServerPlayer player, String args) {
        if (!ModList.get().isLoaded("curios")) {
            return;
        }
        if (args.equals("clear")) {
            put(player, "face", ItemStack.EMPTY);
            put(player, "body", ItemStack.EMPTY);
            return;
        }
        String slot = args.startsWith("body ") ? "body" : "face";
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(args.substring(args.indexOf(' ') + 1))));
        // Masks go on with a fresh filter, so that it is drawn.
        GasMaskItem.load(stack, FilterKind.GENERAL);
        put(player, slot, stack);
    }

    private static void put(ServerPlayer player, String slot, ItemStack stack) {
        CuriosApi.getCuriosInventory(player).resolve()
                .flatMap(handler -> handler.getStacksHandler(slot))
                .ifPresent(slots -> slots.getStacks().setStackInSlot(0, stack));
    }
}
