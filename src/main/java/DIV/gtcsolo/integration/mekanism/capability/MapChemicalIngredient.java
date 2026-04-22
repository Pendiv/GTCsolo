package DIV.gtcsolo.integration.mekanism.capability;

import com.gregtechceu.gtceu.api.recipe.lookup.AbstractMapIngredient;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * ChemicalIngredient をレシピ検索時のキーとして扱うためのラッパ.
 * (type, id) のみで同一判定 (amount は含まない).
 */
public final class MapChemicalIngredient extends AbstractMapIngredient {

    private final ChemicalIngredient.Type type;
    private final ResourceLocation id;

    public MapChemicalIngredient(ChemicalIngredient.Type type, ResourceLocation id) {
        this.type = type;
        this.id = id;
    }

    public MapChemicalIngredient(ChemicalIngredient ingredient) {
        this(ingredient.getType(), ingredient.getId());
    }

    @Override
    protected int hash() {
        return Objects.hash(objClass, type, id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (!(obj instanceof MapChemicalIngredient other)) return false;
        return this.type == other.type && this.id.equals(other.id);
    }
}