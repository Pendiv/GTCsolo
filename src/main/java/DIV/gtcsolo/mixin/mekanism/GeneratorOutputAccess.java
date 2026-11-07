package DIV.gtcsolo.mixin.mekanism;

import mekanism.api.math.FloatingLong;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "mekanism.generators.common.tile.TileEntityGenerator", remap = false)
public interface GeneratorOutputAccess {
    @Invoker(value = "updateMaxOutputRaw", remap = false)
    void gtcsolo$updateMaxOutput(FloatingLong output);
}
