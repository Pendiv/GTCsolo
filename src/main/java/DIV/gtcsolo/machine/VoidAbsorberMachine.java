package DIV.gtcsolo.machine;

import DIV.gtcsolo.machine.mega.MegaMixerRecipeModifiers;
import DIV.gtcsolo.util.GtcsoloRecipeModifiers;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.data.worldgen.bedrockfluid.BedrockFluidVeinSavedData;
import com.gregtechceu.gtceu.api.data.worldgen.bedrockfluid.FluidVeinWorldEntry;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.FluidDrillMachine;
import com.gregtechceu.gtceu.common.machine.trait.FluidDrillLogic;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/** Bedrock installation with native recipe processing and the shared GregTech fluid-vein data. */
public final class VoidAbsorberMachine extends WorkableElectricMultiblockMachine {
    public static final int OCEAN_MODE = 0;
    public static final int VEIN_MODE = 1;
    public static final int MA_MODE = 2;
    private static final int DRILL_TIER = GTValues.EV;
    private static final String VEIN_RECIPE = "gtcsolo_absorber_vein";
    private static final String VEIN_BATCHES = "gtcsolo_absorber_vein_batches";
    private final int shaftOffset;
    private final int baseParallels;
    private long surfaceCheckTime = Long.MIN_VALUE;
    private boolean oceanAbove;

    public VoidAbsorberMachine(IMachineBlockEntity holder) {
        this(holder, 5, 1);
    }

    public static VoidAbsorberMachine deepDrillingRig(IMachineBlockEntity holder) {
        return new VoidAbsorberMachine(holder, 0, 16);
    }

    private VoidAbsorberMachine(IMachineBlockEntity holder, int shaftOffset, int baseParallels) {
        super(holder);
        this.shaftOffset = shaftOffset;
        this.baseParallels = baseParallels;
    }

    /** Foundation blocks are structural requirements; their world altitude is unrestricted. */
    public static TraceabilityPredicate foundationPredicate() {
        var bedrock = Predicates.blocks(Blocks.BEDROCK)
                .setMinGlobalLimited(5).setPreviewCount(5);
        var deepslate = Predicates.blocks(Blocks.DEEPSLATE);
        return bedrock.or(deepslate).addTooltips(Component.translatable("gtcsolo.void_absorber.foundation"));
    }

    /** The marked casing joins the same casing below it, in either the next repeat or the lower decoration. */
    public static TraceabilityPredicate repeatedCasingPredicate(Block casing) {
        return new TraceabilityPredicate(state ->
                state.getBlockState().is(casing) && state.getWorld().getBlockState(state.getPos().below()).is(casing),
                Predicates.blocks(casing).common.get(0).candidates);
    }

    @Override
    protected RecipeLogic createRecipeLogic(Object... args) {
        return new ExtractionLogic(this);
    }

    @Override
    public void onStructureFormed() {
        surfaceCheckTime = Long.MIN_VALUE;
        super.onStructureFormed();
    }

    @Override
    public void onStructureInvalid() {
        surfaceCheckTime = Long.MIN_VALUE;
        super.onStructureInvalid();
    }

    @Override
    public void setActiveRecipeType(int mode) {
        if (mode < OCEAN_MODE || mode > MA_MODE || mode == getActiveRecipeType()) return;
        super.setActiveRecipeType(mode);
        if (getLevel() != null && !getLevel().isClientSide) {
            if (getRecipeLogic().isWorking() || getRecipeLogic().isWaiting()) getRecipeLogic().interruptRecipe();
            getRecipeLogic().resetRecipeLogic();
            getRecipeLogic().updateTickSubscription();
            onChanged();
        }
    }

    private BlockPos drillingColumn() {
        return getPos().relative(getFrontFacing().getOpposite(), shaftOffset);
    }

    private boolean hasOceanAbove() {
        if (!(getLevel() instanceof ServerLevel level)) return false;
        long time = level.getGameTime();
        if (surfaceCheckTime != Long.MIN_VALUE && time >= surfaceCheckTime && time - surfaceCheckTime < 20)
            return oceanAbove;
        BlockPos column = drillingColumn();
        int y = Math.max(level.getSeaLevel(), level.getHeight(Heightmap.Types.WORLD_SURFACE, column.getX(), column.getZ()) - 1);
        y = Math.min(level.getMaxBuildHeight() - 1, y);
        oceanAbove = level.getBiome(new BlockPos(column.getX(), y, column.getZ())).is(BiomeTags.IS_OCEAN);
        surfaceCheckTime = time;
        return oceanAbove;
    }

