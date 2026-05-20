package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2damagetracker.contents.attack.AttackCache;
import dev.xkmc.l2hostility.content.capability.mob.CapStorageData;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2serial.serialization.SerialClass;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * [31] Last Stand (起死回生) — 1 度のみ。 自身が HP 比率 {@link #THRESHOLD} 以下になると、
 * 最寄りプレイヤーと現在 HP 比率を入れ替える。
 *
 * <p>仕様文の注記通り「双方が亀甲状態 (= どちらも低 HP) ではほぼ意味をなさない」 が、
 * 比率交換ベースなので player の maxHP が大きいほど自身の交換後の絶対 HP も大きくなる。
 *
 * <p>state: cap data の triggered フラグ (= 永続化)。
 */
public class LastStandTrait extends MobTrait {

    private static final float THRESHOLD = 0.2f;
    private static final double SEARCH_RADIUS = 32.0;

    public LastStandTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void onDamaged(int level, LivingEntity mob, AttackCache cache) {
        super.onDamaged(level, mob, cache);
        if (mob.level().isClientSide()) return;
        if (!MobTraitCap.HOLDER.isProper(mob)) return;
        MobTraitCap cap = MobTraitCap.HOLDER.get(mob);
        Data data = cap.getOrCreateData(getRegistryName(), Data::new);
        if (data.triggered) return;

        float maxHp = mob.getMaxHealth();
        if (maxHp <= 0) return;
        float myRatio = mob.getHealth() / maxHp;
        if (myRatio > THRESHOLD) return;

        Player nearest = mob.level().getNearestPlayer(mob, SEARCH_RADIUS);
        if (nearest == null) return;
        float playerMax = nearest.getMaxHealth();
        if (playerMax <= 0) return;
        float playerRatio = nearest.getHealth() / playerMax;

        data.triggered = true;
        mob.setHealth(maxHp * playerRatio);
        nearest.setHealth(playerMax * myRatio);
    }

    @SerialClass
    public static class Data extends CapStorageData {
        @SerialClass.SerialField
        public boolean triggered = false;
    }
}
