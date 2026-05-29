package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.trait.base.ISpacetimeTrait;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;

/**
 * 時空の消滅 (Spacetime Annihilation) — player を殺害しようとして totem 等で復活されると、
 * 効果「確殺」 を獲得する。 確殺保持中は、 player の revival 手段 (totem) を無視して殺害する。
 * 確殺保持者は発光し攻撃力が上昇する。
 *
 * <p>実体ロジックは {@code PlayerTotemMixin} (checkTotemDeathProtection 介入) +
 * {@link DIV.gtcsolo.l2.SpacetimeAnnihilationGate} + {@link DIV.gtcsolo.l2.effect.CertainKillEffect} 側にある。
 * この trait は所持判定用の marker として機能する。
 */
public class SpacetimeAnnihilationTrait extends MobTrait implements ISpacetimeTrait {

    public SpacetimeAnnihilationTrait(ChatFormatting style) {
        super(style);
    }
}
