package DIV.gtcsolo.mixin.mekanism;

import DIV.gtcsolo.integration.mekanism.RecipeCleanroom;
import DIV.gtcsolo.integration.mekanism.RecipePower;
import DIV.gtcsolo.integration.mekanism.RecipeVoltage;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.extensions.IForgeRecipeSerializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = IForgeRecipeSerializer.class, remap = false)
public interface RecipeVoltageJsonMixin {
    @Inject(method = "fromJson(Lnet/minecraft/resources/ResourceLocation;Lcom/google/gson/JsonObject;Lnet/minecraftforge/common/crafting/conditions/ICondition$IContext;)Lnet/minecraft/world/item/crafting/Recipe;", at = @At("RETURN"), remap = false)
    private void gtcsolo$readVoltage(ResourceLocation id, JsonObject json, ICondition.IContext context,
                                            CallbackInfoReturnable<Recipe<?>> result) {
        if (!(result.getReturnValue() instanceof RecipeVoltage recipe)) return;
        try {
            recipe.gtcsolo$setPower(RecipePower.read(json));
            ((RecipeCleanroom) recipe).gtcsolo$setRequiredCleanroom(RecipeCleanroom.read(json));
        } catch (ArithmeticException | IllegalArgumentException | JsonSyntaxException e) {
            throw new JsonSyntaxException("Invalid machine requirements for " + id + ": " + e.getMessage(), e);
        }
    }
}
