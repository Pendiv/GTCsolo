package DIV.gtcsolo.util;

public final class RecipeMath {
    private RecipeMath() {}

    public static int saturatedParallelCount(double value) {
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, Math.round(value)));
    }
}
