package DIV.gtcsolo.mixin;

import DIV.gtcsolo.block.EntroniumBlock;
import com.gregtechceu.gtceu.api.block.MaterialBlock;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTMaterialBlocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Change only the Entronium block class; registration, forms, tags and recipes stay native. */
@Mixin(value = GTMaterialBlocks.class, remap = false)
public abstract class EntroniumBlockFactoryMixin {
    @Redirect(method = "*", at = @At(value = "NEW",
            target = "com/gregtechceu/gtceu/api/block/MaterialBlock"), remap = false)
    private static MaterialBlock gtcsolo$transparentEntronium(BlockBehaviour.Properties properties,
                                                              TagPrefix prefix, Material material) {
        return EntroniumBlock.matches(material)
                ? new EntroniumBlock(properties, prefix, material)
                : new MaterialBlock(properties, prefix, material);
    }
}
