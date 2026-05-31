package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.ModL2Traits;
import DIV.gtcsolo.l2.SpacetimeTraits;
import DIV.gtcsolo.l2.trait.base.ISpacetimeTrait;
import DIV.gtcsolo.l2.util.L2TraitAttributes;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

/**
 * 時空の英雄 (Spacetime Hero) — 初期状態は無効。 有効化までに 120 秒を超えると特性自体が消滅する。
 * 周囲で時空タイプ mob が死ぬたびにカウントが貯まり、 閾値に達すると有効化 → 自身を全快し、
 * 攻撃力 +(125+25N)% を固定 10 秒間 獲得する。
 */
public class SpacetimeHeroTrait extends MobTrait implements ISpacetimeTrait {

    private static final UUID MOD_ATK = UUID.fromString("5a1f8c3e-2b6d-4a09-9d17-6e3c1a8f2b50");
    private static final String START_KEY = "gtcsolo.spacetime_hero_start";
    private static final String COUNT_KEY = "gtcsolo.spacetime_hero_count";
    private static final String ENABLED_KEY = "gtcsolo.spacetime_hero_enabled";
    private static final String BUFF_UNTIL_KEY = "gtcsolo.spacetime_hero_buff_until";
    private static final long DEADLINE_TICKS = 2400L;  // 120 秒
    private static final long BUFF_TICKS = 200L;        // 固定 10 秒
    private static final double RADIUS = 24.0;

    public SpacetimeHeroTrait(ChatFormatting style) {
        super(style);
    }

    private static int threshold(int level) {
        return 3 + 2 * level;
    }

    @Override
    public void postInit(LivingEntity mob, int lv) {
        super.postInit(mob, lv);
        if (lv <= 0) return;
        var pd = mob.getPersistentData();
        if (!pd.contains(START_KEY)) pd.putLong(START_KEY, mob.level().getGameTime());
    }

    @Override
    public void tick(LivingEntity mob, int level) {
        super.tick(mob, level);
        if (mob.level().isClientSide()) return;
        if (mob.tickCount % 20 != 0) return;
        var pd = mob.getPersistentData();
        long now = mob.level().getGameTime();
        if (pd.getBoolean(ENABLED_KEY)) {
            // 10 秒経過で攻撃力 buff を解除
            long until = pd.getLong(BUFF_UNTIL_KEY);
            if (until > 0 && now >= until) {
                L2TraitAttributes.remove(mob, Attributes.ATTACK_DAMAGE, MOD_ATK);
                pd.putLong(BUFF_UNTIL_KEY, 0L);
            }
            return;
        }
        if (!pd.contains(START_KEY)) {
            pd.putLong(START_KEY, now);
            return;
        }
        if (now - pd.getLong(START_KEY) > DEADLINE_TICKS && MobTraitCap.HOLDER.isProper(mob)) {
            MobTraitCap.HOLDER.get(mob).removeTrait(ModL2Traits.SPACETIME_HERO.get());  // 期限切れ消滅
        }
    }

    /** 近隣で時空 mob が死んだら count++、 閾値で有効化 */
    public static void onAnyDeath(LivingEntity dead) {
        if (dead.level().isClientSide()) return;
        if (!SpacetimeTraits.isSpacetimeMob(dead)) return;
        AABB area = dead.getBoundingBox().inflate(RADIUS);
        for (LivingEntity e : dead.level().getEntitiesOfClass(LivingEntity.class, area, x -> x != dead && x.isAlive())) {
            if (!MobTraitCap.HOLDER.isProper(e)) continue;
            int lv = MobTraitCap.HOLDER.get(e).getTraitLevel(ModL2Traits.SPACETIME_HERO.get());
            if (lv <= 0) continue;
            var pd = e.getPersistentData();
            if (pd.getBoolean(ENABLED_KEY)) continue;
            int count = pd.getInt(COUNT_KEY) + 1;
            pd.putInt(COUNT_KEY, count);
            if (count >= threshold(lv)) enable(e, lv);
        }
    }

    private static void enable(LivingEntity hero, int level) {
        var pd = hero.getPersistentData();
        pd.putBoolean(ENABLED_KEY, true);
        hero.setHealth(hero.getMaxHealth());
        L2TraitAttributes.setPermanent(hero, Attributes.ATTACK_DAMAGE, MOD_ATK, "gtcsolo.spacetime_hero",
                1.25 + 0.25 * level, AttributeModifier.Operation.MULTIPLY_BASE);  // +(125+25N)%
        pd.putLong(BUFF_UNTIL_KEY, hero.level().getGameTime() + BUFF_TICKS);
    }
}
