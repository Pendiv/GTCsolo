package DIV.gtcsolo.machine;

import java.util.ArrayList;
import java.util.List;

/** User-authored exports, transposed without changing geometry. Regenerate with scripts/generate-industrial-circuit-pattern.py. */
public final class IndustrialCircuitStructure {
    private IndustrialCircuitStructure() {}
    private static final String[][] START = {
        {"#AFSEE#", "AGGGGGA", "AGGGGGA", "AGGGGGA", "AGGGGGA", "AGGGGGA", "#AAMAA#"},
        {"##AAA##", "AT###TA", "A#####A", "A#####A", "A#####A", "A#####A", "#AAAAA#"},
        {"#######", "ATAAATA", "A#####A", "A#####A", "A#####A", "A#####A", "#AAAAA#"},
        {"#######", "AT###TA", "ADAAADA", "A#####A", "A#####A", "A#####A", "#AAAAA#"},
    };
    private static final String[][] MODULE = {
        {"#######", "AT###TA", "ADAIADA", "A#####A", "A#####A", "A#####A", "#AAAAA#"},
        {"#######", "AT###TA", "ADAIADA", "AD###DA", "AD###DA", "ADDPDDA", "#ALLJA#"},
        {"#######", "AT###TA", "ADAIADA", "A#####A", "A#####A", "A#####A", "#AAAAA#"},
    };
    private static final String[][] END = {
        {"#######", "AAAAAAA", "A#####A", "A#####A", "A#####A", "A#####A", "#AAAAA#"},
        {"#AAAAA#", "A#####A", "A#####A", "A#####A", "A#####A", "A#####A", "#AAAAA#"},
        {"#OOOOO#", "AGGGGGA", "AGGGGGA", "AGGGGGA", "AGGGGGA", "AGGGGGA", "#AAAAA#"},
    };

    public static String[][] aisles(int modules) {
        if (modules < 1 || modules > 6) throw new IllegalArgumentException("ICAL supports 1..6 modules");
        List<String[]> aisles = new ArrayList<>();
        add(aisles, START);
        for (int n = 0; n < modules; n++) add(aisles, MODULE);
        add(aisles, END);
        return aisles.toArray(String[][]::new);
    }

    private static void add(List<String[]> target, String[][] section) {
        for (String[] plane : section) target.add(plane.clone());
    }
}
