package DIV.gtcsolo.machine.wen;

import DIV.gtcsolo.block.wen.WENIdSelectMenu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IInteractedMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableEnergyContainer;
import com.gregtechceu.gtceu.common.machine.multiblock.part.EnergyHatchPartMachine;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.slf4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

/**
 * WEN ワイヤレスエネルギーハッチ
 * GTマルチブロックのエネルギーハッチとして機能し、
 * WENメインストレージからEUを引き出してマルチブロックに供給する。
 *
 * PartAbility.INPUT_ENERGY を持つため、既存マルチブロックにそのまま使える。
 */
public class WENEnergyHatchMachine extends EnergyHatchPartMachine implements IInteractedMachine {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            WENEnergyHatchMachine.class, EnergyHatchPartMachine.MANAGED_FIELD_HOLDER);

    @Persisted
    private String linkedNetworkId = "";

    private TickableSubscription wenTickSub;
    private int tickOffset = 0;
    private int logThrottle = 0;

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
        LOGGER.info("[WEN Hatch] Loaded at {}: {} {}A linked='{}'",
                getPos(), GTValues.VN[tier], getAmperage(), linkedNetworkId);
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
        boolean shouldLog = (++logThrottle % 10 == 0);

        if (shouldLog) {
            LOGGER.info("[WEN Hatch] buffer={}/{}EU linked='{}' at {}",
                    stored, capacity, linkedNetworkId, getPos());
        }

        // バッファが半分以下なら補充
        if (stored > capacity / 2) return;

        ServerLevel overworld = getLevel().getServer().overworld();
        WENNetworkData data = WENNetworkData.get(overworld);
        WENNetworkData.WENEntry entry = data.getNetwork(linkedNetworkId);

        if (entry == null) {
            if (shouldLog) LOGGER.info("[WEN Hatch] Network '{}' not found — unlinking", linkedNetworkId);
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
            if (shouldLog) LOGGER.info("[WEN Hatch] Withdrew {}EU from '{}'", withdrawn, linkedNetworkId);
        }
    }

    // =========================================================================
    //  ID管理: 素手+Shift右クリックでUI / レンチクリックでサイクル
    // =========================================================================

    public String getLinkedNetworkId() { return linkedNetworkId; }

    public void setLinkedNetworkId(String id) {
        this.linkedNetworkId = id;
        LOGGER.info("[WEN Hatch] Linked to '{}' at {}", id, getPos());
    }

    @Override
    public boolean shouldOpenUI(Player player, InteractionHand hand, BlockHitResult hit) {
        // GTデフォルトのUI表示を抑止（ワイヤレスハッチは独自UI）
        return false;
    }

    @Override
    protected net.minecraft.world.InteractionResult onWrenchClick(Player player, InteractionHand hand,
                                                                   net.minecraft.core.Direction gridSide,
                                                                   BlockHitResult hit) {
        if (getLevel() == null || getLevel().isClientSide) return net.minecraft.world.InteractionResult.SUCCESS;

        ServerLevel overworld = getLevel().getServer().overworld();
        WENNetworkData data = WENNetworkData.get(overworld);
        List<String> validIds = getValidNetworkIds(data);

        if (validIds.isEmpty()) {
            player.sendSystemMessage(Component.translatable("gui.gtcsolo.wen_input.no_local_networks"));
            return net.minecraft.world.InteractionResult.SUCCESS;
        }

        int idx = validIds.indexOf(linkedNetworkId);
        String newId = validIds.get((idx + 1) % validIds.size());
        setLinkedNetworkId(newId);
        player.sendSystemMessage(Component.translatable("gui.gtcsolo.wen_hatch.linked",
                newId, GTValues.VNF[tier], String.valueOf(getAmperage())));
        return net.minecraft.world.InteractionResult.SUCCESS;
    }

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

    // =========================================================================
    //  素手+Shift右クリック: ID選択UI
    // =========================================================================

    @Override
    public InteractionResult onUse(BlockState state, Level level, net.minecraft.core.BlockPos pos,
                                    Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!player.isShiftKeyDown() || !player.getMainHandItem().isEmpty()) return InteractionResult.PASS;

        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        ServerLevel overworld = level.getServer().overworld();
        WENNetworkData data = WENNetworkData.get(overworld);
        List<String> validIds = getValidNetworkIds(data);

        NetworkHooks.openScreen(sp, new net.minecraft.world.MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("WEN ID Select");
            }

            @Override
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
                    int id, net.minecraft.world.entity.player.Inventory inv, Player p) {
                return new WENIdSelectMenu(id, getPos(), linkedNetworkId, validIds);
            }
        }, (FriendlyByteBuf buf) -> {
            buf.writeBlockPos(getPos());
            buf.writeUtf(linkedNetworkId);
            buf.writeVarInt(validIds.size());
            for (String s : validIds) buf.writeUtf(s, 64);
        });

        return InteractionResult.SUCCESS;
    }

    public static WENEnergyHatchMachine create(IMachineBlockEntity holder, int tier, int amperage) {
        return new WENEnergyHatchMachine(holder, tier, amperage);
    }
}