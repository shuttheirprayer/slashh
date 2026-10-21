package com.misanthropy.slashh.event;

import com.misanthropy.slashh.SlashConfig;
import com.misanthropy.slashh.Slashh;
import com.misanthropy.slashh.compat.CritHelper;
import com.misanthropy.slashh.compat.SwingShape;
import com.misanthropy.slashh.network.SlashEffectPacket;
import com.misanthropy.slashh.network.SlashNetwork;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Slashh.MODID)
public final class CombatEventHandler {
    public static final TagKey<Item> SLASHES = tag("slashes");
    public static final TagKey<Item> NO_SLASH = tag("no_slash");
    public static final TagKey<Item> HEAVY = tag("heavy");
    public static final TagKey<Item> QUICK = tag("quick");

    private static final float MAX_ROLL_DEGREES = 45.0f;

    private record Tint(int rgb, boolean spray, long tick) {}

    private record Combo(int count, long last) {}

    private static final Map<UUID, Combo> COMBOS = new HashMap<>();
    private static final Map<UUID, Integer> SENT_THIS_TICK = new HashMap<>();
    private static final Int2ObjectOpenHashMap<Tint> TINTS = new Int2ObjectOpenHashMap<>();
    private static long capTick = Long.MIN_VALUE;

    private CombatEventHandler() {}

    private static TagKey<Item> tag(String path) {
        return TagKey.create(Registries.ITEM, new ResourceLocation(Slashh.MODID, path));
    }

    public static boolean slashes(ItemStack weapon) {
        return weapon.is(SLASHES) && !weapon.is(NO_SLASH);
    }

    public static void tint(LivingEntity target, int rgb, boolean spray) {
        TINTS.put(target.getId(), new Tint(rgb, spray, target.level().getGameTime()));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        COMBOS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        Level level = target.level();
        if (level.isClientSide) return;
        long now = level.getGameTime();
        if (now != capTick) {
            capTick = now;
            SENT_THIS_TICK.clear();
            if (TINTS.size() > 256) TINTS.clear();
        }
        Tint tint = TINTS.remove(target.getId());
        if (!SlashConfig.enabled || !(event.getSource().getDirectEntity() instanceof Player player) || target == player) return;
        ItemStack weapon = player.getMainHandItem();
        if (!slashes(weapon)) return;

        UUID id = player.getUUID();
        int sent = SENT_THIS_TICK.getOrDefault(id, 0);
        if (sent >= SlashConfig.maxEffectsPerTick) return;
        SENT_THIS_TICK.put(id, sent + 1);
        boolean sweep = sent > 0;

        Combo combo = COMBOS.get(id);
        int count = sweep && combo != null ? combo.count() : combo != null && now - combo.last() <= SlashConfig.comboResetTicks ? combo.count() + 1 : 0;
        COMBOS.put(id, new Combo(count, now));

        RandomSource rng = level.getRandom();
        boolean heavy = weapon.is(HEAVY);
        boolean quick = weapon.is(QUICK);
        SwingShape shape = SwingShape.of(player);
        float rollDeg = 0.0f;
        if (shape == null) {
            float variance = heavy ? 10.0f + rng.nextFloat() * 20.0f : 15.0f + rng.nextFloat() * 30.0f;
            rollDeg = Mth.clamp((count % 2 == 0 ? 1.0f : -1.0f) * variance, -MAX_ROLL_DEGREES, MAX_ROLL_DEGREES);
        }
        float size = (shape != null ? shape.length() : heavy ? 1.4f : quick ? 0.75f : 1.0f) * damageScale(event.getAmount());

        int flags = 0;
        if (CritHelper.isCrit(player, target)) {
            flags |= SlashEffectPacket.CRIT;
            size *= 1.15f;
        }
        int every = SlashConfig.finisherEvery;
        boolean finisher = shape != null ? shape.last() : every > 0 && count > 0 && (count + 1) % every == 0;
        if (!sweep && every > 0 && finisher) flags |= SlashEffectPacket.FINISHER;
        if (sweep) flags |= SlashEffectPacket.SWEEP;
        if (shape != null && shape.offHand()) flags |= SlashEffectPacket.OFFHAND;
        boolean fresh = tint != null && tint.tick() == now;
        int rgb = fresh ? tint.rgb() : -1;
        if (fresh && tint.spray()) flags |= SlashEffectPacket.SPRAY;

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 pos = impact(target, eye, look);
        SlashNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target),
                new SlashEffectPacket(pos.x, pos.y, pos.z, rollDeg, size, rgb, flags, player.getId(), (float) look.x, (float) look.y, (float) look.z,
                        shape == null ? "" : shape.animation(), target.getId()));
    }

    static float damageScale(float damage) {
        return Math.min(3.5f, 1.0f + 0.8f * (float) Math.pow(Math.max(0.0f, damage) / 100.0f, 1.3));
    }

    private static Vec3 impact(LivingEntity target, Vec3 eye, Vec3 look) {
        AABB box = target.getBoundingBox().inflate(0.15);
        Optional<Vec3> hit = box.clip(eye, eye.add(look.scale(6.0)));
        if (hit.isPresent()) return hit.get();
        double y = Mth.clamp(eye.y, target.getY(), target.getY() + target.getBbHeight());
        Vec3 center = new Vec3(target.getX(), y, target.getZ());
        Vec3 toPlayer = eye.subtract(center);
        toPlayer = toPlayer.lengthSqr() < 1.0e-4 ? look.scale(-1.0) : toPlayer.normalize();
        return center.add(toPlayer.scale(target.getBbWidth() * 0.5 + 0.1));
    }
}