    private boolean siteAllowsRecipe(GTRecipe recipe) {
        if (!isFormed() || recipe == null || recipe.recipeType != getRecipeType()) return false;
        return switch (getActiveRecipeType()) {
            case OCEAN_MODE -> hasOceanAbove();
            case VEIN_MODE -> getMaxVoltage() >= GTValues.V[DRILL_TIER];
            case MA_MODE -> getMaxVoltage() >= GTValues.V[GTValues.UV];
            default -> false;
        };
    }

    @Override
    public GTRecipe fullModifyRecipe(GTRecipe recipe) {
        if (!siteAllowsRecipe(recipe)) return null;
        // Vein yield is regenerated after depletion, rather than reusing the preceding batch's yield.
        GTRecipe candidate = getActiveRecipeType() == VEIN_MODE ? createVeinRecipe() : recipe;
        if (candidate == null) return null;
        GTRecipe result = super.fullModifyRecipe(candidate);
        if (result == null) return null;
        if (baseParallels == 1) return GtcsoloRecipeModifiers.industrialOverclock(this, result).apply(result);
        return parallelExtraction(result);
    }

    private GTRecipe parallelExtraction(GTRecipe recipe) {
        int hatchParallels = getParallelHatch().map(hatch -> Math.max(1, hatch.getCurrentParallel())).orElse(1);
        int requested = (int) Math.min(Integer.MAX_VALUE, (long) baseParallels * hatchParallels);
        var energy = getEnergyContainer();
        long availableEUt = energy.getInputVoltage() * energy.getInputAmperage();
        if (availableEUt <= 0) return null;

        GTRecipe best = null;
        int bestParallels = 0;
        int recipeTier = RecipeHelper.getRecipeEUtTier(recipe);
        int maximumMultiplier = MegaMixerRecipeModifiers.safeMultiplier(recipe);
        // Reserve power for parallel extraction before spending the remaining budget on overclocking.
        for (int tier = getTier(); tier >= recipeTier; tier--) {
            if (Math.scalb(1.0, tier - recipeTier) > maximumMultiplier) continue;
            GTRecipe overclocked = GtcsoloRecipeModifiers.industrialOverclock(recipe, tier).apply(recipe);
            long eut = RecipeHelper.getInputEUt(overclocked);
            if (eut <= 0) continue;
            int limit = (int) Math.min(requested, availableEUt / eut);
            limit = Math.min(limit, MegaMixerRecipeModifiers.safeMultiplier(overclocked));
            if (limit <= bestParallels) continue;

            var capacityProbe = overclocked.copy();
            capacityProbe.tickInputs.remove(EURecipeCapability.CAP);
            int parallels = MegaMixerRecipeModifiers.availableParallels(this, capacityProbe, limit);
            if (parallels <= bestParallels) continue;
            best = overclocked;
            bestParallels = parallels;
            if (parallels == requested) break;
        }
        if (best == null) return null;
        var result = ModifierFunction.builder().modifyAllContents(ContentModifier.multiplier(bestParallels))
                .eutMultiplier(bestParallels).parallels(bestParallels).build().apply(best);
        if (result.data.getBoolean(VEIN_RECIPE)) {
            result.data = result.data.copy();
            result.data.putInt(VEIN_BATCHES, bestParallels);
        }
        return result;
    }

    @Override
    public boolean beforeWorking(GTRecipe recipe) {
        return siteAllowsRecipe(recipe) && super.beforeWorking(recipe);
    }

    @Override
    public boolean onWorking() {
        return siteAllowsRecipe(getRecipeLogic().getLastRecipe()) && super.onWorking();
    }

    private GTRecipe createVeinRecipe() {
        if (!(getLevel() instanceof ServerLevel level) || !isFormed()) return null;
        BlockPos column = drillingColumn();
        int chunkX = SectionPos.blockToSectionCoord(column.getX());
        int chunkZ = SectionPos.blockToSectionCoord(column.getZ());
        var data = BedrockFluidVeinSavedData.getOrCreate(level);
        var fluid = data.getFluidInChunk(chunkX, chunkZ);
        if (fluid == null) return null;
        var entry = data.getFluidVeinWorldEntry(chunkX, chunkZ);
        return createVeinRecipe(getRecipeType(), fluid, entry);
    }

