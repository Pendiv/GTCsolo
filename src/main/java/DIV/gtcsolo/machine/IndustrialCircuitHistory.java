package DIV.gtcsolo.machine;

import java.util.ArrayList;
import java.util.List;

/** Two recipe records ordered by the most recent completed run. */
public final class IndustrialCircuitHistory {
    public record Entry(String recipe, long count, int duration, long idleTicks) {
        public Entry {
            if (recipe == null || recipe.isBlank() || count < 0 || duration < 1 || idleTicks < 0 || idleTicks >= duration)
                throw new IllegalArgumentException("Invalid circuit-line history entry");
        }
    }

    private Entry older, newer;

    /** Incoming analysed data occupies a record even before any manufacturing run. */
    public void reserve(String recipe, int duration) {
        if (newer != null && newer.recipe().equals(recipe)) return;
        if (older != null && older.recipe().equals(recipe)) {
            Entry move = older; older = newer; newer = move; return;
        }
        older = newer;
        newer = new Entry(recipe, 0, Math.max(1, duration), 0);
    }

    public void forget(java.util.Set<String> recipes) {
        if (older != null && recipes.contains(older.recipe())) older = null;
        if (newer != null && recipes.contains(newer.recipe())) { newer = older; older = null; }
    }

    public List<Entry> entries() {
        List<Entry> entries = new ArrayList<>(2);
        if (older != null) entries.add(older);
        if (newer != null) entries.add(newer);
        return entries;
    }

    public void restore(List<Entry> entries) {
        if (entries.size() > 2 || entries.size() == 2 && entries.get(0).recipe().equals(entries.get(1).recipe()))
            throw new IllegalArgumentException("History holds at most two distinct recipes");
        older = entries.size() == 2 ? entries.get(0) : null;
        newer = entries.isEmpty() ? null : entries.get(entries.size() - 1);
    }

    /** Returns records retired by displacement or by reaching zero. One batch is one completion. */
    public List<String> complete(String recipe, int duration) {
        List<String> retired = new ArrayList<>();
        long count = 0;
        if (newer != null && newer.recipe().equals(recipe)) {
            count = newer.count();
        } else if (older != null && older.recipe().equals(recipe)) {
            count = older.count();
            older = newer;
        } else {
            if (older != null) retired.add(older.recipe());
            older = newer;
        }
        newer = new Entry(recipe, count == Long.MAX_VALUE ? count : count + 1, Math.max(1, duration), 0);
        if (older != null && older.count() > 0) {
            older = new Entry(older.recipe(), older.count() - 1, older.duration(), older.idleTicks());
            if (older.count() == 0) retired.add(older.recipe());
        }
        return retired;
    }

    /** Idle tick fractions accumulate independently using each record's actual processing duration. */
    public List<String> elapseIdle(long ticks) {
        if (ticks < 0) throw new IllegalArgumentException("Negative idle time");
        List<String> retired = new ArrayList<>();
        older = decay(older, ticks, retired);
        newer = decay(newer, ticks, retired);
        return retired;
    }

    private static Entry decay(Entry entry, long ticks, List<String> retired) {
        if (entry == null || entry.count() == 0 || ticks == 0) return entry;
        long whole = ticks / entry.duration();
        long remainder = ticks % entry.duration() + entry.idleTicks();
        whole = whole == Long.MAX_VALUE ? whole : whole + remainder / entry.duration();
        remainder %= entry.duration();
        long count = whole >= entry.count() ? 0 : entry.count() - whole;
        if (count == 0) retired.add(entry.recipe());
        return new Entry(entry.recipe(), count, entry.duration(), count == 0 ? 0 : remainder);
    }

    public long maximumCount() {
        return Math.max(older == null ? 0 : older.count(), newer == null ? 0 : newer.count());
    }

    public boolean containsPositive(String recipe) {
        return older != null && older.recipe().equals(recipe) && older.count() > 0
                || newer != null && newer.recipe().equals(recipe) && newer.count() > 0;
    }

    public int discountPercent() {
        return discountPercent(maximumCount());
    }

    public static int discountPercent(long count) {
        return count <= 5 ? 0 : count <= 15 ? 10 : count <= 30 ? 20 : 25;
    }

    public int processingDuration(int unmodifiedDuration) {
        return processingDuration(unmodifiedDuration, maximumCount());
    }

    public static int processingDuration(int unmodifiedDuration, long count) {
        long duration = Math.max(1, unmodifiedDuration);
        return (int) Math.max(1, (duration * (100 - discountPercent(count)) + 99) / 100);
    }
}
