package io.github.katyusha8138.yperite.client.compat;

import io.github.katyusha8138.yperite.item.ModItems;
import net.minecraft.world.entity.EquipmentSlot;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

/** Client side of the Curios integration. Only touched when Curios is installed. */
public final class CuriosClient {
    private CuriosClient() {
    }

    public static void register() {
        ModItems.MASKS.forEach(mask -> CuriosRendererRegistry.register(mask.get(), () -> new GasMaskCurioRenderer(EquipmentSlot.HEAD)));
    }
}
