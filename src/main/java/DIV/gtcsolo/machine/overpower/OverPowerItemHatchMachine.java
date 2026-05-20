package DIV.gtcsolo.machine.overpower;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IDropSaveMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.common.machine.multiblock.part.ItemBusPartMachine;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * OverPower アイテムハッチ (Item Input or Output)。
 *
 * <ul>
 *   <li>スロット: 10×10 = 100</li>
 *   <li>各スロットのスタック上限: {@link Integer#MAX_VALUE} (= 2,147,483,647)</li>
 *   <li>tier: UEV (= UI が 10×10 になる便宜上の値、 機能上 tier は無関係)</li>
 *   <li>UI: GT ItemBusPartMachine.createUIWidget をそのまま継承 (10×10 grid)</li>
 * </ul>
 *
 * <p>auto IO の throughput は GT デフォルト挙動 (= 毎 tick 隣接インベントリに可能な限り移動) を流用。
 * 実効レートは隣接インベントリの受入能力で律速される。
 */
public class OverPowerItemHatchMachine extends ItemBusPartMachine implements IDropSaveMachine {

    public static final int SLOT_COUNT = 100;       // 10 x 10
    public static final int SLOT_LIMIT = Integer.MAX_VALUE;

    public OverPowerItemHatchMachine(IMachineBlockEntity holder, IO io, Object... args) {
        // tier=UEV (9) で UI が 10×10 になる (1 + min(9, 9) = 10)
        super(holder, GTValues.UEV, io, args);
    }

    @Override
    protected int getInventorySize() {
        return SLOT_COUNT;
    }

    @Override
    protected NotifiableItemStackHandler createInventory(Object... args) {
        return new NotifiableItemStackHandler(this, SLOT_COUNT, io, io,
                size -> new OverPowerItemStorage(size));
    }

    /**
     * GT 標準では破壊時にインベントリをアイテムエンティティとして spawn する (clearInventory) が、
     * OverPower では NBT で内容物を保持して dropped item に同梱するため spill しない。
     */
    @Override
    public void onMachineRemoved() {
        // do not call super.onMachineRemoved() — skip clearInventory()
        // contents are preserved in dropped ItemStack NBT via IDropSaveMachine
    }

    /** Stack 上限を {@link Integer#MAX_VALUE} に上書きした CustomItemStackHandler */
    public static class OverPowerItemStorage extends CustomItemStackHandler {
        public OverPowerItemStorage(int size) {
            super(size);
        }

        @Override
        public int getSlotLimit(int slot) {
            return SLOT_LIMIT;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return true;
        }
    }
}
