package DIV.gtcsolo.integration.mekanism.capability;

import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;

/**
 * Mekanism chemical IO hatch 用の PartAbility 8 種.
 *
 * マルチブロック構造 pattern で .abilities(INPUT_GAS) のように指定可能.
 * 対応する hatch machine は ChemicalHatches で tier 別に登録される.
 */
public final class ChemicalPartAbilities {

    public static final PartAbility INPUT_GAS       = new PartAbility("input_gas");
    public static final PartAbility OUTPUT_GAS      = new PartAbility("output_gas");
    public static final PartAbility INPUT_INFUSION  = new PartAbility("input_infusion");
    public static final PartAbility OUTPUT_INFUSION = new PartAbility("output_infusion");
    public static final PartAbility INPUT_PIGMENT   = new PartAbility("input_pigment");
    public static final PartAbility OUTPUT_PIGMENT  = new PartAbility("output_pigment");
    public static final PartAbility INPUT_SLURRY    = new PartAbility("input_slurry");
    public static final PartAbility OUTPUT_SLURRY   = new PartAbility("output_slurry");

    private ChemicalPartAbilities() {}

    /** IO と chemical type から対応 PartAbility を取得. */
    public static PartAbility get(com.gregtechceu.gtceu.api.capability.recipe.IO io,
                                    ChemicalIngredient.Type type) {
        boolean input = io == com.gregtechceu.gtceu.api.capability.recipe.IO.IN;
        switch (type) {
            case GAS:      return input ? INPUT_GAS      : OUTPUT_GAS;
            case INFUSION: return input ? INPUT_INFUSION : OUTPUT_INFUSION;
            case PIGMENT:  return input ? INPUT_PIGMENT  : OUTPUT_PIGMENT;
            case SLURRY:   return input ? INPUT_SLURRY   : OUTPUT_SLURRY;
        }
        throw new IllegalStateException("Unknown type: " + type);
    }
}
