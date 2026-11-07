package DIV.gtcsolo.util;

/** Arithmetic for GT's int-sized, non-negative recipe inventory totals. */
public final class RecipeAmountMath {
    private RecipeAmountMath() {}

    /** Adds non-negative quantities, retaining the largest representable total. */
    public static int add(int left, int right) {
        return (int) Math.min(Integer.MAX_VALUE, (long) left + right);
    }

    /**
     * Repairs an int addition immediately after two non-negative operands were added.
     * Such an addition can wrap only into the negative half of the int range.
     * This must not be used for arbitrary negative values or after multiple additions.
     */
    public static int saturateWrappedSum(int sum) {
        return sum < 0 ? Integer.MAX_VALUE : sum;
    }
}
