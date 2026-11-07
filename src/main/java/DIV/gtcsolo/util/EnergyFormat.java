package DIV.gtcsolo.util;

import java.math.BigInteger;

/** エネルギー値の表示フォーマット */
public final class EnergyFormat {

    private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);

    private EnergyFormat() {}

    /** long値を表示用にフォーマット。2^63-1以下ならカンマ区切り。 */
    public static String format(long value) {
        return String.format("%,d", value);
    }

    /** BigInteger文字列を表示用にフォーマット。 */
    public static String format(String bigIntStr) {
        if (bigIntStr == null || bigIntStr.isEmpty() || bigIntStr.equals("0")) return "0";
        try {
            BigInteger bi = new BigInteger(bigIntStr);
            return format(bi);
        } catch (NumberFormatException e) {
            return bigIntStr;
        }
    }

    /** BigIntegerを表示用にフォーマット。longの範囲内ならカンマ区切り、超えたら指数表記。 */
    public static String format(BigInteger value) {
        if (value == null) return "0";
        if (value.compareTo(LONG_MAX) <= 0 && value.signum() >= 0) {
            return String.format("%,d", value.longValue());
        }
        return toSciNotation(value);
    }

    /** 指数表記 (例: 1.234×10^20) */
    public static String toSciNotation(BigInteger value) {
        if (value.signum() == 0) return "0";
        String s = value.abs().toString();
        int exp = s.length() - 1;
        if (exp == 0) return (value.signum() < 0 ? "-" : "") + s;
        String mantissa = s.charAt(0) + "." + s.substring(1, Math.min(4, s.length()));
        return (value.signum() < 0 ? "-" : "") + mantissa + "\u00d710^" + exp;
    }

    /** 短い表記 (例: 92.2E EU, 1.5T EU) */
    public static String formatShort(BigInteger value) {
        if (value == null || value.signum() == 0) return "0";
        double d = value.doubleValue();
        if (d < 1_000) return String.format("%.0f", d);
        if (d < 1_000_000) return String.format("%.1fK", d / 1_000);
        if (d < 1_000_000_000) return String.format("%.1fM", d / 1_000_000);
        if (d < 1_000_000_000_000L) return String.format("%.1fG", d / 1_000_000_000);
        if (d < 1_000_000_000_000_000L) return String.format("%.1fT", d / 1_000_000_000_000L);
        if (d < 1e18) return String.format("%.1fP", d / 1e15);
        if (d < 1e21) return String.format("%.1fE", d / 1e18);
        return toSciNotation(value);
    }
}