package DIV.gtcsolo.machine.fusion;

/**
 * Rigidly rotated from run/cmdex/export/fusion_MK4.txt; no shell blocks were added or removed.
 * The 16 upper output cells directly above energy inputs are now additional energy inputs.
 * Only the sealed reactor channel is mandatory air (.). Exterior and central space remain #.
 * A glass/casing, B casing, C output/casing, D coil, E energy/casing, F input/casing, G controller.
 */
public final class FusionMk4Pattern {
    private FusionMk4Pattern() {}

    public static String[][] aisles() {
        return new String[][] {
            { "#################", "#################", "#######AAA#######", "#################", "#################" },
            { "#################", "#######FAF#######", "#####AA...AA#####", "#######CBC#######", "#################" },
            { "#######BBB#######", "#####AA...AA#####", "####A.......A####", "#####BB...BB#####", "#######BBB#######" },
            { "#####BB###BB#####", "####E..AAA..E####", "###AD.......DA###", "####E..BBB..E####", "#####BB###BB#####" },
            { "####B#######B####", "###E.EA###AE.E###", "##AD.D.AAA.D.DA##", "###E.EB###BE.E###", "####B#######B####" },
            { "###B#########B###", "##A.E#######E.A##", "#A..DAA###AAD..A#", "##B.E#######E.B##", "###B#########B###" },
            { "###B#########B###", "##A.A#######A.A##", "#A...A#####A...A#", "##B.B#######B.B##", "###B#########B###" },
            { "##B###########B##", "#A.A#########A.A#", "A...A#######A...A", "#B.B#########B.B#", "##B###########B##" },
            { "##B###########B##", "#A.A#########A.A#", "A...A#######A...A", "#B.B#########B.B#", "##B###########B##" },
            { "##B###########B##", "#A.A#########A.A#", "A...A#######A...A", "#B.B#########B.B#", "##B###########B##" },
            { "###B#########B###", "##A.A#######A.A##", "#A...A#####A...A#", "##B.B#######B.B##", "###B#########B###" },
            { "###B#########B###", "##A.E#######E.A##", "#A..DAA###AAD..A#", "##B.E#######E.B##", "###B#########B###" },
            { "####B#######B####", "###E.EA###AE.E###", "##AD.D.AAA.D.DA##", "###E.EB###BE.E###", "####B#######B####" },
            { "#####BB###BB#####", "####E..AAA..E####", "###AD.......DA###", "####E..BBB..E####", "#####BB###BB#####" },
            { "#######BBB#######", "#####AA...AA#####", "####A.......A####", "#####BB...BB#####", "#######BBB#######" },
            { "#################", "#######CBC#######", "#####AA...AA#####", "#######FBF#######", "#################" },
            { "#################", "#################", "#######AGA#######", "#################", "#################" },
        };
    }
}
