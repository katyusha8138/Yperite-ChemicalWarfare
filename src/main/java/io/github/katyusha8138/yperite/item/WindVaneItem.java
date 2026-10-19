package io.github.katyusha8138.yperite.item;

import io.github.katyusha8138.yperite.config.YperiteConfig;
import io.github.katyusha8138.yperite.gas.GasManager;
import io.github.katyusha8138.yperite.gas.WindModel;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/** Held in a hand, it shows which way the wind blows and how hard. The wind decides where gas drifts. */
public class WindVaneItem extends Item {
    private static final int READ_INTERVAL = 10;
    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
    private static final String[] COMPASS = {"north", "north_east", "east", "south_east", "south", "south_west", "west", "north_west"};

    public WindVaneItem(Properties properties) {
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
        player.displayClientMessage(reading(player.serverLevel()), true);
    }

    /** The action bar text for the wind in this dimension right now. */
    public static Component reading(ServerLevel level) {
        WindModel.Wind wind = GasManager.wind(level);
        if (!level.dimensionType().hasSkyLight() || wind.strength() < 0.01f) {
            return Component.translatable("item.yperite.wind_vane.calm");
        }
        double speed = wind.strength() * YperiteConfig.WIND_MAX_SPEED.get();
        return Component.translatable("item.yperite.wind_vane.reading",
                arrow(wind), Component.translatable("compass.yperite." + compass(wind)),
                String.format(Locale.ROOT, "%.1f", speed)).withStyle(ChatFormatting.AQUA);
    }

    /** An arrow in the direction the wind blows towards (up is north). */
    public static String arrow(WindModel.Wind wind) {
        double heading = Math.toDegrees(Math.atan2(wind.x(), -wind.z()));
        return ARROWS[(int) Math.floorMod(Math.round(heading / 45.0), 8L)];
    }

    /** The compass point the wind comes from, as used in weather reports. */
    public static String compass(WindModel.Wind wind) {
        return COMPASS[(int) Math.floorMod(Math.round(wind.fromDegrees() / 45.0), 8L)];
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.yperite.wind_vane").withStyle(ChatFormatting.GRAY));
    }
}
