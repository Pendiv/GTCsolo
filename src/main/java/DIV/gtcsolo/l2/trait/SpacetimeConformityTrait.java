package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.trait.base.ISpacetimeTrait;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 時空の迎合 (Spacetime Conformity) — 倒されると、 その dimension の時刻が夜になる。
 * その夜のうちは他の迎合が死んでも夜化は再発動しない (= 1 夜 1 回)。
 *
 * <p>「1 夜 1 回」 は dimension ごとに「最後に夜化した日 index」 を static で記録して判定する。
 * 同じ日 index で既に発動済なら skip する。
 */
public class SpacetimeConformityTrait extends MobTrait implements ISpacetimeTrait {

    private static final Map<ResourceKey<Level>, Long> LAST_CONFORMED_DAY = new ConcurrentHashMap<>();
    private static final long NIGHT_TIME = 13000L;
    private static final long DAY_LENGTH = 24000L;

    public SpacetimeConformityTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    public void onDeath(int level, LivingEntity entity, LivingDeathEvent event) {
        super.onDeath(level, entity, event);
        if (!(entity.level() instanceof ServerLevel sl)) return;
        long dayTime = sl.getDayTime();
        long dayIndex = dayTime / DAY_LENGTH;
        Long last = LAST_CONFORMED_DAY.get(sl.dimension());
        if (last != null && last == dayIndex) return;  // 今夜は既に発動済
        sl.setDayTime(dayIndex * DAY_LENGTH + NIGHT_TIME);
        LAST_CONFORMED_DAY.put(sl.dimension(), dayIndex);
    }
}
