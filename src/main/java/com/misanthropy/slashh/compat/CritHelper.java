package com.misanthropy.slashh.compat;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class CritHelper {
    private static Boolean bcaAvailable = null;

    private CritHelper() {
    }

    public static boolean isCrit(Player player, LivingEntity target) {
        if (isBcaAvailable()) {
            return BcaBridge.isJumpCrit(player, target);
        }
        return isVanillaCrit(player);
    }

    private static boolean isBcaAvailable() {
        if (bcaAvailable == null) {
            try {
                Class.forName("com.misanthropy.linggango.bettercombatadditions.crit.JumpCrit", false, CritHelper.class.getClassLoader());
                bcaAvailable = true;
            } catch (ClassNotFoundException e) {
                bcaAvailable = false;
            }
        }
        return bcaAvailable;
    }

    private static class BcaBridge {
        private static boolean isJumpCrit(Player player, LivingEntity target) {
            return com.misanthropy.linggango.bettercombatadditions.crit.JumpCrit.isJumpCrit(player, target);
        }
    }

    public static boolean isVanillaCrit(Player player) {
        return player.fallDistance > 0.0F
                && !player.onGround()
                && !player.onClimbable()
                && !player.isInWater()
                && !player.hasEffect(MobEffects.BLINDNESS)
                && !player.isPassenger();
    }
}
