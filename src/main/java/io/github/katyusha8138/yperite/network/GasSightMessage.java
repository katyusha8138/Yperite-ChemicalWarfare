package io.github.katyusha8138.yperite.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server to client: how thick the gas is at the player's eyes, and its colour (see GasSight). */
public record GasSightMessage(int color, float density) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(color);
        buf.writeFloat(density);
    }

    public static GasSightMessage decode(FriendlyByteBuf buf) {
        return new GasSightMessage(buf.readInt(), buf.readFloat());
    }

    @SuppressWarnings("removal")
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> io.github.katyusha8138.yperite.client.GasSightClient.receive(color, density)));
        context.get().setPacketHandled(true);
    }
}
