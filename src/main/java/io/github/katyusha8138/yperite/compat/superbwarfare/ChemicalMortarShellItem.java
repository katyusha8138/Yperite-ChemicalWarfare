package io.github.katyusha8138.yperite.compat.superbwarfare;

import com.atsuishio.superbwarfare.item.projectile.MortarShellItem;
import io.github.katyusha8138.yperite.gas.GasType;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * A mortar shell filled with one gas. Superb Warfare's mortar accepts any {@link MortarShellItem}; the shell it
 * fires is marked as chemical by {@link SbwEvents#onShellJoined}. Touches Superb Warfare classes, so it is only
 * loaded when that mod is.
 */
public class ChemicalMortarShellItem extends MortarShellItem {
    private final Supplier<GasType> gas;

    public ChemicalMortarShellItem(Supplier<GasType> gas) {
        this.gas = gas;
    }

    public GasType gas() {
        return gas.get();
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        ChemicalRoundItem.addRoundTooltip(tooltip, ChemicalRound.MORTAR_SHELL, gas());
    }

    /** A dispenser would fire an ordinary shell and lose the gas, so it just drops the item. */
    @NotNull
    @Override
    public DispenseItemBehavior getLaunchBehavior() {
        return new DefaultDispenseItemBehavior();
    }
}
