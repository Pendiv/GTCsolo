package DIV.gtcsolo.render.material.portal;

/** Scoped only around non-world preview draws; chunk renderer threads see the default false. */
public final class GalacticaRenderContext {
    private static final ThreadLocal<Integer> PREVIEW = ThreadLocal.withInitial(() -> 0);
    private GalacticaRenderContext() {}
    public static boolean isPreview() { return PREVIEW.get() > 0; }
    public static void enterPreview() { PREVIEW.set(PREVIEW.get() + 1); }
    public static void leavePreview() {
        int n = PREVIEW.get() - 1;
        if (n <= 0) PREVIEW.remove(); else PREVIEW.set(n);
    }
}
