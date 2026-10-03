package com.skinshift;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.SimpleChannel;

public final class Net {
    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(SkinShiftMod.MODID, "main"))
            .networkProtocolVersion(1)
            .simpleChannel();

    private Net() {}

    public static void init() {
        // клиент -> сервер: "я нажал кнопку"
        CHANNEL.messageBuilder(ToggleRequest.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ToggleRequest::encode)
                .decoder(ToggleRequest::decode)
                .consumerMainThread(ToggleRequest::handle)
                .add();

        // сервер -> клиенты: "игрок X начал анимацию / состояние скина такое-то"
        CHANNEL.messageBuilder(StatePacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(StatePacket::encode)
                .decoder(StatePacket::decode)
                .consumerMainThread(StatePacket::handle)
                .add();
    }
}
