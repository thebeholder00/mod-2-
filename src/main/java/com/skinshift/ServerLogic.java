package com.skinshift;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.network.PacketDistributor;

/** Серверная часть: хранит, у кого сейчас "новый" скин, и рассылает анимацию всем, кто видит игрока. */
public final class ServerLogic {
    private static final Map<UUID, Boolean> STATE = new HashMap<>();
    private static final Map<UUID, Long> BUSY_UNTIL = new HashMap<>();

    private ServerLogic() {}

    public static void toggle(ServerPlayer player) {
        UUID id = player.getUUID();
        long now = player.level().getGameTime();

        Long busy = BUSY_UNTIL.get(id);
        if (busy != null && busy > now) {
            return; // анимация ещё идёт
        }

        boolean target = !STATE.getOrDefault(id, false);
        STATE.put(id, target);
        BUSY_UNTIL.put(id, now + Anim.TOTAL + 2);

        Net.CHANNEL.send(new StatePacket(id, target, false),
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(player));
    }

    /** Когда игрок X попадает в зону видимости игрока Y — отправляем Y текущее состояние X. */
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer viewer) {
            Boolean state = STATE.get(target.getUUID());
            if (state != null && state) {
                Net.CHANNEL.send(new StatePacket(target.getUUID(), true, true),
                        PacketDistributor.PLAYER.with(viewer));
            }
        }
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        STATE.remove(event.getEntity().getUUID());
        BUSY_UNTIL.remove(event.getEntity().getUUID());
    }
}
