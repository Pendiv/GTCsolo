package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.trait.base.TypedMobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * [42] Contrarian (天邪鬼) — クリーパー専用、 [32] Chain Detonation と排他。
 *
 * <p>炎・爆発・着火でダメージは受けるが起爆しない。 通常プロセス (= player 近接 swell) と
 * 水接触でのみ爆発。
 *
 * <p>実装: tick で著火状態 (= getSwellDir() > 0) を毎 tick 検査し、 通常プロセス起源かどうか
 * 判別する API が無いため、 「water 接触時のみ手動 ignite を許可、 それ以外の swelling は
 * setSwellDir(-1) でリセット」 という抑止方針を取る。
 * <p>ただし通常プロセス (= player 接近で AI が自動 ignite) は creeper の AI 内部で発火するため
 * 直接区別困難。 簡略化: water 接触で強制 ignite、 onHurtByOthers で炎/爆発受けても igniteしない。
 * 通常 player 接近の AI は触らない (= vanilla 動作維持)。
 */
public class ContrarianTrait extends TypedMobTrait {

    public ContrarianTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    protected boolean isValidTarget(LivingEntity mob) {
        return mob instanceof Creeper;
    }

    @Override
    protected void onValidHurtByOthers(int level, LivingEntity entity, LivingHurtEvent event) {
        DamageSource src = event.getSource();
        // 炎・爆発でダメージは受けるが、 ignite は無効化 (= ChainDetonation が同居していたら抑制)
        if (src.is(DamageTypeTags.IS_EXPLOSION) || src.is(DamageTypeTags.IS_FIRE)) {
            if (entity instanceof Creeper c && c.getSwellDir() > 0) {
                // 「天邪鬼: 着火を巻き戻す」 = swell カウンタを 1 段戻す動作
                // 完全に取り消すと vanilla 動作を壊すので setSwellDir(-1) で減衰のみ
                c.setSwellDir(-1);
            }
        }
    }

    @Override
    protected void onValidTick(LivingEntity mob, int lv) {
        if (mob.level().isClientSide()) return;
        if (!(mob instanceof Creeper c)) return;
        // 水接触で強制 ignite (= 「水で起爆」 の代替経路)
        if (c.isInWater() && c.getSwellDir() <= 0) {
            c.ignite();
        }
    }
}
