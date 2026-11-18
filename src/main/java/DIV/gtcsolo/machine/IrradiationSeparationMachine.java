package DIV.gtcsolo.machine;

import DIV.gtcsolo.api.tier.RoomStructureTier;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.utils.GTUtil;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class IrradiationSeparationMachine extends WorkableElectricMultiblockMachine {
    private static final ManagedFieldHolder FIELDS =
            new ManagedFieldHolder(IrradiationSeparationMachine.class, MANAGED_FIELD_HOLDER);
    private final RoomStructureTier glass;
    @DescSynced private int glassTier = -1;

    public IrradiationSeparationMachine(IMachineBlockEntity holder, RoomStructureTier glass) {
        super(holder);
        this.glass = glass;
    }

    @Override public ManagedFieldHolder getFieldHolder() { return FIELDS; }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        glassTier = glass.getTierFromMachine(this);
        tier = GTUtil.getFloorTierByVoltage(getMaxVoltage());
        var recipe = getRecipeLogic().getLastRecipe();
        if (recipe != null && RecipeHelper.getInputEUt(recipe) > glassVoltage()) getRecipeLogic().resetRecipeLogic();
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        glassTier = -1;
    }

    @Override
    public void onPartUnload() {
        super.onPartUnload();
        glassTier = -1;
    }

    private long glassVoltage() {
        return glassTier >= GTValues.IV && glassTier <= GTValues.OpV ? GTValues.V[glassTier] : 0;
    }

    @Override public long getMaxVoltage() { return Math.min(super.getMaxVoltage(), glassVoltage()); }
    @Override public long getOverclockVoltage() { return Math.min(super.getOverclockVoltage(), glassVoltage()); }

    @Override
    public void addDisplayText(List<Component> lines) {
        super.addDisplayText(lines);
        if (isFormed() && glassTier >= GTValues.IV) {
            lines.add(Component.translatable("gtcsolo.room.structure_tier", GTValues.VNF[glassTier]));
        }
    }
}
