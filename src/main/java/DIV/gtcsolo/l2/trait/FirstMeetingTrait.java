package DIV.gtcsolo.l2.trait;

import com.mojang.logging.LogUtils;
import dev.xkmc.l2hostility.content.capability.mob.CapStorageData;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2serial.serialization.SerialClass;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import org.slf4j.Logger;

/**
 * [16] First Meeting — プレイヤーから初めて受けるダメージを完全無効化。
 *
 * <p>state 永続化テンプレ #2: 「最初の被弾を検知して無効化、 以降は素通し」 パターン。
 */
public class FirstMeetingTrait extends MobTrait {
    private static final Logger LOGGER = LogUtils.getLogger();

    public FirstMeetingTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void onAttackedByOthers(int level, LivingEntity entity, LivingAttackEvent event) {
        if (entity.level().isClientSide()) return;
        if (!(event.getSource().getEntity() instanceof Player)) return;

        MobTraitCap cap = MobTraitCap.HOLDER.get(entity);
        Data data = cap.getOrCreateData(getRegistryName(), Data::new);
        if (data.firstHitTaken) return;

        data.firstHitTaken = true;
        event.setCanceled(true);
        LOGGER.info("[FirstMeeting] absorbed first hit from player on {}", entity);
    }

    @SerialClass
    public static class Data extends CapStorageData {
        @SerialClass.SerialField
        public boolean firstHitTaken = false;
    }
}
