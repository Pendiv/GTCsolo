package DIV.gtcsolo.integration.jei.multiblock;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.integration.jei.performance.JeiOptimizationOptions;
import DIV.gtcsolo.integration.jei.performance.NativePreviewIngredientIndex;
import DIV.gtcsolo.integration.jei.GtcSoloJeiPlugin;
import DIV.gtcsolo.integration.jei.multiblock.MultiblockIntroduction.PageLink;
import DIV.gtcsolo.registry.IndustrialCircuitAssemblyLines;
import DIV.gtcsolo.registry.IndustrialSuperheatedMegaBlastFurnaces;
import DIV.gtcsolo.registry.IterativeAssemblyLines;
import DIV.gtcsolo.registry.ForgeOfSunMachines;
import DIV.gtcsolo.registry.ResourceRoomMachines;
import DIV.gtcsolo.registry.FusionMk4Machines;
import DIV.gtcsolo.registry.ModMachines;
import DIV.gtcsolo.machine.fusion.FusionMk4Machine;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.FusionReactorMachine;
import com.gregtechceu.gtceu.common.data.machines.GTMultiMachines;
import com.gregtechceu.gtceu.common.data.machines.GTResearchMachines;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.lowdragmc.lowdraglib.jei.ModularUIRecipeCategory;
import com.lowdragmc.lowdraglib.jei.ModularWrapper;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;

public final class MultiblockIntroductionCategory extends ModularUIRecipeCategory<MultiblockIntroduction> {
    public static final RecipeType<MultiblockIntroduction> RECIPE_TYPE = new RecipeType<>(
            new ResourceLocation(Gtcsolo.MODID, "multiblock_introduction"), MultiblockIntroduction.class);
    private final IDrawable background;
    private final IDrawable icon;
    private final Function<MultiblockIntroduction, ModularWrapper<?>> wrappers;
    private final IFocusFactory focusFactory;

    public MultiblockIntroductionCategory(IJeiHelpers helpers) {
        this(helpers, new LiveRecipeWrapperFactory<>(entry -> new ModularWrapper<>(new MultiblockIntroductionWidget(entry))));
    }

