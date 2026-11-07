package DIV.gtcsolo.integration.jei.multiblock;

import java.util.Map;
import java.util.IdentityHashMap;
import java.util.function.Consumer;

/** Owns each visible resource until a later frame releases it, independently of its GUI cache. */
public final class PreviewFrameLease<T> {
    // A weak key could disappear before expire(), skipping the resource's explicit close callback.
    private final Map<T, Long> entries = new IdentityHashMap<>();
    private long frame;
    public void beginFrame() { frame++; }
    public void touch(T value) { entries.put(value, frame); }
    public void expire(Consumer<T> release) {
        var iterator = entries.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue() != frame) {
                T value = entry.getKey(); iterator.remove(); release.accept(value);
            }
        }
    }
    public void clear(Consumer<T> release) {
        for (T value : entries.keySet()) release.accept(value);
        entries.clear();
    }
}
