package com.skinshift.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.UUID;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/** Клиентское состояние эффекта для одного игрока. */
public final class PlayerFx {
    final UUID id;

    /** Скин зафиксирован как новый (когда анимации нет). */
    boolean skinNew;
    /** Идёт анимация. */
    boolean animating;
    /** Направление текущей анимации: true = к новому скину. */
    boolean toNew;
    /** tickCount игрока в момент старта анимации. */
    int startAge;
    int lastStep = -1;

    DynamicTexture tex;
    ResourceLocation loc;
    /** Настоящий (оригинальный) скин игрока, считанный с видеокарты. Может быть null. */
    NativeImage realOld;
    boolean triedCapture;

    PlayerFx(UUID id) {
        this.id = id;
    }

    /** Нужно ли рисовать игрока нашим рендерером с динамической текстурой. */
    public boolean useCustom() {
        return (skinNew || animating) && tex != null;
    }

    public boolean isAnimating() {
        return animating;
    }

    public int getStartAge() {
        return startAge;
    }

    public ResourceLocation getTexture() {
        return loc;
    }
}
