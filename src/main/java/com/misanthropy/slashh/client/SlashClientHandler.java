package com.misanthropy.slashh.client;

import com.misanthropy.slashh.SlashClientConfig;
import com.misanthropy.slashh.SlashConfig;
import com.misanthropy.slashh.network.SlashEffectPacket;
import com.misanthropy.slashh.particle.DropletParticle;
import com.misanthropy.slashh.particle.SlashParticle;
import com.misanthropy.slashh.particle.WoundParticle;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class SlashClientHandler {
    private static final int BLOOD = 0xB01020;
    private static final int PLAIN = 0xDCE6F8;
    private static final int CRIT = 0xFFC070;
    private static final int LIGHTNING = 0xFFE14D;

    private record Arc(Vec3 center, Vec3 forward, Vec3 side, Vec3 normal, double radius, double hit, double lift) {
        static Arc through(Vec3 center, Vec3 forward, Vec3 side, Vec3 target) {
            Vec3 normal = forward.cross(side).normalize();
            Vec3 rel = target.subtract(center);
            double lift = rel.dot(normal);
            Vec3 flat = rel.subtract(normal.scale(lift));
            return new Arc(center, forward, side, normal, Math.max(0.6, flat.length()), Math.atan2(flat.dot(side), flat.dot(forward)), lift);
        }

        Vec3 at(double theta) {
            return center.add(forward.scale(Math.cos(theta) * radius)).add(side.scale(Math.sin(theta) * radius)).add(normal.scale(lift));
        }

        Vec3 travel() {
            return forward.scale(Math.sin(hit)).subtract(side.scale(Math.cos(hit)));
        }

        Vec3 outward() {
            return forward.scale(Math.cos(hit)).add(side.scale(Math.sin(hit)));
        }
    }

    private static long capTick = Long.MIN_VALUE;
    private static int spawnedThisTick = 0;

    private SlashClientHandler() {}

    private static boolean ready(ClientLevel level) {
        if (!SlashClientConfig.show || level == null || SlashClientEvents.slash == null) return false;
        long now = level.getGameTime();
        if (now != capTick) {
            capTick = now;
            spawnedThisTick = 0;
        }
        if (spawnedThisTick >= SlashConfig.maxEffectsPerTick) return false;
        spawnedThisTick++;
        return true;
    }

    private static boolean mine(int attacker) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getId() == attacker;
    }

    private static void add(Particle particle) {
        Minecraft.getInstance().particleEngine.add(particle);
    }

    public static void slash(SlashEffectPacket m) {
        ClientLevel level = Minecraft.getInstance().level;
        boolean mine = mine(m.attacker());
        if (!mine && !SlashClientConfig.others) return;
        if (!ready(level)) return;
        boolean crit = m.has(SlashEffectPacket.CRIT);
        int rgb = SlashClientConfig.colours ? m.rgb() : -1;
        if (crit && rgb >= 0) rgb = brighten(rgb);
        List<SwingSpecs.Spec> specs = m.animation().isEmpty() ? List.of(SwingSpecs.arc(m.rollDeg())) : SwingSpecs.specs(m.animation());
        if (specs.isEmpty()) return;
        if (specs.stream().allMatch(SwingSpecs.Spec::stab)) {
            if (SlashClientConfig.classic || SlashClientEvents.wound == null) stab(level, m, rgb, crit);
            else pierce(level, m, rgb, crit);
            if (crit && mine) ScreenShake.add(0.25f);
            return;
        }
        Vec3 impact = new Vec3(m.x(), m.y(), m.z());
        Vec3 look = new Vec3(m.dx(), m.dy(), m.dz());
        Entity attacker = level.getEntity(m.attacker());
        boolean leftArm = attacker instanceof LivingEntity living && living.getMainArm() == HumanoidArm.LEFT;
        boolean mirror = m.has(SlashEffectPacket.OFFHAND) != leftArm;
        Vec3 eye = attacker != null ? attacker.getEyePosition() : impact.subtract(look.scale(2.5));
        float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
        float pitch = (float) Math.toDegrees(-Math.asin(Mth.clamp(look.y, -1.0, 1.0)));
        Vec3 right = Vec3.directionFromRotation(0.0f, yaw + 90.0f);
        boolean sweep = m.has(SlashEffectPacket.SWEEP) && SlashClientConfig.sweeps;
        float length = m.size() * SlashClientConfig.size * (sweep ? 1.25f : 1.0f);
        double mx = specs.stream().filter(sp -> !sp.stab()).mapToDouble(SwingSpecs.Spec::x).average().orElse(0.0);
        double my = specs.stream().filter(sp -> !sp.stab()).mapToDouble(SwingSpecs.Spec::y).average().orElse(0.0);
        boolean finisher = m.has(SlashEffectPacket.FINISHER) && SlashClientConfig.finishers;
        for (SwingSpecs.Spec spec : specs) {
            if (spec.stab()) continue;
            Vec3 target = impact.add(right.scale((mirror ? -1.0 : 1.0) * (spec.x() - mx))).add(0.0, spec.y() - my, 0.0);
            spawn(level, m, arc(spec, spec.tilt(), eye, yaw, pitch, mirror, target), length, 0, rgb, crit, sweep);
            if (finisher) {
                spawn(level, m, arc(spec, spec.tilt() + 90.0, eye, yaw, pitch, mirror, target), length * 1.2f, 2, rgb, crit, false);
                finisher = false;
            }
        }
        if (m.has(SlashEffectPacket.FINISHER) && SlashClientConfig.finishers) {
            if (mine) ScreenShake.add(0.45f);
        } else if (crit && mine) {
            ScreenShake.add(0.25f);
        }
        if (m.has(SlashEffectPacket.SPRAY) && SlashClientConfig.spray) {
            spray(level, m.x(), m.y(), m.z(), look, m.rgb() >= 0 ? m.rgb() : BLOOD, crit ? 12 : 7);
        }
    }

    private static Arc arc(SwingSpecs.Spec spec, double tiltDeg, Vec3 eye, float yaw, float pitch, boolean mirror, Vec3 target) {
        double tilt = Math.toRadians(mirror ? 180.0 - tiltDeg : tiltDeg);
        Vec3 right = Vec3.directionFromRotation(0.0f, yaw + 90.0f);
        Vec3 forward = Vec3.directionFromRotation(pitch + (float) spec.pitch(), yaw);
        Vec3 up = right.cross(forward);
        Vec3 side = right.scale(Math.cos(tilt)).subtract(up.scale(Math.sin(tilt)));
        Vec3 center = new Vec3(eye.x, eye.y - 0.25 + spec.y(), eye.z).add(forward.scale(spec.z())).add(right.scale(mirror ? -spec.x() : spec.x()));
        return Arc.through(center, forward, side, target);
    }

    private static void spawn(ClientLevel level, SlashEffectPacket m, Arc arc, float length, int delay, int rgb, boolean crit, boolean sweep) {
        float alpha = SlashClientConfig.opacity * (sweep ? 0.8f : 1.0f);
        Vec3 at = arc.at(arc.hit());
        if (SlashClientConfig.classic) {
            SpriteSet sprites = crit && rgb < 0 ? SlashClientEvents.crit : SlashClientEvents.slash;
            add(new SlashParticle(level, at.x, at.y, at.z, sprites, classicRoll(arc), length * 0.5f, sweep ? 1.3f : 1.0f, rgb, alpha, delay, 10, 0.2f));
        } else {
            wound(level, m, at, arc.travel(), arc.outward(), length, delay, rgb, crit, alpha);
        }
    }

    private static void wound(ClientLevel level, SlashEffectPacket m, Vec3 at, Vec3 dir, Vec3 bulge, float length, int delay, int rgb, boolean crit, float alpha) {
        if (SlashClientEvents.wound == null) return;
        add(new WoundParticle(level, SlashClientEvents.wound, level.getEntity(m.target()), at, dir, bulge, length, crit,
                m.rgb() == LIGHTNING, delay, rgb >= 0 ? rgb : crit ? CRIT : PLAIN, alpha));
    }

    private static float screenAngle(Vec3 dir) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        double left = dir.dot(new Vec3(camera.getLeftVector()));
        double up = dir.dot(new Vec3(camera.getUpVector()));
        return left * left + up * up < 1.0e-4 ? 0.0f : (float) Math.toDegrees(Math.atan2(up, left));
    }

    private static float classicRoll(Arc arc) {
        return screenAngle(arc.travel()) - 135.0f;
    }

    private static void pierce(ClientLevel level, SlashEffectPacket m, int rgb, boolean crit) {
        Vec3 impact = new Vec3(m.x(), m.y(), m.z());
        Vec3 dir = new Vec3(m.dx(), m.dy(), m.dz());
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        Vec3 slit = up.subtract(dir.scale(up.dot(dir) / Math.max(1.0e-6, dir.lengthSqr())));
        if (slit.lengthSqr() < 1.0e-6) slit = new Vec3(1.0, 0.0, 0.0);
        wound(level, m, impact, slit, dir.cross(slit), m.size() * SlashClientConfig.size * 0.6f, 0, rgb, crit, SlashClientConfig.opacity);
        if (m.has(SlashEffectPacket.SPRAY) && SlashClientConfig.spray) spray(level, m.x(), m.y(), m.z(), dir, m.rgb() >= 0 ? m.rgb() : BLOOD, crit ? 10 : 6);
    }

    private static void stab(ClientLevel level, SlashEffectPacket m, int rgb, boolean crit) {
        RandomSource r = level.getRandom();
        Vec3 dir = new Vec3(m.dx(), m.dy(), m.dz());
        int count = crit ? 12 : 7;
        for (int i = 0; i < count; i++) {
            Vec3 v = dir.scale(0.25 + r.nextDouble() * 0.35).add(r.nextGaussian() * 0.08, r.nextGaussian() * 0.08, r.nextGaussian() * 0.08);
            level.addParticle(crit ? ParticleTypes.ENCHANTED_HIT : ParticleTypes.CRIT, m.x(), m.y(), m.z(), v.x, v.y, v.z);
        }
        if (m.has(SlashEffectPacket.SPRAY) && SlashClientConfig.spray) spray(level, m.x(), m.y(), m.z(), dir, m.rgb() >= 0 ? m.rgb() : BLOOD, crit ? 10 : 6);
    }

    private static void spray(ClientLevel level, double x, double y, double z, Vec3 dir, int rgb, int count) {
        if (SlashClientEvents.droplet == null) return;
        RandomSource r = level.getRandom();
        for (int i = 0; i < count; i++) {
            double speed = 0.12 + r.nextDouble() * 0.18;
            Vec3 v = dir.scale(speed * 0.8).add(r.nextGaussian() * 0.09, 0.08 + r.nextDouble() * 0.16, r.nextGaussian() * 0.09);
            add(new DropletParticle(level, x, y, z, v.x, v.y, v.z, SlashClientEvents.droplet, rgb, 0.035f + r.nextFloat() * 0.035f));
        }
    }

    private static int brighten(int rgb) {
        int r = (rgb >> 16) & 255;
        int g = (rgb >> 8) & 255;
        int b = rgb & 255;
        return ((r + (255 - r) / 3) << 16) | ((g + (255 - g) / 3) << 8) | (b + (255 - b) / 3);
    }
}