    /** Rebuild the base batch from current reserves, before applying the shared industrial OC. */
    static GTRecipe createVeinRecipe(GTRecipeType recipeType, Fluid fluid, FluidVeinWorldEntry entry) {
        if (entry == null || entry.getDefinition() == null || entry.getOperationsRemaining() <= 0) return null;
        long yield = Math.max(entry.getDefinition().getDepletedYield(),
                (long) entry.getFluidYield() * Math.max(0, entry.getOperationsRemaining()) /
                        BedrockFluidVeinSavedData.MAXIMUM_VEIN_OPERATIONS);
        yield *= FluidDrillMachine.getRigMultiplier(DRILL_TIER);
        if (yield <= 0) return null;
        GTRecipe recipe = recipeType.recipeBuilder(new ResourceLocation("gtcsolo", "void_absorber/vein"))
                .duration(FluidDrillLogic.MAX_PROGRESS).EUt(GTValues.VA[DRILL_TIER])
                .outputFluids(new FluidStack(fluid, (int) Math.min(Integer.MAX_VALUE, yield)))
                .buildRawRecipe();
        recipe.data.putBoolean(VEIN_RECIPE, true);
        return recipe;
    }

    private void depleteCompletedVein(int batches) {
        if (!(getLevel() instanceof ServerLevel level)) return;
        int chance = FluidDrillMachine.getDepletionChance(DRILL_TIER);
        BlockPos column = drillingColumn();
        int chunkX = SectionPos.blockToSectionCoord(column.getX());
        int chunkZ = SectionPos.blockToSectionCoord(column.getZ());
        var data = BedrockFluidVeinSavedData.getOrCreate(level);
        var entry = data.getFluidVeinWorldEntry(chunkX, chunkZ);
        if (entry == null || entry.getDefinition() == null || entry.getDefinition().getDepletionChance() == 0) return;
        for (int batch = 0; batch < batches && entry.getOperationsRemaining() > 0; batch++) {
            if (chance == 1 || GTValues.RNG.nextInt(chance) == 0) data.depleteVein(chunkX, chunkZ, 0, false);
        }
    }

    @Override
    public void addDisplayText(List<Component> lines) {
        super.addDisplayText(lines);
        if (!isFormed() || !(getLevel() instanceof ServerLevel)) return;
        if (getActiveRecipeType() == OCEAN_MODE && !hasOceanAbove()) {
            lines.add(Component.translatable("gtcsolo.void_absorber.no_ocean").withStyle(ChatFormatting.RED));
        } else if (getActiveRecipeType() == MA_MODE && getMaxVoltage() < GTValues.V[GTValues.UV]) {
            lines.add(Component.translatable("gtcsolo.void_absorber.ma_voltage").withStyle(ChatFormatting.RED));
        } else if (getActiveRecipeType() == VEIN_MODE && createVeinRecipe() == null) {
            lines.add(Component.translatable("gtceu.multiblock.fluid_rig.no_fluid_in_area").withStyle(ChatFormatting.RED));
        }
    }

    private static final class ExtractionLogic extends RecipeLogic {
        private final VoidAbsorberMachine absorber;

        private ExtractionLogic(VoidAbsorberMachine machine) {
            super(machine);
            absorber = machine;
        }

        @Override
        public Iterator<GTRecipe> searchRecipe() {
            if (absorber.getActiveRecipeType() != VEIN_MODE) {
                // GT's ingredient index cannot discover a recipe with no item/fluid inputs.
                // Read this mode's native RecipeManager list; RecipeLogic still performs
                // all condition, power, output-capacity and input-consumption checks.
                if (absorber.getLevel() instanceof ServerLevel level)
                    return level.getRecipeManager().getAllRecipesFor(absorber.getRecipeType()).iterator();
                return Collections.emptyIterator();
            }
            GTRecipe recipe = absorber.createVeinRecipe();
            return recipe == null ? Collections.emptyIterator() : List.of(recipe).iterator();
        }

        @Override
        protected boolean handleRecipeIO(GTRecipe recipe, IO io) {
            boolean handled = super.handleRecipeIO(recipe, io);
            if (handled && io == IO.OUT && recipe.data.getBoolean(VEIN_RECIPE))
                absorber.depleteCompletedVein(Math.max(1, recipe.data.getInt(VEIN_BATCHES)));
            return handled;
        }
    }
}
