package DIV.gtcsolo.machine;

import DIV.gtcsolo.api.tier.RoomStructureTier;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.utils.GTUtil;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Structure grade limits both recipe search and the integrated industrial OC. */
public class ResourceRoomMachine extends CoilWorkableElectricMultiblockMachine {
    protected static final ManagedFieldHolder FIELDS = new ManagedFieldHolder(ResourceRoomMachine.class, MANAGED_FIELD_HOLDER);
    private final RoomStructureTier roomTier;
    private final boolean construction;
    @DescSynced private int structureTier = -1;
    @DescSynced private int roomTemperature;

    public ResourceRoomMachine(IMachineBlockEntity holder, RoomStructureTier roomTier, boolean construction) {
        super(holder);
        this.roomTier = roomTier;
        this.construction = construction;
    }

    @Override public ManagedFieldHolder getFieldHolder() { return FIELDS; }
    public boolean isConstructionRoom() { return construction; }
    public int getStructureTier() { return structureTier; }
    /** Absolute temperatures are in kelvin, as in the native GT coils. */
    public int getRoomTemperature() { return roomTemperature; }
    public int limitExtraOutputs(com.gregtechceu.gtceu.api.recipe.GTRecipe capacity, int parallel) { return parallel; }

    @Override public void onStructureFormed() {
        super.onStructureFormed();
        structureTier = roomTier.getTierFromMachine(this);
        roomTemperature = construction ? getCoilType().getCoilTemperature() : 0;
        tier = GTUtil.getFloorTierByVoltage(getMaxVoltage());
    }

    @Override public void onStructureInvalid() {
        super.onStructureInvalid();
        structureTier = -1;
        roomTemperature = 0;
    }

    private long structureVoltage() {
        return structureTier >= GTValues.LV && structureTier <= GTValues.OpV ? GTValues.V[structureTier] : 0;
    }

    @Override public long getMaxVoltage() { return Math.min(super.getMaxVoltage(), structureVoltage()); }
    @Override public long getOverclockVoltage() { return Math.min(super.getOverclockVoltage(), structureVoltage()); }

    @Override public void addDisplayText(List<Component> lines) {
        super.addDisplayText(lines);
        if (!isFormed() || structureTier < GTValues.LV) return;
        lines.add(Component.translatable("gtcsolo.room.structure_tier", GTValues.VNF[structureTier]));
        if (construction) lines.add(Component.translatable("gtceu.recipe.temperature", roomTemperature));
        var recipe = getRecipeLogic().getLastRecipe();
        if (recipe != null && getRecipeLogic().isActive()) {
            lines.add(Component.translatable("gtcsolo.room.batch",
                    recipe.data.getInt(RoomRecipeRules.IO_BONUS),
                    recipe.data.getInt(RoomRecipeRules.PAID_PARALLELS),
                    recipe.data.getInt(RoomRecipeRules.SUBTICK_PARALLELS)));
        }
    }
}
