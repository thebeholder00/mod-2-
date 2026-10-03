package com.skinshift.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/** Яркая жёлтая искра, которая летит по спирали вокруг центра (позиции игрока) и поднимается вверх. */
public class VortexParticle extends TextureSheetParticle {
    private final double cx;
    private final double cz;
    private final float rise;
    private final float r0;
    private float angle;
    private final float baseSize;

    protected VortexParticle(ClientLevel level, double x, double y, double z,
                             double startAngle, double rise, double radius, SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.cx = x;
        this.cz = z;
        this.angle = (float) startAngle;
        this.rise = (float) rise;
        this.r0 = (float) radius;
        this.lifetime = 24 + this.random.nextInt(14);
        this.baseSize = 0.16f + this.random.nextFloat() * 0.12f;
        this.quadSize = baseSize;
        this.hasPhysics = false;
        this.setColor(1.0f, 0.86f, 0.08f);   // ярко-жёлтый
        this.pickSprite(sprites);
        this.setPos(x + Mth.cos(angle) * r0, y, z + Mth.sin(angle) * r0);
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        float f = this.age / (float) this.lifetime;
        this.angle += 0.30f;
        float r = r0 * (1.0f - 0.35f * f);
        double ny = this.y + this.rise;
        this.setPos(cx + Mth.cos(angle) * r, ny, cz + Mth.sin(angle) * r);

        this.alpha = f < 0.65f ? 1.0f : 1.0f - (f - 0.65f) / 0.35f;
        this.quadSize = baseSize * (1.0f - 0.4f * f);
    }

    /** Полная яркость независимо от освещения. */
    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double startAngle, double rise, double radius) {
            return new VortexParticle(level, x, y, z, startAngle, rise, radius, sprites);
        }
    }
}
