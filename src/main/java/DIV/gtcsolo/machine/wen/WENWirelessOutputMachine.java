package DIV.gtcsolo.machine.wen;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TieredEnergyMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableEnergyContainer;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.annotation.DropSaved;
import com.gregtechceu.gtceu.api.machine.feature.IDropSaveMachine;
import net.minecraft.nbt.CompoundTag;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

/**
 * WEN ワイヤレスアウトプットポート
 * リンク先WENメインストレージからEUを引き出し、GTケーブルに放出する。
 */
public class WENWirelessOutputMachine extends TieredEnergyMachine
        implements com.gregtechceu.gtceu.api.machine.feature.IInteractedMachine, IDropSaveMachine {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER =
            new ManagedFieldHolder(
                    WENWirelessOutputMachine.class, TieredEnergyMachine.MANAGED_FIELD_HOLDER);

    private final int amperage;

    @Persisted @DropSaved private String linkedNetworkId = "";

    private TickableSubscription transferSub;
    private int transferOffset = 0;

    public WENWirelessOutputMachine(IMachineBlockEntity holder, int tier, int amperage) {
        super(holder, tier, amperage);
        this.amperage = amperage;
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    /** EU出力（発電機扱い） */
    @Override
    protected boolean isEnergyEmitter() {
        return true;
    }

    @Override
    protected long getMaxInputOutputAmperage() {
        return amperage;
    }

    @Override
    protected NotifiableEnergyContainer createEnergyContainer(Object... args) {
        int amp = args.length > 0 && args[0] instanceof Integer ? (int) args[0] : 1;
        long voltage = GTValues.V[tier];
        long capacity = voltage * amp * 16L;
        var container = NotifiableEnergyContainer.emitterContainer(this, capacity, voltage, amp);
        container.setSideOutputCondition(side -> true); // 全面出力
        return container;
    }

    // =========================================================================
    //  ライフサイクル
    // =========================================================================

    @Override
    public void onLoad() {
        super.onLoad();
        updateTransferSubscription();
    }

    private void updateTransferSubscription() {
        transferSub =
                WENLinking.transferSubscription(
                        this, transferSub, linkedNetworkId, this::onTransferTick);
    }

    @Override
    public void loadFromItem(CompoundTag tag) {
        IDropSaveMachine.super.loadFromItem(tag);
        updateTransferSubscription();
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (transferSub != null) {
            unsubscribe(transferSub);
            transferSub = null;
        }
    }

    // =========================================================================
    //  WEN → バッファ補充 (10tickごと)
    //  バッファが半分以下になったらWENから引き出す。
    //  実際のケーブルへの放出はTieredEnergyMachineのserverTickが行う。
    // =========================================================================

    private void onTransferTick() {
        if (++transferOffset % 10 != 0) return;
        if (linkedNetworkId.isEmpty()) return;
        if (getLevel() == null || getLevel().isClientSide) return;

        long stored = energyContainer.getEnergyStored();
        long capacity = energyContainer.getEnergyCapacity();

        // バッファが半分以下なら補充
        if (stored > capacity / 2) return;

        ServerLevel overworld = getLevel().getServer().overworld();
        WENNetworkData data = WENNetworkData.get(overworld);
        WENNetworkData.WENEntry entry = data.getNetwork(linkedNetworkId);

        if (entry == null) {
            setLinkedNetworkId("");
            return;
        }

        if (!entry.isFormed()) return;

        String myDim = getLevel().dimension().location().toString();
        if (!myDim.equals(entry.dimension) && !entry.crossDimensionEnabled) return;

        long need = capacity - stored;
        long withdrawn = data.removeEnergy(linkedNetworkId, need);
        if (withdrawn > 0) {
            energyContainer.addEnergy(withdrawn);
        }
    }

    // =========================================================================
    //  ID管理
    // =========================================================================

    public String getLinkedNetworkId() {
        return linkedNetworkId;
    }

    public void setLinkedNetworkId(String id) {
        this.linkedNetworkId = id == null ? "" : id;
        markDirty();
        updateTransferSubscription();
    }

    // =========================================================================
    //  レンチ右クリック: IDサイクル切り替え
    // =========================================================================

    @Override
    protected InteractionResult onWrenchClick(
            Player player, InteractionHand hand, Direction gridSide, BlockHitResult hit) {
        return WENLinking.cycleId(
                player,
                getLevel(),
                linkedNetworkId,
                this::setLinkedNetworkId,
                "gui.gtcsolo.wen_output.linked",
                tier,
                amperage);
    }

    @Override
    public InteractionResult onUse(
            net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.Level level,
            net.minecraft.core.BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        return WENLinking.openIdSelect(player, level, getPos(), linkedNetworkId);
    }

    public static WENWirelessOutputMachine create(
            IMachineBlockEntity holder, int tier, int amperage) {
        return new WENWirelessOutputMachine(holder, tier, amperage);
    }
}
