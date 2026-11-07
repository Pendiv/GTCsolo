package DIV.gtcsolo.registry.freeform;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconType;
import com.gregtechceu.gtceu.client.renderer.item.TagPrefixItemRenderer;

import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Objects;

/**
 * GTCEu の MaterialIcon モデルだけを借り、Material / TagPrefix には属さない自由色アイテム。
 */
public final class FreeformMaterialIconItem extends Item {

    private final int[] layerColors;

    public FreeformMaterialIconItem(Properties properties, MaterialIconType iconType,
                                    MaterialIconSet iconSet, int... layerColors) {
        super(properties);
        Objects.requireNonNull(iconType, "iconType");
        Objects.requireNonNull(iconSet, "iconSet");
        if (layerColors.length == 0) {
            throw new IllegalArgumentException("At least one layer color is required");
        }
        this.layerColors = layerColors.clone();

        if (GTCEu.isClientSide()) {
            TagPrefixItemRenderer.create(this, iconType, iconSet);
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
    public static ItemColor tintColor() {
        return (stack, layerIndex) -> {
            if (stack.getItem() instanceof FreeformMaterialIconItem item) {
                return item.getLayerColor(layerIndex);
            }
            return -1;
        };
    }
}
