package DIV.gtcsolo.worldgen;

import com.gregtechceu.gtceu.api.data.worldgen.IWorldGenLayer;
import com.gregtechceu.gtceu.api.data.worldgen.SimpleWorldGenLayer;
import com.gregtechceu.gtceu.api.data.worldgen.WorldGeneratorUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

import java.util.Set;

/**
 * gtcsolo 独自の IWorldGenLayer 登録.
 *
 * GTCEu 本家は STONE/DEEPSLATE/NETHERRACK/ENDSTONE の 4 種を持つ.
 * 独自次元の鉱脈を生成させるには対応 layer を追加する必要がある.
 */
public final class GtcsoloWorldGenLayers {

    /** Ameijia's native stone/deepslate generation and their dimension-specific replacements. */
    public static final IWorldGenLayer AMEIJIA = new SimpleWorldGenLayer(
            "ameijia",
            () -> new TagMatchTest(TagKey.create(Registries.BLOCK,
                    new ResourceLocation("gtcsolo", "ameijia_ore_replaceables"))),
            Set.of(new ResourceLocation("gtcsolo", "ameijia"))
    );

    private GtcsoloWorldGenLayers() {}

    public static void init() {
        WorldGeneratorUtils.WORLD_GEN_LAYERS.put(AMEIJIA.getSerializedName(), AMEIJIA);
    }
}
