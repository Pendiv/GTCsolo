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
 * WEN ワイヤレスエネルギーハッチ
 * GTマルチブロックのエネルギーハッチとして機能し、
 * WENメインストレージからEUを引き出してマルチブロックに供給する。
 *
 * PartAbility.INPUT_ENERGY を持つため、既存マルチブロックにそのまま使える。
 */
public class WENEnergyHatchMachine extends EnergyHatchPartMachine implements IInteractedMachine {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            WENEnergyHatchMachine.class, EnergyHatchPartMachine.MANAGED_FIELD_HOLDER);

    @Persisted
    private String linkedNetworkId = "";

    private TickableSubscription wenTickSub;
    private int tickOffset = 0;

    public WENEnergyHatchMachine(IMachineBlockEntity holder, int tier, int amperage) {
        super(holder, tier, IO.IN, amperage);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() { return MANAGED_FIELD_HOLDER; }

    // =========================================================================
    //  ライフサイクル
    // =========================================================================

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

    // =========================================================================
    //  WEN → ハッチバッファ補充 (10tickごと)
    //  バッファが半分以下ならWENから引き出す。
    //  マルチブロックのRecipeLogicがバッファからEUを消費する。
    // =========================================================================

    private void onWenTick() {
        if (++tickOffset % 10 != 0) return;
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
            linkedNetworkId = "";
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
    //  ID管理: 素手+Shift右クリックでUI / レンチクリックでサイクル
    // =========================================================================

    public String getLinkedNetworkId() { return linkedNetworkId; }

    public void setLinkedNetworkId(String id) {
        this.linkedNetworkId = id;
    }

    @Override
    public boolean shouldOpenUI(Player player, InteractionHand hand, BlockHitResult hit) {
        // GTデフォルトのUI表示を抑止（ワイヤレスハッチは独自UI）
        return false;
    }

    @Override
    protected InteractionResult onWrenchClick(Player player, InteractionHand hand,
                                               net.minecraft.core.Direction gridSide,
                                               BlockHitResult hit) {
        return WENLinking.cycleId(player, getLevel(), linkedNetworkId, this::setLinkedNetworkId,
                "gui.gtcsolo.wen_hatch.linked", tier, getAmperage());
    }

    // =========================================================================
    //  素手+Shift右クリック: ID選択UI
    // =========================================================================

    @Override
    public InteractionResult onUse(BlockState state, Level level, net.minecraft.core.BlockPos pos,
                                    Player player, InteractionHand hand, BlockHitResult hit) {
        return WENLinking.openIdSelect(player, level, getPos(), linkedNetworkId);
    }

    public static WENEnergyHatchMachine create(IMachineBlockEntity holder, int tier, int amperage) {
        return new WENEnergyHatchMachine(holder, tier, amperage);
    }
}
