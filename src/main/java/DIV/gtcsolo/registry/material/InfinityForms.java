package DIV.gtcsolo.registry.material;

import DIV.gtcsolo.registry.ModMaterials;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconType;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import java.util.function.Predicate;

/** 自作Infinity/TimelineセットのうちGT1.6.4が標準生成しない形状を補完する。 */
public final class InfinityForms {
    private InfinityForms() {}
    public static final TagPrefix DOUBLE_INGOT = new TagPrefix("infinityDoubleIngot")
            .idPattern("double_%s_ingot").defaultTagPath("double_ingots/%s")
            .langValue("Double %s Ingot").materialAmount(GTValues.M * 2)
            .materialIconType(MaterialIconType.ingotDouble).unificationEnabled(true)
            .generateItem(true).generationCondition(InfinityForms::isInfinity);
    public static final TagPrefix SMALL_ORE = new TagPrefix("infinitySmallOre")
            .idPattern("small_%s_ore").defaultTagPath("small_ores/%s")
            .langValue("Small %s Ore").materialAmount(GTValues.M)
            .materialIconType(new MaterialIconType("infinitySmallOre"))
            .generateBlock(true).generationCondition(InfinityForms::isInfinity);

    private static boolean isInfinity(Material material) {
        return material == ModMaterials.INFINITY || material == ModMaterials.TIMELINE;
    }
    public static void init() {}

    /** 工具/タービン本体の性能は未指定なので、部品だけ生成する。 */
    public static void enableParts() {
        for (TagPrefix prefix : new TagPrefix[]{TagPrefix.toolHeadBuzzSaw, TagPrefix.toolHeadDrill,
                TagPrefix.toolHeadChainsaw, TagPrefix.toolHeadScrewdriver, TagPrefix.toolHeadWrench,
                TagPrefix.toolHeadWireCutter, TagPrefix.turbineBlade}) {
            Predicate<Material> original = prefix.generationCondition();
            prefix.generationCondition(material -> isInfinity(material)
                    || original == null || original.test(material));
        }
        // 低品質gemをパック全体で無効にしていても、このセットの全形状は生成する。
        for (TagPrefix prefix : new TagPrefix[]{TagPrefix.gemChipped, TagPrefix.gemFlawed}) {
            Predicate<Material> original = prefix.generationCondition();
            prefix.generationCondition(material -> (material == ModMaterials.INFINITY_CRYSTAL || material == ModMaterials.TIMELINE_CRYSTAL)
                    || original == null || original.test(material));
        }
    }
    public static void addToCreativeTab(net.minecraft.world.item.CreativeModeTab.Output output) {
        addToCreativeTab(output, ModMaterials.INFINITY, ModMaterials.INFINITY_CRYSTAL);
        addToCreativeTab(output, ModMaterials.TIMELINE, ModMaterials.TIMELINE_CRYSTAL);
    }
    private static void addToCreativeTab(net.minecraft.world.item.CreativeModeTab.Output output,
                                         Material material, Material crystal) {
        for (TagPrefix prefix : new TagPrefix[]{DOUBLE_INGOT, SMALL_ORE, TagPrefix.ingotHot,
                TagPrefix.plateDouble, TagPrefix.dustSmall, TagPrefix.dustTiny, TagPrefix.dustPure,
                TagPrefix.dustImpure, TagPrefix.crushed, TagPrefix.crushedPurified,
                TagPrefix.crushedRefined, TagPrefix.rawOreBlock, TagPrefix.wireFine, TagPrefix.rotor,
                TagPrefix.toolHeadBuzzSaw, TagPrefix.toolHeadDrill, TagPrefix.toolHeadChainsaw,
                TagPrefix.toolHeadScrewdriver, TagPrefix.toolHeadWrench, TagPrefix.toolHeadWireCutter,
                TagPrefix.turbineBlade}) {
            var stack = com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper.get(prefix, material);
            if (!stack.isEmpty()) output.accept(stack);
        }
        for (TagPrefix prefix : new TagPrefix[]{TagPrefix.gemChipped, TagPrefix.gemFlawed,
                TagPrefix.gemFlawless, TagPrefix.gemExquisite}) {
            var stack = com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper.get(prefix, crystal);
            if (!stack.isEmpty()) output.accept(stack);
        }
        for (var key : new com.gregtechceu.gtceu.api.fluids.store.FluidStorageKey[]{
                com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys.LIQUID,
                com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys.MOLTEN,
                com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys.GAS,
                com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys.PLASMA}) {
            var bucket = material.getFluid(key).getBucket();
            if (bucket != net.minecraft.world.item.Items.AIR) output.accept(bucket);
        }
    }

    public static void recipes(java.util.function.Consumer<net.minecraft.data.recipes.FinishedRecipe> provider) {
        recipes(provider, ModMaterials.INFINITY, GTValues.VA[GTValues.OpV]);
        recipes(provider, ModMaterials.TIMELINE, GTValues.VA[GTValues.MAX]);
    }
    private static void recipes(java.util.function.Consumer<net.minecraft.data.recipes.FinishedRecipe> provider,
                                Material material, int voltage) {
        com.gregtechceu.gtceu.common.data.GTRecipeTypes.BENDER_RECIPES.recipeBuilder(
                new net.minecraft.resources.ResourceLocation("gtcsolo", material.getName() + "/double_ingot"))
                .inputItems(TagPrefix.ingot, material, 2).outputItems(DOUBLE_INGOT, material)
                .circuitMeta(2).duration(100).EUt(voltage).save(provider);
        com.gregtechceu.gtceu.common.data.GTRecipeTypes.MACERATOR_RECIPES.recipeBuilder(
                new net.minecraft.resources.ResourceLocation("gtcsolo", material.getName() + "/double_ingot_dust"))
                .inputItems(DOUBLE_INGOT, material).outputItems(TagPrefix.dust, material, 2)
                .duration(100).EUt(voltage).save(provider);
        com.gregtechceu.gtceu.common.data.GTRecipeTypes.MACERATOR_RECIPES.recipeBuilder(
                new net.minecraft.resources.ResourceLocation("gtcsolo", material.getName() + "/small_ore_dust"))
                .inputItems(SMALL_ORE, material).outputItems(TagPrefix.dust, material)
                .duration(100).EUt(voltage).save(provider);
    }

}
