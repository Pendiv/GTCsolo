package DIV.gtcsolo.mixin.apoth;

import DIV.gtcsolo.apoth.gem.GemSkin;
import DIV.gtcsolo.apoth.gem.GemSkinRegistry;
import dev.shadowsoffire.apotheosis.adventure.client.GemModel;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemItem;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

/**
 * GemModel.resolve は apotheosis:item/gems/<path> をハードコードしている。
 * gtcsolo namespace の gem については、登録された GemSkin の iconset に対応する
 * GTCEu material_sets gem model を返すように介入する。
 */
@Mixin(value = GemModel.class, remap = false)
public class GemModelMixin {

    @Inject(method = "resolve", at = @At("HEAD"), cancellable = true)
    private void gtcsolo$useIconsetForGtcsoloGems(BakedModel original, ItemStack stack,
                                                   @Nullable ClientLevel world,
                                                   @Nullable LivingEntity entity, int seed,
                                                   CallbackInfoReturnable<BakedModel> cir) {
        DynamicHolder<Gem> gem = GemItem.getGem(stack);
        if (!gem.isBound()) return;
        GemSkin skin = GemSkinRegistry.get(gem.getId());
        if (skin == null) return;
        ResourceLocation modelId = new ResourceLocation("gtceu",
            "item/material_sets/" + skin.iconset() + "/gem");
        BakedModel resolved = Minecraft.getInstance().getModelManager().getModel(modelId);
        if (resolved != null) {
            cir.setReturnValue(resolved);
        }
    }
}
