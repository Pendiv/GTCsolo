package DIV.gtcsolo.integration.jei.performance;

import DIV.gtcsolo.Gtcsolo;
import com.lowdragmc.lowdraglib.gui.editor.annotation.ConfigSetter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Avoids copying every public Method for each RTUI widget during the synchronous GT recipe index.
 * Only immutable lookup descriptors survive a call; caller-visible Method objects remain fresh.
 */
public final class JeiSetterLookup {
    private static final Method[] NO_METHODS = new Method[0];
    private static final ThreadLocal<Session> ACTIVE = new ThreadLocal<>();

    private static Lookup describe(Class<?> type, Session session) {
        Method[] methods = type.getMethods();
        session.metadataCopies += methods.length;
        List<Descriptor> descriptors = new ArrayList<>();
        Set<String> fields = new HashSet<>();
        for (Method method : methods) {
            if (!method.isAnnotationPresent(ConfigSetter.class)) continue;
            // Native first-winner selection is ambiguous for duplicate field keys or bridges.
            String field = method.getAnnotation(ConfigSetter.class).field();
            if (method.isBridge() || method.isSynthetic() || !fields.add(field))
                return new Lookup(methods.length, null);
            try {
                Method fresh = type.getMethod(method.getName(), method.getParameterTypes());
                session.metadataCopies++;
                Descriptor descriptor = new Descriptor(method.getName(), method.getParameterTypes(),
                        method.getDeclaringClass(), method.getReturnType(), method.getModifiers());
                if (!descriptor.matches(fresh)) return new Lookup(methods.length, null);
                descriptors.add(descriptor);
            } catch (NoSuchMethodException | SecurityException unavailable) {
                return new Lookup(methods.length, null);
            }
        }
        return new Lookup(methods.length, List.copyOf(descriptors));
    }

    private JeiSetterLookup() {}

    /** No work is deferred: the original registration runs on the same thread inside this scope. */
    public static void runScoped(Runnable action) {
        if (!JeiOptimizationOptions.setterLookupEnabled()) {
            action.run();
            return;
        }
        Session previous = ACTIVE.get();
        Session session = new Session();
        ACTIVE.set(session);
        try {
            action.run();
        } finally {
            if (previous == null) ACTIVE.remove();
            else ACTIVE.set(previous);
            Gtcsolo.LOGGER.info("[JEI-setters] calls={} optimized={} native={} metadataClasses={} avoidedMethodCopies={}",
                    session.calls, session.optimized, session.nativeCalls, session.lookups.size(),
                    session.avoidedCopies - session.metadataCopies);
        }
    }

    public static Method[] publicMethods(Class<?> type, Map<String, Method> setters) {
        Session session = ACTIVE.get();
        if (session == null) return type.getMethods();
        session.calls++;
        // LDLib's widget entry point and its recursive sub-configurable path use a plain HashMap.
        // A caller-supplied map implementation could observe insertion order or extra side effects.
        if (setters == null || setters.getClass() != HashMap.class) return nativeMethods(type, session);
        Lookup lookup = session.lookups.get(type);
        if (lookup == null) {
            lookup = describe(type, session);
            session.lookups.put(type, lookup);
        }
        if (lookup.descriptors == null) return nativeMethods(type, session);
        Method[] selected = lookup.descriptors.isEmpty() ? NO_METHODS : new Method[lookup.descriptors.size()];
        try {
            for (int i = 0; i < selected.length; i++) {
                Descriptor descriptor = lookup.descriptors.get(i);
                Method method = type.getMethod(descriptor.name, descriptor.parameters);
                if (!descriptor.matches(method)) return nativeMethods(type, session);
                selected[i] = method;
            }
        } catch (NoSuchMethodException | SecurityException unavailable) {
            return nativeMethods(type, session);
        }
        session.optimized++;
        session.avoidedCopies += lookup.publicMethodCount - selected.length;
        return selected;
    }

    private static Method[] nativeMethods(Class<?> type, Session session) {
        session.nativeCalls++;
        return type.getMethods();
    }

    private record Lookup(int publicMethodCount, List<Descriptor> descriptors) {}

    private record Descriptor(String name, Class<?>[] parameters, Class<?> declaringClass,
                              Class<?> returnType, int modifiers) {
        boolean matches(Method method) {
            return method.getDeclaringClass() == declaringClass && method.getReturnType() == returnType
                    && method.getModifiers() == modifiers && !method.isBridge() && !method.isSynthetic();
        }
    }

    private static final class Session {
        final Map<Class<?>, Lookup> lookups = new HashMap<>();
        long calls, optimized, nativeCalls, avoidedCopies, metadataCopies;
    }
}
