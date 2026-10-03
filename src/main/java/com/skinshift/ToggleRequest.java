package com.skinshift;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record ToggleRequest() {
    public void encode(FriendlyByteBuf buf) {
    }

    public static ToggleRequest decode(FriendlyByteBuf buf) {
        return new ToggleRequest();
    }

    public void handle(CustomPayloadEvent.Context ctx) {
        ServerPlayer sender = ctx.getSender();
        if (sender != null) {
            ServerLogic.toggle(sender);
        }
    }
}
