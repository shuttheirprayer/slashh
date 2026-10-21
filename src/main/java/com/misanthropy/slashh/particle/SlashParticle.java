package com.misanthropy.slashh.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SlashParticle extends TextureSheetParticle {
    private static final int FULL_BRIGHT = 0xF000F0;

    private final SpriteSet sprites;
    private final float baseSize;
    private final float aspect;
    private final float fullAlpha;
    private final int delay;
    private final float grow;

    public SlashParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites, float rollDeg, float size,
                         float aspect, int rgb, float alpha, int delay, int lifetime, float grow) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.baseSize = size;
        this.aspect = aspect;
        this.fullAlpha = alpha;
        this.delay = delay;
        this.grow = grow;
        this.lifetime = lifetime + delay;
        this.gravity = 0.0f;
        this.hasPhysics = false;
        this.xd = this.yd = this.zd = 0.0;
        this.roll = this.oRoll = (float) Math.toRadians(rollDeg);
        this.quadSize = size;
        this.alpha = delay > 0 ? 0.0f : alpha;
        if (rgb >= 0) setColor(((rgb >> 16) & 255) / 255.0f, ((rgb >> 8) & 255) / 255.0f, (rgb & 255) / 255.0f);
        setSprite(sprites.get(0, lifetime));
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            remove();
            return;
        }
        int active = this.age - this.delay;
        if (active < 0) return;
        this.alpha = this.fullAlpha;
        setSprite(this.sprites.get(active, this.lifetime - this.delay));
    }

    @Override
    public float getQuadSize(float partialTicks) {
        float t = Mth.clamp((this.age - this.delay + partialTicks) / (float) (this.lifetime - this.delay), 0.0f, 1.0f);
        return this.baseSize * (1.0f - this.grow * 0.5f + this.grow * t);
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        if (this.alpha <= 0.0f) return;
        Vec3 cam = camera.getPosition();
        float px = (float) (Mth.lerp(partialTicks, this.xo, this.x) - cam.x());
        float py = (float) (Mth.lerp(partialTicks, this.yo, this.y) - cam.y());
        float pz = (float) (Mth.lerp(partialTicks, this.zo, this.z) - cam.z());
        Quaternionf rotation = new Quaternionf(camera.rotation()).rotateZ(Mth.lerp(partialTicks, this.oRoll, this.roll));
        float size = getQuadSize(partialTicks);
        Vector3f[] corners = {
                new Vector3f(-this.aspect, -1.0f, 0.0f), new Vector3f(-this.aspect, 1.0f, 0.0f),
                new Vector3f(this.aspect, 1.0f, 0.0f), new Vector3f(this.aspect, -1.0f, 0.0f)};
        for (Vector3f corner : corners) corner.rotate(rotation).mul(size).add(px, py, pz);
        float u0 = getU0();
        float u1 = getU1();
        float v0 = getV0();
        float v1 = getV1();
        buffer.vertex(corners[0].x(), corners[0].y(), corners[0].z()).uv(u1, v1).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(FULL_BRIGHT).endVertex();
        buffer.vertex(corners[1].x(), corners[1].y(), corners[1].z()).uv(u1, v0).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(FULL_BRIGHT).endVertex();
        buffer.vertex(corners[2].x(), corners[2].y(), corners[2].z()).uv(u0, v0).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(FULL_BRIGHT).endVertex();
        buffer.vertex(corners[3].x(), corners[3].y(), corners[3].z()).uv(u0, v1).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(FULL_BRIGHT).endVertex();
    }

    @Override
    protected int getLightColor(float partialTicks) {
        return FULL_BRIGHT;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
