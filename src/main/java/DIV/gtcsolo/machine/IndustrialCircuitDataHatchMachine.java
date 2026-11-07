package DIV.gtcsolo.machine;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.common.machine.multiblock.part.DataAccessHatchMachine;
import com.gregtechceu.gtceu.utils.ResearchManager;

/** Local physical media intake; high-CWU data modules are valid here without a separate data bank. */
public final class IndustrialCircuitDataHatchMachine extends DataAccessHatchMachine {
    public IndustrialCircuitDataHatchMachine(IMachineBlockEntity holder) {
        super(holder, com.gregtechceu.gtceu.api.GTValues.LuV, false);
    }

    @Override
    protected NotifiableItemStackHandler createImportItemHandler() {
        return new NotifiableItemStackHandler(this, getInventorySize(), IO.BOTH)
                .setFilter(stack -> {
                    var research = ResearchManager.readResearchId(stack);
                    return ResearchManager.isStackDataItem(stack, true) && research != null
                            && research.getFirst() == DIV.gtcsolo.registry.ModRecipeTypes.CIRCUIT_ASSEMBLY_LINE;
                });
    }
}
