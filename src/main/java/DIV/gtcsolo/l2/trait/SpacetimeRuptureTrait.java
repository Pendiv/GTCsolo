package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.trait.base.ISpacetimeTrait;
import dev.xkmc.l2damagetracker.contents.attack.AttackCache;
import dev.xkmc.l2hostility.content.logic.TraitEffectCache;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * 時空の断裂 (Spacetime Rupture) — player を攻撃する際、 player の HP 割合が高いほど
 * 与ダメージが増え、 防御を無視する度合いも増す (= 高 HP の相手ほど大きく削る)。
 *
 * <p>防御無視は {@link AllOrNothingTrait} と同じく armor 値を amount に足し戻す近似で表現する。
 * 足し戻す量と攻撃倍率の双方を HP 割合 r でスケールする。
 */
public class SpacetimeRuptureTrait extends MobTrait implements ISpacetimeTrait {

    public SpacetimeRuptureTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void onHurtTarget(int level, LivingEntity attacker, AttackCache cache, TraitEffectCache traitCache) {
        super.onHurtTarget(level, attacker, cache, traitCache);
        LivingHurtEvent event = cache.getLivingHurtEvent();
        if (event == null) return;
        if (!(traitCache.target instanceof Player player)) return;
        float r = Mth.clamp(player.getHealth() / player.getMaxHealth(), 0f, 1f);
        if (r <= 0f) return;
        double bonus = r * (0.18 + 0.02 * level);    // (18 + 2N)% を HP 割合でスケール (満 HP・N=1 で +20%)
        double armor = player.getArmorValue();
        double restored = event.getAmount() + armor * (bonus * 0.5);  // 防御無視はこの半分
        event.setAmount((float) (restored * (1.0 + bonus)));
    }
}
