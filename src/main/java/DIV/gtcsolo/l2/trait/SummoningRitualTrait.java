package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

/**
 * [10] Summoning Ritual (召喚の儀式) — 死亡時、 同種のより強大な (= 高 level) 敵を 1 体召喚。
 *
 * <p>特性付与は L2H の本来のスポーン時 init に任せる。 ここでは spawn 後に level を底上げするだけ
 * (= HP scale が掛かる、 自然に得た特性も維持される)。
 * <p>level 加算 = 本特性の rank (= lv1 で +1、 lv5 で +5)。
 */
public class SummoningRitualTrait extends MobTrait {

    public SummoningRitualTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void onDeath(int level, LivingEntity entity, LivingDeathEvent event) {
        if (entity.level().isClientSide()) return;
        if (!(entity.level() instanceof ServerLevel sl)) return;

        EntityType<?> type = entity.getType();
        Entity spawned = type.spawn(sl, entity.blockPosition(), MobSpawnType.MOB_SUMMONED);
        if (!(spawned instanceof LivingEntity summoned)) return;
        if (!MobTraitCap.HOLDER.isProper(summoned)) return;

        // L2H の natural init は EntityJoinLevelEvent で既に走っているはず。
        // その上で召喚先 level を「自身 level の 10n%」 ぶん底上げ = 「より強大な敵」 を実現。
        MobTraitCap dstCap = MobTraitCap.HOLDER.get(summoned);
        int self = MobTraitCap.HOLDER.get(entity).getLevel();
        int bonus = (int) Math.round(self * 0.10 * level);
        int newLevel = dstCap.getLevel() + bonus;
        dstCap.setLevel(summoned, newLevel);
    }
}
