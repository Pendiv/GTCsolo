package DIV.gtcsolo.mixin.performance;

import DIV.gtcsolo.client.performance.SpriteVisibility;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Retains atlas identity when LDLib turns a sprite into cached numeric UV coordinates. */
@Mixin(value = ResourceTexture.class, remap = false)
public abstract class ResourceTextureSpriteVisibilityMixin implements SpriteVisibility.AtlasTexture {
    @Shadow public ResourceLocation imageLocation;
    @Unique private ResourceLocation gtcsolo$spriteSource;

    @Override public ResourceLocation gtcsolo$getSpriteSource() { return gtcsolo$spriteSource; }
    @Override public void gtcsolo$setSpriteSource(ResourceLocation source) { gtcsolo$spriteSource = source; }

    @Inject(method = "fromSpirit", at = @At("RETURN"), remap = false)
    private static void gtcsolo$rememberSprite(ResourceLocation texture, CallbackInfoReturnable<ResourceTexture> cir) {
        if (SpriteVisibility.isAvailable()) {
            ((SpriteVisibility.AtlasTexture) (Object) cir.getReturnValue()).gtcsolo$setSpriteSource(texture);
        }
    }

    // copy() delegates to this overload; the double overload constructs its own texture.
    @Inject(method = "getSubTexture(FFFF)Lcom/lowdragmc/lowdraglib/gui/texture/ResourceTexture;",
            at = @At("RETURN"), remap = false)
    private void gtcsolo$inheritSprite(float x, float y, float width, float height,
                                      CallbackInfoReturnable<ResourceTexture> cir) {
        if (gtcsolo$spriteSource != null) {
            ((SpriteVisibility.AtlasTexture) (Object) cir.getReturnValue()).gtcsolo$setSpriteSource(gtcsolo$spriteSource);
        }
    }

    @Inject(method = "getSubTexture(DDDD)Lcom/lowdragmc/lowdraglib/gui/texture/ResourceTexture;",
            at = @At("RETURN"), remap = false)
    private void gtcsolo$inheritSpriteDouble(double x, double y, double width, double height,
                                            CallbackInfoReturnable<ResourceTexture> cir) {
        if (gtcsolo$spriteSource != null) {
            ((SpriteVisibility.AtlasTexture) (Object) cir.getReturnValue()).gtcsolo$setSpriteSource(gtcsolo$spriteSource);
        }
    }

    @Inject(method = "drawSubAreaInternal", at = @At("HEAD"), remap = false)
    private void gtcsolo$markVisible(GuiGraphics graphics, float x, float y, float width, float height,
                                     float drawnU, float drawnV, float drawnWidth, float drawnHeight,
                                     CallbackInfo ci) {
        if (gtcsolo$spriteSource != null && TextureAtlas.LOCATION_BLOCKS.equals(imageLocation)) {
            SpriteVisibility.markAtlasTexture(gtcsolo$spriteSource);
        }
    }
}
