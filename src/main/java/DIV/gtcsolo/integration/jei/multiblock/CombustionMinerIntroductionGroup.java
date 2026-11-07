package DIV.gtcsolo.integration.jei.multiblock;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** The tier variants share one introduction and retain their own text and structure. */
final class CombustionMinerIntroductionGroup {
    private static final List<ResourceLocation> MACHINES = List.of(
            new ResourceLocation("gtcsolo", "hv_combustion_miner"),
            new ResourceLocation("gtcsolo", "iv_combustion_miner"));

    private CombustionMinerIntroductionGroup() {}

    static void mergeInto(List<MultiblockIntroduction> introductions) {
        var members = introductions.stream()
                .filter(page -> !page.isSingleBlock() && page.variants().size() == 1
                        && MACHINES.contains(page.definition().getId()))
                .sorted(java.util.Comparator.comparingInt(page -> MACHINES.indexOf(page.definition().getId())))
                .toList();
        if (members.size() != MACHINES.size()
                || members.stream().map(page -> page.definition().getId()).distinct().count() != MACHINES.size()) return;
        int index = members.stream().mapToInt(introductions::indexOf).min().orElseThrow();
        var group = MultiblockIntroduction.grouped(members);
        introductions.removeAll(members);
        introductions.add(index, group);
    }
}
