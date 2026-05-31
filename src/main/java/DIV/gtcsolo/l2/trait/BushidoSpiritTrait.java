package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.ModL2Traits;
import DIV.gtcsolo.l2.trait.base.TypedMobTrait;
import DIV.gtcsolo.l2.util.L2EntityUtil;
import DIV.gtcsolo.l2.util.L2TraitAttributes;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * [20] Bushido Spirit — ボス専用。 ステータス全強化 + Innocence Battle 内部付与 + 周囲他 mob 排除。
 *
 * <p>除外: MASTER minion (= MobTraitCap で master 親リンク持ち) + Cataclysm ボス階級。
 *
 * <p>仕様上ボス専用 = {@link TypedMobTrait#isValidTarget} で {@link L2EntityUtil#isBoss} を必須化。
 * datapack/command/誤継承で雑魚に付いた場合は全 hook が no-op となり buff されない。
 */
public class BushidoSpiritTrait extends TypedMobTrait {

    private static final UUID MOD_HP = UUID.fromString("b1f3e9c7-3d44-6b1b-9a14-7b8e2b0c1e3a");
    private static final UUID MOD_ATK = UUID.fromString("b2f3e9c7-3d44-6b1b-9a14-7b8e2b0c1e3a");
    private static final UUID MOD_DEF = UUID.fromString("b3f3e9c7-3d44-6b1b-9a14-7b8e2b0c1e3a");

    public BushidoSpiritTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    protected boolean isValidTarget(LivingEntity mob) {
        return L2EntityUtil.isBoss(mob);
    }

    @Override
    protected void onValidPostInit(LivingEntity mob, int lv) {
        // ステータス強化 (= HP +(125 + 25n)%、 ATTACK +(25 + 25n)%、 防具 +10n 実数)
        L2TraitAttributes.addPermanentIfAbsent(mob, Attributes.MAX_HEALTH, MOD_HP, "gtcsolo.bushido",
                1.25 + 0.25 * lv, AttributeModifier.Operation.MULTIPLY_BASE);
        L2TraitAttributes.addPermanentIfAbsent(mob, Attributes.ATTACK_DAMAGE, MOD_ATK, "gtcsolo.bushido",
                0.25 + 0.25 * lv, AttributeModifier.Operation.MULTIPLY_BASE);
        L2TraitAttributes.addPermanentIfAbsent(mob, Attributes.ARMOR, MOD_DEF, "gtcsolo.bushido",
                10.0 * lv, AttributeModifier.Operation.ADDITION);
        mob.setHealth(mob.getMaxHealth());

        // Innocence Battle を内部付与
        MobTraitCap cap = MobTraitCap.HOLDER.get(mob);
        if (!cap.hasTrait(ModL2Traits.INNOCENCE_BATTLE.get())) {
            cap.setTrait(ModL2Traits.INNOCENCE_BATTLE.get(), lv);
        }
    }

    @Override
    protected void onValidTick(LivingEntity mob, int level) {
        if (mob.level().isClientSide()) return;
        if (mob.tickCount % 40 != 0) return; // 2 秒おき

        // 周囲 mob 排除 (= 例外を除く)
        var area = mob.getBoundingBox().inflate(32);
        for (Mob other : mob.level().getEntitiesOfClass(Mob.class, area, e -> e != mob)) {
            if (isExcluded(other)) continue;
            other.discard();
        }
    }

    private static boolean isExcluded(Mob other) {
        // MASTER minion: MobTraitCap.minion フラグ持ち
        if (MobTraitCap.HOLDER.isProper(other) && MobTraitCap.HOLDER.get(other).minion) return true;
        // Cataclysm ボス階級 (= L2EntityUtil に集約)
        return L2EntityUtil.isCataclysmBoss(other.getType());
    }
}
