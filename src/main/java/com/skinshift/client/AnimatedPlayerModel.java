package com.skinshift.client;

import com.skinshift.Anim;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;

/** Модель игрока, которая после ванильной позы накладывает позу "руки подняты к груди". */
public class AnimatedPlayerModel extends PlayerModel<AbstractClientPlayer> {
    private static final float ARM_X = -1.25f;   // руки вперёд, на уровень груди
    private static final float ARM_Y = 0.60f;    // руки сведены к центру
    private static final float ARM_Z = 0.0f;

    public AnimatedPlayerModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    @Override
    public void setupAnim(AbstractClientPlayer p, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(p, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        PlayerFx fx = ClientFx.get(p.getUUID());
        if (fx == null || !fx.isAnimating()) return;

        float t = ageInTicks - fx.getStartAge();
        float k = Anim.armBlend(t);
        if (k <= 0f) return;

        // лёгкое "дыхание" рук, пока крутится вихрь
        float sway = Mth.sin(ageInTicks * 0.35f) * 0.04f * k;

        this.rightArm.xRot = Mth.lerp(k, this.rightArm.xRot, ARM_X + sway);
        this.rightArm.yRot = Mth.lerp(k, this.rightArm.yRot, -ARM_Y);
        this.rightArm.zRot = Mth.lerp(k, this.rightArm.zRot, -ARM_Z);

        this.leftArm.xRot = Mth.lerp(k, this.leftArm.xRot, ARM_X + sway);
        this.leftArm.yRot = Mth.lerp(k, this.leftArm.yRot, ARM_Y);
        this.leftArm.zRot = Mth.lerp(k, this.leftArm.zRot, ARM_Z);

        this.rightSleeve.copyFrom(this.rightArm);
        this.leftSleeve.copyFrom(this.leftArm);
    }
}
