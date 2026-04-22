package DIV.gtcsolo.integration.mekanism.capability;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mekanism.api.MekanismAPI;
import org.slf4j.Logger;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.infuse.InfuseType;
import mekanism.api.chemical.infuse.InfusionStack;
import mekanism.api.chemical.pigment.Pigment;
import mekanism.api.chemical.pigment.PigmentStack;
import mekanism.api.chemical.slurry.Slurry;
import mekanism.api.chemical.slurry.SlurryStack;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * GTCEu recipe system で Mekanism chemical を第一級ingredient として扱うためのデータ保持クラス.
 *
 * 4つの chemical type (GAS/INFUSION/PIGMENT/SLURRY) を 1 つのクラスで扱い、
 * type field で識別する。単純な (type, id, amount) の 3 つ組。
 */
public final class ChemicalIngredient {

    private static final Logger LOGGER = LogUtils.getLogger();

    public enum Type {
        GAS, INFUSION, PIGMENT, SLURRY;

        public String lowerName() {
            return name().toLowerCase();
        }
    }

    public static final Codec<ChemicalIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(s -> Type.valueOf(s.toUpperCase()), Type::lowerName)
                    .fieldOf("type").forGetter(ChemicalIngredient::getType),
            ResourceLocation.CODEC.fieldOf("id").forGetter(ChemicalIngredient::getId),
            Codec.LONG.fieldOf("amount").forGetter(ChemicalIngredient::getAmount)
    ).apply(instance, ChemicalIngredient::new));

    private final Type type;
    private final ResourceLocation id;
    private long amount;

    public ChemicalIngredient(Type type, ResourceLocation id, long amount) {
        this.type = type;
        this.id = id;
        this.amount = amount;
    }

    public static ChemicalIngredient of(Type type, String chemId, long amount) {
        return new ChemicalIngredient(type, new ResourceLocation(chemId), amount);
    }

    public static ChemicalIngredient empty(Type type) {
        return new ChemicalIngredient(type, new ResourceLocation("minecraft:empty"), 0L);
    }

    // ---- type-specific factory helpers (Java recipe書き用) ----
    public static ChemicalIngredient gas(String id, long amount) {
        return new ChemicalIngredient(Type.GAS, new ResourceLocation(id), amount);
    }
    public static ChemicalIngredient infusion(String id, long amount) {
        return new ChemicalIngredient(Type.INFUSION, new ResourceLocation(id), amount);
    }
    public static ChemicalIngredient pigment(String id, long amount) {
        return new ChemicalIngredient(Type.PIGMENT, new ResourceLocation(id), amount);
    }
    public static ChemicalIngredient slurry(String id, long amount) {
        return new ChemicalIngredient(Type.SLURRY, new ResourceLocation(id), amount);
    }

    public Type getType() { return type; }
    public ResourceLocation getId() { return id; }
    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }

    public boolean isEmpty() {
        return amount <= 0L;
    }

    public ChemicalIngredient copy() {
        return new ChemicalIngredient(type, id, amount);
    }

    /** Mek registry から Chemical 本体を引く. 見つからなければ null. */
    public Chemical<?> resolve() {
        Chemical<?> chem;
        switch (type) {
            case GAS:      chem = MekanismAPI.gasRegistry().getValue(id); break;
            case INFUSION: chem = MekanismAPI.infuseTypeRegistry().getValue(id); break;
            case PIGMENT:  chem = MekanismAPI.pigmentRegistry().getValue(id); break;
            case SLURRY:   chem = MekanismAPI.slurryRegistry().getValue(id); break;
            default:       chem = null;
        }
        if (chem == null || chem.isEmptyType()) {
            LOGGER.warn("[ChemCap] resolve failed: {} not in Mek {} registry", id, type);
        }
        return chem;
    }

    /** type 固有の ChemicalStack を生成. empty なら EMPTY stack. */
    public ChemicalStack<?> toStack() {
        Chemical<?> chem = resolve();
        if (chem == null || chem.isEmptyType()) {
            return emptyStackFor(type);
        }
        switch (type) {
            case GAS:      return new GasStack((Gas) chem, amount);
            case INFUSION: return new InfusionStack((InfuseType) chem, amount);
            case PIGMENT:  return new PigmentStack((Pigment) chem, amount);
            case SLURRY:   return new SlurryStack((Slurry) chem, amount);
        }
        return GasStack.EMPTY;
    }

    public static ChemicalStack<?> emptyStackFor(Type type) {
        switch (type) {
            case GAS:      return GasStack.EMPTY;
            case INFUSION: return InfusionStack.EMPTY;
            case PIGMENT:  return PigmentStack.EMPTY;
            case SLURRY:   return SlurryStack.EMPTY;
        }
        return GasStack.EMPTY;
    }

    /** 外部 ChemicalStack との一致判定. */
    public boolean test(ChemicalStack<?> stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (!matchesType(stack)) return false;
        return stack.getTypeRegistryName().equals(id);
    }

    private boolean matchesType(ChemicalStack<?> stack) {
        switch (type) {
            case GAS:      return stack instanceof GasStack;
            case INFUSION: return stack instanceof InfusionStack;
            case PIGMENT:  return stack instanceof PigmentStack;
            case SLURRY:   return stack instanceof SlurryStack;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, id);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ChemicalIngredient other)) return false;
        return type == other.type && id.equals(other.id) && amount == other.amount;
    }

    @Override
    public String toString() {
        return type.lowerName() + ":" + id + " x" + amount;
    }
}