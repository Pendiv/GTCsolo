package DIV.gtcsolo.integration.mekanism.capability;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.trait.ICapabilityTrait;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableRecipeHandlerTrait;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.ChemicalTankBuilder;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.infuse.InfusionStack;
import mekanism.api.chemical.pigment.PigmentStack;
import mekanism.api.chemical.slurry.SlurryStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Mekanism chemical を GT レシピシステムに橋渡しするトレイト.
 *
 * 1 つのハッチは GAS/INFUSION/PIGMENT/SLURRY のいずれか 1 種類を扱う.
 * 型は constructor で固定し、内部で Mek の IChemicalTank インスタンスを生成する.
 *
 * GT recipe 側は ChemicalIngredient として扱い、
 * Mek 側は IGasHandler (他3種) として capability を露出する.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class NotifiableChemicalTank
        extends NotifiableRecipeHandlerTrait<ChemicalIngredient>
        implements ICapabilityTrait {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final ChemicalIngredient.Type type;
    private final long capacity;
    private final IO handlerIO;
    private final IO capabilityIO;

    /** Mek の IChemicalTank インスタンス. type により具象クラスは変わる. */
    private final IChemicalTank mekTank;

    public NotifiableChemicalTank(MetaMachine machine, ChemicalIngredient.Type type,
                                    long capacity, IO io) {
        super(machine);
        this.type = type;
        this.capacity = capacity;
        this.handlerIO = io;
        this.capabilityIO = io;
        this.mekTank = createTank(type, capacity, this::notifyListeners);
        LOGGER.debug("[ChemCap] Tank created type={} io={} cap={} machine={}",
                type, io, capacity, machine.getClass().getSimpleName());
    }

    private static IChemicalTank createTank(ChemicalIngredient.Type type, long capacity,
                                              Runnable onChange) {
        mekanism.api.functions.ConstantPredicates.class.hashCode(); // ensure loaded
        mekanism.api.IContentsListener listener = onChange::run;
        switch (type) {
            case GAS:      return ChemicalTankBuilder.GAS.create(capacity, listener);
            case INFUSION: return ChemicalTankBuilder.INFUSION.create(capacity, listener);
            case PIGMENT:  return ChemicalTankBuilder.PIGMENT.create(capacity, listener);
            case SLURRY:   return ChemicalTankBuilder.SLURRY.create(capacity, listener);
        }
        throw new IllegalStateException("Unknown chemical type: " + type);
    }

    public ChemicalIngredient.Type getType() { return type; }
    public long getCapacity() { return capacity; }
    public IChemicalTank getMekTank() { return mekTank; }

    @Override
    public IO getHandlerIO() { return handlerIO; }

    @Override
    public IO getCapabilityIO() { return capabilityIO; }

    @Override
    public RecipeCapability<ChemicalIngredient> getCapability() {
        switch (type) {
            case GAS:      return ChemicalCapabilities.GAS;
            case INFUSION: return ChemicalCapabilities.INFUSION;
            case PIGMENT:  return ChemicalCapabilities.PIGMENT;
            case SLURRY:   return ChemicalCapabilities.SLURRY;
        }
        throw new IllegalStateException();
    }

    @Override
    public List<ChemicalIngredient> handleRecipeInner(IO io, GTRecipe recipe,
                                                        List<ChemicalIngredient> left,
                                                        String slotName, boolean simulate) {
        LOGGER.debug("[ChemCap] Tank[{}/{}] handleRecipeInner sim={} recipe={} left.size={} stored={}/{} currentChem={}",
                type, handlerIO, simulate, recipe.id, left.size(),
                mekTank.getStored(), capacity,
                mekTank.isEmpty() ? "<empty>" : mekTank.getStack().getTypeRegistryName());
        if (left.isEmpty()) return null;
        Action action = simulate ? Action.SIMULATE : Action.EXECUTE;

        for (int i = 0; i < left.size(); i++) {
            ChemicalIngredient req = left.get(i);
            if (req.getType() != this.type) {
                LOGGER.debug("[ChemCap] Tank[{}/{}] skip wrong type: req={} tank={}",
                        type, handlerIO, req.getType(), this.type);
                continue;
            }
            if (req.isEmpty()) { left.remove(i--); continue; }

            if (io == IO.IN) {
                // レシピ入力: tank から抽出
                ChemicalStack<?> stored = mekTank.getStack();
                if (stored.isEmpty()) {
                    LOGGER.debug("[ChemCap] Tank[{}/{}] IN empty, skip", type, handlerIO);
                    continue;
                }
                if (!stored.getTypeRegistryName().equals(req.getId())) {
                    LOGGER.debug("[ChemCap] Tank[{}/{}] IN id mismatch: stored={} req={}",
                            type, handlerIO, stored.getTypeRegistryName(), req.getId());
                    continue;
                }
                long extracted = mekTank.extract(req.getAmount(), action,
                        AutomationType.INTERNAL).getAmount();
                LOGGER.debug("[ChemCap] Tank[{}/{}] IN extract req={} got={} sim={}",
                        type, handlerIO, req.getAmount(), extracted, simulate);
                if (extracted > 0) {
                    long remaining = req.getAmount() - extracted;
                    if (remaining <= 0) {
                        left.remove(i--);
                    } else {
                        left.set(i, new ChemicalIngredient(req.getType(), req.getId(), remaining));
                    }
                }
            } else if (io == IO.OUT) {
                // レシピ出力: tank に挿入
                ChemicalStack<?> inputStack = req.toStack();
                if (inputStack.isEmpty()) {
                    LOGGER.warn("[ChemCap] Tank[{}/{}] OUT req.toStack() is empty (unresolved chemical?) req={}",
                            type, handlerIO, req);
                    left.remove(i--);
                    continue;
                }
                long remainder = mekTank.insert(inputStack, action,
                        AutomationType.INTERNAL).getAmount();
                long inserted = req.getAmount() - remainder;
                LOGGER.debug("[ChemCap] Tank[{}/{}] OUT insert req={} inserted={} remain={} sim={}",
                        type, handlerIO, req.getAmount(), inserted, remainder, simulate);
                if (inserted > 0) {
                    if (remainder <= 0) {
                        left.remove(i--);
                    } else {
                        left.set(i, new ChemicalIngredient(req.getType(), req.getId(), remainder));
                    }
                }
            }
        }
        LOGGER.debug("[ChemCap] Tank[{}/{}] handleRecipeInner done leftOver={}",
                type, handlerIO, left);
        return left.isEmpty() ? null : left;
    }

    @Override
    public List<Object> getContents() {
        ChemicalStack<?> stored = mekTank.getStack();
        if (stored.isEmpty()) return new ArrayList<>();
        ChemicalIngredient ci = new ChemicalIngredient(type,
                stored.getTypeRegistryName(), stored.getAmount());
        List<Object> result = new ArrayList<>(1);
        result.add(ci);
        return result;
    }

    @Override
    public double getTotalContentAmount() {
        return mekTank.getStored();
    }

    /** 現在格納中の ChemicalStack を取得. 空なら type に応じた EMPTY stack. */
    public ChemicalStack<?> getStack() {
        ChemicalStack<?> s = mekTank.getStack();
        return s.isEmpty() ? ChemicalIngredient.emptyStackFor(type) : s;
    }

    /** 外部から直接挿入 (デバッグ/NBT復元用). */
    public long insert(ChemicalStack<?> stack, Action action) {
        return mekTank.insert((ChemicalStack) stack, action, AutomationType.INTERNAL).getAmount();
    }
}