package DIV.gtcsolo.dump;

import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class RecipeDumpModels {
    private RecipeDumpModels() {
    }

    public record DumpFile(
            String recipe_type,
            Map<String, Object> schema,
            Map<String, Object> defaults,
            List<Map<String, Object>> recipes
    ) {
    }

    public record DumpExecutionResult(
            ResourceLocation recipeTypeId,
            Path outputPath,
            int recipeCount
    ) {
    }

    public record DumpIngredient(
            List<DumpIngredientOption> options
    ) {
    }

    public record DumpIngredientOption(
            String item,
            int count
    ) {
    }

    public record DumpResultItem(
            String item,
            int count
    ) {
    }

    public record DumpStack(
            String id,
            int amount
    ) {
    }
}