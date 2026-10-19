package io.github.katyusha8138.yperite.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.github.katyusha8138.yperite.gas.GasManager;
import io.github.katyusha8138.yperite.gas.GasSavedData;
import io.github.katyusha8138.yperite.gas.GasType;
import io.github.katyusha8138.yperite.gas.GasTypes;
import io.github.katyusha8138.yperite.gas.sim.GasLayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import java.util.Locale;
import java.util.Map;

/**
 * Debug commands for the gas system. They need permission level 2.
 *
 * <pre>
 * /yperite gas burst &lt;type&gt; &lt;mass&gt; &lt;radius&gt; [pos]
 * /yperite gas clear [type]
 * /yperite gas info
 * /yperite gas probe
 * </pre>
 */
public final class YperiteCommand {
    private static final SimpleCommandExceptionType UNKNOWN_TYPE =
            new SimpleCommandExceptionType(Component.translatable("commands.yperite.gas.unknown_type"));
    private static final SimpleCommandExceptionType NOTHING_PLACED =
            new SimpleCommandExceptionType(Component.translatable("commands.yperite.gas.burst.blocked"));

    private YperiteCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("yperite")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("gas")
                        .then(Commands.literal("burst")
                                .then(Commands.argument("type", ResourceLocationArgument.id())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(GasTypes.REGISTRY.get().getKeys(), builder))
                                        .then(Commands.argument("mass", FloatArgumentType.floatArg(0.01f, 100000f))
                                                .then(Commands.argument("radius", FloatArgumentType.floatArg(0f, 24f))
                                                        .executes(ctx -> burst(ctx, BlockPos.containing(ctx.getSource().getPosition())))
                                                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                                .executes(ctx -> burst(ctx, BlockPosArgument.getLoadedBlockPos(ctx, "pos"))))))))
                        .then(Commands.literal("clear")
                                .executes(ctx -> clear(ctx, null))
                                .then(Commands.argument("type", ResourceLocationArgument.id())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(GasTypes.REGISTRY.get().getKeys(), builder))
                                        .executes(ctx -> clear(ctx, gasType(ctx)))))
                        .then(Commands.literal("info").executes(YperiteCommand::info))
                        .then(Commands.literal("probe").executes(YperiteCommand::probe))));
    }

    private static GasType gasType(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "type");
        GasType type = GasTypes.REGISTRY.get().getValue(id);
        if (type == null) {
            throw UNKNOWN_TYPE.create();
        }
        return type;
    }

    private static int burst(CommandContext<CommandSourceStack> ctx, BlockPos pos) throws CommandSyntaxException {
        GasType type = gasType(ctx);
        float mass = FloatArgumentType.getFloat(ctx, "mass");
        float radius = FloatArgumentType.getFloat(ctx, "radius");
        ServerLevel level = ctx.getSource().getLevel();
        float placed = GasManager.release(level, type, pos, radius, mass);
        if (placed <= 0f) {
            throw NOTHING_PLACED.create();
        }
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.yperite.gas.burst.success", type.displayName(), fmt(placed), pos.getX(), pos.getY(), pos.getZ()), true);
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> ctx, GasType type) {
        GasManager.clear(ctx.getSource().getLevel(), type);
        ctx.getSource().sendSuccess(() -> type == null
                ? Component.translatable("commands.yperite.gas.clear.all")
                : Component.translatable("commands.yperite.gas.clear.one", type.displayName()), true);
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> ctx) {
        GasSavedData data = GasManager.data(ctx.getSource().getLevel());
        int shown = 0;
        for (Map.Entry<GasType, GasLayer> entry : data.layers().entrySet()) {
            GasLayer layer = entry.getValue();
            if (layer.isEmpty()) {
                continue;
            }
            shown++;
            ctx.getSource().sendSuccess(() -> Component.translatable("commands.yperite.gas.info.line",
                    entry.getKey().displayName(),
                    layer.cellCount(), fmt((float) layer.totalMass()),
                    layer.residueCellCount(), fmt((float) layer.totalResidue())), false);
        }
        if (shown == 0) {
            ctx.getSource().sendSuccess(() -> Component.translatable("commands.yperite.gas.info.none"), false);
        }
        return shown;
    }

    private static int probe(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos pos = BlockPos.containing(ctx.getSource().getPosition());
        GasSavedData data = GasManager.data(level);
        int found = 0;
        for (Map.Entry<GasType, GasLayer> entry : data.layers().entrySet()) {
            float gas = entry.getValue().get(pos.getX(), pos.getY(), pos.getZ());
            float residue = entry.getValue().getResidue(pos.getX(), pos.getY(), pos.getZ());
            if (gas <= 0f && residue <= 0f) {
                continue;
            }
            found++;
            ctx.getSource().sendSuccess(() -> Component.translatable("commands.yperite.gas.probe.line",
                    entry.getKey().displayName(), fmt(gas), fmt(residue)), false);
        }
        if (found == 0) {
            ctx.getSource().sendSuccess(() -> Component.translatable("commands.yperite.gas.probe.none"), false);
        }
        return found;
    }

    private static String fmt(float value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
