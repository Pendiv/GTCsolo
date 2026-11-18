package DIV.gtcsolo.block;

import DIV.gtcsolo.recipe.condition.GtcsoloRecipeConditions;
import DIV.gtcsolo.registry.ModBlocks;
import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.block.IFilterType;
import com.gregtechceu.gtceu.api.machine.multiblock.CleanroomType;

public enum DampedWashingFilterType implements IFilterType {
    INSTANCE;

    public static void register() {
        GTCEuAPI.CLEANROOM_FILTERS.put(INSTANCE, ModBlocks.DAMPED_WASHING_FILTER_CASING);
    }

    @Override
    public String getSerializedName() {
        return ModBlocks.DAMPED_WASHING_FILTER_CASING.getId().getPath();
    }

    @Override
    public CleanroomType getCleanroomType() {
        return GtcsoloRecipeConditions.DAMPED_WASHING_CLEANROOM;
    }
}
