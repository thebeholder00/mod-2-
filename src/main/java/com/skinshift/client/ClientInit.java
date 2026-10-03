package com.skinshift.client;

import com.skinshift.ModParticles;
import com.skinshift.Net;
import com.skinshift.ToggleRequest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.PacketDistributor;

public final class ClientInit {
    private static SkinShiftRenderer CLASSIC;
    private static SkinShiftRenderer SLIM;
    private static boolean rendering = false;

    private ClientInit() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(ClientInit::registerKeys);
        modBus.addListener(ClientInit::registerParticles);
        modBus.addListener(ClientInit::registerRenderers);

        MinecraftForge.EVENT_BUS.addListener(ClientInit::onLivingTick);
        MinecraftForge.EVENT_BUS.addListener(ClientInit::onRenderPlayer);
        MinecraftForge.EVENT_BUS.addListener(ClientInit::onRenderArm);
        MinecraftForge.EVENT_BUS.addListener(ClientInit::onLogout);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ClientFx.KEY);
    }

    private static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.VORTEX.get(), VortexParticle.Provider::new);
    }

    /**
     * Ванильные PlayerRenderer создаются в обход реестра, поэтому регистрируем провайдер для EntityType.PLAYER
     * только чтобы получить EntityRendererProvider.Context и создать наши рендереры (пересоздаются при F3+T).
     */
    @SuppressWarnings("unchecked")
    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityType.PLAYER, ctx -> {
            CLASSIC = new SkinShiftRenderer(ctx, false);
            SLIM = new SkinShiftRenderer(ctx, true);
            return (EntityRenderer<Player>) (EntityRenderer<?>) CLASSIC;
        });
    }

    private static SkinShiftRenderer rendererFor(AbstractClientPlayer p) {
        boolean slim = p.getSkin().model() == PlayerSkin.Model.SLIM;
        return slim ? SLIM : CLASSIC;
    }

    // ------------------------------------------------------------------ события

    private static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof AbstractClientPlayer p) || !p.level().isClientSide()) return;

        if (p == Minecraft.getInstance().player) {
            while (ClientFx.KEY.consumeClick()) {
                Net.CHANNEL.send(new ToggleRequest(), PacketDistributor.SERVER.noArg());
            }
        }
        ClientFx.tick(p);
    }

    /** Вместо ванильной отрисовки игрока рисуем нашим рендерером (если у игрока активен эффект). */
    private static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (rendering) return;
        if (!(event.getEntity() instanceof AbstractClientPlayer p)) return;

        PlayerFx fx = ClientFx.get(p.getUUID());
        if (fx == null || !fx.useCustom()) return;

        SkinShiftRenderer r = rendererFor(p);
        if (r == null) return;

        rendering = true;
        try {
            event.setCanceled(true);
            r.render(p, p.getYRot(), event.getPartialTick(), event.getPoseStack(),
                    event.getMultiBufferSource(), event.getPackedLight());
        } finally {
            rendering = false;
        }
    }

    /** Руки от первого лица тоже с новым скином. */
    private static void onRenderArm(RenderArmEvent event) {
        AbstractClientPlayer p = event.getPlayer();
        PlayerFx fx = ClientFx.get(p.getUUID());
        if (fx == null || !fx.useCustom()) return;

        SkinShiftRenderer r = rendererFor(p);
        if (r == null) return;

        event.setCanceled(true);
        if (event.getArm() == HumanoidArm.RIGHT) {
            r.renderRightHand(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), p);
        } else {
            r.renderLeftHand(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), p);
        }
    }

    private static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientFx.clearAll();
    }
}
