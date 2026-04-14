package DIV.gtcsolo.machine;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class FECMachine extends CoilWorkableElectricMultiblockMachine {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 内部プラズマストレージ: fluidId -> 残量(mb) */
    private final Map<String, Long> plasmaStorage = new HashMap<>();

    /** 直近レシピのOC回数（消費計算用） */
    private int lastOcCount = 0;

    /** プラズマ取り込み用ティックサブスクリプション */
    private com.gregtechceu.gtceu.api.machine.TickableSubscription plasmaTickSub;
    private int plasmaTickOffset = 0;

    public FECMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    // =========================================================================
    //  ライフサイクル: プラズマ取り込みをティック駆動に
    // =========================================================================

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        if (plasmaTickSub == null) {
            plasmaTickSub = subscribeServerTick(this::onPlasmaCheck);
        }
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        if (plasmaTickSub != null) {
            unsubscribe(plasmaTickSub);
            plasmaTickSub = null;
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (plasmaTickSub != null) {
            unsubscribe(plasmaTickSub);
            plasmaTickSub = null;
        }
    }

    /** 1秒(20tick)ごとにプラズマを取り込む */
    private void onPlasmaCheck() {
        if (!isFormed()) return;
        if (++plasmaTickOffset % 20 != 0) return;
        intakePlasma();
    }

    // =========================================================================
    //  プラズマ判定: gtceu MODIDかつ末尾が _plasma
    // =========================================================================

    private static boolean isPlasma(FluidStack fluid) {
        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(fluid.getFluid());
        if (id == null) return false;
        return id.getNamespace().equals("gtceu") && id.getPath().endsWith("_plasma");
    }

    // =========================================================================
    //  プラズマ取り込み（レシピ開始時）
    // =========================================================================

    public void intakePlasma() {
        for (var part : getParts()) {
            for (var handler : part.getRecipeHandlers()) {
                if (handler.getHandlerIO() != IO.IN) continue;
                if (!(handler instanceof NotifiableFluidTank tank)) continue;
                for (int i = 0; i < tank.getTanks(); i++) {
                    FluidStack contained = tank.getFluidInTank(i);
                    if (contained.isEmpty() || !isPlasma(contained)) continue;

                    String fluidId = ForgeRegistries.FLUIDS.getKey(contained.getFluid()).toString();
                    int intakeAmount = (contained.getAmount() / 2000) * 2000;
                    if (intakeAmount <= 0) continue;

                    tank.drain(new FluidStack(contained.getFluid(), intakeAmount),
                            net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                    plasmaStorage.merge(fluidId, (long) intakeAmount, Long::sum);

                    LOGGER.info("[FEC] Plasma intake: {} {}mb (stored: {}mb)",
                            fluidId, intakeAmount, plasmaStorage.get(fluidId));
                }
            }
        }
    }

    // =========================================================================
    //  プラズマ種類数
    // =========================================================================

    public int getPlasmaTypeCount() {
        return plasmaStorage.size();
    }

    // =========================================================================
    //  プラズマ消費（レシピ完了時）
    // =========================================================================

    public void consumePlasma(int ocCount) {
        if (plasmaStorage.isEmpty() || ocCount <= 0) return;

        long consumePerType = (long) ocCount * 2L;
        Iterator<Map.Entry<String, Long>> it = plasmaStorage.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            long remaining = entry.getValue() - consumePerType;
            if (remaining <= 0) {
                LOGGER.info("[FEC] Plasma depleted: {}", entry.getKey());
                it.remove();
            } else {
                entry.setValue(remaining);
            }
        }
        LOGGER.info("[FEC] Plasma consumed: {}mb/type, {} types remaining",
                consumePerType, plasmaStorage.size());
    }

    // =========================================================================
    //  レシピ完了フック
    // =========================================================================

    @Override
    public void afterWorking() {
        super.afterWorking();
        consumePlasma(lastOcCount);
    }

    // =========================================================================
    //  レシピモディファイア
    //  signature: (MetaMachine, GTRecipe) -> ModifierFunction
    //  = RecipeModifier functional interface
    // =========================================================================

    public static ModifierFunction fecOverclock(MetaMachine machine, @Nonnull GTRecipe recipe) {
        if (!(machine instanceof FECMachine fec)) {
            return ModifierFunction.IDENTITY;
        }

        // プラズマ取り込みはティック駆動（onPlasmaCheck）で行う。
        // ここでは現在のストレージ状態のみ参照する（副作用なし）。
        int plasmaTypes = fec.getPlasmaTypeCount();

        int recipeTier = RecipeHelper.getRecipeEUtTier(recipe);
        int machineTier = fec.getTier();

        // OC回数
        int totalOcLevels = Math.max(0, machineTier - recipeTier);
        // PERFECT昇格数 = min(プラズマ種類数, OC回数)
        int perfectLevels = Math.min(plasmaTypes, totalOcLevels);
        int nonPerfectLevels = totalOcLevels - perfectLevels;

        fec.lastOcCount = totalOcLevels;

        // OC倍率計算
        double eutMultiplier = Math.pow(4.0, totalOcLevels);
        double durationMultiplier = Math.pow(0.25, perfectLevels) * Math.pow(0.5, nonPerfectLevels);

        LOGGER.info("[FEC] OC applied: {} total ({} PERFECT, {} NON_PERFECT), plasma types: {}",
                totalOcLevels, perfectLevels, nonPerfectLevels, plasmaTypes);

        return ModifierFunction.builder()
                .eutMultiplier(eutMultiplier)
                .durationMultiplier(durationMultiplier)
                .build();
    }

    // =========================================================================
    //  NBT永続化
    // =========================================================================

    @Override
    public void saveCustomPersistedData(@Nonnull CompoundTag tag, boolean forDrop) {
        super.saveCustomPersistedData(tag, forDrop);
        CompoundTag plasmaTag = new CompoundTag();
        plasmaStorage.forEach((id, amount) -> plasmaTag.putLong(id, amount));
        tag.put("fec_plasma", plasmaTag);
    }

    @Override
    public void loadCustomPersistedData(@Nonnull CompoundTag tag) {
        super.loadCustomPersistedData(tag);
        plasmaStorage.clear();
        if (tag.contains("fec_plasma")) {
            CompoundTag plasmaTag = tag.getCompound("fec_plasma");
            for (String key : plasmaTag.getAllKeys()) {
                plasmaStorage.put(key, plasmaTag.getLong(key));
            }
        }
    }
}