package DIV.gtcsolo.machine;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gregtechceu.gtceu.common.data.GTMachines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;

public class CCMachine extends WorkableElectricMultiblockMachine {

    private static final int AIR_PER_UNIT = 100000; // 10万mb = 1単位
    private static final int BASE_PARALLELS = 4;

    /** 現在の並列数 */
    private int currentParallels = BASE_PARALLELS;

    /** 初回レシピ稼働済みフラグ（1MC日サイクル内） */
    private boolean consumedThisCycle = false;

    /** tick カウンター（消費サイクル管理用） */
    private int tickCounter = 0;

    private com.gregtechceu.gtceu.api.machine.TickableSubscription tickSub;

    public CCMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    // =========================================================================
    //  Air判定: レジストリパスが "air" なら全MOD許容
    // =========================================================================

    private static boolean isAirFluid(FluidStack fluid) {
        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(fluid.getFluid());
        if (id == null) return false;
        String path = id.getPath();
        // "air" 完全一致、または末尾が "_air"（他MOD対応）。 ただし "liquid_air" 等は除外
        return path.equals("air") || (path.endsWith("_air") && !path.contains("liquid"));
    }

    // =========================================================================
    //  ライフサイクル
    // =========================================================================

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        tickCounter = 0;
        consumedThisCycle = false;
        if (tickSub == null) {
            tickSub = subscribeServerTick(this::onServerTick);
        }
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        if (tickSub != null) {
            unsubscribe(tickSub);
            tickSub = null;
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (tickSub != null) {
            unsubscribe(tickSub);
            tickSub = null;
        }
    }

    // =========================================================================
    //  サーバーTick: 1MC日サイクルのリセットのみ
    // =========================================================================

    private void onServerTick() {
        if (!isFormed()) return;

        tickCounter++;
        if (tickCounter >= 24000) {
            tickCounter = 0;
            consumedThisCycle = false; // 次のレシピ開始で再消費可能
        }
    }

    // =========================================================================
    //  レシピ開始時に呼ばれる空気消費
    // =========================================================================

    private void tryConsumeAirOnRecipeStart() {
        if (consumedThisCycle) return;
        consumedThisCycle = true;
        performAirConsumption();
    }

    // =========================================================================
    //  空気消費 & 並列計算
    // =========================================================================

    private void performAirConsumption() {
        // 1. EV(tier 4)液体搬入バスの個数を数える (GTMachines.FLUID_IMPORT_HATCH[EV] のブロックと直接比較)
        Block evFluidHatchBlock = GTMachines.FLUID_IMPORT_HATCH[GTValues.EV].getBlock();
        int evHatchCount = 0;
        for (var part : getParts()) {
            if (part instanceof MetaMachine mm) {
                if (mm.getBlockState().getBlock() == evFluidHatchBlock) {
                    evHatchCount++;
                }
            }
        }

        if (evHatchCount <= 0) {
            currentParallels = BASE_PARALLELS;
            return;
        }

        // 2. 必要な空気量 = EVハッチ数 × 10万mb
        long totalAirNeeded = (long) evHatchCount * AIR_PER_UNIT;
        long totalAirConsumed = 0;

        // 3. 全液体入力ハッチからAir流体を消費（全MODの "air" を許容）
        for (var part : getParts()) {
            for (var handler : part.getRecipeHandlers()) {
                if (handler.getHandlerIO() != IO.IN) continue;
                if (!(handler instanceof NotifiableFluidTank tank)) continue;
                for (int i = 0; i < tank.getTanks(); i++) {
                    FluidStack contained = tank.getFluidInTank(i);
                    if (contained.isEmpty()) continue;
                    if (!isAirFluid(contained)) continue;

                    long stillNeeded = totalAirNeeded - totalAirConsumed;
                    if (stillNeeded <= 0) break;

                    int canDrain = (int) Math.min(contained.getAmount(), stillNeeded);
                    FluidStack drained = tank.drainInternal(
                            new FluidStack(contained.getFluid(), canDrain),
                            net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                    totalAirConsumed += drained.getAmount();
                }
            }
            if (totalAirConsumed >= totalAirNeeded) break;
        }

        // 4. 消費した単位数から並列計算: 4 + 8 * 1.5^n (int オーバーフロー防止で飽和)
        int n = (int) (totalAirConsumed / AIR_PER_UNIT);
        double bonus = 8 * Math.pow(1.5, n);
        currentParallels = (int) Math.min((double) BASE_PARALLELS + bonus, Integer.MAX_VALUE);
    }

    // =========================================================================
    //  並列数アクセス
    // =========================================================================

    public int getCurrentParallels() {
        return currentParallels;
    }

    // =========================================================================
    //  レシピモディファイア（レシピ開始時に消費を実行）
    // =========================================================================

    public static ModifierFunction ccParallel(MetaMachine machine, @Nonnull GTRecipe recipe) {
        if (!(machine instanceof CCMachine cc)) {
            return ModifierFunction.IDENTITY;
        }

        cc.tryConsumeAirOnRecipeStart();

        int maxParallels = cc.getCurrentParallels();
        if (maxParallels <= 1) {
            return ModifierFunction.IDENTITY;
        }

        // 入力材料から実際に可能な並列数を計算
        int actualParallels = ParallelLogic.getParallelAmount(machine, recipe, maxParallels);
        if (actualParallels <= 0) {
            return ModifierFunction.NULL;
        }

        // 入出力をN倍に + EU/tもN倍に（マルチスメルター方式）
        ModifierFunction contentMultiplier = ModifierFunction.builder()
                .modifyAllContents(ContentModifier.multiplier(actualParallels))
                .parallels(actualParallels)
                .build();

        // OCをその上に適用
        ModifierFunction ocModifier = OverclockingLogic.PERFECT_OVERCLOCK
                .getModifier(machine, recipe, cc.getOverclockVoltage());

        return contentMultiplier.andThen(ocModifier);
    }

    // =========================================================================
    //  NBT永続化
    // =========================================================================

    @Override
    public void saveCustomPersistedData(@Nonnull CompoundTag tag, boolean forDrop) {
        super.saveCustomPersistedData(tag, forDrop);
        tag.putInt("cc_parallels", currentParallels);
        tag.putInt("cc_tick_counter", tickCounter);
        tag.putBoolean("cc_consumed", consumedThisCycle);
    }

    @Override
    public void loadCustomPersistedData(@Nonnull CompoundTag tag) {
        super.loadCustomPersistedData(tag);
        currentParallels = tag.getInt("cc_parallels");
        tickCounter = tag.getInt("cc_tick_counter");
        consumedThisCycle = tag.getBoolean("cc_consumed");
        if (currentParallels < BASE_PARALLELS) currentParallels = BASE_PARALLELS;
    }
}
