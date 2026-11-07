package DIV.gtcsolo.mixin;

import DIV.gtcsolo.machine.CircuitResearchStorage;
import com.gregtechceu.gtceu.utils.ResearchManager;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Preserve the native scanner copy recipe but do not copy manufacturing experience. */
@Mixin(value = ResearchManager.DataStickCopyScannerLogic.class, remap = false)
public abstract class CircuitResearchCopyMixin {
    @ModifyArg(method = "createDataRecipe", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;setTag(Lnet/minecraft/nbt/CompoundTag;)V", remap = true),
            index = 0, require = 1)
    private CompoundTag gtcsolo$copyResearchOnly(CompoundTag copied) {
        return CircuitResearchStorage.clearCopiedExperience(copied);
    }
}
