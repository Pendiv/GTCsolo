package DIV.gtcsolo.integration.kubejs;

import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTMaterialBlocks;
import dev.latvian.mods.kubejs.server.tag.TagEventJS;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

/** Collect raw-ore blocks in Java, avoiding Rhino's collection-iteration issues. */
public final class StoneSlurryTags {
    private static final ResourceLocation TARGET = new ResourceLocation("gtcsolo", "raw_ore_blocks");
    private StoneSlurryTags() {}

    public static void addRawOreBlocks(TagEventJS event) {
        // Snapshot before event.add mutates the tag map. Other mods' Forge raw-storage
        // tags are included even when their item names do not follow GT's convention.
        var references = event.tags.keySet().stream()
                .filter(id -> id.getNamespace().equals("forge") && id.getPath().startsWith("storage_blocks/raw_"))
                // A material such as raw_rubber has a similarly named storage block.
                // Require its corresponding raw-ore item tag before treating it as ore.
                .filter(id -> event.tags.containsKey(new ResourceLocation("forge", "raw_materials/"
                        + id.getPath().substring("storage_blocks/raw_".length()))))
                .map(id -> "#" + id).toArray();
        event.add(TARGET, references);
        for (var entry : GTMaterialBlocks.MATERIAL_BLOCKS.row(TagPrefix.rawOreBlock).values()) {
            var id = ForgeRegistries.ITEMS.getKey(entry.get().asItem());
            if (id != null) event.add(TARGET, id.toString());
        }
        event.add(TARGET, "minecraft:raw_iron_block", "minecraft:raw_copper_block", "minecraft:raw_gold_block");
    }
}
