package DIV.gtcsolo.l2.trait;

import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * [50] Toss Up (胴上げ) — プレイヤーへの攻撃命中時、 プレイヤーを上方向に打ち上げる。 クールダウン有。
 *
 * <p>クールタイム: 60t × lv の逆数 (= lv1 で 60t、 lv5 で 12t)、 最低 12t は確保 (= 体勢崩しスパム防止)。
 * <p>State: 静的 Map で mob UUID → last toss tick。 server 起動中のみ、 永続不要。
 */
public class TossUpTrait extends MobTrait {

    private static final Map<UUID, Long> LAST_TOSS = new HashMap<>();
    private static final long BASE_COOLDOWN_TICKS = 60L;
    private static final long MIN_COOLDOWN_TICKS = 12L;
    private static final double UPWARD_BOOST = 1.2;

    public TossUpTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void postHurtImpl(int level, LivingEntity attacker, LivingEntity target) {
        if (!(target instanceof Player p)) return;
        if (attacker.level().isClientSide()) return;
        long now = attacker.level().getGameTime();
        long cooldown = Math.max(MIN_COOLDOWN_TICKS, BASE_COOLDOWN_TICKS / Math.max(1, level));
        Long last = LAST_TOSS.get(attacker.getUUID());
        if (last != null && now - last < cooldown) return;
        LAST_TOSS.put(attacker.getUUID(), now);
        Vec3 v = p.getDeltaMovement();
        p.setDeltaMovement(v.x, Math.max(v.y, UPWARD_BOOST), v.z);
        p.hurtMarked = true; // client への物理同期トリガー
    }
}
