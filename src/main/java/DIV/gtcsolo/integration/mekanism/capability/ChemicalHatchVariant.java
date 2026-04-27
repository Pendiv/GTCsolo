package DIV.gtcsolo.integration.mekanism.capability;

import java.util.List;

public enum ChemicalHatchVariant {
    GAS(List.of(ChemicalIngredient.Type.GAS), 64_000L, "gas"),
    INFUSION(List.of(ChemicalIngredient.Type.INFUSION), 16_000L, "infusion"),
    OTHER(List.of(ChemicalIngredient.Type.PIGMENT, ChemicalIngredient.Type.SLURRY), 64_000L, "other");

    public final List<ChemicalIngredient.Type> types;
    public final long baseCapacity;
    public final String key;

    ChemicalHatchVariant(List<ChemicalIngredient.Type> types, long baseCapacity, String key) {
        this.types = types;
        this.baseCapacity = baseCapacity;
        this.key = key;
    }
}