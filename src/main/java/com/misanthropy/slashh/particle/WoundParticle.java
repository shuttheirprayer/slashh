package com.misanthropy.slashh.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class WoundParticle extends TextureSheetParticle {
    private static final int SEGMENTS = 24;
    private static final int LIGHT = 0xF000F0;
    private static final float PEAK = 0.88f;
    private static final double BEND = 0.07;
    private static final double CARVE = 1.4;
    private static final double HOLD = 6.0;
    private static final double FADE = 4.0;
    private static final float[][] LAYERS = {{2.3f, 0.38f, 0.0f}, {1.0f, 0.95f, 0.0f}, {0.42f, 1.0f, 0.9f}};

    private final @Nullable Entity anchor;
    private final Vec3 offset;
    private final Vec3 dir;
    private final Vec3 bulge;
    private final double length;
    private final double width;
    private final int delay;
    private final float strength;
    private final boolean jitter;
    private final float[][] tint;
    private final Vec3[] spine = new Vec3[SEGMENTS + 1];
    private final Vec3[] across = new Vec3[SEGMENTS + 1];

    public WoundParticle(ClientLevel level, SpriteSet sprites, @Nullable Entity anchor, Vec3 center, Vec3 dir, Vec3 bulge, double length,
                         boolean crit, boolean jitter, int delay, int rgb, float alpha) {
        super(level, center.x, center.y, center.z);
        this.anchor = anchor;
        this.offset = anchor == null ? center : center.subtract(anchor.position());
        this.dir = dir.normalize();
        Vec3 side = bulge.subtract(this.dir.scale(bulge.dot(this.dir)));
        this.bulge = side.lengthSqr() < 1.0e-8 ? Vec3.ZERO : side.normalize();
        this.length = length;
        this.width = length * (crit ? 0.035 : 0.0275);
        this.delay = delay;
        this.strength = alpha;
        this.jitter = jitter;
        float r = (rgb >> 16 & 255) / 255.0f;
        float g = (rgb >> 8 & 255) / 255.0f;
        float b = (rgb & 255) / 255.0f;
        this.tint = new float[LAYERS.length][];
        for (int l = 0; l < LAYERS.length; l++) {
            float m = LAYERS[l][2];
            this.tint[l] = new float[]{Mth.lerp(m, r, 1.0f), Mth.lerp(m, g, 1.0f), Mth.lerp(m, b, 1.0f)};
        }
        this.lifetime = delay + (int) Math.ceil(HOLD + FADE) + 1;
        this.gravity = 0.0f;
        this.hasPhysics = false;
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        if (this.age++ >= this.lifetime || this.anchor != null && this.anchor.isRemoved()) remove();
    }

    @Override
    public void render(VertexConsumer vc, Camera camera, float partialTicks) {
        double t = this.age + partialTicks - this.delay;
        if (t < 0.0) return;
        double head = ease(t / CARVE);
        double fade = t < HOLD ? 1.0 : 1.0 - (t - HOLD) / FADE;
        double flicker = t > 4.0 ? 0.85 + 0.15 * Math.sin(t * 9.0) : 1.0;
        float alpha = (float) (Mth.clamp(fade, 0.0, 1.0) * flicker) * this.strength;
        if (head < 0.004 || alpha <= 0.0f) return;
        Vec3 base = this.anchor == null ? this.offset : this.anchor.getPosition(partialTicks).add(this.offset);
        Vec3 c = base.subtract(camera.getPosition());
        double w = this.width * Mth.lerp(Mth.clamp(t - 1.5, 0.0, 1.0), 1.0, 0.55);
        for (int i = 0; i <= SEGMENTS; i++) {
            double d = (double) i / SEGMENTS * head - 0.5;
            spine[i] = c.add(this.dir.scale(d * this.length)).add(this.bulge.scale(BEND * (1.0 - 4.0 * d * d) * this.length));
        }
        for (int i = 0; i <= SEGMENTS; i++) {
            Vec3 tangent = spine[Math.min(SEGMENTS, i + 1)].subtract(spine[Math.max(0, i - 1)]);
            Vec3 a = tangent.cross(spine[i]);
            across[i] = a.lengthSqr() < 1.0e-12 ? this.bulge : a.normalize();
        }
        for (int l = 0; l < LAYERS.length; l++) {
            double pull = 1.0 - 0.0015 * (l + 1);
            float la = alpha * LAYERS[l][1];
            float[] rgb = this.tint[l];
            for (int i = 0; i < SEGMENTS; i++) {
                double s0 = (double) i / SEGMENTS;
                double s1 = (double) (i + 1) / SEGMENTS;
                Vec3 p0 = spine[i].add(across[i].scale(shake(i, t, w))).scale(pull);
                Vec3 p1 = spine[i + 1].add(across[i + 1].scale(shake(i + 1, t, w))).scale(pull);
                Vec3 o0 = across[i].scale(w * LAYERS[l][0] * Math.pow(Math.sin(Math.PI * s0), 0.7));
                Vec3 o1 = across[i + 1].scale(w * LAYERS[l][0] * Math.pow(Math.sin(Math.PI * s1), 0.7));
                float ua = Mth.lerp((float) s0, getU0(), getU1());
                float ub = Mth.lerp((float) s1, getU0(), getU1());
                quad(vc, p0.subtract(o0), p0, p1, p1.subtract(o1), ua, ub, 0.0f, PEAK, rgb, la);
                quad(vc, p0, p0.add(o0), p1.add(o1), p1, ua, ub, PEAK, 0.0f, rgb, la);
            }
        }
    }

    private double shake(int i, double t, double w) {
        return this.jitter ? Math.sin(i * 12.9 + t * 7.1) * 0.6 * w : 0.0;
    }

    private void quad(VertexConsumer vc, Vec3 a0, Vec3 b0, Vec3 b1, Vec3 a1, float ua, float ub, float va, float vb, float[] rgb, float alpha) {
        float v0 = Mth.lerp(va, getV0(), getV1());
        float v1 = Mth.lerp(vb, getV0(), getV1());
        vertex(vc, a0, ua, v0, rgb, alpha);
        vertex(vc, b0, ua, v1, rgb, alpha);
        vertex(vc, b1, ub, v1, rgb, alpha);
        vertex(vc, a1, ub, v0, rgb, alpha);
        vertex(vc, a1, ub, v0, rgb, alpha);
        vertex(vc, b1, ub, v1, rgb, alpha);
        vertex(vc, b0, ua, v1, rgb, alpha);
        vertex(vc, a0, ua, v0, rgb, alpha);
    }

    private static void vertex(VertexConsumer vc, Vec3 p, float u, float v, float[] rgb, float a) {
        vc.vertex(p.x, p.y, p.z).uv(u, v).color(rgb[0], rgb[1], rgb[2], a).uv2(LIGHT).endVertex();
    }

    private static double ease(double x) {
        double k = 1.0 - Mth.clamp(x, 0.0, 1.0);
        return 1.0 - k * k * k;
    }

    @Override
    public boolean shouldCull() {
        return false;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
