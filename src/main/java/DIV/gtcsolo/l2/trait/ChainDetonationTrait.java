package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.trait.base.TypedMobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * [32] Chain Detonation (誘爆) — クリーパー専用。 爆発・炎ダメージで起爆 (連鎖)。
 *
 * <p>被弾 source が {@link DamageTypeTags#IS_EXPLOSION} or {@link DamageTypeTags#IS_FIRE} の場合
 * {@link Creeper#ignite()} を呼ぶ。 爆発全般を一括判定するため発生源 mob 個別 check は不要。
 * <p>排他: [42] Contrarian と排他 (= 着火挙動真逆)。 datapack 配布側で排他制御するか、
 * Contrarian 側で誘爆も無効化することで両立。
 */
public class ChainDetonationTrait extends TypedMobTrait {

    public ChainDetonationTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    protected boolean isValidTarget(LivingEntity mob) {
        return mob instanceof Creeper;
    }

    @Override
    protected void onValidHurtByOthers(int level, LivingEntity entity, LivingHurtEvent event) {
        DamageSource src = event.getSource();
        if (!(src.is(DamageTypeTags.IS_EXPLOSION) || src.is(DamageTypeTags.IS_FIRE))) return;
        if (entity instanceof Creeper c) c.ignite();
    }
}
