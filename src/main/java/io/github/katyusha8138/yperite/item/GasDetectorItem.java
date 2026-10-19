package io.github.katyusha8138.yperite.item;

import io.github.katyusha8138.yperite.gas.GasCategory;
import io.github.katyusha8138.yperite.protection.GasScanner;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Held in a hand, it tells which categories of gas are around and how strong (low, medium, high, extreme), and
 * clicks faster the stronger the gas. It does not tell the exact gas, and it is how nerve agents, which are nearly
 * invisible, are noticed.
 */
public class GasDetectorItem extends Item {
    /** Cells around the middle of the body that the detector looks at. */
    private static final int RADIUS = 1;
    private static final int READ_INTERVAL = 10;

    public GasDetectorItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player) || player.tickCount % READ_INTERVAL != 0) {
            return;
        }
        if (player.getMainHandItem() != stack && player.getOffhandItem() != stack) {
            return;
        }
        ServerLevel serverLevel = player.serverLevel();
        BlockPos middle = BlockPos.containing(player.getX(), player.getY() + player.getBbHeight() * 0.5, player.getZ());
        GasScanner.Reading reading = GasScanner.scan(serverLevel, middle, RADIUS, player.blockPosition());
        player.displayClientMessage(describe(reading), true);

        int strongest = reading.maxLevel();
        if (strongest > 0) {
            // The stronger the gas, the more often it clicks.
            int every = switch (strongest) {
                case 1 -> 4;
                case 2 -> 2;
                default -> 1;
            };
            if ((player.tickCount / READ_INTERVAL) % every == 0) {
                player.playNotifySound(SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.5f, 0.8f + 0.2f * strongest);
            }
        }
    }

    /** The action bar text for a reading: each category found with its level, strongest first. */
    public static Component describe(GasScanner.Reading reading) {
        if (reading.isClear()) {
            return Component.translatable("item.yperite.gas_detector.clear").withStyle(ChatFormatting.GREEN);
        }
        List<GasCategory> found = new ArrayList<>();
        for (GasCategory category : GasCategory.values()) {
            if (reading.level(category) > 0) {
                found.add(category);
            }
        }
        found.sort(Comparator.comparingInt((GasCategory c) -> reading.level(c)).reversed());

        MutableComponent text = Component.translatable("item.yperite.gas_detector.prefix");
        for (int i = 0; i < found.size(); i++) {
            GasCategory category = found.get(i);
            text.append(i == 0 ? " " : " / ").append(Component.translatable("item.yperite.gas_detector.entry",
                    category.displayName(), Component.translatable("detector.yperite.level." + reading.level(category))));
        }
        if (reading.residue()) {
            text.append(found.isEmpty() ? " " : " / ").append(Component.translatable("item.yperite.gas_detector.residue"));
        }
        return text.withStyle(reading.maxLevel() >= 3 ? ChatFormatting.RED : ChatFormatting.GOLD);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.yperite.gas_detector").withStyle(ChatFormatting.GRAY));
    }
}
