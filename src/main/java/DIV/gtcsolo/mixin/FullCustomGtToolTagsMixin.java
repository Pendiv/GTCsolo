package DIV.gtcsolo.mixin;

import DIV.gtcsolo.registry.tool.fullcustom.FullCustomGtTools;

import com.gregtechceu.gtceu.core.IGTTagLoader;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagLoader;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;

/**
 * Adds the selected GTToolType's normal item tags to full custom tools at the same dynamic tag-loading phase
 * used by GTCEu's generated material tools.
 */
@Mixin(TagLoader.class)
public class FullCustomGtToolTagsMixin {

    @Inject(method = "load", at = @At("RETURN"))
    private void gtcsolo$addFullCustomGtToolTags(
            ResourceManager resourceManager,
            CallbackInfoReturnable<Map<ResourceLocation, List<TagLoader.EntryWithSource>>> cir) {
        Object self = this;
        if (self instanceof IGTTagLoader<?> gtTagLoader &&
                gtTagLoader.gtceu$getRegistry() == BuiltInRegistries.ITEM) {
            FullCustomGtTools.injectItemTags(cir.getReturnValue());
        }
    }
}
