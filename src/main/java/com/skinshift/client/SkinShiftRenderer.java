package com.skinshift.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.PlayerModelPart;

/** Рендерер игрока с нашей моделью (поза рук) и динамической текстурой (смена скина). */
public class SkinShiftRenderer extends PlayerRenderer {
    public SkinShiftRenderer(EntityRendererProvider.Context ctx, boolean slim) {
        super(ctx, slim);
        this.model = new AnimatedPlayerModel(
                ctx.bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim);
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractClientPlayer player) {
        PlayerFx fx = ClientFx.get(player.getUUID());
        if (fx != null && fx.useCustom()) {
            return fx.getTexture();
        }
        return super.getTextureLocation(player);
    }

    // Ванильные renderRightHand/LeftHand берут player.getSkin().texture() напрямую (мимо getTextureLocation),
    // поэтому рисуем руки от первого лица сами, с нашей текстурой.
    @Override
    public void renderRightHand(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer p) {
        PlayerModel<AbstractClientPlayer> m = this.getModel();
        renderHandCustom(ps, buf, light, p, m.rightArm, m.rightSleeve);
    }

    @Override
    public void renderLeftHand(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer p) {
        PlayerModel<AbstractClientPlayer> m = this.getModel();
        renderHandCustom(ps, buf, light, p, m.leftArm, m.leftSleeve);
    }

    private void renderHandCustom(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer p,
                                  ModelPart arm, ModelPart sleeve) {
        PlayerModel<AbstractClientPlayer> m = this.getModel();
        m.setAllVisible(true);
        m.leftSleeve.visible = p.isModelPartShown(PlayerModelPart.LEFT_SLEEVE);
        m.rightSleeve.visible = p.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE);
        m.attackTime = 0.0F;
        m.crouching = false;
        m.swimAmount = 0.0F;
        m.setupAnim(p, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

        ResourceLocation tex = this.getTextureLocation(p);
        arm.xRot = 0.0F;
        arm.render(ps, buf.getBuffer(RenderType.entitySolid(tex)), light, OverlayTexture.NO_OVERLAY);
        sleeve.xRot = 0.0F;
        sleeve.render(ps, buf.getBuffer(RenderType.entityTranslucent(tex)), light, OverlayTexture.NO_OVERLAY);
    }
}
