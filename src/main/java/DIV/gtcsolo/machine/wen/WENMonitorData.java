package DIV.gtcsolo.machine.wen;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.List;
import java.util.Objects;

/** Immutable, bounded monitor view. Energy values retain the network's full precision. */
public record WENMonitorData(
        String networkId,
        BigInteger stored,
        BigInteger capacity,
        boolean formed,
        long inputPerSecond,
        long outputPerSecond,
        List<BigInteger> history,
        List<NetworkInfo> networks,
        int networkOffset,
        int networkCount,
        boolean operator,
        boolean crossDimension,
        int storageLevel,
        int nextUpgradeCost,
        int page) {
    public static final int NETWORK_ROWS = 12;
    public static final int PAGE_COUNT = 5;
    public static final WENMonitorData EMPTY =
            new WENMonitorData(
                    "",
                    BigInteger.ZERO,
                    BigInteger.ZERO,
                    false,
                    0,
                    0,
                    List.of(),
                    List.of(),
                    0,
                    0,
                    false,
                    false,
                    0,
                    2,
                    0);

    public WENMonitorData {
        Objects.requireNonNull(networkId);
        nonnegative(stored);
        nonnegative(capacity);
        history = List.copyOf(history);
        networks = List.copyOf(networks);
        history.forEach(WENMonitorData::nonnegative);
        if (history.size() > WENNetworkData.HISTORY_SIZE
                || networks.size() > NETWORK_ROWS
                || networkOffset < 0
                || networkCount < networkOffset
                || networks.size() > networkCount - networkOffset
                || storageLevel < 0
                || storageLevel > 36
                || nextUpgradeCost < 0
                || page < 0
                || page >= PAGE_COUNT
                || inputPerSecond < 0
                || outputPerSecond < 0) throw new IllegalArgumentException("Invalid monitor view");
    }

    public record NetworkInfo(
            String id, boolean formed, BigInteger stored, BigInteger capacity, String dimension) {
        public NetworkInfo {
            Objects.requireNonNull(id);
            Objects.requireNonNull(dimension);
            nonnegative(stored);
            nonnegative(capacity);
        }
    }

    private static void nonnegative(BigInteger value) {
        if (Objects.requireNonNull(value).signum() < 0)
            throw new IllegalArgumentException("Negative network energy");
    }

    public static double fraction(BigInteger value, BigInteger limit) {
        if (limit.signum() <= 0 || value.signum() <= 0) return 0;
        if (value.compareTo(limit) >= 0) return 1;
        return new BigDecimal(value)
                .divide(new BigDecimal(limit), MathContext.DECIMAL64)
                .doubleValue();
    }
}
