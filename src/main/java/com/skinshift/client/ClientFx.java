package com.skinshift.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.skinshift.Anim;
import com.skinshift.ModParticles;
import com.skinshift.SkinShiftMod;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

public final class ClientFx {
    /**
     * true  = "старый" скин берётся с настоящего скина игрока (возврат получается бесшовным).
     * false = всегда используется файл textures/skin/old_skin.png из мода.
     * Если настоящий скин считать не удалось — автоматически используется old_skin.png.
     */
    public static final boolean USE_REAL_SKIN_AS_OLD = true;

    public static final KeyMapping KEY = new KeyMapping(
            "key.skinshift.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.skinshift");

    private static final ResourceLocation OLD_PATH =
            ResourceLocation.fromNamespaceAndPath(SkinShiftMod.MODID, "textures/skin/old_skin.png");
    private static final ResourceLocation NEW_PATH =
            ResourceLocation.fromNamespaceAndPath(SkinShiftMod.MODID, "textures/skin/new_skin.png");

    private static final Map<UUID, PlayerFx> MAP = new HashMap<>();
    private static NativeImage placeholderOld;
    private static NativeImage newImg;

    /** Порог "проявки" каждого пикселя: сверху вниз + шум = органичное растворение. */
    private static final float[] THRESH = new float[64 * 64];

    static {
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                int h = x * 73856093 ^ y * 19349663;
                h ^= h >>> 13;
                h *= 1274126177;
                h ^= h >>> 16;
                float noise = (h & 0xFFFF) / 65536f;
                THRESH[y * 64 + x] = 0.35f * (y / 63f) + 0.65f * noise * 0.999f;
            }
        }
    }

    private ClientFx() {}

    public static PlayerFx get(UUID id) {
        return MAP.get(id);
    }

    // ------------------------------------------------------------------ пакеты

    public static void onPacket(UUID id, boolean target, boolean instant) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        Player p = level.getPlayerByUUID(id);
        PlayerFx fx = MAP.computeIfAbsent(id, PlayerFx::new);

        if (p instanceof AbstractClientPlayer acp) {
            captureRealSkin(fx, acp);
        }
        ensureTexture(fx);

        if (instant || p == null) {
            fx.animating = false;
            fx.skinNew = target;
            paint(fx, 1f, target);
            return;
        }

        fx.animating = true;
        fx.toNew = target;
        fx.startAge = p.tickCount;
        fx.lastStep = -1;
        paint(fx, 0f, target);
    }

    // ------------------------------------------------------------------ тик игрока

    public static void tick(AbstractClientPlayer p) {
        PlayerFx fx = MAP.get(p.getUUID());
        if (fx == null || !fx.animating) return;

        int t = p.tickCount - fx.startAge;
        if (t < 0) return;

        if (t >= Anim.TOTAL) {
            fx.animating = false;
            fx.skinNew = fx.toNew;
            paint(fx, 1f, fx.toNew);
            return;
        }

        float prog = Anim.skinProgress(t);
        int step = (int) (prog * 48f);
        if (step != fx.lastStep) {
            fx.lastStep = step;
            paint(fx, prog, fx.toNew);
        }
        spawnParticles(p, t);
    }

    private static void spawnParticles(AbstractClientPlayer p, int t) {
        int local = t - Anim.RAISE;
        if (local < 0 || local > Anim.SWIRL + 6) return;

        float ramp = Math.min(1f, Math.min(local / 6f, (Anim.SWIRL + 6 - local) / 8f));
        if (ramp <= 0f) return;

        RandomSource r = p.getRandom();
        int n = 2 + (int) (6 * ramp);
        for (int i = 0; i < n; i++) {
            double angle = r.nextDouble() * Math.PI * 2.0;
            double radius = 0.6 + r.nextDouble() * 0.6;
            double y = p.getY() + r.nextDouble() * 0.5;
            double rise = 0.035 + r.nextDouble() * 0.04;
            // xSpeed/ySpeed/zSpeed используются частицей как: стартовый угол / скорость подъёма / радиус
            p.level().addParticle(ModParticles.VORTEX.get(), p.getX(), y, p.getZ(), angle, rise, radius);
        }
    }

    // ------------------------------------------------------------------ текстура

    private static void ensureTexture(PlayerFx fx) {
        if (fx.tex != null) return;
        fx.tex = new DynamicTexture(64, 64, true);
        fx.loc = ResourceLocation.fromNamespaceAndPath(SkinShiftMod.MODID, "dyn/" + fx.id.toString().toLowerCase());
        Minecraft.getInstance().getTextureManager().register(fx.loc, fx.tex);
    }

    /** Заполняет динамическую текстуру: пиксели с порогом < p берутся из "to", остальные из "from". */
    private static void paint(PlayerFx fx, float p, boolean toNew) {
        NativeImage px = fx.tex.getPixels();
        if (px == null) return;

        NativeImage oldImg = oldSource(fx);
        NativeImage newI = newSource();
        NativeImage from = toNew ? oldImg : newI;
        NativeImage to = toNew ? newI : oldImg;

        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                boolean reveal = p >= 1f || THRESH[y * 64 + x] < p;
                px.setPixelRGBA(x, y, sample(reveal ? to : from, x, y));
            }
        }
        fx.tex.upload();
    }

    private static int sample(NativeImage img, int x, int y) {
        if (img == null || x >= img.getWidth() || y >= img.getHeight()) return 0;
        return img.getPixelRGBA(x, y);
    }

    private static NativeImage oldSource(PlayerFx fx) {
        if (USE_REAL_SKIN_AS_OLD && fx.realOld != null) return fx.realOld;
        if (placeholderOld == null) placeholderOld = load(OLD_PATH);
        return placeholderOld;
    }

    private static NativeImage newSource() {
        if (newImg == null) newImg = load(NEW_PATH);
        return newImg;
    }

    private static NativeImage load(ResourceLocation loc) {
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(loc)) {
            return NativeImage.read(in);
        } catch (Exception e) {
            return new NativeImage(64, 64, true);
        }
    }

    /** Считываем реальный скин игрока из видеопамяти (только 64x64), чтобы "оригинал" был настоящим. */
    private static void captureRealSkin(PlayerFx fx, AbstractClientPlayer p) {
        if (!USE_REAL_SKIN_AS_OLD || fx.triedCapture) return;
        fx.triedCapture = true;
        try {
            ResourceLocation skinLoc = p.getSkin().texture();
            AbstractTexture t = Minecraft.getInstance().getTextureManager().getTexture(skinLoc);
            RenderSystem.bindTexture(t.getId());
            int w = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
            int h = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
            if (w != 64 || h != 64) return;
            NativeImage img = new NativeImage(64, 64, false);
            img.downloadTexture(0, false);
            fx.realOld = img;
        } catch (Throwable ignored) {
            fx.realOld = null;
        }
    }

    // ------------------------------------------------------------------ очистка

    public static void clearAll() {
        Minecraft mc = Minecraft.getInstance();
        for (PlayerFx fx : MAP.values()) {
            if (fx.loc != null) mc.getTextureManager().release(fx.loc);
            if (fx.realOld != null) fx.realOld.close();
        }
        MAP.clear();
        if (placeholderOld != null) { placeholderOld.close(); placeholderOld = null; }
        if (newImg != null) { newImg.close(); newImg = null; }
    }
}
