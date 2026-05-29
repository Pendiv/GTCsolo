package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2hostility.content.capability.mob.CapStorageData;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2serial.serialization.SerialClass;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.HashMap;

/**
 * [29] Rejection Unknown — 初めて受けるタイプのダメージを 100% 軽減。 以降同タイプを受けるたびに軽減率低下。
 *
 * <p>軽減率 = max(0, 1 - count × step)、 step = 24÷(n+1)% (= lv1 で 1 回毎 -12%、 lv3 で -6%)
 * <p>damage type 別の被弾回数を Map で永続化。 種類を問わず 30 秒被弾が無いと count をリセット。
 */
public class RejectionUnknownTrait extends MobTrait {

    private static final long RESET_TICKS = 600;  // 30 秒被弾が無いと count リセット

    public RejectionUnknownTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void onHurtByOthers(int level, LivingEntity entity, LivingHurtEvent event) {
        if (entity.level().isClientSide()) return;
        MobTraitCap cap = MobTraitCap.HOLDER.get(entity);
        Data data = cap.getOrCreateData(getRegistryName(), Data::new);
        long now = entity.level().getGameTime();
        // 種類を問わず最後の被弾から 30 秒経過していれば適応をリセット
        if (now - data.lastHitTick > RESET_TICKS) data.counts.clear();
        data.lastHitTick = now;
        String id = event.getSource().getMsgId();
        int prev = data.counts.getOrDefault(id, 0);
        // 軽減率: 0 回 → 100%、 1 回被弾毎に step = 24÷(n+1)% ずつ減少
        double step = 0.24 / (level + 1);
        double reduction = Math.max(0.0, 1.0 - prev * step);
        event.setAmount(event.getAmount() * (float) (1.0 - reduction));
        data.counts.put(id, prev + 1);
    }

    @SerialClass
    public static class Data extends CapStorageData {
        @SerialClass.SerialField
        public final HashMap<String, Integer> counts = new HashMap<>();
        @SerialClass.SerialField
        public long lastHitTick = -100000;
    }
}
