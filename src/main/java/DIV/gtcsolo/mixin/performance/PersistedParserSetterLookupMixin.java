package DIV.gtcsolo.mixin.performance;

import DIV.gtcsolo.integration.jei.performance.JeiSetterLookup;
import com.lowdragmc.lowdraglib.gui.editor.runtime.PersistedParser;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.Method;
import java.util.Map;

/** Bounded to the original setter-discovery call; outside GT JEI registration it stays native. */
@Mixin(value = PersistedParser.class, remap = false)
public abstract class PersistedParserSetterLookupMixin {
    @Redirect(method = "deserializeNBT", at = @At(value = "INVOKE",
            target = "Ljava/lang/Class;getMethods()[Ljava/lang/reflect/Method;"))
    private static Method[] gtcsolo$lookupSetters(Class<?> owner, CompoundTag tag,
                                                Map<String, Method> setters, Class<?> type, Object object) {
        return JeiSetterLookup.publicMethods(owner, setters);
    }
}
