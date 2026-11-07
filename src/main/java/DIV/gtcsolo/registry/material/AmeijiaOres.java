package DIV.gtcsolo.registry.material;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconType;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialStack;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.function.Supplier;

/** Native ore forms, models, unification, loot and processing for the Ameijia rock family. */
public final class AmeijiaOres {
    public static final TagPrefix STONE = ore("ameijia", "Ameijia %s Ore", "ameijia_stone",
            () -> GTMaterials.Stone, Blocks.STONE, 3.0F);
    public static final TagPrefix DEEPSLATE = ore("ameijia_deepslate", "Ameijia Deepslate %s Ore", "ameijia_deepslate",
            () -> GTMaterials.Deepslate, Blocks.DEEPSLATE, 4.5F);

    private AmeijiaOres() {}

    public static void init() {}

    private static TagPrefix ore(String name, String label, String rockId, Supplier<Material> host,
                                 Block template, float hardness) {
        ResourceLocation rock = new ResourceLocation("gtcsolo", rockId);
        TagPrefix prefix = new FlaggedOrePrefix(name)
                .defaultTagPath("ores/%s").prefixOnlyTagPath("ores_in_ground/%s").unformattedTagPath("ores")
                .materialIconType(MaterialIconType.ore).miningToolTag(BlockTags.MINEABLE_WITH_PICKAXE)
                .unificationEnabled(true).generationCondition(AmeijiaOreFlags::enabled).langValue(label)
                .registerOre(() -> BuiltInRegistries.BLOCK.get(rock).defaultBlockState(), host,
                        () -> BlockBehaviour.Properties.copy(template).requiresCorrectToolForDrops()
                                .strength(hardness, 3.0F).sound(template == Blocks.DEEPSLATE ? SoundType.DEEPSLATE : SoundType.STONE),
                        new ResourceLocation("gtcsolo", "block/ore_bases/" + rockId), false, false, true);
        // Addon prefixes are initialized after GTMaterials has populated native secondary materials.
        prefix.addSecondaryMaterial(new MaterialStack(host.get(), TagPrefix.dust.materialAmount()));
        return prefix;
    }

    /** 1.6.4's ore-block generator checks isIgnored, not generationCondition. */
    private static final class FlaggedOrePrefix extends TagPrefix {
        private FlaggedOrePrefix(String name) { super(name); }
        @Override public boolean isIgnored(Material material) {
            return super.isIgnored(material) || !AmeijiaOreFlags.enabled(material);
        }
        // ChemicalHelper otherwise falls back to the shared forge:ores/<material> tag,
        // making an ungenerated form appear obtainable to native recipe handlers.
        @Override @SuppressWarnings("unchecked") public TagKey<Item>[] getItemTags(Material material) {
            return AmeijiaOreFlags.enabled(material) ? super.getItemTags(material) : new TagKey[0];
        }
        @Override @SuppressWarnings("unchecked") public TagKey<Block>[] getBlockTags(Material material) {
            return AmeijiaOreFlags.enabled(material) ? super.getBlockTags(material) : new TagKey[0];
        }
    }

    public static void addTerrainReplacements(Map<BlockState, BlockState> replacements) {
        for (Material material : GTCEuAPI.materialManager.getRegisteredMaterials()) {
            if (!AmeijiaOreFlags.enabled(material)) continue;
            addReplacement(replacements, TagPrefix.ore, STONE, material);
            addReplacement(replacements, TagPrefix.oreDeepslate, DEEPSLATE, material);
        }
    }

    private static void addReplacement(Map<BlockState, BlockState> replacements, TagPrefix source,
                                       TagPrefix target, Material material) {
        Block from = ChemicalHelper.getBlock(source, material);
        Block to = ChemicalHelper.getBlock(target, material);
        if (from == null || to == null || from == Blocks.AIR || to == Blocks.AIR || from == to) return;
        for (BlockState state : from.getStateDefinition().getPossibleStates())
            replacements.put(state, to.defaultBlockState());
    }

    public static void addToCreativeTab(CreativeModeTab.Output output) {
        for (Material material : GTCEuAPI.materialManager.getRegisteredMaterials()) {
            if (!AmeijiaOreFlags.enabled(material)) continue;
            for (TagPrefix prefix : new TagPrefix[]{STONE, DEEPSLATE}) {
                var stack = ChemicalHelper.get(prefix, material);
                if (!stack.isEmpty()) output.accept(stack);
            }
        }
    }
}
