package com.skinshift;

import net.minecraft.util.Mth;

/** Тайминги анимации (в тиках, 20 тиков = 1 сек). */
public final class Anim {
    public static final int RAISE = 14;   // руки поднимаются (0.7 сек)
    public static final int SWIRL = 40;   // вихрь + смена скина (2 сек)
    public static final int LOWER = 14;   // руки опускаются (0.7 сек)
    public static final int TOTAL = RAISE + SWIRL + LOWER;

    private Anim() {}

    /** smootherstep 0..1 */
    public static float ease(float x) {
        x = Mth.clamp(x, 0f, 1f);
        return x * x * x * (x * (x * 6f - 15f) + 10f);
    }

    /** Насколько руки подняты (0..1) в момент t тиков от старта. */
    public static float armBlend(float t) {
        if (t <= 0f) return 0f;
        if (t < RAISE) return ease(t / RAISE);
        if (t < RAISE + SWIRL) return 1f;
        float u = (t - RAISE - SWIRL) / LOWER;
        return u >= 1f ? 0f : 1f - ease(u);
    }

    /** Прогресс смены скина 0..1 — идёт только пока крутится вихрь. */
    public static float skinProgress(float t) {
        float x = Mth.clamp((t - RAISE) / SWIRL, 0f, 1f);
        return x < 0.5f ? 2f * x * x : 1f - 2f * (1f - x) * (1f - x);
    }
}
