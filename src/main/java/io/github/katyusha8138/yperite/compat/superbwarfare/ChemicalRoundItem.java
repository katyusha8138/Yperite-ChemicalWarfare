package io.github.katyusha8138.yperite.compat.superbwarfare;

import io.github.katyusha8138.yperite.gas.GasType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Ammunition filled with one gas: a 40mm grenade or a large cannon shell. It does nothing by itself; Superb Warfare's
 * weapons take it as ammunition through the entries that {@link SbwDataPatcher} adds.
 */
public class ChemicalRoundItem extends Item {
    private final ChemicalRound round;
    private final Supplier<GasType> gas;

    public ChemicalRoundItem(ChemicalRound round, Supplier<GasType> gas, Properties properties) {
        super(properties);
        this.round = round;
        this.gas = gas;
    }

    public ChemicalRound round() {
        return round;
    }

    public GasType gas() {
        return gas.get();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        addRoundTooltip(tooltip, round, gas());
    }

    static void addRoundTooltip(List<Component> tooltip, ChemicalRound round, GasType gas) {
        tooltip.add(Component.translatable("item.yperite.chemical_round.contents", gas.displayName()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.yperite.chemical_round.hint." + round.dataName().toLowerCase(java.util.Locale.ROOT))
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
