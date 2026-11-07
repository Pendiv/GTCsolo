package DIV.gtcsolo.registry.material;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.utils.SupplierMemoizer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;

/**
 * GTCEuのMaterialを主幹に残しつつ、指定したTagPrefixの実体だけを他MOD品へ差し替える。
 * 外部MODが無い環境では何もせず、通常どおりGT側の形状を生成する。
 */
public final class ExternalMaterialBridge {

    private final Material material;
    private final String modId;
    private final boolean active;

    private ExternalMaterialBridge(Material material, String modId) {
        this.material = Objects.requireNonNull(material, "material");
        this.modId = Objects.requireNonNull(modId, "modId");
        this.active = ModList.get().isLoaded(modId);
    }

    public static ExternalMaterialBridge bind(Material material, String modId) {
        return new ExternalMaterialBridge(material, modId);
    }

    /** このprefixのGT生成Itemを止め、外部MODのItemをMaterial形状として統合する。 */
    public ExternalMaterialBridge item(TagPrefix prefix, String path) {
        if (active) {
            ResourceLocation id = new ResourceLocation(modId, path);
            prefix.setIgnored(material, () -> requireItem(id));
        }
        return this;
    }

    /**
     * block prefix用。GTCEuがBlock統合表にも登録できるMemoizedBlockSupplierを必ず使う。
     */
    public ExternalMaterialBridge block(TagPrefix prefix, String path) {
        if (active) {
            ResourceLocation id = new ResourceLocation(modId, path);
            prefix.setIgnored(material,
                    SupplierMemoizer.memoizeBlockSupplier(() -> requireBlock(id)));
        }
        return this;
    }

    private static Item requireItem(ResourceLocation id) {
        Item item = ForgeRegistries.ITEMS.getValue(id);
        // Forge's defaulted registry returns AIR for an unknown ID instead of null.
        if (item == null || item == Items.AIR) {
            throw new IllegalStateException("External material item is not registered or is air: " + id);
        }
        return item;
    }

    private static Block requireBlock(ResourceLocation id) {
        Block block = ForgeRegistries.BLOCKS.getValue(id);
        if (block == null || block == Blocks.AIR) {
            throw new IllegalStateException("External material block is not registered or is air: " + id);
        }
        return block;
    }

}
