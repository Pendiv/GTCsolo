package DIV.gtcsolo.mixin;

import com.gregtechceu.gtceu.api.gui.widget.PatternPreviewWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

/** Invalid preview patterns have no predicate map; clicking one must not close the game. */
@Mixin(value = PatternPreviewWidget.class, remap = false)
public abstract class PatternPreviewSafetyMixin {
    @Redirect(method = "onPosSelected", at = @At(value = "INVOKE",
            target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"), require = 1)
    private Object gtcsolo$selectAvailablePredicate(Map<?, ?> predicates, Object position) {
        // Preserve the native formation warning and valid-part selection behavior.
        return predicates == null ? null : predicates.get(position);
    }
}
