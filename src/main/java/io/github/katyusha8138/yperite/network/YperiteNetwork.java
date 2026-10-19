package io.github.katyusha8138.yperite.network;

import io.github.katyusha8138.yperite.YperiteMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** The mod's network channel. Both sides need the mod (its items and particles), so the versions must match. */
public final class YperiteNetwork {
    private static final String VERSION = "1";

    @SuppressWarnings("removal")
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(YperiteMod.MODID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private YperiteNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(GasSightMessage.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(GasSightMessage::encode)
                .decoder(GasSightMessage::decode)
                .consumerMainThread((message, context) -> message.handle(context))
                .add();
    }

    public static void toPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
