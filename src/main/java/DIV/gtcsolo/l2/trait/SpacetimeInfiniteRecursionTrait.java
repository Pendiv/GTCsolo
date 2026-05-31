package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.ModL2Traits;
import DIV.gtcsolo.l2.trait.base.ISpacetimeTrait;
import DIV.gtcsolo.l2.util.L2TraitAttributes;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import java.util.List;
import java.util.UUID;

/**
 * 時空の無限再帰 (Spacetime Infinite Recursion) — 死亡時、 この特性を近隣のランダムな mob へ移譲する。
 * 獲得時、 最大 HP が +(30+5n)% 上昇し同量回復する。 近隣に mob がいなければ player に憑き、 player も
 * 同様に最大 HP が上昇する。 その player の死亡時にさらに近隣の mob へ移譲する。
 */
public class SpacetimeInfiniteRecursionTrait extends MobTrait implements ISpacetimeTrait {

    private static final UUID MOD_HP = UUID.fromString("2b6f1d83-7c4a-4e29-9d18-5a3e7c2b1f60");
    private static final String CARRIER_KEY = "gtcsolo.spacetime_infinite_recursion_carrier";
    private static final double RADIUS = 16.0;

    public SpacetimeInfiniteRecursionTrait(ChatFormatting style) {
        super(style);
    }

    /** 最大 HP +(30+5n)% を付与し、 増えた分を回復する (重複付与は guard)。 */
    private static void applyBuff(LivingEntity e, int level) {
        if (level <= 0) return;
        float before = e.getMaxHealth();
        L2TraitAttributes.addPermanentIfAbsent(e, Attributes.MAX_HEALTH, MOD_HP, "gtcsolo.spacetime_infinite_recursion",
                0.30 + 0.05 * level, AttributeModifier.Operation.MULTIPLY_BASE);  // +(30+5n)%
        e.heal(e.getMaxHealth() - before);  // 増えた分を回復 (= 既付与なら delta 0)
    }

    @Override
    public void postInit(LivingEntity mob, int lv) {
        super.postInit(mob, lv);
        applyBuff(mob, lv);
    }

    @Override
    public void onDeath(int level, LivingEntity entity, LivingDeathEvent event) {
        super.onDeath(level, entity, event);
        if (entity.level().isClientSide()) return;
        int rank = Math.max(1, level);
        if (!transferToRandomMob(entity, rank)) {
            Player p = entity.level().getNearestPlayer(entity, RADIUS);
            if (p != null) {
                p.getPersistentData().putInt(CARRIER_KEY, rank);
                applyBuff(p, rank);  // プレイヤーにも同様
            }
        }
    }

    private static boolean transferToRandomMob(LivingEntity center, int rank) {
        AABB area = center.getBoundingBox().inflate(RADIUS);
        List<LivingEntity> cands = center.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e != center && e.isAlive() && e instanceof Mob && MobTraitCap.HOLDER.isProper(e));
        if (cands.isEmpty()) return false;
        LivingEntity pick = cands.get(center.getRandom().nextInt(cands.size()));
        MobTraitCap cap = MobTraitCap.HOLDER.get(pick);
        MobTrait self = ModL2Traits.SPACETIME_INFINITE_RECURSION.get();
        Integer existing = cap.traits.get(self);
        if (existing == null || existing < rank) cap.setTrait(self, rank);
        return true;
    }

    /** player キャリアの死亡で近隣 mob へ再移譲 */
    public static void onAnyDeath(LivingEntity dead) {
        if (dead.level().isClientSide()) return;
        if (!(dead instanceof Player p)) return;
        int rank = p.getPersistentData().getInt(CARRIER_KEY);
        if (rank <= 0) return;
        p.getPersistentData().remove(CARRIER_KEY);
        transferToRandomMob(p, rank);
    }
}
