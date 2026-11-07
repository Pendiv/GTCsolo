package DIV.gtcsolo.machine;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.List;

/** Non-consuming environmental fluids, independent of electrical overclocking. */
public final class StoneGeneratorRules {
    public static final int FLUID_PER_PAIR = 1000;
    public static final int MAX_PARALLELS = 8;
    public static final String PARALLELS = "gtcsolo_stone_generator_pairs";

    private StoneGeneratorRules() {}

    public record Environment(Fluid first, Fluid second) {
        public int parallels(List<? extends IFluidHandler> tanks) {
            long firstAmount = 0, secondAmount = 0;
            for (var handler : tanks) for (int slot = 0; slot < handler.getTanks(); slot++) {
                var stack = handler.getFluidInTank(slot);
                if (stack.isEmpty()) continue;
                if (first.isSame(stack.getFluid())) firstAmount += stack.getAmount();
                if (second.isSame(stack.getFluid())) secondAmount += stack.getAmount();
            }
            long pairs = first.isSame(second) ? firstAmount / (2L * FLUID_PER_PAIR)
                    : Math.min(firstAmount, secondAmount) / FLUID_PER_PAIR;
            return (int) Math.min(MAX_PARALLELS, pairs);
        }
    }

    public static Environment environment(GTRecipe recipe) {
        return environment(recipe.data);
    }

    public static Environment environment(CompoundTag data) {
        Fluid first = fluid(data.getString("fluidA")), second = fluid(data.getString("fluidB"));
        return first == Fluids.EMPTY || second == Fluids.EMPTY ? null : new Environment(first, second);
    }

    public static String environmentDescription(CompoundTag data) {
        var environment = environment(data);
        return environment == null ? "" : Component.translatable("gtcsolo.rock_breaker.environment_fluids",
                new net.minecraftforge.fluids.FluidStack(environment.first(), 1000).getDisplayName(),
                new net.minecraftforge.fluids.FluidStack(environment.second(), 1000).getDisplayName()).getString();
    }

    private static Fluid fluid(String value) {
        var id = ResourceLocation.tryParse(value);
        return id == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.get(id);
    }

    /** Native ingredient amounts are ints; reject an unrepresentable batch before copying it. */
    public static boolean canScale(GTRecipe recipe, double multiplier) {
        if (!Double.isFinite(multiplier) || multiplier < 1) return false;
        if ((double) Math.max(1, recipe.parallels) * multiplier > Integer.MAX_VALUE) return false;
        for (var map : List.of(recipe.inputs, recipe.outputs, recipe.tickInputs, recipe.tickOutputs)) {
            for (var cap : List.of(ItemRecipeCapability.CAP, FluidRecipeCapability.CAP)) {
                for (var content : map.getOrDefault(cap, List.of())) {
                    if (content.chance == 0) continue;
                    long amount;
                    if (cap == FluidRecipeCapability.CAP) amount = FluidRecipeCapability.CAP.of(content.content).getAmount();
                    else {
                        var ingredient = ItemRecipeCapability.CAP.of(content.content);
                        amount = ingredient instanceof SizedIngredient sized ? sized.getAmount()
                                : ingredient instanceof IntProviderIngredient ranged ? ranged.getCountProvider().getMaxValue() : 1;
                    }
                    long rolls = content.chance < content.maxChance ? Math.max(1, recipe.parallels) : 1;
                    if (amount < 0 || (double) amount * rolls * multiplier > Integer.MAX_VALUE) return false;
                }
            }
        }
        return true;
    }
}
