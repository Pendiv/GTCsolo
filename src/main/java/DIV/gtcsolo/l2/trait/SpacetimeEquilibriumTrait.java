package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.SpacetimeTraits;
import DIV.gtcsolo.l2.trait.base.ISpacetimeTrait;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

/**
 * 時空の均衡 (Spacetime Equilibrium) — 周囲の時空タイプ mob と player の攻撃力を、 時間とともに
 * 徐々に下げる。 1 層 = 攻撃力 -2.5n%、 5 秒ごとに 1 層増え、 上限 (15 + 10n) 層。
 *
 * <p>保持者側に ramp カウンタ (persistentData) を持ち、 算出した減少率を固定 UUID の transient
 * modifier として範囲内の対象に毎秒付け替える (= 範囲外へ出ると最後値が残る点に注意)。
 */
public class SpacetimeEquilibriumTrait extends MobTrait implements ISpacetimeTrait {

    private static final UUID MOD_WEAKEN = UUID.fromString("8b2e5c91-4a7d-4f06-9c38-2d1a7e5b3c80");
    private static final int INTERVAL = 20;
    private static final double RADIUS = 12.0;
    private static final String RAMP_KEY = "gtcsolo.spacetime_equilibrium_ramp";
    private static final int RAMP_STEP = 5;   // 5 秒ごとに 1 層
    private static final double PCT_PER_LAYER_PER_LEVEL = 0.025;  // 1 層 = -2.5n%

    public SpacetimeEquilibriumTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void tick(LivingEntity mob, int level) {
        super.tick(mob, level);
        if (mob.level().isClientSide()) return;
        if (mob.tickCount % INTERVAL != 0) return;
        int maxLayers = 15 + 10 * level;
        var pdata = mob.getPersistentData();
        int ramp = Math.min(pdata.getInt(RAMP_KEY) + 1, maxLayers * RAMP_STEP);
        pdata.putInt(RAMP_KEY, ramp);
        int layers = Math.min(ramp / RAMP_STEP, maxLayers);
        double reduction = layers * PCT_PER_LAYER_PER_LEVEL * level;  // -2.5n% × 層数
        if (reduction <= 0) return;
        AABB area = mob.getBoundingBox().inflate(RADIUS);
        for (LivingEntity t : mob.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != mob)) {
            boolean target = (t instanceof Player) || SpacetimeTraits.isSpacetimeMob(t);
            if (!target) continue;
            AttributeInstance atk = t.getAttribute(Attributes.ATTACK_DAMAGE);
            if (atk == null) continue;
            atk.removeModifier(MOD_WEAKEN);
            atk.addTransientModifier(new AttributeModifier(MOD_WEAKEN, "gtcsolo.spacetime_equilibrium",
                    -reduction, AttributeModifier.Operation.MULTIPLY_BASE));
        }
    }
}
