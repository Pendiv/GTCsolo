package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.trait.base.ISpacetimeTrait;
import dev.xkmc.l2damagetracker.init.data.L2DamageTypes;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * 時空の潜航 (Spacetime Dive) — 潜航状態にあり、 物理ダメージを無効化する。 魔術 (魔法) ダメージのみ
 * 受け付ける。 さらに他 mob の AI ターゲットから除外される (= {@code TargetingConditionsMixin})。
 *
 * <p>魔術判定は L2DamageTracker の {@code forge:is_magic} タグ ({@link L2DamageTypes#MAGIC})。
 * vanilla MAGIC/WITHER/SONIC_BOOM や IronsSpellbooks/ArsNouveau の呪文がこれに含まれる。
 */
public class SpacetimeDiveTrait extends MobTrait implements ISpacetimeTrait {

    public SpacetimeDiveTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void onHurtByOthers(int level, LivingEntity entity, LivingHurtEvent event) {
        super.onHurtByOthers(level, entity, event);
        if (!event.getSource().is(L2DamageTypes.MAGIC)) {
            event.setCanceled(true);  // 物理は無効、 魔術のみ通す
        }
    }
}
