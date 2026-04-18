package DIV.gtcsolo.integration.jei;

import DIV.gtcsolo.integration.mekanism.ChemicalBridge;
import net.minecraftforge.fluids.FluidStack;

/**
 * JEI表示用データクラス。GT液体 ↔ Mek Chemical の対応を保持する。
 */
public record ChemicalConversionRecipe(
        FluidStack fluid,
        String chemKey,
        ChemicalBridge.ChemType chemType,
        String materialName
) {}