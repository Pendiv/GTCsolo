package DIV.gtcsolo.registry.freeform;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.block.AppearanceBlock;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconType;
import com.gregtechceu.gtceu.client.renderer.block.MaterialBlockRenderer;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Objects;

/**
 * GTCEu の MaterialIcon ブロックモデルだけを借り、Material / TagPrefix には属さない自由色ブロック。
 */
public final class FreeformMaterialIconBlock extends AppearanceBlock {

    private final int[] layerColors;

    public FreeformMaterialIconBlock(Properties properties, MaterialIconType iconType,
                                     MaterialIconSet iconSet, int... layerColors) {
        super(properties);
        Objects.requireNonNull(iconType, "iconType");
        Objects.requireNonNull(iconSet, "iconSet");
        if (layerColors.length == 0) {
            throw new IllegalArgumentException("At least one layer color is required");
        }
        this.layerColors = layerColors.clone();

        if (GTCEu.isClientSide()) {
            MaterialBlockRenderer.create(this, iconType, iconSet);
        }
    }

    private int getLayerColor(int layerIndex) {
        if (layerIndex < -100) {
            layerIndex = (Math.abs(layerIndex) % 100) / 10;
        }
        if (layerIndex < 0 || layerIndex >= layerColors.length) {
            return -1;
        }
        int color = layerColors[layerIndex];
        return color != -1 || layerIndex == 0 ? color : layerColors[0];
    }

    @OnlyIn(Dist.CLIENT)
    public static BlockColor blockTintColor() {
        return (state, level, pos, layerIndex) -> {
            if (state.getBlock() instanceof FreeformMaterialIconBlock block) {
                return block.getLayerColor(layerIndex);
            }
            return -1;
        };
    }

    @OnlyIn(Dist.CLIENT)
    public static ItemColor itemTintColor() {
        return (stack, layerIndex) -> {
            if (stack.getItem() instanceof BlockItem blockItem &&
                    blockItem.getBlock() instanceof FreeformMaterialIconBlock block) {
                return block.getLayerColor(layerIndex);
            }
            return -1;
        };
    }

    public static BlockItem createItem(FreeformMaterialIconBlock block, Item.Properties properties) {
        return new BlockItem(block, properties);
    }
}
