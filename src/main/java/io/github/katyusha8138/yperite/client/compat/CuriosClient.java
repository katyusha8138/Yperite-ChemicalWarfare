package io.github.katyusha8138.yperite.client.compat;

import io.github.katyusha8138.yperite.item.ModItems;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

/** Client side of the Curios integration. Only touched when Curios is installed. */
public final class CuriosClient {
    private CuriosClient() {
    }

    public static void register() {
        CuriosRendererRegistry.register(ModItems.GAS_MASK.get(), GasMaskCurioRenderer::new);
    }
}
