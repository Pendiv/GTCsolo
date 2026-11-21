package DIV.gtcsolo.mixin.mekanism;

import com.gregtechceu.gtceu.common.blockentity.CableBlockEntity;
import com.gregtechceu.gtceu.common.pipelike.cable.AveragingPerTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = CableBlockEntity.class, remap = false)
public interface CableLoadAccess {
    @Accessor("averageAmperageCounter")
    AveragingPerTickCounter gtcsolo$getAmperageCounter();
}
