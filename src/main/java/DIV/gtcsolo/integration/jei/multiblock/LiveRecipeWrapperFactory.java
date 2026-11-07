package DIV.gtcsolo.integration.jei.multiblock;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.function.Function;

/** Reuse a wrapper while a JEI layout still owns its listeners, without retaining closed layouts. */
public final class LiveRecipeWrapperFactory<K, V> implements Function<K, V> {
    private final LoadingCache<K, V> live;

    public LiveRecipeWrapperFactory(Function<K, V> factory) {
        live = CacheBuilder.newBuilder().weakValues().build(new CacheLoader<>() {
            @Override public V load(K key) { return factory.apply(key); }
        });
    }

    @Override public V apply(K key) { return live.getUnchecked(key); }
}
