package com.misanthropy.slashh.compat;

import net.bettercombat.api.AttackHand;
import net.bettercombat.api.EntityPlayer_BetterCombat;
import net.bettercombat.api.WeaponAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public record SwingShape(String animation, boolean offHand, boolean last, float length) {
    private static final boolean BETTER_COMBAT = ModList.get().isLoaded("bettercombat");
    private static final Set<String> LONG_BLADES = Set.of("claymore", "greatsword", "katana", "warglaive", "twin_blade", "coral_blade");
    private static final Set<String> HEADS = Set.of("halberd", "glaive", "spear", "lance", "trident", "scythe", "sickle", "greataxe",
            "heavy_axe", "double_axe", "axe", "greathammer", "hammer", "mace", "anchor", "battlestaff", "quarterstaff", "staff", "pickaxe");
    private static final Set<String> SMALL = Set.of("dagger", "sai", "claw", "fist", "soul_knife", "rapier", "sickle");

    public static @Nullable SwingShape of(Player player) {
        return BETTER_COMBAT ? Bridge.of(player) : null;
    }

    static float length(@Nullable WeaponAttributes attributes) {
        if (attributes == null) return 1.0f;
        String category = attributes.category() == null ? "" : attributes.category();
        if (SMALL.contains(category)) return 0.75f;
        if (LONG_BLADES.contains(category)) return 1.6f;
        if (HEADS.contains(category)) return 1.3f;
        return attributes.isTwoHanded() ? 1.5f : 1.0f;
    }

    private static final class Bridge {
        static @Nullable SwingShape of(Player player) {
            if (!(player instanceof EntityPlayer_BetterCombat bc)) return null;
            AttackHand hand = bc.getCurrentAttack();
            if (hand == null || hand.attack() == null || hand.attack().animation() == null) return null;
            boolean last = hand.combo() != null && hand.combo().total() > 1 && hand.combo().current() == hand.combo().total();
            return new SwingShape(hand.attack().animation(), hand.isOffHand(), last, length(hand.attributes()));
        }
    }
}
