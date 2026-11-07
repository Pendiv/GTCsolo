package DIV.gtcsolo.mixin.mekanism;

import mekanism.api.chemical.BasicChemicalTank;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Predicate;

/** Only the original generator's fuel tank receives an additional predicate. */
@Mixin(value = BasicChemicalTank.class, remap = false)
public interface ChemicalTankValidatorAccess {
    @Accessor("validator") Predicate<?> gtcsolo$getValidator();
    @Mutable @Accessor("validator") void gtcsolo$setValidator(Predicate<?> validator);
}
