package io.github.katyusha8138.yperite.client;

import io.github.katyusha8138.yperite.YperiteMod;
import io.github.katyusha8138.yperite.client.compat.CuriosClient;
import io.github.katyusha8138.yperite.protection.compat.CuriosCompat;
import io.github.katyusha8138.yperite.block.GasCylinderBlockEntity;
import io.github.katyusha8138.yperite.block.ModBlocks;
import io.github.katyusha8138.yperite.entity.ModEntities;
import io.github.katyusha8138.yperite.gas.GasType;
import io.github.katyusha8138.yperite.item.GasCylinderItem;
import io.github.katyusha8138.yperite.item.ModItems;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Client-only setup. Servers never load this class. */
@Mod.EventBusSubscriber(modid = YperiteMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.GAS_GRENADE.get(), ThrownItemRenderer::new);
    }

    /** The gas cylinder's band takes the colour of the gas inside (tint 0 of its model), placed or held. */
    @SubscribeEvent
    public static void onBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> {
            if (tint == 0 && level != null && pos != null && level.getBlockEntity(pos) instanceof GasCylinderBlockEntity cylinder) {
                GasType gas = cylinder.gas();
                return gas == null ? EMPTY_BAND : gas.color();
            }
            return -1;
        }, ModBlocks.GAS_CYLINDER.get());
    }

    @SubscribeEvent
    public static void onItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> {
            if (tint != 0) {
                return -1;
            }
            GasType gas = GasCylinderItem.contentsOf(stack);
            return gas == null ? EMPTY_BAND : gas.color();
        }, ModItems.GAS_CYLINDER.get());
    }

    /** The band of an empty cylinder: plain grey. */
    private static final int EMPTY_BAND = 0x8A8F94;

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            if (CuriosCompat.isLoaded()) {
                CuriosClient.register();
            }
        });
    }
}
