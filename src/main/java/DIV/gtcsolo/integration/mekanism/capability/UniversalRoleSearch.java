package DIV.gtcsolo.integration.mekanism.capability;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/** Finds exclusive fluid/gas roles without depending on Minecraft or live tank state. */
public final class UniversalRoleSearch {
    private UniversalRoleSearch() {}

    /**
     * Returns a complete role assignment, or {@code null} when none fits.
     * Roles are 0 = undecided, 1 = fluid and 2 = gas. The predicate must treat an
     * undecided role as an optimistic upper bound and must not mutate or retain
     * the supplied working array. A complete assignment must be checked exactly.
     *
     * <p>Equal group IDs assert that the corresponding hatches are interchangeable
     * for this predicate, including capacity and filters. Within each such group,
     * only fluid-then-gas assignments are visited. Group IDs need not be consecutive
     * or nonnegative. The returned array is independent of the working array.</p>
     */
    public static int[] find(int[] groups, Predicate<int[]> feasible) {
        Objects.requireNonNull(groups, "groups");
        Objects.requireNonNull(feasible, "feasible");
        int[] previous = new int[groups.length];
        Map<Integer, Integer> lastByGroup = new HashMap<>();
        for (int i = 0; i < groups.length; i++) {
            Integer last = lastByGroup.put(groups[i], i);
            previous[i] = last == null ? -1 : last;
        }

        int[] roles = new int[groups.length];
        int depth = 0;
        while (true) {
            // Once a group uses gas, its remaining members cannot use fluid.
            // Apply consecutive forced roles together instead of repeatedly
            // simulating the same deterministic tail. An iterative DFS also
            // avoids recursion limits for large multiblocks.
            while (depth < roles.length && previous[depth] >= 0 && roles[previous[depth]] == 2) {
                roles[depth++] = 2;
            }
            if (feasible.test(roles)) {
                if (depth == roles.length) return roles.clone();
                roles[depth++] = 1;
                continue;
            }

            boolean nextBranch = false;
            while (depth > 0) {
                int index = --depth;
                if (roles[index] == 1) {
                    roles[index] = 2;
                    depth++;
                    nextBranch = true;
                    break;
                }
                roles[index] = 0;
            }
            if (!nextBranch) return null;
        }
    }
}
