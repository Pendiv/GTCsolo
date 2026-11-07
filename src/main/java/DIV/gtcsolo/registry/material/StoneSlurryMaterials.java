package DIV.gtcsolo.registry.material;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.api.fluids.FluidBuilder;
import net.minecraft.resources.ResourceLocation;

/** Fluid-only process intermediates; recipes are authored together in stone_slurry.js. */
public final class StoneSlurryMaterials {
    public static Material STONE_SLURRY, MOLTEN_ORE_SLURRY, SETTLED_ORE_SLURRY,
            REFRACTORY_MINERAL_SLURRY, OXIDIZED_MINERAL_SLURRY, RESIDUAL_MINERAL_SLURRY,
            ACIDIC_MINERAL_SLURRY, ACIDIC_MINERAL_RESIDUE;

    private StoneSlurryMaterials() {}

    public static void init() {
        STONE_SLURRY = fluid("stone_slurry", 0x8D8980, 300);
        MOLTEN_ORE_SLURRY = fluid("molten_ore_slurry", 0xBA6941, 1200);
        SETTLED_ORE_SLURRY = fluid("settled_ore_slurry", 0x756A4D, 400);
        REFRACTORY_MINERAL_SLURRY = fluid("refractory_mineral_slurry", 0x686B70, 300);
        OXIDIZED_MINERAL_SLURRY = fluid("oxidized_mineral_slurry", 0xAD7953, 300);
        RESIDUAL_MINERAL_SLURRY = fluid("residual_mineral_slurry", 0x77775C, 300);
        ACIDIC_MINERAL_SLURRY = fluid("acidic_mineral_slurry", 0xA4A05C, 300);
        ACIDIC_MINERAL_RESIDUE = fluid("acidic_mineral_residue", 0x646D43, 300);
    }

    private static Material fluid(String name, int color, int temperature) {
        return new Material.Builder(new ResourceLocation("gtcsolo", name))
                .liquid(new FluidBuilder().temperature(temperature)).color(color)
                .flags(MaterialFlags.DISABLE_DECOMPOSITION).buildAndRegister();
    }
}
