package DIV.gtcsolo.machine.overpower;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IDropSaveMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableEnergyContainer;
import com.gregtechceu.gtceu.common.machine.multiblock.part.EnergyHatchPartMachine;

/**
 * OverPower エネルギー出力ハッチ (output only)。
 *
 * <ul>
 *   <li>容量: {@link Long#MAX_VALUE} EU (= 約 9.22 × 10^18 = 922 京)</li>
 *   <li>搬出 EU/t: 容量と同じ (= 1 tick で全量放出可能、 接続電線の受入で律速)</li>
 *   <li>tier: MAX (= UI / casing 表示用)</li>
 * </ul>
 */
public class OverPowerEnergyHatchMachine extends EnergyHatchPartMachine implements IDropSaveMachine {

    public static final long OP_CAPACITY = Long.MAX_VALUE;       // ≈ 9.22 × 10^18 = 922 京
    public static final long OP_VOLTAGE = Long.MAX_VALUE;        // 搬出 EU/t (= 容量同等)
    public static final int OP_AMPERAGE = 1;

    public OverPowerEnergyHatchMachine(IMachineBlockEntity holder, Object... args) {
        // 必ず OUT、 tier = MAX (= UI/casing 表示用、 機能上は overrideContainer で完全独自値)
        super(holder, GTValues.MAX, IO.OUT, OP_AMPERAGE, args);
    }

    @Override
    protected NotifiableEnergyContainer createEnergyContainer(Object... args) {
        NotifiableEnergyContainer container = NotifiableEnergyContainer.emitterContainer(
                this, OP_CAPACITY, OP_VOLTAGE, OP_AMPERAGE);
        container.setSideOutputCondition(s -> s == getFrontFacing() && isWorkingEnabled());
        container.setCapabilityValidator(s -> s == null || s == getFrontFacing());
        return container;
    }
}
