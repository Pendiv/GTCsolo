package DIV.gtcsolo.machine.wen;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TieredEnergyMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableEnergyContainer;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;
import java.util.stream.Collectors;

public class WENWirelessInputMachine extends TieredEnergyMachine {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            WENWirelessInputMachine.class, TieredEnergyMachine.MANAGED_FIELD_HOLDER);

    private final int amperage;

    @Persisted
    private String linkedNetworkId = "";

    private TickableSubscription transferSub;
    private int transferOffset = 0;

    public WENWirelessInputMachine(IMachineBlockEntity holder, int tier, int amperage) {
        super(holder, tier, amperage);
        this.amperage = amperage;
    }

    @Override
    public ManagedFieldHolder getFieldHolder() { return MANAGED_FIELD_HOLDER; }

    @Override
    protected boolean isEnergyEmitter() { return false; }

    @Override
    protected long getMaxInputOutputAmperage() { return amperage; }

    @Override
    protected NotifiableEnergyContainer createEnergyContainer(Object... args) {
        int amp = args.length > 0 && args[0] instanceof Integer ? (int) args[0] : 1;
        long voltage = GTValues.V[tier];
        long capacity = voltage * amp * 16L;
        var container = NotifiableEnergyContainer.receiverContainer(this, capacity, voltage, amp);
        container.setSideInputCondition(side -> true);
        return container;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (transferSub == null) transferSub = subscribeServerTick(this::onTransferTick);
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (transferSub != null) { unsubscribe(transferSub); transferSub = null; }
    }

    private void onTransferTick() {
        if (++transferOffset % 10 != 0) return;
        if (linkedNetworkId.isEmpty()) return;
        if (getLevel() == null || getLevel().isClientSide) return;

        long stored = energyContainer.getEnergyStored();
        if (stored <= 0) return;

        ServerLevel overworld = getLevel().getServer().overworld();
        WENNetworkData data = WENNetworkData.get(overworld);
        WENNetworkData.WENEntry entry = data.getNetwork(linkedNetworkId);

        if (entry == null) {
            linkedNetworkId = ""; // 存在しないIDは自動解除
            return;
        }

        // 構造チェック
        if (!entry.isFormed()) return;

        // ディメンション制限
        String myDim = getLevel().dimension().location().toString();
        if (!myDim.equals(entry.dimension) && !entry.crossDimensionEnabled) return;

        long transferred = data.addEnergy(linkedNetworkId, stored);
        if (transferred > 0) {
            energyContainer.removeEnergy(transferred);
        }
    }

    public String getLinkedNetworkId() { return linkedNetworkId; }
    public void setLinkedNetworkId(String id) {
        this.linkedNetworkId = id;
    }

    /** レンチクリック: IDサイクル */
    @Override
    protected InteractionResult onWrenchClick(Player player, InteractionHand hand,
                                               Direction gridSide, BlockHitResult hit) {
        if (getLevel() == null || getLevel().isClientSide) return InteractionResult.SUCCESS;

        ServerLevel overworld = getLevel().getServer().overworld();
        WENNetworkData data = WENNetworkData.get(overworld);
        List<String> validIds = getValidNetworkIds(data);

        if (validIds.isEmpty()) {
            player.sendSystemMessage(Component.translatable("gui.gtcsolo.wen_input.no_local_networks"));
            return InteractionResult.SUCCESS;
        }

        int idx = validIds.indexOf(linkedNetworkId);
        String newId = validIds.get((idx + 1) % validIds.size());
        setLinkedNetworkId(newId);
        player.sendSystemMessage(Component.translatable("gui.gtcsolo.wen_input.linked",
                newId, GTValues.VNF[tier], String.valueOf(amperage)));
        return InteractionResult.SUCCESS;
    }

    /** 利用可能なネットワークIDリストを取得 */
    private List<String> getValidNetworkIds(WENNetworkData data) {
        String myDim = getLevel().dimension().location().toString();
        return data.getAllNetworkIds().stream()
                .filter(id -> {
                    WENNetworkData.WENEntry e = data.getNetwork(id);
                    if (e == null || !e.isFormed()) return false;
                    return myDim.equals(e.dimension) || e.crossDimensionEnabled;
                })
                .sorted()
                .collect(Collectors.toList());
    }

    public static WENWirelessInputMachine create(IMachineBlockEntity holder, int tier, int amperage) {
        return new WENWirelessInputMachine(holder, tier, amperage);
    }
}
