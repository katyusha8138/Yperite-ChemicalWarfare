package io.github.katyusha8138.yperite.compat.superbwarfare;

import io.github.katyusha8138.yperite.YperiteMod;
import io.github.katyusha8138.yperite.gas.GasTypes;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Entry point of the Superb Warfare integration. Load this class only when {@link #isLoaded()} is true: it registers
 * the chemical rounds and the listeners, and those use Superb Warfare classes.
 */
public final class SbwCompat {
    public static final String MODID = "superbwarfare";

    private SbwCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MODID);
    }

    /** Registers one item per round and gas, and the event listeners. */
    public static void init(IEventBus modBus) {
        DeferredRegister<Item> items = DeferredRegister.create(ForgeRegistries.ITEMS, YperiteMod.MODID);
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
        MinecraftForge.EVENT_BUS.register(new SbwEvents());
        YperiteMod.LOGGER.info("Superb Warfare found: chemical rounds are available");
    }
}
