package DIV.gtcsolo.integration.jei.performance;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Builds on explicit use, then installs atomically from the caller's point of view. */
public final class DeferredPreviewValue<T> {
    private Supplier<T> supplier;
    private T value;
    private boolean loading;
    private boolean installed;

    public DeferredPreviewValue(Supplier<T> supplier) { this.supplier = Objects.requireNonNull(supplier); }

    public boolean isInstalled() { return installed; }

    public T install(Consumer<T> installer) {
        if (installed) return value;
        if (loading) throw new IllegalStateException("Recursive deferred multiblock preview initialization");
        loading = true;
        try {
            if (value == null) {
                value = Objects.requireNonNull(supplier.get(), "Native multiblock preview");
                supplier = null;
            }
            installer.accept(value);
            installed = true;
            return value;
        } finally {
            loading = false;
        }
    }
}
