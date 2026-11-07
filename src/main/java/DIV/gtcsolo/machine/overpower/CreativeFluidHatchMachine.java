package DIV.gtcsolo.machine.overpower;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.PhantomFluidWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IDropSaveMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.transfer.fluid.CustomFluidTank;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

/**
 * Creative 流体ハッチ (Input or Output)。
 *
 * <ul>
 *   <li>10×10 = 100 ファントムタンク (= 実態無し、 設定した流体を「あるように見せかけて」 渡す)</li>
 *   <li><b>Input</b>: 各タンクに流体登録 → マルチブロック要求時に無限供給</li>
 *   <li><b>Output</b>: 挿入された流体を void (= creative sink)</li>
 *   <li>UI: PhantomFluidWidget 10×10 grid、 JEI ドラッグ + バケット右クリック登録対応</li>
 * </ul>
 */
public class CreativeFluidHatchMachine extends FluidHatchPartMachine implements IDropSaveMachine {

    public static final int TANK_COUNT = 100;

    public CreativeFluidHatchMachine(IMachineBlockEntity holder, IO io, Object... args) {
        super(holder, GTValues.MAX, io, 1000, TANK_COUNT, args);
    }

    @Override
    protected NotifiableFluidTank createTank(int initialCapacity, int slots, Object... args) {
        return new CreativeFluidRecipeHandler(this, slots, io);
    }

    public static class CreativeFluidRecipeHandler extends NotifiableFluidTank {
        public CreativeFluidRecipeHandler(com.gregtechceu.gtceu.api.machine.MetaMachine machine, int slots, IO io) {
            // The list constructor wires content-change listeners onto the actual phantom tanks.
            super(machine, createStorages(slots, io), io);
        }

        private static java.util.List<CustomFluidTank> createStorages(int slots, IO io) {
            java.util.List<CustomFluidTank> storages = new java.util.ArrayList<>(slots);
            for (int i = 0; i < slots; i++) storages.add(new CreativeFluidStorage(1000, io == IO.IN));
            return storages;
        }

        @Override
        public java.util.List<com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient> handleRecipeInner(
                IO io, com.gregtechceu.gtceu.api.recipe.GTRecipe recipe,
                java.util.List<com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient> left,
                @org.jetbrains.annotations.Nullable String slotName, boolean simulate) {
            if (io != handlerIO) return left;
            if (io != IO.IN && io != IO.OUT) return left.isEmpty() ? null : left;
            return HatchRecipeTransfers.creativeFluids(left, storages, io == IO.IN);
        }

        @Override
        public java.util.List<Object> getContents() {
            return handlerIO == IO.IN ? HatchRecipeTransfers.virtualFluids(storages) : super.getContents();
        }
    }

    /** Creative: 破壊時に流体内容物を spill せず、 NBT で保持 */
    @Override
    public void onMachineRemoved() {
        // skip
    }

    @Override
    public Widget createUIWidget() {
        int rowSize = 10;
        int colSize = 10;
        var group = new WidgetGroup(0, 0, 18 * rowSize + 16, 18 * colSize + 16);
        var container = new WidgetGroup(4, 4, 18 * rowSize + 8, 18 * colSize + 8);
        int index = 0;
        for (int y = 0; y < colSize; y++) {
            for (int x = 0; x < rowSize; x++) {
                CustomFluidTank storage = tank.getStorages()[index];
                final int idx = index;
                container.addWidget(new PhantomFluidWidget(tank, idx,
                        4 + x * 18, 4 + y * 18, 18, 18,
                        () -> storage.getFluid(),
                        f -> {
                            if (f == null || f.isEmpty()) {
                                storage.setFluid(FluidStack.EMPTY);
                            } else {
                                FluidStack one = f.copy();
                                one.setAmount(1);
                                storage.setFluid(one);
                            }
                        })
                        .setShowAmount(false)
                        .setBackground(GuiTextures.FLUID_SLOT));
                index++;
            }
        }
        group.addWidget(container);
        return group;
    }

    /**
     * ファントムタンク。
     * <ul>
     *   <li>各タンクは {@code amount=1} のファントム流体を保持 (= 設定登録のみ)</li>
     *   <li>IN: drain は要求量を copy で返す (= 無限供給)、 登録はファントムUIのみ</li>
     *   <li>OUT: fill は何でも受け取り void、 drain はゼロ</li>
     * </ul>
     */
    public static class CreativeFluidStorage extends CustomFluidTank implements DIV.gtcsolo.api.transfer.InfiniteInputSupply {
        private final boolean isInput;

        @Override public boolean isInfiniteInput() { return isInput; }

        public CreativeFluidStorage(int capacity, boolean isInput) {
            super(capacity);
            this.isInput = isInput;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!isInput) {
                // output: void everything, accept full amount
                return resource == null ? 0 : resource.getAmount();
            }
            // Refusing a pipe fill must not mutate every phantom while reporting zero accepted.
            // PhantomFluidWidget registers the chosen fluid directly with setFluid.
            return 0;
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0) return FluidStack.EMPTY;
            if (!isInput) return FluidStack.EMPTY;
            FluidStack stored = getFluid();
            if (stored.isEmpty()) return FluidStack.EMPTY;
            FluidStack result = stored.copy();
            result.setAmount(maxDrain);
            return result;
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource == null || resource.isEmpty()) return FluidStack.EMPTY;
            if (!isInput) return FluidStack.EMPTY;
            FluidStack stored = getFluid();
            if (stored.isEmpty() || !stored.isFluidEqual(resource)) return FluidStack.EMPTY;
            FluidStack result = stored.copy();
            result.setAmount(resource.getAmount());
            return result;
        }

        @Override
        public boolean isFluidValid(@NotNull FluidStack stack) {
            return true;
        }

        @Override
        public int getCapacity() {
            return 1;  // phantom: 表示上 1 mB
        }
    }
}