    private MultiblockIntroductionCategory(IJeiHelpers helpers,
                                          Function<MultiblockIntroduction, ModularWrapper<?>> wrappers) {
        // Create the interactive world/widget only when JEI actually lays out an entry.
        // LDLib's 10-entry/10-second cache may evict a wrapper still owned by a JEI input listener.
        super(wrappers.andThen(wrapper -> {
            ((MultiblockIntroductionWidget) wrapper.getWidget()).refreshDocument();
            return wrapper;
        }));
        this.wrappers = wrappers;
        focusFactory = helpers.getFocusFactory();
        background = helpers.getGuiHelper().createBlankDrawable(
                MultiblockIntroductionWidget.WIDTH, MultiblockIntroductionWidget.HEIGHT);
        icon = helpers.getGuiHelper().createDrawableItemStack(IndustrialCircuitAssemblyLines.CORE.asStack());
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        var introductions = new ArrayList<>(List.of(
                MultiblockIntroduction.numbered(
                        GTMultiMachines.LARGE_CHEMICAL_REACTOR, "gtcsolo.lcr.introduction.", 2),
                MultiblockIntroduction.numbered(
                        GTMultiMachines.DISTILLATION_TOWER, "gtcsolo.distillation_tower.introduction.", 2),
                MultiblockIntroduction.numbered(
                        GTMultiMachines.ELECTRIC_BLAST_FURNACE, "gtcsolo.ebf.introduction.", 2),
                MultiblockIntroduction.numbered(
                        GTMultiMachines.PYROLYSE_OVEN, "gtcsolo.pyrolyse_oven.introduction.", 2),
                MultiblockIntroduction.numbered(
                        GTMultiMachines.CRACKER, "gtcsolo.cracker.introduction.", 2),
                MultiblockIntroduction.numbered(
                        GTMultiMachines.CLEANROOM, "gtcsolo.cleanroom.introduction.", 6),
                MultiblockIntroduction.numbered(
                        GTMultiMachines.ASSEMBLY_LINE, "gtcsolo.assembly_line.introduction.", 6),
                MultiblockIntroduction.numbered(
                        IndustrialCircuitAssemblyLines.CORE, "gtcsolo.ical.description.", 28),
                MultiblockIntroduction.numbered(
                        IndustrialSuperheatedMegaBlastFurnaces.CORE, "gtcsolo.ismbf.description.", 35),
                MultiblockIntroduction.numbered(
                        IterativeAssemblyLines.CORE, "gtcsolo.iterative_assembly.description.", 9),
                MultiblockIntroduction.numbered(
                        ForgeOfSunMachines.CORE, "gtcsolo.forge_of_sun.description.", 4),
                MultiblockIntroduction.numbered(ResourceRoomMachines.FAMING_ROOM, "gtcsolo.room.farming.description.", 5),
                MultiblockIntroduction.numbered(ResourceRoomMachines.CONSTRUCTION_ROOM, "gtcsolo.room.construction.description.", 6)));

        var boilers = new ArrayList<MultiblockIntroduction>();
        for (var boiler : List.of(GTMultiMachines.LARGE_BOILER_BRONZE, GTMultiMachines.LARGE_BOILER_STEEL,
                GTMultiMachines.LARGE_BOILER_TITANIUM, GTMultiMachines.LARGE_BOILER_TUNGSTENSTEEL)) {
            boilers.add(MultiblockIntroduction.named(boiler, "gtcsolo.large_boiler.introduction.", 3));
        }
        introductions.add(MultiblockIntroduction.grouped(boilers));
        var turbines = new ArrayList<MultiblockIntroduction>();
        for (var turbine : List.of(GTMultiMachines.LARGE_STEAM_TURBINE, GTMultiMachines.LARGE_GAS_TURBINE,
                GTMultiMachines.LARGE_PLASMA_TURBINE)) {
            turbines.add(MultiblockIntroduction.named(turbine, "gtcsolo.large_turbine.introduction.", 3));
        }
        introductions.add(MultiblockIntroduction.grouped(turbines));
        var fusionReactors = new ArrayList<MultiblockIntroduction>();
        for (var fusion : GTMultiMachines.FUSION_REACTOR) {
            if (fusion != null) fusionReactors.add(fusionIntroduction(fusion, false));
        }
        if (FusionMk4Machines.FUSION_REACTOR_MK4 != null)
            fusionReactors.add(fusionIntroduction(FusionMk4Machines.FUSION_REACTOR_MK4, true));
        introductions.add(MultiblockIntroduction.grouped(fusionReactors));

        var hpcaDefinition = (MultiblockMachineDefinition) GTResearchMachines.HIGH_PERFORMANCE_COMPUTING_ARRAY;
        introductions.add(MultiblockIntroduction.named(hpcaDefinition, "gtcsolo.hpca.introduction.", 4)
                .withNavigation("hpca", link("computation"), link("hpca_components")));
        introductions.add(MultiblockIntroduction.named(GTResearchMachines.RESEARCH_STATION,
                        "gtcsolo.research_station.introduction.", 3)
                .withNavigation("research_station", link("computation"), link("hpca")));
        introductions.add(MultiblockIntroduction.named((MultiblockMachineDefinition) GTResearchMachines.NETWORK_SWITCH,
                        "gtcsolo.network_switch.introduction.", 2)
                .withNavigation("network_switch", link("computation"), link("hpca"), link("hpca_components")));

        introductions.add(MultiblockIntroduction.named(ModMachines.WEN_MAIN_STORAGE,
                        "gtcsolo.wen.introduction.", 6)
                .withNavigation("wen", new PageLink("glossary:wen_cells", "gtcsolo.jei.introduction.link.wen_cells")));

        introductions.addAll(MultiblockIntroductionResources.load(Minecraft.getInstance().getResourceManager()));
        IndustrialIntroductionGroup.mergeInto(introductions);
        CombustionMinerIntroductionGroup.mergeInto(introductions);

        // Fail early on duplicate ids or dangling links instead of showing unresponsive text.
        var pageIds = new HashSet<String>();
        for (var introduction : introductions) {
            if (!introduction.pageId().isEmpty() && !pageIds.add(introduction.pageId()))
                throw new IllegalStateException("Duplicate introduction page: " + introduction.pageId());
        }
        for (var introduction : introductions) for (var link : introduction.relatedPages()) {
            if (!pageIds.contains(link.targetId()) && !GlossaryCatalog.isGlossaryTarget(link.targetId()))
                throw new IllegalStateException("Missing introduction page: " + link.targetId());
        }
        GlossaryCatalog.validateLinks(pageIds);
        registration.addRecipes(RECIPE_TYPE, introductions);
        MultiblockIntroductionTooltips.replaceCatalog(introductions);
    }

    private static PageLink link(String targetId) {
        String target = targetId.equals("computation") || targetId.equals("hpca_components")
                ? "glossary:" + targetId : targetId;
        return new PageLink(target, "gtcsolo.jei.introduction.link." + targetId);
    }

