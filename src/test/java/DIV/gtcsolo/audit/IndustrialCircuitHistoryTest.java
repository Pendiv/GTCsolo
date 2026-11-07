package DIV.gtcsolo.audit;

import DIV.gtcsolo.machine.IndustrialCircuitHistory;
import java.util.List;

/** Independent, hand-calculated examples from the requested two-record behavior. */
public final class IndustrialCircuitHistoryTest {
    private static int checks;
    private static void check(boolean condition) { checks++; if (!condition) throw new AssertionError("check " + checks); }
    private static long count(IndustrialCircuitHistory h, String name) {
        return h.entries().stream().filter(e -> e.recipe().equals(name)).mapToLong(IndustrialCircuitHistory.Entry::count).findFirst().orElse(-1);
    }
    public static void main(String[] args) {
        IndustrialCircuitHistory h = new IndustrialCircuitHistory();
        for (int i = 0; i < 10; i++) h.complete("UEV", 100);
        for (int i = 0; i < 9; i++) h.complete("UIV", 50);
        check(count(h, "UEV") == 1); check(count(h, "UIV") == 9); check(h.maximumCount() == 9);
        h.complete("UIV", 50); check(count(h, "UEV") == 0); check(count(h, "UIV") == 10);
        h.complete("UXV", 200); check(count(h, "UEV") == -1); check(count(h, "UIV") == 9); check(count(h, "UXV") == 1);
        h.complete("UIV", 50); check(h.entries().get(1).recipe().equals("UIV")); check(count(h, "UIV") == 10); check(count(h, "UXV") == 0);

        h.restore(List.of(new IndustrialCircuitHistory.Entry("A", 6, 100, 0), new IndustrialCircuitHistory.Entry("B", 4, 50, 0)));
        h.elapseIdle(125); check(count(h, "A") == 5); check(count(h, "B") == 2);
        h.elapseIdle(25); check(count(h, "A") == 5); check(count(h, "B") == 1);
        var retired = h.elapseIdle(450); check(count(h, "A") == 0); check(count(h, "B") == 0);
        check(retired.contains("A") && retired.contains("B"));
        for (int n : new int[]{1, 5, 6, 15, 16, 30, 31, 1000}) {
            h.restore(List.of(new IndustrialCircuitHistory.Entry("R", n, 100, 0)));
            int expected = n <= 5 ? 0 : n <= 15 ? 10 : n <= 30 ? 20 : 25;
            check(h.discountPercent() == expected); check(h.processingDuration(100) == 100 - expected);
            check(h.processingDuration(1) == 1);
        }
        h.restore(List.of(new IndustrialCircuitHistory.Entry("Huge", Long.MAX_VALUE, Integer.MAX_VALUE, 0)));
        h.complete("Huge", Integer.MAX_VALUE); check(h.maximumCount() == Long.MAX_VALUE);
        h.elapseIdle(Long.MAX_VALUE); check(h.maximumCount() == Long.MAX_VALUE - Long.MAX_VALUE / Integer.MAX_VALUE);
        h.restore(List.of(new IndustrialCircuitHistory.Entry("One", 1, 1, 0)));
        check(h.elapseIdle(Long.MAX_VALUE).equals(List.of("One"))); check(h.maximumCount() == 0);
        var snapshot = h.entries(); IndustrialCircuitHistory restored = new IndustrialCircuitHistory(); restored.restore(snapshot);
        check(restored.entries().equals(snapshot));
        h.restore(List.of());h.reserve("A",100);h.reserve("B",200);h.reserve("C",300);
        check(h.entries().size()==2);check(count(h,"A")==-1);check(count(h,"B")==0);check(count(h,"C")==0);
        h.complete("B",180);check(h.entries().get(1).recipe().equals("B"));check(count(h,"B")==1);
        h.forget(java.util.Set.of("B"));check(h.entries().size()==1&&count(h,"C")==0);
        System.out.println("IndustrialCircuitHistory: " + checks + " checks passed");
    }
}
