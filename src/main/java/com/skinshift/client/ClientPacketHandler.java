package com.skinshift.client;

import com.skinshift.StatePacket;

public final class ClientPacketHandler {
    private ClientPacketHandler() {}

    public static void handle(StatePacket pkt) {
        ClientFx.onPacket(pkt.player(), pkt.skinNew(), pkt.instant());
    }
}
