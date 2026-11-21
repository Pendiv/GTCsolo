package DIV.gtcsolo.mixin.mekanism;

import DIV.gtcsolo.integration.mekanism.CleanroomContaminant;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.CleanroomMachine;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Mixin(value = CleanroomMachine.class, remap = false)
public abstract class CleanroomGeneratorMixin {
    @Unique private static final TagKey<Block> gtcsolo$contaminants = TagKey.create(Registries.BLOCK,
            new ResourceLocation("gtcsolo", "cleanroom_contaminants"));

    @Inject(method = "innerPredicate", at = @At("RETURN"))
    private void gtcsolo$excludeGenerators(CallbackInfoReturnable<TraceabilityPredicate> result) {
        // Keep GT's original interior predicate, including its receiver discovery and block-change tracking.
        Predicate<MultiblockState> allowed = state -> !state.getBlockState().is(gtcsolo$contaminants)
                && !(state.getTileEntity() instanceof CleanroomContaminant);
        for (var candidate : result.getReturnValue().common) {
            candidate.predicate = allowed.and(candidate.predicate);
        }
    }
}
