package DIV.gtcsolo.integration.jei.multiblock;

import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.List;

/**
 * Common industrial processors share one page; specialized machines keep their own introductions.
 */
final class IndustrialIntroductionGroup {
    static final String PAGE_ID = "industrial_large_machines";
    private static final List<ResourceLocation> MACHINES =
            List.of(
                    id("industrial_macerator"),
                    id("industrial_sifter"),
                    id("industrial_material_press"),
                    id("industrial_cutter"),
                    id("industrial_mixer"),
                    id("industrial_centrifuge"),
                    id("industrial_electrolyzer"),
                    id("heat_exchange_industrial_vacuum_freezer"),
                    id("industrial_rock_breaker"));

    private IndustrialIntroductionGroup() {}

    private static ResourceLocation id(String name) {
        return new ResourceLocation("gtceu", name);
    }

    static void mergeInto(List<MultiblockIntroduction> introductions) {
        var members =
                introductions.stream()
                        .filter(
                                page ->
                                        !page.isSingleBlock()
                                                && page.variants().size() == 1
                                                && MACHINES.contains(page.definition().getId()))
                        .sorted(
                                Comparator.comparingInt(
                                        page -> MACHINES.indexOf(page.definition().getId())))
                        .toList();
        if (members.isEmpty()) return;
        int index = members.stream().mapToInt(introductions::indexOf).min().orElseThrow();
        var group = MultiblockIntroduction.grouped(members).withNavigation(PAGE_ID);
        introductions.removeAll(members);
        introductions.add(index, group);
    }
}
