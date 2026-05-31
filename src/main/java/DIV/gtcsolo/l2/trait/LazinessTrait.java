package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.util.L2TraitAttributes;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * [14] Laziness — 周囲の敵味方を問わず攻撃力を大幅低下させる。
 *
 * <p>毎 tick 周囲の LivingEntity に攻撃力低下 modifier を付与し、 範囲外へ出た対象からは除去する。
 * <p>低下率 = -(30 + 10n)% (MULTIPLY_TOTAL)。
 *
 * <p>modifier は <b>transient</b> で付与する (= reload で自動消滅)。 これにより laziness mob に近づいた
 * player の debuff が NBT に焼き付いて永続化するのを防ぐ (= 旧実装の永続バグ修正)。 範囲外への退出は
 * 保持者ごとの追跡 set で検知して除去する。
 */
public class LazinessTrait extends MobTrait {

    private static final double RADIUS = 12.0;
    private static final int INTERVAL = 10;  // 10 tick おきに軽量化
    private static final UUID MODIFIER_ID = UUID.fromString("9c8e3f17-1c44-4b1b-9a14-8b8e1b0c1d2a");
    private static final String MODIFIER_NAME = "gtcsolo.laziness.atk_down";

    /** laziness 保持 mob UUID → 現在 debuff 中の対象 UUID 集合 (= 範囲外退出検知用、 server 起動中のみ) */
    private static final Map<UUID, Set<UUID>> AFFECTED = new HashMap<>();

    public LazinessTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void tick(LivingEntity mob, int level) {
        if (mob.level().isClientSide()) return;
        if (mob.tickCount % INTERVAL != 0) return;

        double drop = -(0.30 + 0.10 * level);  // 低下率 = -(30 + 10n)%
        AABB area = mob.getBoundingBox().inflate(RADIUS);
        List<LivingEntity> nearby = mob.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != mob);
        Set<UUID> current = new HashSet<>();
        for (LivingEntity le : nearby) {
            current.add(le.getUUID());
            L2TraitAttributes.setTransient(le, Attributes.ATTACK_DAMAGE, MODIFIER_ID, MODIFIER_NAME,
                    drop, AttributeModifier.Operation.MULTIPLY_TOTAL);
        }

        // 範囲外へ出た前回対象から modifier を除去 (= 永続化させない)
        Set<UUID> prev = AFFECTED.put(mob.getUUID(), current);
        if (prev != null && mob.level() instanceof ServerLevel sl) {
            prev.removeAll(current);
            for (UUID gone : prev) {
                if (sl.getEntity(gone) instanceof LivingEntity le) {
                    L2TraitAttributes.remove(le, Attributes.ATTACK_DAMAGE, MODIFIER_ID);
                }
            }
        }
    }
}
