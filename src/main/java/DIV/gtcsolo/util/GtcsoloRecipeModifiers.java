package DIV.gtcsolo.util;

import DIV.gtcsolo.machine.UpgradeHatchMachine;
import DIV.gtcsolo.registry.ModItems;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TieredMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;

import javax.annotation.Nonnull;

public final class GtcsoloRecipeModifiers {

    private GtcsoloRecipeModifiers() {}

    public static final double INDUSTRIAL_DURATION_FACTOR = 0.9;
    public static final double INDUSTRIAL_EUT_FACTOR = 4.0;
    public static final double INDUSTRIAL_IO_FACTOR = 2.0;

    public static ModifierFunction industrialOverclock(@Nonnull MetaMachine machine, @Nonnull GTRecipe recipe) {
        int machineTier = resolveTier(machine);
        if (machineTier < 0) return ModifierFunction.NULL;

        int recipeTier = RecipeHelper.getRecipeEUtTier(recipe);
        if (recipeTier > machineTier) {
            return ModifierFunction.NULL;
        }
        int n = machineTier - recipeTier;
        if (n == 0) return ModifierFunction.IDENTITY;

        double durationMult = Math.pow(INDUSTRIAL_DURATION_FACTOR, n);
        double eutMult = Math.pow(INDUSTRIAL_EUT_FACTOR, n);
        double ioMult = Math.pow(INDUSTRIAL_IO_FACTOR, n);
        int ioMultInt = (int) Math.round(ioMult);

        return ModifierFunction.builder()
                .modifyAllContents(ContentModifier.multiplier(ioMult))
                .eutMultiplier(eutMult)
                .durationMultiplier(durationMult)
                .addOCs(n)
                .parallels(ioMultInt)
                .build();
    }

    private static final double TIME_UPGRADE_DURATION_FACTOR = 0.8;
    private static final double PARALLEL_UPGRADE_FACTOR = 1.25;

    public static ModifierFunction industrialOverclockWithUpgradeHatch(
            @Nonnull MetaMachine machine, @Nonnull GTRecipe recipe) {
        ModifierFunction industrial = industrialOverclock(machine, recipe);
        if (industrial == ModifierFunction.NULL) return ModifierFunction.NULL;

        int timeUpgrades = countTimeUpgrades(machine);
        if (timeUpgrades == 0) return industrial;

        double mult = Math.pow(TIME_UPGRADE_DURATION_FACTOR, timeUpgrades);
        ModifierFunction upgradeFn = ModifierFunction.builder()
                .durationMultiplier(mult)
                .build();

        return industrial.andThen(upgradeFn);
    }

    /**
     * Conversion 機械専用: 工業OC + パラレルアップグレード乗算 (1.25^N)。
     * industrialOverclock の parallel に PARALLEL_UPGRADE 個数分の累乗倍率をかける。
     */
    public static ModifierFunction industrialOverclockWithParallelUpgrade(
            @Nonnull MetaMachine machine, @Nonnull GTRecipe recipe) {
        int machineTier = resolveTier(machine);
        if (machineTier < 0) return ModifierFunction.NULL;

        int recipeTier = RecipeHelper.getRecipeEUtTier(recipe);
        if (recipeTier > machineTier) return ModifierFunction.NULL;

        int n = machineTier - recipeTier;
        int parallelUpgrades = countParallelUpgrades(machine);

        if (n == 0 && parallelUpgrades == 0) return ModifierFunction.IDENTITY;

        double durationMult = Math.pow(INDUSTRIAL_DURATION_FACTOR, n);
        double eutMult = Math.pow(INDUSTRIAL_EUT_FACTOR, n);
        double ioMult = Math.pow(INDUSTRIAL_IO_FACTOR, n);
        double upgradeMult = Math.pow(PARALLEL_UPGRADE_FACTOR, parallelUpgrades);
        int parallels = Math.max(1, (int) Math.round(ioMult * upgradeMult));

        return ModifierFunction.builder()
                .modifyAllContents(ContentModifier.multiplier(parallels))
                .eutMultiplier(eutMult)
                .durationMultiplier(durationMult)
                .addOCs(n)
                .parallels(parallels)
                .build();
    }

    private static int resolveTier(MetaMachine machine) {
        if (machine instanceof WorkableElectricMultiblockMachine wem) {
            if (machine instanceof IMultiController mc && !mc.isFormed()) return -1;
            return wem.getTier();
        }
        if (machine instanceof TieredMachine tm) {
            return tm.getTier();
        }
        return -1;
    }

    private static int countTimeUpgrades(MetaMachine machine) {
        return countUpgradeItem(machine, ModItems.TIME_UPGRADE.get());
    }

    private static int countParallelUpgrades(MetaMachine machine) {
        return countUpgradeItem(machine, ModItems.PARALLEL_UPGRADE.get());
    }

    private static int countUpgradeItem(MetaMachine machine, net.minecraft.world.item.Item targetItem) {
        if (!(machine instanceof IMultiController controller)) return 0;
        int total = 0;
        for (var part : controller.getParts()) {
            if (!(part instanceof UpgradeHatchMachine hatch)) continue;
            var inv = hatch.getInventory();
            for (int i = 0; i < inv.getSlots(); i++) {
                var stack = inv.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() == targetItem) {
                    total += stack.getCount();
                }
            }
        }
        return total;
    }
}