    static MultiblockIntroduction fusionIntroduction(MultiblockMachineDefinition definition, boolean mk4) {
        int tier = mk4 ? FusionMk4Machine.TIER : definition.getTier();
        int ports = mk4 ? FusionMk4Machine.ENERGY_PORTS : 16;
        long capacity = mk4 ? FusionMk4Machine.STARTUP_CAPACITY
                : FusionReactorMachine.calculateEnergyStorageFactor(tier, ports);
        var introduction = MultiblockIntroduction.named(definition, "gtcsolo.fusion.introduction.", 2)
                .withParagraph("gtcsolo.fusion.introduction.limits", GTValues.VN[tier], ports, capacity / 1_000_000L);
        return mk4 ? introduction.withParagraph("gtcsolo.fusion.introduction.fixed_hatches", ports)
                : introduction.withParagraph("gtcsolo.fusion.introduction.expand_storage");
    }

    public static void openPage(String pageId) {
        if (pageId.startsWith("glossary:")) {
            GlossaryCategory.openPage(pageId.substring("glossary:".length()));
            return;
        }
        // Preserve the old entry points after moving these text-only topics out of machine introductions.
        if (pageId.equals("computation") || pageId.equals("hpca_components")) {
            GlossaryCategory.openPage(pageId);
            return;
        }
        var runtime = GtcSoloJeiPlugin.getRuntimeOrNull();
        if (runtime == null || pageId.isBlank()) return;
        // Resolve from this runtime's catalog; JEI keeps the previous page in its own history.
        runtime.getRecipeManager().createRecipeLookup(RECIPE_TYPE).get()
                .filter(introduction -> pageId.equals(introduction.pageId()))
                .findFirst().ifPresent(introduction -> runtime.getRecipesGui().showRecipes(
                        runtime.getRecipeManager().getRecipeCategory(RECIPE_TYPE), List.of(introduction), List.of()));
    }

    public static void openVariant(MultiblockIntroduction introduction, int index) {
        var runtime = GtcSoloJeiPlugin.getRuntimeOrNull();
        if (runtime == null) return;
        var category = runtime.getRecipeManager().getRecipeCategory(RECIPE_TYPE);
        if (!(category instanceof MultiblockIntroductionCategory ownCategory)) return;
        var focus = ownCategory.focusFactory.createFocus(RecipeIngredientRole.CATALYST, VanillaTypes.ITEM_STACK,
                introduction.subject(index));
        runtime.getRecipesGui().showRecipes(category, List.of(introduction), List.of(focus));
    }

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull MultiblockIntroduction introduction,
                          @NotNull IFocusGroup focuses) {
        if (introduction.isSingleBlock()) {
            if (!NativePreviewIngredientIndex.isIngredientIndexBuilder(builder)) super.setRecipe(builder, introduction, focuses);
            builder.addInvisibleIngredients(RecipeIngredientRole.CATALYST).addItemStacks(introduction.subjects());
            builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStacks(introduction.subjects());
            return;
        }
        if (NativePreviewIngredientIndex.isIngredientIndexBuilder(builder)) {
            // Index outputs even when the structure's cached inputs skip widget creation.
            var fluids = introduction.fluidSection().groups().stream().flatMap(g -> g.fluids().stream()).toList();
            if (!fluids.isEmpty()) builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT)
                    .addIngredients(ForgeTypes.FLUID_STACK, fluids);
            // Every member remains searchable, without constructing already-indexed preview worlds.
            for (var variant : introduction.variants()) {
                if (!JeiOptimizationOptions.introductionIndexEnabled()
                        || !NativePreviewIngredientIndex.addCachedInputs(builder, variant.definition())) {
                    super.setRecipe(builder, new MultiblockIntroduction(List.of(variant), "", List.of()), focuses);
                }
            }
        } else {
            focuses.getFocuses(VanillaTypes.ITEM_STACK)
                    .mapToInt(focus -> introduction.indexOfController(focus.getTypedValue().getIngredient().getItem()))
                    .filter(index -> index >= 0).findFirst()
                    .ifPresent(index -> ((MultiblockIntroductionWidget) wrappers.apply(introduction).getWidget()).selectVariant(index));
            super.setRecipe(builder, introduction, focuses);
        }
        builder.addInvisibleIngredients(RecipeIngredientRole.CATALYST).addItemStacks(
                introduction.variants().stream().map(v -> v.definition().asStack()).toList());
    }

    @Override
    public @NotNull RecipeType<MultiblockIntroduction> getRecipeType() { return RECIPE_TYPE; }

    @Override
    public @NotNull Component getTitle() { return Component.translatable("gtcsolo.jei.multiblock_introduction.title"); }

    @Override
    public @NotNull IDrawable getBackground() { return background; }

    @Override
    public @NotNull IDrawable getIcon() { return icon; }
}
