package io.github.katyusha8138.yperite.item;

import io.github.katyusha8138.yperite.gas.GasManager;
import io.github.katyusha8138.yperite.gas.GasSavedData;
import io.github.katyusha8138.yperite.gas.sim.GasLayer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Scattered on the ground, it neutralises the residue around the spot. It does not clear gas in the air. */
public class DeconPowderItem extends Item {
    private static final int RADIUS = 2;

    public DeconPowderItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ServerLevel serverLevel = (ServerLevel) level;
        BlockPos spot = context.getClickedPos().relative(context.getClickedFace());
        GasSavedData data = GasManager.data(serverLevel);
        float removed = 0f;
        for (GasLayer layer : data.layers().values()) {
            removed += layer.clearResidue(spot.getX(), spot.getY(), spot.getZ(), RADIUS);
        }
        if (removed > 0f) {
            data.setDirty();
        }
        serverLevel.playSound(null, spot, SoundEvents.SAND_PLACE, SoundSource.PLAYERS, 1f, 1.2f);
        serverLevel.sendParticles(ParticleTypes.CLOUD, spot.getX() + 0.5, spot.getY() + 0.2, spot.getZ() + 0.5, 12, 1.0, 0.1, 1.0, 0.01);
        if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        if (context.getPlayer() != null) {
            context.getPlayer().displayClientMessage(Component.translatable(removed > 0f
                    ? "item.yperite.decon_powder.cleaned" : "item.yperite.decon_powder.nothing"), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.yperite.decon_powder").withStyle(ChatFormatting.GRAY));
    }
}
