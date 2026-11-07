package DIV.gtcsolo.block;

import DIV.gtcsolo.registry.ModBlocks;
import DIV.gtcsolo.registry.material.DraconicMaterials;
import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.block.SimpleCoilType;
import net.minecraft.resources.ResourceLocation;

/** Two user-approved successors to GT1.6.4's tritanium heating coil. */
public final class DraconicCoilTypes {
    public static final SimpleCoilType DRACONIUM = new SimpleCoilType(
            "draconium", 12_600, 16, 16, 8, () -> DraconicMaterials.DRACONIUM,
            new ResourceLocation("gtcsolo", "block/casings/coils/machine_coil_draconium"));
    public static final SimpleCoilType AWAKENED_DRACONIUM = new SimpleCoilType(
            "draconium_awakened", 14_400, 32, 16, 9, () -> DraconicMaterials.AWAKENED_DRACONIUM,
            new ResourceLocation("gtcsolo", "block/casings/coils/machine_coil_draconium_awakened"));

    private DraconicCoilTypes() {}

    /** Before previews/recipe loading, without resolving block or material suppliers early. */
    public static void register() {
        GTCEuAPI.HEATING_COILS.put(DRACONIUM, ModBlocks.DRACONIUM_COIL_BLOCK);
        GTCEuAPI.HEATING_COILS.put(AWAKENED_DRACONIUM, ModBlocks.AWAKENED_DRACONIUM_COIL_BLOCK);
    }
}
