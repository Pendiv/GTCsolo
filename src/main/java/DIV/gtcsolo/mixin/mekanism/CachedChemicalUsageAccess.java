package DIV.gtcsolo.mixin.mekanism;

import mekanism.api.recipes.cache.ItemStackConstantChemicalToItemStackCachedRecipe.ChemicalUsageMultiplier;
import mekanism.api.recipes.cache.ItemStackConstantChemicalToItemStackCachedRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ItemStackConstantChemicalToItemStackCachedRecipe.class, remap = false)
public interface CachedChemicalUsageAccess {
    @Accessor("chemicalUsage")
    ChemicalUsageMultiplier gtcsolo$getChemicalUsage();

    @Mutable @Accessor("chemicalUsage")
    void gtcsolo$setChemicalUsage(ChemicalUsageMultiplier multiplier);
}
