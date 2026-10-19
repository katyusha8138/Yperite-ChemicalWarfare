package io.github.katyusha8138.yperite.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.arguments.EntityArgument;
import io.github.katyusha8138.yperite.gas.GasCategory;
import io.github.katyusha8138.yperite.exposure.model.ExposureState;
import io.github.katyusha8138.yperite.exposure.model.ExposureModel;
import io.github.katyusha8138.yperite.exposure.ExposureManager;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.github.katyusha8138.yperite.config.YperiteConfig;
import io.github.katyusha8138.yperite.gas.GasManager;
import io.github.katyusha8138.yperite.gas.GasSavedData;
import io.github.katyusha8138.yperite.gas.GasType;
import io.github.katyusha8138.yperite.gas.GasTypes;
import io.github.katyusha8138.yperite.gas.WindModel;
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

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Debug commands for the gas system. They need permission level 2.
 *
 * <pre>
 * /yperite gas burst &lt;type&gt; &lt;mass&gt; &lt;radius&gt; [pos]
 * /yperite gas contaminate &lt;type&gt; &lt;amount&gt; &lt;radius&gt; [pos]
 * /yperite gas clear [type]
 * /yperite gas info
 * /yperite gas probe
 * /yperite wind
 * /yperite wind set &lt;fromDegrees&gt; &lt;strength&gt;
 * /yperite wind auto
 * /yperite exposure [player]
 * /yperite exposure add &lt;category&gt; &lt;amount&gt; [player]
 * /yperite exposure clear [player]
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
                        .then(Commands.literal("contaminate")
                                .then(Commands.argument("type", ResourceLocationArgument.id())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(GasTypes.REGISTRY.get().getKeys(), builder))
                                        .then(Commands.argument("amount", FloatArgumentType.floatArg(0.01f, 1000f))
                                                .then(Commands.argument("radius", IntegerArgumentType.integer(0, 16))
                                                        .executes(ctx -> contaminate(ctx, BlockPos.containing(ctx.getSource().getPosition())))
                                                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                                .executes(ctx -> contaminate(ctx, BlockPosArgument.getLoadedBlockPos(ctx, "pos"))))))))
                        .then(Commands.literal("clear")
                                .executes(ctx -> clear(ctx, null))
                                .then(Commands.argument("type", ResourceLocationArgument.id())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(GasTypes.REGISTRY.get().getKeys(), builder))
                                        .executes(ctx -> clear(ctx, gasType(ctx)))))
                        .then(Commands.literal("info").executes(YperiteCommand::info))
                        .then(Commands.literal("probe").executes(YperiteCommand::probe)))
                .then(Commands.literal("wind")
                        .executes(YperiteCommand::windInfo)
                        .then(Commands.literal("set")
                                .then(Commands.argument("fromDegrees", FloatArgumentType.floatArg(0f, 360f))
                                        .then(Commands.argument("strength", FloatArgumentType.floatArg(0f, 1f))
                                                .executes(YperiteCommand::windSet))))
                        .then(Commands.literal("auto").executes(YperiteCommand::windAuto)))
                .then(exposureNode()));
    }

    private static final String[] COMPASS = {"north", "north_east", "east", "south_east", "south", "south_west", "west", "north_west"};

    private static int windInfo(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        if (!level.dimensionType().hasSkyLight()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("commands.yperite.wind.calm"), false);
            return 0;
        }
        WindModel.Wind wind = GasManager.wind(level);
        boolean fixed = GasManager.data(level).windOverride() != null;
        float degrees = wind.fromDegrees();
        double speed = wind.strength() * YperiteConfig.WIND_MAX_SPEED.get();
        Component direction = Component.translatable("compass.yperite." + COMPASS[Math.round(degrees / 45f) % 8]);
        Component mode = Component.translatable(fixed ? "commands.yperite.wind.mode.fixed" : "commands.yperite.wind.mode.auto");
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.yperite.wind.info",
                direction, Math.round(degrees), fmt2(wind.strength()), fmt2((float) speed), mode), false);
        return 1;
    }

    private static int windSet(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        float degrees = FloatArgumentType.getFloat(ctx, "fromDegrees");
        float strength = FloatArgumentType.getFloat(ctx, "strength");
        GasManager.data(level).setWindOverride(new WindModel.Override(degrees, strength));
        Component direction = Component.translatable("compass.yperite." + COMPASS[Math.round(degrees / 45f) % 8]);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.yperite.wind.set",
                direction, Math.round(degrees), fmt2(strength)), true);
        return 1;
    }

    private static int windAuto(CommandContext<CommandSourceStack> ctx) {
        GasManager.data(ctx.getSource().getLevel()).setWindOverride(null);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.yperite.wind.auto"), true);
        return 1;
    }

    private static final List<String> CATEGORY_IDS = Arrays.stream(GasCategory.values()).map(GasCategory::id).toList();

    private static LiteralArgumentBuilder<CommandSourceStack> exposureNode() {
        return Commands.literal("exposure")
                .executes(ctx -> exposureInfo(ctx, ctx.getSource().getPlayerOrException()))
                .then(Commands.literal("add")
                        .then(Commands.argument("category", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(CATEGORY_IDS, builder))
                                .then(Commands.argument("amount", FloatArgumentType.floatArg(0f, 1000f))
                                        .executes(ctx -> exposureAdd(ctx, ctx.getSource().getPlayerOrException()))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> exposureAdd(ctx, EntityArgument.getPlayer(ctx, "player")))))))
                .then(Commands.literal("clear")
                        .executes(ctx -> exposureClear(ctx, ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> exposureClear(ctx, EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> exposureInfo(ctx, EntityArgument.getPlayer(ctx, "player"))));
    }

    private static int exposureInfo(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        ExposureState state = ExposureManager.stateOf(target);
        Component name = target.getDisplayName();
        int lines = 0;
        if (state != null) {
            for (int k = 0; k < state.categories(); k++) {
                if (state.skin[k] == 0f && state.latent[k] == 0f && state.active[k] == 0f && state.aftermath[k] == 0f) {
                    continue;
                }
                if (lines == 0) {
                    ctx.getSource().sendSuccess(() -> Component.translatable("commands.yperite.exposure.header", name), false);
                }
                lines++;
                GasCategory category = GasCategory.values()[k];
                int index = k;
                ctx.getSource().sendSuccess(() -> Component.translatable("commands.yperite.exposure.line",
                        category.displayName(), fmt2(state.skin[index]), fmt2(state.latent[index]), fmt2(state.active[index]),
                        ExposureModel.stage(state.active[index]), fmt2(state.aftermath[index])), false);
            }
        }
        if (lines == 0) {
            ctx.getSource().sendSuccess(() -> Component.translatable("commands.yperite.exposure.none", name), false);
        }
        return lines;
    }

    private static int exposureAdd(CommandContext<CommandSourceStack> ctx, ServerPlayer target) throws CommandSyntaxException {
        String id = StringArgumentType.getString(ctx, "category");
        GasCategory category = Arrays.stream(GasCategory.values()).filter(c -> c.id().equals(id)).findFirst()
                .orElseThrow(() -> new SimpleCommandExceptionType(Component.translatable("commands.yperite.exposure.unknown_category")).create());
        float amount = FloatArgumentType.getFloat(ctx, "amount");
        ExposureManager.addLatent(target, category, amount);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.yperite.exposure.add",
                target.getDisplayName(), category.displayName(), fmt2(amount)), true);
        return 1;
    }

    private static int exposureClear(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        ExposureManager.clear(target);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.yperite.exposure.clear", target.getDisplayName()), true);
        return 1;
    }

    private static String fmt2(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
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

    private static int contaminate(CommandContext<CommandSourceStack> ctx, BlockPos pos) throws CommandSyntaxException {
        GasType type = gasType(ctx);
        float amount = FloatArgumentType.getFloat(ctx, "amount");
        int radius = IntegerArgumentType.getInteger(ctx, "radius");
        int cells = GasManager.contaminate(ctx.getSource().getLevel(), type, pos, radius, amount);
        if (cells == 0) {
            throw NOTHING_PLACED.create();
        }
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.yperite.gas.contaminate.success", cells, type.displayName(), pos.getX(), pos.getY(), pos.getZ()), true);
        return cells;
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
