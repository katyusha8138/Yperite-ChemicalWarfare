package io.github.katyusha8138.yperite.compat.superbwarfare;

import io.github.katyusha8138.yperite.YperiteMod;
import io.github.katyusha8138.yperite.compat.superbwarfare.client.StickGrenadeClient;
import io.github.katyusha8138.yperite.gas.GasTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Entry point of the Superb Warfare integration. Load this class only when {@link #isLoaded()} is true: it registers
 * the chemical rounds and the listeners, and those use Superb Warfare classes.
 */
public final class SbwCompat {
    public static final String MODID = "superbwarfare";

    /** The stick grenade and its thrown entity (null until {@link #init} has run). */
    public static RegistryObject<Item> STICK_GRENADE;
    public static RegistryObject<EntityType<StickGrenadeEntity>> STICK_GRENADE_ENTITY;

    private SbwCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MODID);
    }

    /** Registers one item per round and gas, the stick grenade, and the event listeners. */
    public static void init(IEventBus modBus) {
        DeferredRegister<Item> items = DeferredRegister.create(ForgeRegistries.ITEMS, YperiteMod.MODID);
        DeferredRegister<EntityType<?>> entities = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, YperiteMod.MODID);
        // the stick grenade: Superb Warfare's hand grenade with its own look (same size of entity, same tracking)
        STICK_GRENADE = items.register("stick_grenade", StickGrenadeItem::new);
        STICK_GRENADE_ENTITY = entities.register("stick_grenade", () -> EntityType.Builder
                .<StickGrenadeEntity>of(StickGrenadeEntity::new, MobCategory.MISC)
                .sized(0.3f, 0.3f)
                .setShouldReceiveVelocityUpdates(true)
                .setTrackingRange(64)
                .setUpdateInterval(1)
                .build("stick_grenade"));
        SbwItems.ITEMS.add(STICK_GRENADE);
        for (ChemicalRound round : ChemicalRound.values()) {
            for (var gas : GasTypes.BUILT_IN) {
                String name = round.itemName(gas.getId());
                var item = items.register(name, () -> round == ChemicalRound.MORTAR_SHELL
                        ? new ChemicalMortarShellItem(gas)
                        : new ChemicalRoundItem(round, gas, new Item.Properties().stacksTo(round.stackSize())));
                ChemicalRound.ITEMS.add(item);
            }
        }
        items.register(modBus);
        entities.register(modBus);
        // a dispenser throws it like Superb Warfare's grenades (Superb Warfare only does this for its own items)
        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            StickGrenadeItem grenade = (StickGrenadeItem) STICK_GRENADE.get();
            DispenserBlock.registerBehavior(grenade, grenade.getLaunchBehavior());
        }));
        if (FMLEnvironment.dist.isClient()) {
            modBus.addListener(StickGrenadeClient::registerRenderers);
        }
        MinecraftForge.EVENT_BUS.register(new SbwEvents());
        YperiteMod.LOGGER.info("Superb Warfare found: chemical rounds and the stick grenade are available");
    }
}
