package DIV.gtcsolo.machine.wen;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IInteractedMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.EnergyHatchPartMachine;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * WEN ワイヤレスエネルギー出力ハッチ（ダイナモハッチ）
 * GTマルチブロックのエネルギー出力ハッチとして機能し、
 * マルチブロックが生成したEUをWENメインストレージに送り込む。
 */
public class WENEnergyOutputHatchMachine extends EnergyHatchPartMachine implements IInteractedMachine {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            WENEnergyOutputHatchMachine.class, EnergyHatchPartMachine.MANAGED_FIELD_HOLDER);

    @Persisted
    private String linkedNetworkId = "";

    private TickableSubscription wenTickSub;
    private int tickOffset = 0;

    public WENEnergyOutputHatchMachine(IMachineBlockEntity holder, int tier, int amperage) {
        super(holder, tier, IO.OUT, amperage);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() { return MANAGED_FIELD_HOLDER; }

    @Override
    public void onLoad() {
        super.onLoad();
        if (wenTickSub == null) wenTickSub = subscribeServerTick(this::onWenTick);
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (wenTickSub != null) { unsubscribe(wenTickSub); wenTickSub = null; }
    }

    private void onWenTick() {
        if (++tickOffset % 10 != 0) return;
        if (linkedNetworkId.isEmpty()) return;
        if (getLevel() == null || getLevel().isClientSide) return;

        long stored = energyContainer.getEnergyStored();
        if (stored <= 0) return;

        ServerLevel overworld = getLevel().getServer().overworld();
        WENNetworkData data = WENNetworkData.get(overworld);
        WENNetworkData.WENEntry entry = data.getNetwork(linkedNetworkId);

        if (entry == null) {
            linkedNetworkId = "";
            return;
        }

        if (!entry.isFormed()) return;

        String myDim = getLevel().dimension().location().toString();
        if (!myDim.equals(entry.dimension) && !entry.crossDimensionEnabled) return;

        long pushed = data.addEnergy(linkedNetworkId, stored);
        if (pushed > 0) {
            energyContainer.removeEnergy(pushed);
        }
    }

    public String getLinkedNetworkId() { return linkedNetworkId; }

    public void setLinkedNetworkId(String id) {
        this.linkedNetworkId = id;
    }

    @Override
    public boolean shouldOpenUI(Player player, InteractionHand hand, BlockHitResult hit) {
        return false;
    }

    @Override
    protected InteractionResult onWrenchClick(Player player, InteractionHand hand,
                                               net.minecraft.core.Direction gridSide,
                                               BlockHitResult hit) {
        return WENLinking.cycleId(player, getLevel(), linkedNetworkId, this::setLinkedNetworkId,
                "gui.gtcsolo.wen_output_hatch.linked", tier, getAmperage());
    }

    @Override
    public InteractionResult onUse(BlockState state, Level level, net.minecraft.core.BlockPos pos,
                                    Player player, InteractionHand hand, BlockHitResult hit) {
        return WENLinking.openIdSelect(player, level, getPos(), linkedNetworkId);
    }

    public static WENEnergyOutputHatchMachine create(IMachineBlockEntity holder, int tier, int amperage) {
        return new WENEnergyOutputHatchMachine(holder, tier, amperage);
    }
}