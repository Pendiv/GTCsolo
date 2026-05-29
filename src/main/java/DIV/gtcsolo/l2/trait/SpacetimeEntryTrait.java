package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.trait.base.ISpacetimeTrait;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;

/**
 * 時空の参入 (Spacetime Entry) — 時空タイプの特性を所持しているとみなされる。 それだけ。
 *
 * <p>{@link ISpacetimeTrait} を実装するだけの効果なし marker。 持っているだけで
 * {@link DIV.gtcsolo.l2.SpacetimeTraits#isSpacetimeMob} が true を返す。
 * <p>代わりにレアリティが低い (= weight 高め) ので頻繁に見かける想定 (= TraitConfig 後日調整)。
 */
public class SpacetimeEntryTrait extends MobTrait implements ISpacetimeTrait {

    public SpacetimeEntryTrait(ChatFormatting style) {
        super(style);
    }
}
