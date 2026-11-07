package DIV.gtcsolo.integration.kubejs;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Function;
import dev.latvian.mods.rhino.ScriptRuntime;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.ScriptableObject;

/** Runtime targets missing from Rhino build.18's generated JavaAdapter callbacks. */
public final class RhinoAdapterCallbacks {
    private RhinoAdapterCallbacks() {}

    public static Function getFunction(Scriptable delegate, String name, Context context) {
        Object value = ScriptableObject.getProperty(delegate, name, context);
        if (value == Scriptable.NOT_FOUND) return null;
        if (value instanceof Function function) return function;
        throw ScriptRuntime.notFunctionError(context, value, name);
    }

    public static Object callMethod(Context context, Scriptable self, Function function,
                                    Object[] arguments, long wrapMask) {
        if (function == null) return null;
        Scriptable scope = function.getParentScope();
        if (scope == null) scope = ScriptableObject.getTopLevelScope(self);
        for (int i = 0; i < arguments.length; i++) {
            if ((wrapMask & (1L << i)) != 0 && !(arguments[i] instanceof Scriptable)) {
                arguments[i] = context.getWrapFactory().wrap(context, scope, arguments[i], null);
            }
        }
        // Use the Context captured by the adapter, including on non-script threads.
        return context.callSync(function, scope, self, arguments);
    }
}
