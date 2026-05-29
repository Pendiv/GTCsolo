package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2hostility.content.capability.mob.CapStorageData;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2serial.serialization.SerialClass;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 割れ窓理論 (Broken Window) — 周囲の最もレベルの高く、 特性を 1 つ以上所持している MOB の
 * 特性をランダムに 1 つ参照し、 それらを自身と周囲にばら撒く。
 *
 * <p>仕様:
 * <ul>
 *   <li>{@link #INTERVAL_TICKS} = 100 tick (= 5 秒) ごとに伝播試行</li>
 *   <li>radius {@link #RADIUS} = 12 ブロック内の mob を scan</li>
 *   <li>候補 = trait 1 個以上持ち、 MobTraitCap.lv が自身以上の mob</li>
 *   <li>最高 lv の mob を選定、 trait set から 1 個ランダム抽出 (= broken_window 自体は除外)</li>
 *   <li>ばら撒く対象 = 自身 + 周囲 mob (= 最大 5 体)</li>
 *   <li>付与 rank = 1〜参照元 rank のランダム (= 参照元以下)</li>
 *   <li>state: lastTriggerTick (= cooldown)</li>
 * </ul>
 */
public class BrokenWindowTrait extends MobTrait {

    private static final double RADIUS = 12.0;
    private static final int INTERVAL_TICKS = 100;
    private static final int SPREAD_TARGET_LIMIT = 5;

    public BrokenWindowTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void tick(LivingEntity mob, int level) {
        super.tick(mob, level);
        if (mob.level().isClientSide()) return;
        if (mob.tickCount % INTERVAL_TICKS != 0) return;
        if (!MobTraitCap.HOLDER.isProper(mob)) return;

        AABB area = mob.getBoundingBox().inflate(RADIUS);
        List<Mob> neighbors = mob.level().getEntitiesOfClass(Mob.class, area, e -> e != mob);

        Mob highest = null;
        int highestLv = -1;
        for (Mob other : neighbors) {
            if (!MobTraitCap.HOLDER.isProper(other)) continue;
            MobTraitCap c = MobTraitCap.HOLDER.get(other);
            if (c.traits.isEmpty()) continue;
            int otherLv = c.getLevel();
            if (otherLv > highestLv) {
                highestLv = otherLv;
                highest = other;
            }
        }
        if (highest == null) return;

        MobTraitCap sourceCap = MobTraitCap.HOLDER.get(highest);
        List<Map.Entry<MobTrait, Integer>> candidates = new ArrayList<>();
        for (Map.Entry<MobTrait, Integer> e : sourceCap.traits.entrySet()) {
            if (e.getKey() == this) continue;
            candidates.add(e);
        }
        if (candidates.isEmpty()) return;

        Random rand = new Random();
        Map.Entry<MobTrait, Integer> picked = candidates.get(rand.nextInt(candidates.size()));
        MobTrait targetTrait = picked.getKey();
        int sourceRank = picked.getValue();

        applyTrait(mob, targetTrait, sourceRank, rand);
        int spread = 0;
        for (Mob other : neighbors) {
            if (spread >= SPREAD_TARGET_LIMIT) break;
            if (!MobTraitCap.HOLDER.isProper(other)) continue;
            applyTrait(other, targetTrait, sourceRank, rand);
            spread++;
        }
    }

    private void applyTrait(LivingEntity target, MobTrait trait, int sourceRank, Random rand) {
        MobTraitCap cap = MobTraitCap.HOLDER.get(target);
        Integer existing = cap.traits.get(trait);
        int newRank = 1 + rand.nextInt(Math.max(1, sourceRank));
        if (existing != null && existing >= newRank) return;
        cap.setTrait(trait, newRank);
    }

    @SerialClass
    public static class Data extends CapStorageData {
        @SerialClass.SerialField
        public int lastTriggerTick = 0;
    }
}
