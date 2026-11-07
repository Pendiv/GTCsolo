package DIV.gtcsolo.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.blakebr0.mysticalagriculture.block.MysticalCropBlock", remap = false)
public abstract class IndustrialCultivationCropMixin {
    private static final TagKey<Item> INDUSTRIAL = TagKey.create(Registries.ITEM,
            new ResourceLocation("gtcsolo", "industrial_cultivation_seeds"));

    @Inject(method = "canGrow", at = @At("HEAD"), cancellable = true, remap = false)
    private void requireCultivationRoom(Level level, BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
        if (((Block) (Object) this).getCloneItemStack(level, pos, level.getBlockState(pos)).is(INDUSTRIAL))
            callback.setReturnValue(false);
    }
}
