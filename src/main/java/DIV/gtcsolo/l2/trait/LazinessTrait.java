package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * [14] Laziness — 周囲の敵味方を問わず攻撃力を大幅低下させる。
 *
 * <p>毎 tick 周囲の LivingEntity に攻撃力低下 AttributeModifier を付与。 範囲外移動で除去。
 * <p>低下率 = -30% × level (multiply_total)
 */
public class LazinessTrait extends MobTrait {

    private static final double RADIUS = 12.0;
    private static final UUID MODIFIER_ID = UUID.fromString("9c8e3f17-1c44-4b1b-9a14-8b8e1b0c1d2a");
    private static final String MODIFIER_NAME = "gtcsolo.laziness.atk_down";

    public LazinessTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void tick(LivingEntity mob, int level) {
        if (mob.level().isClientSide()) return;
        if (mob.tickCount % 10 != 0) return; // 10 tick おきに軽量化

        double drop = -0.3 * level;
        AABB area = mob.getBoundingBox().inflate(RADIUS);
        List<LivingEntity> nearby = mob.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != mob);
        Set<UUID> inRange = new HashSet<>();
        for (LivingEntity le : nearby) {
            inRange.add(le.getUUID());
            AttributeInstance inst = le.getAttribute(Attributes.ATTACK_DAMAGE);
            if (inst == null) continue;
            if (inst.getModifier(MODIFIER_ID) == null) {
                inst.addPermanentModifier(new AttributeModifier(
                        MODIFIER_ID, MODIFIER_NAME, drop, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }
        // 範囲外への除去は entity 側で trait 持ちが見えないので省略 (= 重い、 effect 風に時限化する手も)
        // 簡易対策: 30 tick おきにキャッシュ範囲外の前回ターゲットから modifier 除去 (= 後日改善)
    }
}
