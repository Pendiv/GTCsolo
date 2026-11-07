package DIV.gtcsolo.machine.wen;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.block.wen.WENDataMonitorBlockEntity;
import DIV.gtcsolo.block.wen.WENDataMonitorMenu;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** One history clock and synchronization only for players who currently have a monitor open. */
@Mod.EventBusSubscriber(modid = Gtcsolo.MODID)
public final class WENNetworkEvents {
    private WENNetworkEvents() {}

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        var server = event.getServer();
        if (event.phase != TickEvent.Phase.END || server.getTickCount() % 20 != 0) return;
        var data = WENNetworkData.find(server.overworld());
        if (data != null) data.recordHistory(server.overworld().getGameTime());
        for (var player : server.getPlayerList().getPlayers()) {
            if (player.containerMenu instanceof WENDataMonitorMenu menu
                    && menu.stillValid(player)
                    && player.level().getBlockEntity(menu.getMonitorPos())
                            instanceof WENDataMonitorBlockEntity monitor) monitor.syncTo(player);
        }
    }
}
