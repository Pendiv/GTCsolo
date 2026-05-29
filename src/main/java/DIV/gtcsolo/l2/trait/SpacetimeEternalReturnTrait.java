package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.ModL2Traits;
import DIV.gtcsolo.l2.trait.base.ISpacetimeTrait;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

/**
 * 時空の永劫回帰 (Spacetime Eternal Return) — 死亡時、 別 entity として復活する。 復活体はランダムな
 * 時空タイプの特性を獲得し、 その抽選は永劫回帰を引きやすい (= 連鎖しやすい)。
 *
 * <p>「別 entity」 = 同 type の新規 mob を死亡地点に spawn する (元 mob は通常通り死亡)。 暴走防止に
 * 世代カウンタ ({@code GEN_KEY}) を持ち、 {@link #MAX_GEN} 世代で連鎖を止める。
 */
public class SpacetimeEternalReturnTrait extends MobTrait implements ISpacetimeTrait {

    private static final String GEN_KEY = "gtcsolo.spacetime_eternal_return_gen";
    private static final int MAX_GEN = 5;         // 復活連鎖の上限 (暴走防止)
    private static final double SELF_BIAS = 0.6;  // 復活体が永劫回帰を引く確率

    public SpacetimeEternalReturnTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void onDeath(int level, LivingEntity entity, LivingDeathEvent event) {
        super.onDeath(level, entity, event);
        if (!(entity.level() instanceof ServerLevel sl)) return;
        int gen = entity.getPersistentData().getInt(GEN_KEY);
        if (gen >= MAX_GEN) return;
        Entity created = entity.getType().create(sl);
        if (!(created instanceof Mob newMob)) return;
        newMob.moveTo(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
        newMob.getPersistentData().putInt(GEN_KEY, gen + 1);
        sl.addFreshEntity(newMob);
        newMob.setHealth(newMob.getMaxHealth());
        if (!MobTraitCap.HOLDER.isProper(newMob)) return;
        MobTraitCap cap = MobTraitCap.HOLDER.get(newMob);
        RandomSource rand = entity.getRandom();
        MobTrait granted = rand.nextDouble() < SELF_BIAS
                ? ModL2Traits.SPACETIME_ETERNAL_RETURN.get()
                : ModL2Traits.randomGrantableSpacetime(rand);
        if (granted != null) cap.setTrait(granted, Math.max(1, level));
    }
}
