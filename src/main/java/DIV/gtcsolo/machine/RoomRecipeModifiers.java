package DIV.gtcsolo.machine;

import DIV.gtcsolo.machine.mega.MegaMixerRecipeModifiers;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gregtechceu.gtceu.utils.GTUtil;

import java.util.ArrayList;
import java.util.List;

/** A single electrical OC pass, with inventory-aware hatch and sub-tick parallelism. */
public final class RoomRecipeModifiers {
    private RoomRecipeModifiers() {}

    public static ModifierFunction modify(MetaMachine machine, GTRecipe recipe) {
        if (!(machine instanceof ResourceRoomMachine room) || !room.isFormed()
                || room.getStructureTier() < 0) return ModifierFunction.NULL;
        return source -> apply(room, source);
    }

    private static GTRecipe apply(ResourceRoomMachine room, GTRecipe source) {
        long eut = RecipeHelper.getInputEUt(source);
        long voltage = room.getOverclockVoltage();
        int maximumStages = RoomRecipeRules.maximumStages(eut, voltage);
        if (maximumStages < 0) return null;
        // Preserve industrial tier-based OC counts even for recipes far below
        // their tier's nominal EU/t (especially very small ULV recipes).
        maximumStages = Math.min(maximumStages,
                Math.max(0, GTUtil.getOCTierByVoltage(voltage) - GTUtil.getTierByVoltage(eut)));
        boolean construction = room.isConstructionRoom();
        if (construction && (!source.data.contains(RoomRecipeRules.REQUIRED_TEMPERATURE)
                || source.data.getInt(RoomRecipeRules.REQUIRED_TEMPERATURE) < 0)) return null;
        int required = construction ? source.data.getInt(RoomRecipeRules.REQUIRED_TEMPERATURE) : 0;
        int requested = room.getParallelHatch().map(hatch -> Math.max(1, hatch.getCurrentParallel())).orElse(1);
        GTRecipe best = null;
        int bestParallels = 0;
        // Prioritize paid hatch parallelism, as the ordinary GT parallel-then-OC
        // chain does. For the same paid count use the largest feasible OC count.
        // Only the small number of electrical OC stages is enumerated.
        for (int stages = 0; stages <= maximumStages; stages++) {
            var profile = RoomRecipeRules.profile(eut, source.duration, stages, construction,
                    room.getRoomTemperature(), required);
            if (profile == null) continue;
            GTRecipe boosted = MegaMixerRecipeModifiers.scaleBatch(source, profile.ioBonus());
            if (boosted == null || MegaMixerRecipeModifiers.safeMultiplier(boosted) < profile.subtickParallels()) continue;
            GTRecipe clocked = ModifierFunction.builder()
                    .modifyAllContents(ContentModifier.multiplier(profile.subtickParallels()))
                    .parallels(profile.subtickParallels()).addOCs(profile.stages()).build().apply(boosted);
            clocked.duration = profile.duration();
            clocked.data = source.data.copy();
            EURecipeCapability.putEUContent(clocked.tickInputs, profile.eut());
            int limit = (int) Math.min(requested, voltage / profile.eut());
            limit = Math.min(limit, MegaMixerRecipeModifiers.safeMultiplier(clocked));
            if (limit < bestParallels || limit < 1) continue;
            // Native EU parallel matching limits by voltage, whereas sub-tick
            // rolls already share the OC's power. Enforce paid power explicitly.
            GTRecipe capacity = clocked.copy(ContentModifier.multiplier(1), false);
            capacity.tickInputs.remove(EURecipeCapability.CAP);
            reserveChanceRolls(capacity);
            int parallel = availableParallels(room, capacity, limit);
            parallel = room.limitExtraOutputs(capacity, parallel);
            if (parallel < 1 || parallel < bestParallels) continue;
            GTRecipe result = ModifierFunction.builder()
                    .modifyAllContents(ContentModifier.multiplier(parallel)).parallels(parallel).build().apply(clocked);
            EURecipeCapability.putEUContent(result.tickInputs, profile.eut() * parallel);
            result.data = clocked.data.copy();
            result.data.putInt(RoomRecipeRules.PAID_PARALLELS, parallel);
            result.data.putInt(RoomRecipeRules.SUBTICK_PARALLELS, profile.subtickParallels());
            result.data.putInt(RoomRecipeRules.IO_BONUS, profile.ioBonus());
            result.data.putInt(RoomRecipeRules.HEAT_STEPS, profile.heatSteps());
            best = result;
            bestParallels = parallel;
        }
        return best;
    }

    private static void reserveChanceRolls(GTRecipe capacity) {
        // Native Item/Fluid parallel-capacity methods inspect the amount but do
        // not expand the recipe's existing chanced rolls. Reserve the worst-case
        // quantity for every roll; keep the executable recipe's chance unchanged.
        for (var map : List.of(capacity.inputs, capacity.outputs, capacity.tickInputs, capacity.tickOutputs)) {
            for (RecipeCapability<?> cap : List.of(ItemRecipeCapability.CAP, FluidRecipeCapability.CAP)) {
                List<Content> old = map.get(cap);
                if (old == null) continue;
                List<Content> reserved = new ArrayList<>(old.size());
                for (Content content : old) {
                    reserved.add(content.chance > 0 && content.chance < content.maxChance ? new Content(
                            cap.copyContent(content.content, ContentModifier.multiplier(Math.max(1, capacity.parallels))),
                            content.maxChance, content.maxChance, 0, content.slotName, content.uiName) : content);
                }
                map.put(cap, reserved);
            }
        }
    }

    private static int availableParallels(ResourceRoomMachine room, GTRecipe capacity, int limit) {
        // getParallelAmount skips all inventory checks for limit=1.
        boolean hasInputs = capacity.inputs.keySet().stream().anyMatch(cap -> cap.doMatchInRecipe())
                || capacity.tickInputs.keySet().stream().anyMatch(cap -> cap.doMatchInRecipe());
        int inputs = hasInputs ? Math.min(limit, ParallelLogic.limitByInput(room, capacity, limit)) : limit;
        return inputs <= 0 ? 0 : Math.min(inputs,
                ParallelLogic.limitByOutputMerging(room, capacity, inputs, room::canVoidRecipeOutputs));
    }
}
