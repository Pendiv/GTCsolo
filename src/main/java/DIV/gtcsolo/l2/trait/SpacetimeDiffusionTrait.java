package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.ModL2Traits;
import DIV.gtcsolo.l2.trait.base.ISpacetimeTrait;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;

/**
 * 時空の敷衍 (Spacetime Diffusion) — 近くに新しく湧いた mob が、 確率で時空タイプの特性を獲得する。
 *
 * <p>{@link DIV.gtcsolo.l2.L2EventHandlers} の {@code EntityJoinLevelEvent} から {@link #onMobSpawn}
 * を呼ぶ。 chunk ロードによる再出現は除外 (= {@code loadedFromDisk})。 付与する特性は
 * {@link ModL2Traits#randomGrantableSpacetime} のプール (= 暴走系/敷衍自体を除外) から抽選する。
 */
public class SpacetimeDiffusionTrait extends MobTrait implements ISpacetimeTrait {

    private static final double RADIUS = 16.0;
    private static final double P_BASE = 0.04;       // (4 + N)%
    private static final double P_PER_LEVEL = 0.01;
    private static final double P_CAP = 0.5;

    public SpacetimeDiffusionTrait(ChatFormatting style) {
        super(style);
    }

    public static void onMobSpawn(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk()) return;
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Mob newMob)) return;
        if (!MobTraitCap.HOLDER.isProper(newMob)) return;

        int best = 0;
        AABB area = newMob.getBoundingBox().inflate(RADIUS);
        for (LivingEntity e : newMob.level().getEntitiesOfClass(LivingEntity.class, area, x -> x != newMob)) {
            if (!MobTraitCap.HOLDER.isProper(e)) continue;
            int lv = MobTraitCap.HOLDER.get(e).getTraitLevel(ModL2Traits.SPACETIME_DIFFUSION.get());
            if (lv > best) best = lv;
        }
        if (best <= 0) return;

        double p = Math.min(P_CAP, P_BASE + P_PER_LEVEL * best);
        if (newMob.getRandom().nextDouble() >= p) return;
        MobTrait granted = ModL2Traits.randomGrantableSpacetime(newMob.getRandom());
        if (granted == null) return;
        MobTraitCap cap = MobTraitCap.HOLDER.get(newMob);
        int rank = 1 + newMob.getRandom().nextInt(best);
        Integer existing = cap.traits.get(granted);
        if (existing == null || existing < rank) cap.setTrait(granted, rank);
    }
}
