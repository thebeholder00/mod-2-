package com.skinshift;

import com.skinshift.client.ClientPacketHandler;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * @param player   чей скин
 * @param skinNew  целевое состояние: true = новый скин, false = оригинальный
 * @param instant  true = без анимации (игрок только что появился в зоне видимости)
 */
public record StatePacket(UUID player, boolean skinNew, boolean instant) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(player);
        buf.writeBoolean(skinNew);
        buf.writeBoolean(instant);
    }

    public static StatePacket decode(FriendlyByteBuf buf) {
        return new StatePacket(buf.readUUID(), buf.readBoolean(), buf.readBoolean());
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientPacketHandler.handle(this);
        }
    }
}
