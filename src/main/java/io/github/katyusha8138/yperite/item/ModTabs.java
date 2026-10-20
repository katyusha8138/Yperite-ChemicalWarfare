package io.github.katyusha8138.yperite.item;

import io.github.katyusha8138.yperite.YperiteMod;
import io.github.katyusha8138.yperite.compat.superbwarfare.ChemicalRound;
import io.github.katyusha8138.yperite.protection.FilterKind;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModTabs {
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, YperiteMod.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.yperite"))
            .icon(() -> new ItemStack(ModItems.GAS_MASK.get()))
            .displayItems((parameters, output) -> {
                ItemStack mask = new ItemStack(ModItems.GAS_MASK.get());
                output.accept(mask);
                output.accept(GasMaskItem.withFilter(mask, FilterKind.GENERAL));
                output.accept(GasMaskItem.withFilter(mask, FilterKind.SPECIAL));
                // The First World War patterns, ready to wear.
                ModItems.MASKS.stream().skip(1).forEach(old -> {
                    ItemStack stack = new ItemStack(old.get());
                    output.accept(GasMaskItem.typeOf(stack).acceptsFilters() ? GasMaskItem.withFilter(stack, FilterKind.GENERAL) : stack);
                });
                output.accept(ModItems.GAS_FILTER_GENERAL.get());
                output.accept(ModItems.GAS_FILTER_SPECIAL.get());
                output.accept(ModItems.SEALED_FABRIC.get());
                output.accept(ModItems.PROTECTIVE_HOOD.get());
                output.accept(ModItems.PROTECTIVE_JACKET.get());
                output.accept(ModItems.PROTECTIVE_TROUSERS.get());
                output.accept(ModItems.PROTECTIVE_BOOTS.get());
                output.accept(ModItems.GAS_DETECTOR.get());
                output.accept(ModItems.GAS_ALARM.get());
                output.accept(ModItems.WIND_VANE.get());
                output.accept(ModItems.DECON_SOLUTION.get());
                output.accept(ModItems.DECON_POWDER.get());
                output.accept(ModItems.MEDICINE_GENERAL.get());
                output.accept(ModItems.ANTIDOTE_NERVE.get());
                output.accept(ModItems.OINTMENT_BLISTER.get());
                output.accept(ModItems.BRODIE_MK1.get());
                output.accept(ModItems.ADRIAN_M15.get());
                output.accept(ModItems.STAHLHELM_M1916.get());
                output.accept(ModItems.BERNDORFER.get());
                output.accept(ModItems.ADRIAN_M16_RUSSIAN.get());
                ModItems.UNIFORMS.forEach(piece -> output.accept(piece.get()));
                ModItems.BODY_ARMOUR.forEach(armour -> output.accept(armour.get()));
                ModItems.GRENADES.values().forEach(grenade -> output.accept(grenade.get()));
                output.accept(ModItems.GAS_CYLINDER.get());
                ModItems.GRENADES.values().forEach(grenade -> output.accept(GasCylinderItem.full(((GasGrenadeItem) grenade.get()).gas())));
                ChemicalRound.ITEMS.forEach(round -> output.accept(round.get()));
            })
            .build());

    private ModTabs() {
    }

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }
}
