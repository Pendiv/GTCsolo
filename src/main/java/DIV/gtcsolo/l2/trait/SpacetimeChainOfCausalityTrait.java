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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import java.util.UUID;

/**
 * 時空と因果の鎖 (Spacetime Chain of Causality) — player が mob を殺害すると 3% でその地点へ瞬間移動する。
 * 近隣で mob が殺されるたびに攻撃力が +6%/stack 上昇し、 1 回の死につき (1+level) stack 累積する
 * (= レベル分を追加殺害とみなす)。 さらにその stack の 1/3 を周囲の時空タイプ mob へ分配する。
 */
public class SpacetimeChainOfCausalityTrait extends MobTrait implements ISpacetimeTrait {

    private static final UUID MOD_ATK = UUID.fromString("4d9a2f6c-8b1e-4c37-9d05-6e3a1f8c2b71");
    private static final String COUNT_KEY = "gtcsolo.spacetime_chain_count";
    private static final double TP_SCAN_RADIUS = 64.0;
    private static final double TP_CHANCE = 0.03;
    private static final double BUFF_RADIUS = 16.0;
    private static final double ATK_PER_STACK = 0.06;  // +6%/stack (固定)
    private static final int COUNT_CAP = 200;

    public SpacetimeChainOfCausalityTrait(ChatFormatting style) {
        super(style);
    }

    public static void onAnyDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide()) return;
        boolean byPlayer = event.getSource().getEntity() instanceof Player;

        // 1) player kill → 3% で因果の鎖 holder を死亡地点へ TP
        if (byPlayer) {
            AABB wide = dead.getBoundingBox().inflate(TP_SCAN_RADIUS);
            for (LivingEntity e : dead.level().getEntitiesOfClass(LivingEntity.class, wide, x -> x != dead && x.isAlive())) {
                if (!MobTraitCap.HOLDER.isProper(e)) continue;
                if (MobTraitCap.HOLDER.get(e).getTraitLevel(ModL2Traits.SPACETIME_CHAIN_OF_CAUSALITY.get()) <= 0) continue;
                if (e.getRandom().nextDouble() < TP_CHANCE) {
                    e.teleportTo(dead.getX(), dead.getY(), dead.getZ());
                }
            }
        }

        // 2) 近隣で mob 死亡 → holder の攻撃力 up (1 死 = 1+level stack) + 1/3 を周囲時空 mob へ
        AABB near = dead.getBoundingBox().inflate(BUFF_RADIUS);
        for (LivingEntity e : dead.level().getEntitiesOfClass(LivingEntity.class, near, x -> x != dead && x.isAlive())) {
            if (!MobTraitCap.HOLDER.isProper(e)) continue;
            int lv = MobTraitCap.HOLDER.get(e).getTraitLevel(ModL2Traits.SPACETIME_CHAIN_OF_CAUSALITY.get());
            if (lv <= 0) continue;
            int gain = 1 + lv;                  // レベル分を追加殺害とみなす
            addStacks(e, gain);
            int share = Math.max(1, gain / 3);  // 1/3 を周囲時空 mob へ
            AABB allyArea = e.getBoundingBox().inflate(BUFF_RADIUS);
            for (LivingEntity ally : e.level().getEntitiesOfClass(LivingEntity.class, allyArea, x -> x != e && !(x instanceof Player))) {
                if (SpacetimeTraits.isSpacetimeMob(ally)) addStacks(ally, share);
            }
        }
    }

    private static void addStacks(LivingEntity mob, int add) {
        var pd = mob.getPersistentData();
        int count = Math.min(pd.getInt(COUNT_KEY) + add, COUNT_CAP);
        pd.putInt(COUNT_KEY, count);
        L2TraitAttributes.setPermanent(mob, Attributes.ATTACK_DAMAGE, MOD_ATK, "gtcsolo.spacetime_chain",
                count * ATK_PER_STACK, AttributeModifier.Operation.MULTIPLY_BASE);
    }
}
