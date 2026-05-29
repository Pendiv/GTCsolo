package DIV.gtcsolo.l2.trait.base;

/**
 * 時空タイプ marker interface。
 *
 * <p>これを implement する MobTrait は「時空タイプの特性」 とみなされる。
 * {@link DIV.gtcsolo.l2.SpacetimeTraits#isSpacetimeMob} が mob の保持 trait を走査して
 * これを検出することで、 「時空タイプを持つ/持たない mob」 を参照する trait 群が統一判定できる。
 *
 * <p>空 interface (= marker)。 効果は各 trait 側で実装する。
 */
public interface ISpacetimeTrait {
}
