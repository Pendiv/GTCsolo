package DIV.gtcsolo.mixin;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** {@link RangedAttribute} の final な maxValue/minValue を書き換え可能にする (上限拡張用)。 */
@Mixin(RangedAttribute.class)
public interface RangedAttributeAccessor {

    @Mutable
    @Accessor("maxValue")
    void gtcsolo$setMaxValue(double value);

    @Mutable
    @Accessor("minValue")
    void gtcsolo$setMinValue(double value);
}
