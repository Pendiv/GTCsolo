package DIV.gtcsolo.machine;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import java.util.List;

/** Unordered AL processing with a physical, non-consumable output sample above the core. */
public class IterativeAssemblyLineMachine extends WorkableElectricMultiblockMachine {
    private static final String MATCHED = "gtcsolo_iterative_sample_matched";
    private MetaMachine sampleBus;

    public IterativeAssemblyLineMachine(IMachineBlockEntity holder) { super(holder); }

    @Override public void onStructureFormed() {
        super.onStructureFormed();
        var machine = MetaMachine.getMachine(getLevel(), getPos().above());
        sampleBus = machine;
        if (sampleInventory() == null) {
            onStructureInvalid(); return;
        }
        // Reserve every sample bus in this structure, including a neighbouring line's
        // sample on a shared wall. Keep listeners/UI, but remove all its item handlers
        // (including the distinct-bus proxy), not only its raw inventory.
        for (var part : getParts()) {
            if (!(MetaMachine.getMachine(getLevel(), part.self().getPos().below())
                    instanceof IterativeAssemblyLineMachine)) continue;
            var sampleHandlers = part.getRecipeHandlers();
            for (IO io : new IO[]{IO.IN, IO.BOTH}) {
                var handlers = capabilitiesProxy.get(io, ItemRecipeCapability.CAP);
                if (handlers != null) handlers.removeIf(sampleHandlers::contains);
            }
        }
    }

    @Override public void onStructureInvalid() {
        sampleBus = null;
        super.onStructureInvalid();
    }

    public boolean matchesSample(GTRecipe recipe) {
        var inventory = sampleInventory();
        if (inventory == null) return false;
        var samples = IterativeSampleInventory.count(inventory);
        for (var content : recipe.getOutputContents(ItemRecipeCapability.CAP))
            for (ItemStack output : ItemRecipeCapability.CAP.of(content.content).getItems())
                if (!output.isEmpty() && IterativeSampleInventory.qualifies(samples, output.getItem())) return true;
        return false;
    }

    private IItemHandler sampleInventory() {
        // Read the bus's own exposed inventory, without sided cover wrappers or extraction.
        return sampleBus == null ? null : sampleBus.getItemHandlerCap(null, false);
    }

    @Override public GTRecipe fullModifyRecipe(GTRecipe recipe) {
        if (!isRecipeLogicAvailable() || recipe.recipeType != GTRecipeTypes.ASSEMBLY_LINE_RECIPES) return null;
        GTRecipe detached = recipe.copy(ContentModifier.multiplier(1), false);
        detached.data = recipe.data.copy();
        return super.fullModifyRecipe(detached);
    }

    public static ModifierFunction overclock(MetaMachine machine, GTRecipe recipe) {
        if (!(machine instanceof IterativeAssemblyLineMachine line)
                || RecipeHelper.getInputEUt(recipe) <= 0
                || RecipeHelper.getInputEUt(recipe) > line.getOverclockVoltage()) return ModifierFunction.NULL;
        boolean matched = line.matchesSample(recipe);
        ModifierFunction oc = IterativeAssemblyRules.profile(matched)
                .getModifier(line, recipe, line.getOverclockVoltage(), false);
        return source -> {
            GTRecipe result = oc.apply(source);
            if (result != null) {
                result.data = result.data.copy();
                result.data.putBoolean(MATCHED, matched);
            }
            return result;
        };
    }

    @Override public void addDisplayText(List<Component> text) {
        super.addDisplayText(text);
        GTRecipe recipe = getRecipeLogic().getLastRecipe();
        boolean matched = recipe != null && getRecipeLogic().isActive() && recipe.data.getBoolean(MATCHED);
        text.add(Component.translatable("gtcsolo.iterative_assembly.display.oc",
                matched ? IterativeAssemblyRules.MATCHED_SPEED : IterativeAssemblyRules.NORMAL_SPEED));
        var inventory = sampleInventory();
        if (inventory != null) {
            int types = IterativeSampleInventory.count(inventory).size();
            text.add(Component.translatable("gtcsolo.iterative_assembly.display.sample", types, types));
        }
    }
}
