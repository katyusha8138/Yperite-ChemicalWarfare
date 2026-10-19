package io.github.katyusha8138.yperite.item;

import io.github.katyusha8138.yperite.exposure.ExposureManager;
import io.github.katyusha8138.yperite.gas.GasCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Treatment for the dose already in the body. The nerve and blister agents need their own medicine; the general one
 * does nothing against them. Lasting damage cannot be treated.
 */
public class MedicineItem extends Item {
    /** What a medicine does to one category: the shares of latent and active dose that remain, and the skin. */
    public record Effect(GasCategory category, float latentLeft, float activeLeft, boolean clearSkin) {
    }

    public enum Kind {
        /** Against riot control, choking, blood and incapacitating agents. */
        GENERAL(
                new Effect(GasCategory.RIOT_CONTROL, 0.4f, 0.4f, false),
                new Effect(GasCategory.CHOKING, 0.4f, 0.4f, false),
                new Effect(GasCategory.BLOOD, 0.4f, 0.4f, false),
                new Effect(GasCategory.INCAPACITANT, 0.4f, 0.4f, false)),
        /** Against nerve agents. */
        NERVE_ANTIDOTE(new Effect(GasCategory.NERVE, 0.15f, 0.15f, false)),
        /** Against blister agents; also takes the contamination off the skin. */
        BLISTER_OINTMENT(new Effect(GasCategory.BLISTER, 0.4f, 0.5f, true));

        private final Effect[] effects;

        Kind(Effect... effects) {
            this.effects = effects;
        }

        public Effect[] effects() {
            return effects;
        }
    }

    private final Kind kind;

    public MedicineItem(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    /** Applies the treatment to a player. */
    public static void treat(Player player, Kind kind) {
        for (Effect effect : kind.effects()) {
            ExposureManager.treat(player, effect.category(), effect.latentLeft(), effect.activeLeft(), effect.clearSkin());
        }
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof Player player) {
            treat(player, kind);
            player.displayClientMessage(Component.translatable("item.yperite.medicine.used"), true);
            return ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE));
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.yperite.medicine." + kind.name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.GRAY));
    }
}
