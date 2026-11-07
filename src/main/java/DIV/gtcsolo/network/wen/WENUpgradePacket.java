package DIV.gtcsolo.network.wen;

import DIV.gtcsolo.block.wen.WENDataMonitorBlockEntity;
import DIV.gtcsolo.block.wen.WENDataMonitorMenu;
import DIV.gtcsolo.machine.WENMainStorageMachine;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** 0/1: insert/remove crystal; 2/3/4: upgrade once/all/downgrade. */
public record WENUpgradePacket(BlockPos monitorPos, int action) {
    public static void encode(WENUpgradePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.monitorPos());
        buffer.writeVarInt(packet.action());
    }

    public static WENUpgradePacket decode(FriendlyByteBuf buffer) {
        return new WENUpgradePacket(buffer.readBlockPos(), buffer.readVarInt());
    }

    public static void handle(WENUpgradePacket packet, Supplier<NetworkEvent.Context> context) {
        context.get()
                .enqueueWork(
                        () -> {
                            var player = context.get().getSender();
                            if (player == null
                                    || packet.action() < 0
                                    || packet.action() > 4
                                    || !(player.containerMenu instanceof WENDataMonitorMenu menu)
                                    || !menu.getMonitorPos().equals(packet.monitorPos())
                                    || !menu.stillValid(player)
                                    || !(player.level().getBlockEntity(packet.monitorPos())
                                            instanceof WENDataMonitorBlockEntity monitor)) return;
                            WENMainStorageMachine core = monitor.getController();
                            if (core == null || !core.isNetworkReady()) return;
                            Component message = null;
                            boolean success = true;
                            switch (packet.action()) {
                                case 0 -> {
                                    success = core.insertCrystal(player);
                                    message =
                                            success
                                                    ? Component.translatable(
                                                            "gui.gtcsolo.wen.cross_dimension_enabled")
                                                    : Component.translatable(
                                                            "gui.gtcsolo.wen.crystal_hint",
                                                            Component.translatable(
                                                                    "item.gtcsolo.evernight_crystal"));
                                }
                                case 1 -> {
                                    success = core.removeCrystal(player);
                                    if (success)
                                        message =
                                                Component.translatable(
                                                        "gui.gtcsolo.wen.cross_dimension_disabled");
                                }
                                case 2 -> {
                                    if (core.getStorageUpgradeLevel()
                                            >= WENMainStorageMachine.MAX_UPGRADE_LEVEL) {
                                        success = false;
                                        message =
                                                Component.translatable(
                                                        "gui.gtcsolo.wen.maximum_level");
                                    } else {
                                        int cost = core.getNextUpgradeCost(),
                                                held = WENMainStorageMachine.countFcore(player);
                                        success = core.performStorageUpgrade(player);
                                        message =
                                                success
                                                        ? Component.translatable(
                                                                "gui.gtcsolo.wen.upgraded",
                                                                core.getStorageUpgradeLevel())
                                                        : Component.translatable(
                                                                "gui.gtcsolo.wen.insufficient_cores",
                                                                cost,
                                                                Component.translatable(
                                                                        "item.gtcsolo.fcore"),
                                                                held);
                                    }
                                }
                                case 3 -> {
                                    int levels = core.performBulkUpgrade(player);
                                    success = levels > 0;
                                    message =
                                            success
                                                    ? Component.translatable(
                                                            "gui.gtcsolo.wen.bulk_upgraded",
                                                            levels,
                                                            core.getStorageUpgradeLevel())
                                                    : Component.translatable(
                                                            "gui.gtcsolo.wen.no_upgrades");
                                }
                                case 4 -> {
                                    success = core.downgradeStorage();
                                    if (success)
                                        message =
                                                Component.translatable(
                                                        "gui.gtcsolo.wen.downgraded",
                                                        core.getStorageUpgradeLevel());
                                }
                            }
                            if (message != null)
                                player.sendSystemMessage(
                                        message.copy()
                                                .withStyle(
                                                        success
                                                                ? ChatFormatting.GREEN
                                                                : ChatFormatting.RED));
                            monitor.forceSyncNow();
                        });
        context.get().setPacketHandled(true);
    }
}
