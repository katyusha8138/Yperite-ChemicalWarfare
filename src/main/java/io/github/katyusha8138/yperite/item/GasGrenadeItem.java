package io.github.katyusha8138.yperite.item;

import io.github.katyusha8138.yperite.entity.GasGrenadeEntity;
import io.github.katyusha8138.yperite.gas.GasType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/** A hand grenade filled with one kind of gas. Right-click throws it; it goes off after it lands. */
public class GasGrenadeItem extends Item {
    private final Supplier<GasType> gas;

    public GasGrenadeItem(Supplier<GasType> gas, Properties properties) {
        super(properties);
        this.gas = gas;
    }

    public GasType gas() {
        return gas.get();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS,
                0.5f, 0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));
        if (!level.isClientSide) {
            GasGrenadeEntity grenade = new GasGrenadeEntity(level, player);
            ItemStack thrown = stack.copy();
            thrown.setCount(1);
            grenade.setItem(thrown);
            grenade.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 1.2f, 1.0f);
            level.addFreshEntity(grenade);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        player.getCooldowns().addCooldown(this, 10);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.yperite.gas_grenade.contents", gas().displayName())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.yperite.gas_grenade.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
