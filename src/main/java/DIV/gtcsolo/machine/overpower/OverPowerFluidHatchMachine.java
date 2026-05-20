package DIV.gtcsolo.machine.overpower;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IDropSaveMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;

/**
 * OverPower 流体ハッチ (Fluid Input or Output)。
 *
 * <ul>
 *   <li>スロット: 10×10 = 100 タンク</li>
 *   <li>各タンク容量: {@link Integer#MAX_VALUE} mB (= 2,147,483,647 mB)</li>
 *   <li>tier: UEV (= UI が 10×10 になる便宜上の値、 機能上 tier は無関係)</li>
 *   <li>UI: GT FluidHatchPartMachine.createMultiSlotGUI を継承 (slots=100 で自動 10×10)</li>
 * </ul>
 */
public class OverPowerFluidHatchMachine extends FluidHatchPartMachine implements IDropSaveMachine {

    public static final int TANK_COUNT = 100;       // 10 x 10
    public static final int TANK_CAPACITY = Integer.MAX_VALUE;

    public OverPowerFluidHatchMachine(IMachineBlockEntity holder, IO io, Object... args) {
        // initialCapacity は基底側で tier 倍率かかるが、 createTank() を override して無視するので任意。
        super(holder, GTValues.UEV, io, TANK_CAPACITY, TANK_COUNT, args);
    }

    @Override
    protected NotifiableFluidTank createTank(int initialCapacity, int slots, Object... args) {
        // tier 倍率を無視し、 各タンクに Integer.MAX_VALUE mB を直接付与
        return new NotifiableFluidTank(this, slots, TANK_CAPACITY, io);
    }

    /** OverPower: 破壊時に流体内容物を spill せず、 NBT で保持して dropped item に同梱 */
    @Override
    public void onMachineRemoved() {
        // do not call super.onMachineRemoved() — skip inventory spill
    }
}
