package DIV.gtcsolo.recipe.condition;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.recipe.condition.RecipeConditionType;
import net.minecraftforge.eventbus.api.IEventBus;

public final class GtcsoloRecipeConditions {
    public static RecipeConditionType<MultiblockOnlyCondition> MULTIBLOCK_ONLY;

    private GtcsoloRecipeConditions() {}

    @SuppressWarnings("unchecked")
    public static void register(IEventBus bus) {
        Class<RecipeConditionType<?>> type = (Class<RecipeConditionType<?>>) (Class<?>) RecipeConditionType.class;
        bus.addGenericListener(type, GtcsoloRecipeConditions::registerTypes);
    }

    private static void registerTypes(GTCEuAPI.RegisterEvent<String, RecipeConditionType<?>> event) {
        MULTIBLOCK_ONLY = new RecipeConditionType<>(MultiblockOnlyCondition::new, MultiblockOnlyCondition.CODEC);
        event.register("gtcsolo:multiblock_only", MULTIBLOCK_ONLY);
    }
}
