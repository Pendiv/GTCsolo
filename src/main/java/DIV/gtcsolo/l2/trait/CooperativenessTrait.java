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

/**
 * [12] Cooperativeness — 周囲の敵にこの特性を配り、 同特性持ち敵と体力を合算 → 均等再配分。
 *
 * <p>耐久型と攻撃特化型を均す。 結果: 硬い敵は脆く、 脆い敵は硬くなる。
 * <p>初期化時に 1 回だけ合算・再配分、 以降は独立 (= 常時同期なし)。
 */
public class CooperativenessTrait extends MobTrait {

    private static final double RADIUS = 16.0;

    public CooperativenessTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void postInit(LivingEntity mob, int lv) {
        super.postInit(mob, lv);
        if (mob.level().isClientSide()) return;
        MobTraitCap srcCap = MobTraitCap.HOLDER.get(mob);
        Data srcData = srcCap.getOrCreateData(getRegistryName(), Data::new);
        if (srcData.merged) return;

        // 周囲の同類 mob を探して trait 付与 + 合算リスト構築
        AABB area = mob.getBoundingBox().inflate(RADIUS);
        List<LivingEntity> group = new ArrayList<>();
        group.add(mob);
        for (Mob other : mob.level().getEntitiesOfClass(Mob.class, area, e -> e != mob)) {
            if (!MobTraitCap.HOLDER.isProper(other)) continue;
            MobTraitCap oc = MobTraitCap.HOLDER.get(other);
            if (!oc.hasTrait(this)) oc.setTrait(this, lv);
            Data od = oc.getOrCreateData(getRegistryName(), Data::new);
            if (!od.merged) group.add(other);
        }

        if (group.size() < 2) return; // 自分だけなら何もしない

        // 合算 → 均等再配分
        double totalHp = 0, totalMaxHp = 0;
        for (LivingEntity e : group) {
            totalHp += e.getHealth();
            totalMaxHp += e.getMaxHealth();
        }
        double avgRatio = totalHp / totalMaxHp;
        for (LivingEntity e : group) {
            e.setHealth((float) (e.getMaxHealth() * avgRatio));
            MobTraitCap c = MobTraitCap.HOLDER.get(e);
            c.getOrCreateData(getRegistryName(), Data::new).merged = true;
        }
    }

    @SerialClass
    public static class Data extends CapStorageData {
        @SerialClass.SerialField
        public boolean merged = false;
    }
}
