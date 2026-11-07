package DIV.gtcsolo.machine.wen;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;

import DIV.gtcsolo.block.wen.WENIdSelectMenu;
import com.gregtechceu.gtceu.api.GTValues;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;
import java.util.function.Consumer;

/**
 * WEN 系マシン (wireless input/output, energy hatch, energy output hatch) が共有する
 * ネットワークID リンク操作。4 クラスに同一実装が複製されていたのを集約 (2026-07-02)。
 */
public final class WENLinking {
    public static TickableSubscription transferSubscription(
            MetaMachine machine, TickableSubscription current, String networkId, Runnable tick) {
        if (machine.getLevel() == null || machine.isRemote()) return current;
        if (networkId.isEmpty()) {
            machine.unsubscribe(current);
            return null;
        }
        return machine.subscribeServerTick(current, tick);
    }

    private WENLinking() {}

    /** この次元から接続可能な (formed かつ 同次元 or crossDim 許可) ネットワークID一覧 (sorted)。 */
    public static List<String> validNetworkIds(WENNetworkData data, Level level) {
        return level == null
                ? List.of()
                : data.getAccessibleNetworkIds(level.dimension().location().toString());
    }

    /**
     * レンチクリック共通: 有効IDを巡回して setter に渡し、{@code linkedLangKey} で通知する。
     * 通知は (id, 電圧名, アンペア) の 3 引数フォーマット。
     */
    public static InteractionResult cycleId(
            Player player,
            Level level,
            String currentId,
            Consumer<String> setter,
            String linkedLangKey,
            int tier,
            long amperage) {
        if (level == null || level.isClientSide) return InteractionResult.SUCCESS;

        WENNetworkData data = WENNetworkData.get(level.getServer().overworld());
        List<String> validIds = validNetworkIds(data, level);

        if (validIds.isEmpty()) {
            player.sendSystemMessage(
                    Component.translatable("gui.gtcsolo.wen_input.no_local_networks"));
            return InteractionResult.SUCCESS;
        }

        int idx = validIds.indexOf(currentId);
        String newId = validIds.get((idx + 1) % validIds.size());
        setter.accept(newId);
        player.sendSystemMessage(
                Component.translatable(
                        linkedLangKey, newId, GTValues.VNF[tier], String.valueOf(amperage)));
        return InteractionResult.SUCCESS;
    }

    /** 素手+Shift右クリック共通: ID選択UI ({@link WENIdSelectMenu}) を開く。 */
    public static InteractionResult openIdSelect(
            Player player, Level level, BlockPos pos, String currentId) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!player.isShiftKeyDown() || !player.getMainHandItem().isEmpty())
            return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        WENNetworkData data = WENNetworkData.get(level.getServer().overworld());
        List<String> validIds = validNetworkIds(data, level);

        NetworkHooks.openScreen(
                sp,
                new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.translatable("gui.gtcsolo.wen.select_network");
                    }

                    @Override
                    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
                            int id, net.minecraft.world.entity.player.Inventory inv, Player p) {
                        return new WENIdSelectMenu(id, pos, currentId, validIds);
                    }
                },
                (FriendlyByteBuf buf) -> {
                    buf.writeBlockPos(pos);
                    buf.writeUtf(currentId);
                    buf.writeVarInt(validIds.size());
                    for (String s : validIds) buf.writeUtf(s, 64);
                });

        return InteractionResult.SUCCESS;
    }
}
