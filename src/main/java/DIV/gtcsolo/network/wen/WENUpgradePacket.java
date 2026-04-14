package DIV.gtcsolo.network.wen;

import DIV.gtcsolo.block.wen.WENDataMonitorBlockEntity;
import DIV.gtcsolo.machine.WENMainStorageMachine;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * action: 0=crystal insert, 1=crystal remove, 2=storage +1, 3=storage bulk, 4=storage downgrade
 */
public class WENUpgradePacket {

    private static final Logger LOGGER = LogUtils.getLogger();
    private final BlockPos monitorPos;
    private final int action;

    public WENUpgradePacket(BlockPos monitorPos, int slot, int action) {
        this.monitorPos = monitorPos;
        this.action = action;
    }

    public static void encode(WENUpgradePacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.monitorPos);
        buf.writeVarInt(p.action);
    }

    public static WENUpgradePacket decode(FriendlyByteBuf buf) {
        return new WENUpgradePacket(buf.readBlockPos(), 0, buf.readVarInt());
    }

    public static void handle(WENUpgradePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (!(player.level().getBlockEntity(pkt.monitorPos)
                    instanceof WENDataMonitorBlockEntity monitor)) return;
            WENMainStorageMachine wen = monitor.getController();
            if (wen == null || !wen.isFormed()) return;

            switch (pkt.action) {
                case 0 -> { // Crystal insert
                    if (wen.insertCrystal(player))
                        player.sendSystemMessage(Component.literal("§aCross-Dimension enabled"));
                    else
                        player.sendSystemMessage(Component.literal("§cHold Evernight Crystal in hand"));
                }
                case 1 -> { // Crystal remove
                    if (wen.removeCrystal(player))
                        player.sendSystemMessage(Component.literal("§aCross-Dimension disabled"));
                }
                case 2 -> { // Storage +1
                    int cost = wen.getNextUpgradeCost();
                    int have = WENMainStorageMachine.countFcore(player);
                    if (have < cost) {
                        player.sendSystemMessage(Component.literal(
                                "§cNeed " + cost + " Fantasy Core (have " + have + ")"));
                        return;
                    }
                    if (wen.performStorageUpgrade(player)) {
                        player.sendSystemMessage(Component.literal(
                                "§aStorage Lv" + wen.getStorageUpgradeLevel() +
                                " (×" + (1L << wen.getStorageUpgradeLevel()) + ")"));
                    }
                }
                case 3 -> { // Bulk upgrade
                    int count = wen.performBulkUpgrade(player);
                    if (count > 0) {
                        player.sendSystemMessage(Component.literal(
                                "§a+" + count + " levels → Lv" + wen.getStorageUpgradeLevel() +
                                " (×" + (1L << Math.min(wen.getStorageUpgradeLevel(), 62)) + ")"));
                    } else {
                        player.sendSystemMessage(Component.literal("§cNo upgrades (max or no cores)"));
                    }
                }
                case 4 -> { // Downgrade
                    if (wen.downgradeStorage()) {
                        player.sendSystemMessage(Component.literal(
                                "§eDowngraded to Lv" + wen.getStorageUpgradeLevel()));
                    }
                }
            }
            monitor.forceSyncNow();
        });
        ctx.get().setPacketHandled(true);
    }
}