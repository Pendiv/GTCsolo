package DIV.gtcsolo.common;

import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.SleepingTimeCheckEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * バニラはベッドで眠れる時刻が早すぎる (日没直後 ~12542t = 夕方) ので、
 * きちんと夜 ({@link #NIGHT_START} = 14500t、 十分に夜が更けた頃) になるまで就寝を禁止する。
 *
 * <p>14500t 以降〜翌朝の判定はバニラ ({@code DEFAULT}) に委ねるので、 朝になれば従来どおり起きられる。
 * 雷雨中の昼寝はバニラ仕様を尊重し DENY しない。 自然次元 (オーバーワールド系) のみ対象。
 */
public class SleepTimeHandler {

    private static final long NIGHT_START = 14500L;

    @SubscribeEvent
    public void onSleepCheck(SleepingTimeCheckEvent event) {
        Level level = event.getEntity().level();
        if (!level.dimensionType().natural()) return;   // 時刻が夜に対応しない次元は触らない
        if (level.isThundering()) return;               // 雷雨はバニラ通り昼でも就寝可

        long t = level.getDayTime() % 24000L;
        if (t < NIGHT_START) {
            event.setResult(Event.Result.DENY);   // 日没〜13000 の夕方はまだ眠らせない
        }
    }
}
