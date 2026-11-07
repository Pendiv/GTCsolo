package DIV.gtcsolo.mixin.performance;

import DIV.gtcsolo.client.performance.SpriteVisibility;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Captures the optional renderer's existing sprite marks without changing vanilla getters. */
@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.render.texture.SpriteUtil", remap = false)
public abstract class EmbeddiumSpriteCaptureMixin {
    @ModifyVariable(method = "markSpriteActive", at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false)
    private static TextureAtlasSprite gtcsolo$captureSprite(TextureAtlasSprite sprite) {
        SpriteVisibility.record(sprite);
        return sprite;
    }
}
