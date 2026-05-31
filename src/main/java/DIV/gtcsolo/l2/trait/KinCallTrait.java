package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.trait.base.TypedMobTrait;
import DIV.gtcsolo.l2.util.L2TraitAttributes;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;

import java.util.UUID;

/**
 * [34] Kin Call (類は友を呼ぶ) — ゾンビ専用。
 * バニラの「被弾時に味方を呼ぶ」 確率を強化する。
 *
 * <p>仕様: {@code SPAWN_REINFORCEMENTS_CHANCE} に固定 +100% (MULTIPLY_TOTAL = 2 倍) の modifier (level 非依存)。
 * デフォ値は出現時 random 0..0.1 (= 0~10%) で、 これを倍化する。
 *
 * <p>派生対応: {@code instanceof Zombie} で Husk / Drowned / ZombieVillager / ZombifiedPiglin
 * 全てカバー (= 全て Zombie を継承)。
 */
public class KinCallTrait extends TypedMobTrait {

    private static final UUID MOD_KIN_CALL = UUID.fromString("3f8a2b1d-1c4e-4a7b-9c0a-7e8f1d2c3b4a");
    private static final double MULTIPLIER = 1.0;  // reinforcement chance を固定で 2 倍 (level 非依存)

    public KinCallTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    protected boolean isValidTarget(LivingEntity mob) {
        return mob instanceof Zombie;
    }

    @Override
    protected void onValidPostInit(LivingEntity mob, int lv) {
        L2TraitAttributes.addPermanentIfAbsent(mob, Attributes.SPAWN_REINFORCEMENTS_CHANCE, MOD_KIN_CALL,
                "gtcsolo.kin_call", MULTIPLIER, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
