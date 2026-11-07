package DIV.gtcsolo.torcherino;

import java.util.Random;

public final class BudgetTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        Budget.State baseline = new Budget.State(0, 0, 0, 1);
        check(Budget.valid(baseline, 0), "Baseline is free");
        check(Budget.valid(new Budget.State(0, 0, 1, 1), 1), "One radius increment fits one point");
        check(Budget.valid(new Budget.State(0, 0, 0, 2), 1), "One speed increment fits one point");
        check(!Budget.valid(new Budget.State(0, 0, 1, 2), 1), "Range and speed share their budget");
        check(Budget.valid(new Budget.State(1, 1, 0, 1), 2), "LV can grow two axes");
        check(!Budget.valid(new Budget.State(1, 1, 1, 1), 2), "LV cannot grow all three axes");
        check(!Budget.valid(new Budget.State(0, 0, 0, 0), 2), "Speed minimum is 100 percent");
        check(!Budget.valid(new Budget.State(-1, 0, 0, 1), 2), "Negative ranges rejected");
        check(Budget.valid(new Budget.State(4, 1, 4, 1), 11), "9x3x9 is accepted within the point budget");
        check(!Budget.valid(new Budget.State(5, 0, 0, 1), 1024), "Horizontal range stays at nine even with spare points");
        check(!Budget.valid(new Budget.State(0, 2, 0, 1), 1024), "Height stays at three even with spare points");
        check(!Budget.valid(new Budget.State(0, 0, 5, 1), 1024), "Both horizontal axes have the same cap");
        check(Budget.normalize(new Budget.State(20, 12, 20, 3), 43).equals(new Budget.State(4, 1, 4, 3)),
                "Old wide saves shrink to the cap without changing their speed");
        check(Budget.edit(baseline, new Budget.State(40, 0, 0, 1), 1, 43).x() == 4, "X slider clamps to nine");
        check(Budget.edit(baseline, new Budget.State(0, 40, 0, 1), 3, 43).y() == 1, "Y slider clamps to three");
        check(Budget.edit(baseline, new Budget.State(0, 0, 40, 1), 2, 43).z() == 4, "Z slider clamps to nine");
        check(Budget.edit(new Budget.State(4, 1, 4, 1), new Budget.State(4, 1, 4, 100), 0, 43).speed() == 35,
                "Range cap leaves remaining points available for acceleration");
        check(!Budget.valid(new Budget.State(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE), 43), "No integer overflow acceptance");
        Budget.State occupied = new Budget.State(1, 0, 0, 2);
        check(Budget.edit(occupied, new Budget.State(1, 0, 50, 2), 2, 2).equals(occupied), "Do not steal allocations from other sliders");
        Budget.State released = Budget.edit(occupied, new Budget.State(0, 0, 0, 2), 1, 2);
        check(Budget.edit(released, new Budget.State(0, 0, 0, 3), 0, 2).speed() == 3, "Released points can be reassigned");
        Random random = new Random(625);
        for (int power : new int[]{1, 2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 1024}) {
            Budget.State old = baseline;
            for (int i = 0; i < 10000; i++) {
                Budget.State requested = new Budget.State(random.nextInt(), random.nextInt(), random.nextInt(), random.nextInt());
                check(Budget.valid(Budget.normalize(requested, power), power), "Tampered state normalizes safely");
                old = Budget.edit(old, requested, i % 4, power);
                check(Budget.valid(old, power), "Sequential edits stay within budget");
            }
        }
        System.out.println("BudgetTest PASS: 9x3x9 cap, shared allocation, LV boundaries, overflow, tampered saves, 160000 edits");
    }
}
