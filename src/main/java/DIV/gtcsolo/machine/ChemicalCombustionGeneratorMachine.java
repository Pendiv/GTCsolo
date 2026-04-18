package DIV.gtcsolo.machine;

import DIV.gtcsolo.registry.ModMaterials;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraftforge.fluids.FluidStack;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.concurrent.ThreadLocalRandom;

public class ChemicalCombustionGeneratorMachine extends WorkableElectricMultiblockMachine {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            ChemicalCombustionGeneratorMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    private static final int SBF5_AMOUNT = 1000;
    private static final double BOOST_MULTIPLIER = 1.5;
    private static final double BREAK_CHANCE = 0.09;
    private static final Logger LOGGER = LogUtils.getLogger();

    private final GTRecipe sbf5Recipe = GTRecipeBuilder.ofRaw()
            .inputFluids(ModMaterials.ANTIMONY_PENTAFLUORIDE.getFluid(SBF5_AMOUNT))
            .buildRawRecipe();

    @Persisted @DescSynced
    private boolean isBoosted = false;

    private long baseOutputEUt = 0;
    private TickableSubscription boostTickSub;

    public ChemicalCombustionGeneratorMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() { return MANAGED_FIELD_HOLDER; }

    public boolean isBoosted() { return isBoosted; }

    @Override
    public void onLoad() {
        super.onLoad();
        boostTickSub = subscribeServerTick(this::onBoostTick);
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (boostTickSub != null) { unsubscribe(boostTickSub); boostTickSub = null; }
    }

    private void onBoostTick() {
        if (getLevel() == null || getLevel().isClientSide) return;
        if (!isFormed()) return;

        var recipeLogic = getRecipeLogic();
        boolean working = recipeLogic != null && recipeLogic.isWorking();

        if (getOffsetTimer() % 100 == 0) {
            LOGGER.info("[CCG] tick: formed={} working={} boosted={} baseEUt={}",
                    isFormed(), working, isBoosted, baseOutputEUt);
        }

        if (recipeLogic == null || !recipeLogic.isWorking()) {
            if (baseOutputEUt != 0) {
                baseOutputEUt = 0;
                isBoosted = false;
            }
            return;
        }

        if (baseOutputEUt == 0 && recipeLogic.getLastRecipe() != null) {
            baseOutputEUt = Math.abs(RecipeHelper.getOutputEUt(recipeLogic.getLastRecipe()));
        }

        long tick = getOffsetTimer();

        if (tick % 20 == 0) {
            if (isBoosted) {
                if (ThreadLocalRandom.current().nextDouble() < BREAK_CHANCE) {
                    isBoosted = false;
                }
            }

            if (!isBoosted) {
                if (tryConsumeSbF5()) {
                    isBoosted = true;
                    LOGGER.info("[CCG] SbF5 consumed, boost ACTIVE");
                } else {
                    LOGGER.debug("[CCG] SbF5 not found in fluid hatches");
                }
            }
        }

        if (isBoosted && baseOutputEUt > 0) {
            long bonus = (long) (baseOutputEUt * (BOOST_MULTIPLIER - 1.0));
            var energyContainers = getCapabilitiesProxy().get(
                    com.gregtechceu.gtceu.api.capability.recipe.IO.OUT,
                    com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability.CAP);
            if (energyContainers != null) {
                for (var handler : energyContainers) {
                    if (handler instanceof com.gregtechceu.gtceu.api.machine.trait.NotifiableEnergyContainer container) {
                        container.addEnergy(bonus);
                        break;
                    }
                }
            }
        }
    }

    private boolean tryConsumeSbF5() {
        if (sbf5Recipe.matchRecipe(this).isSuccess()) {
            return sbf5Recipe.handleRecipeIO(IO.IN, this,
                    this.recipeLogic.getChanceCaches());
        }
        return false;
    }
}