package com.misanthropy.slashh.api;

import com.misanthropy.slashh.event.CombatEventHandler;
import net.minecraft.world.entity.LivingEntity;

public final class SlashhApi {
    private SlashhApi() {}

    public static void tint(LivingEntity target, int rgb, boolean spray) {
        if (target.level().isClientSide()) return;
        CombatEventHandler.tint(target, rgb, spray);
    }
}
