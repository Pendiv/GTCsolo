package DIV.gtcsolo.l2.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 確殺 (Certain Kill) — 時空の消滅が獲得する effect。 攻撃力を上昇させる。
 *
 * <p>発光は付与側 (= {@link DIV.gtcsolo.l2.SpacetimeAnnihilationGate}) で {@code MobEffects.GLOWING}
 * を同時付与して表現する。 「revival 手段 (トーテム) を無視して殺害」 は確殺保持者の攻撃に対し
 * {@code PlayerTotemMixin} が totem を不発化することで実現する。
 */
public class CertainKillEffect extends MobEffect {

    private static final String ATK_MODIFIER_UUID = "b8e3f7a2-1c4d-4e69-9a52-3f7d1e8c2b50";

    public CertainKillEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x8b0000);
        addAttributeModifier(Attributes.ATTACK_DAMAGE, ATK_MODIFIER_UUID, 0.5, AttributeModifier.Operation.MULTIPLY_BASE);
    }
}
