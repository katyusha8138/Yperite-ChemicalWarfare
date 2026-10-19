package io.github.katyusha8138.yperite.gas;

import io.github.katyusha8138.yperite.gas.sim.GasLayer;
import io.github.katyusha8138.yperite.network.GasSightMessage;
import io.github.katyusha8138.yperite.network.YperiteNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * How thick the gas hangs where a player's eyes are. The client hazes the view with it: the colour of the gas
 * mix, and a shorter view the thicker it is ({@code GasSightClient}). Thickness is the sum over gases of
 * concentration times opacity, so a nearly invisible gas hardly dims the view however strong it is.
 */
public final class GasSight {
    /** Ticks between updates, and how often an unchanged sight inside gas is sent again anyway. */
    private static final int INTERVAL = 5;
    private static final int REFRESH = 40;
    /** Changes smaller than this are not sent. */
    private static final float STEP = 0.02f;

    private static final Map<UUID, Sight> SENT = new HashMap<>();

    private GasSight() {
    }

    public record Sight(int color, float density) {
        public static final Sight CLEAR = new Sight(0xFFFFFF, 0f);

        public boolean clear() {
            return density <= 0.001f;
        }
    }

    /**
     * The mix of gases: {@code amounts[k]} is concentration times opacity of gas k, {@code colours[k]} its colour.
     * The colour is the average weighted by amount.
     */
    public static Sight mix(float[] amounts, int[] colours) {
        float total = 0f;
        float r = 0f, g = 0f, b = 0f;
        for (int k = 0; k < amounts.length; k++) {
            float a = Math.max(0f, amounts[k]);
            total += a;
            r += a * ((colours[k] >> 16) & 0xFF);
            g += a * ((colours[k] >> 8) & 0xFF);
            b += a * (colours[k] & 0xFF);
        }
        if (total <= 0f) {
            return Sight.CLEAR;
        }
        int rgb = (Math.round(r / total) << 16) | (Math.round(g / total) << 8) | Math.round(b / total);
        return new Sight(rgb, total);
    }

    /** Share of the view's colour that turns to the gas's colour: 0 in clear air, towards 1 in thick gas. */
    public static float tint(float density) {
        return 1f - (float) Math.exp(-1.4 * Math.max(0f, density));
    }

    /** How far one can see through gas of this thickness, in blocks. */
    public static float visibility(float density) {
        return 4f + 60f * (float) Math.exp(-2.5 * Math.max(0f, density));
    }

    /** The sight at a point of the level. */
    public static Sight at(ServerLevel level, GasSavedData data, Vec3 eye) {
        int x = Mth.floor(eye.x), y = Mth.floor(eye.y), z = Mth.floor(eye.z);
        var layers = data.layers().entrySet();
        float[] amounts = new float[layers.size()];
        int[] colours = new int[layers.size()];
        int k = 0;
        for (Map.Entry<GasType, GasLayer> entry : layers) {
            amounts[k] = entry.getValue().get(x, y, z) * entry.getKey().opacity();
            colours[k] = entry.getKey().color();
            k++;
        }
        return mix(amounts, colours);
    }

    /** Sends each player of the level how thick the gas is at their eyes, when it changed. */
    static void update(ServerLevel level, GasSavedData data, long time) {
        if (time % INTERVAL != 0) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            Sight now = data.isEmpty() ? Sight.CLEAR : at(level, data, player.getEyePosition());
            Sight last = SENT.getOrDefault(player.getUUID(), Sight.CLEAR);
            boolean changed = Math.abs(now.density() - last.density()) > STEP || (!now.clear() && now.color() != last.color());
            boolean refresh = !now.clear() && time % REFRESH == 0;
            if (changed || refresh || (now.clear() && !last.clear())) {
                SENT.put(player.getUUID(), now);
                YperiteNetwork.toPlayer(player, new GasSightMessage(now.color(), now.density()));
            }
        }
    }

    /** Forgets a player who left (they start in clear air when they come back). */
    static void forget(UUID player) {
        SENT.remove(player);
    }
}
