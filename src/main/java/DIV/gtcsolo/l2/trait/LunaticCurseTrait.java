package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2damagetracker.contents.attack.AttackCache;
import dev.xkmc.l2hostility.content.capability.mob.CapStorageData;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.logic.TraitEffectCache;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2serial.serialization.SerialClass;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * [06] Lunatic Curse — 1 度のみ、 攻撃したプレイヤーの体力を 20 (= ハート 10) まで削る。
 *
 * <p>maxHealth は触らず {@code setHealth(20)} で現在 HP を切り下げるのみ。
 * 過去版は {@code getBaseValue()} を判定に使っていたが、 vanilla プレイヤーは base=20 のため
 * Apoth modifier で maxHealth が増えていても base check で常に early-return = 発動しないバグがあった。
 *
 * <p>発動済フラグで 1 回限り。
 */
public class LunaticCurseTrait extends MobTrait {

    public LunaticCurseTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void onHurtTarget(int level, LivingEntity attacker, AttackCache cache, TraitEffectCache traitCache) {
        super.onHurtTarget(level, attacker, cache, traitCache);
        var target = traitCache.target;
        if (!(target instanceof Player player)) return;
        if (target.level().isClientSide()) return;

        MobTraitCap cap = MobTraitCap.HOLDER.get(attacker);
        Data data = cap.getOrCreateData(getRegistryName(), Data::new);
        if (data.triggered) return;
        data.triggered = true;

        // 現在 HP > 20 なら 20 まで削る (= ハート 10)。 maxHealth は触らない。
        if (player.getHealth() > 20.0f) {
            player.setHealth(20.0f);
        }
    }

    @SerialClass
    public static class Data extends CapStorageData {
        @SerialClass.SerialField
        public boolean triggered = false;
    }
}
