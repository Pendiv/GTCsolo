package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;

/**
 * [13] Soul Destruction — トーテム復活を貫通させる。
 *
 * <p>実体: {@link DIV.gtcsolo.mixin.PlayerTotemMixin} が
 * {@code Player.checkTotemDeathProtection} を HEAD で横取りし、
 * 攻撃者が SoulDestruction を持つなら totem を発火させずに死亡を確定する。
 *
 * <p>本クラスは登録のみ (= trait 有無の判定は cap 経由で mixin が行う)。
 */
public class SoulDestructionTrait extends MobTrait {

    public SoulDestructionTrait(ChatFormatting style) {
        super(style);
    }
}
