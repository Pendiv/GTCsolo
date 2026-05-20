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
        NotifiableFluidTank tank = new NotifiableFluidTank(this, slots, 1000, io);
        // 各 storage を CreativeFluidStorage に差し替える
        // 注: NotifiableFluidTank.getStorages() の中身をいじりたいが、 storages は final 配列なので
        //     replaceTankStorage で個別差し替えする経路がない。
        //     代わりに NotifiableFluidTank をそのまま使い、 fill/drain は CreativeFluidStorage 経由にする。
        for (int i = 0; i < slots; i++) {
            tank.getStorages()[i] = new CreativeFluidStorage(1000, io == IO.IN);
        }
        return tank;
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
     *   <li>IN: drain は要求量を copy で返す (= 無限供給)、 fill は登録 (count=1)</li>
     *   <li>OUT: fill は何でも受け取り void、 drain はゼロ</li>
     * </ul>
     */
    public static class CreativeFluidStorage extends CustomFluidTank {
        private final boolean isInput;

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
            // input: register phantom (= amount=1 で保持)
            if (resource == null || resource.isEmpty()) return 0;
            if (action.execute()) {
                FluidStack one = resource.copy();
                one.setAmount(1);
                setFluid(one);
            }
            return 0;  // 実際にはアイテム消費なし (= プレイヤー手中の流体は減らない)
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            if (!isInput) return FluidStack.EMPTY;
            FluidStack stored = getFluid();
            if (stored.isEmpty()) return FluidStack.EMPTY;
            FluidStack result = stored.copy();
            result.setAmount(maxDrain);
            return result;
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
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
