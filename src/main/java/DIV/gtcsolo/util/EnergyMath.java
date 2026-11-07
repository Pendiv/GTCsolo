package DIV.gtcsolo.util;

/** Integer unit conversions that bound the value before multiplication. */
public final class EnergyMath {
    private EnergyMath() {}

    public static long clampEuChange(long storedFe, long capacityFe, long requestedEu, int fePerEu) {
        if (requestedEu >= 0) return Math.min(requestedEu, (capacityFe - storedFe) / fePerEu);
        // Compare signed amounts rather than negating Long.MIN_VALUE.
        return Math.max(requestedEu, -(storedFe / fePerEu));
    }
    public record OutputSettlement(long eu, double prepaid) {}
    public record InputSettlement(long eu, double pending) {}

    /** Debit whole EU once, carrying already-paid external units into the next transfer. */
    public static OutputSettlement settleOutput(double accepted, double prepaid, double unitsPerEu) {
        long eu = (long) (accepted / unitsPerEu);
        // Keep the small credit separate: subtracting it from a huge double can round it away.
        double remainder = prepaid - accepted % unitsPerEu;
        if (remainder < 0) {
            eu++;
            remainder += unitsPerEu;
        } else if (remainder >= unitsPerEu && eu > 0) {
            eu--;
            remainder -= unitsPerEu;
        }
        return new OutputSettlement(eu, Math.max(0, Math.min(unitsPerEu, remainder)));
    }

    /** Convert received external units without losing a small, previously received remainder. */
    public static InputSettlement settleInput(double received, double pending, double unitsPerEu) {
        long eu = (long) (received / unitsPerEu);
        double remainder = received % unitsPerEu + pending;
        long carried = (long) (remainder / unitsPerEu);
        return new InputSettlement(eu + carried, remainder - carried * unitsPerEu);
    }

    /** Largest nearby double that can be paid from this integer balance plus prepaid units. */
    public static double payableOutput(long availableEu, double prepaid, double unitsPerEu) {
        double offered = boundedExternalAmount(availableEu * unitsPerEu + prepaid, unitsPerEu);
        if (settleOutput(offered, prepaid, unitsPerEu).eu() > availableEu) {
            offered = Math.nextDown(offered);
        }
        return Math.max(0, offered);
    }

    /** Preserve the existing transfer limit, rounding down only when capacity would be exceeded. */
    public static double receivableInput(long spaceEu, double pending, double unitsPerEu, double transferLimit) {
        double wanted = boundedExternalAmount(Math.min(spaceEu * unitsPerEu - pending, transferLimit), unitsPerEu);
        InputSettlement result = settleInput(wanted, pending, unitsPerEu);
        if (result.eu() > spaceEu || (result.eu() == spaceEu && result.pending() > 0)) {
            wanted = Math.nextDown(wanted);
        }
        return Math.max(0, wanted);
    }

    private static double boundedExternalAmount(double amount, double unitsPerEu) {
        // A double can round Long.MAX_VALUE up to 2^63; do not let the subsequent long cast saturate.
        return Math.max(0, Math.min(amount, Math.nextDown(0x1p63 * unitsPerEu)));
    }
}

