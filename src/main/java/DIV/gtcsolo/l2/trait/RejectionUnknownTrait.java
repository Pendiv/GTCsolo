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
 * <p>軽減率 = max(0, 1 - count × step)、 step = 1/level (= lv1 で 1 回毎に -100%、 lv5 で -20%)
 * <p>damage type 別の被弾回数を Map で永続化。
 */
public class RejectionUnknownTrait extends MobTrait {

    public RejectionUnknownTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void onHurtByOthers(int level, LivingEntity entity, LivingHurtEvent event) {
        if (entity.level().isClientSide()) return;
        String id = event.getSource().getMsgId();
        MobTraitCap cap = MobTraitCap.HOLDER.get(entity);
        Data data = cap.getOrCreateData(getRegistryName(), Data::new);
        int prev = data.counts.getOrDefault(id, 0);
        // 軽減率: 0 回 → 100%、 1 回目以降 step ずつ減少
        double step = 1.0 / Math.max(1, level);
        double reduction = Math.max(0.0, 1.0 - prev * step);
        // damage 適用
        event.setAmount(event.getAmount() * (float) (1.0 - reduction));
        // count 更新 (= 次回はもう少し通る)
        data.counts.put(id, prev + 1);
    }

    @SerialClass
    public static class Data extends CapStorageData {
        @SerialClass.SerialField
        public final HashMap<String, Integer> counts = new HashMap<>();
    }
}
