package DIV.gtcsolo.machine.overpower;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.PhantomSlotWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IDropSaveMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.common.machine.multiblock.part.ItemBusPartMachine;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Creative アイテムハッチ (Input or Output)。
 *
 * <ul>
 *   <li>10×10 = 100 ファントムスロット (= 実態無し、 設定したアイテムを「あるように見せかけて」 渡す)</li>
 *   <li><b>Input</b>: 各スロットにアイテム登録 → マルチブロック要求時に無限供給</li>
 *   <li><b>Output</b>: 挿入されたアイテムを void (= creative sink)</li>
 *   <li>UI: 各スロット PhantomSlotWidget で JEI ドラッグ + インベントリ右クリック登録対応</li>
 * </ul>
 *
 * <p>処理軽量化: extract のたびに count を copy するだけで実 inventory 操作なし。
 */
public class CreativeItemHatchMachine extends ItemBusPartMachine implements IDropSaveMachine {

    public static final int SLOT_COUNT = 100;

    public CreativeItemHatchMachine(IMachineBlockEntity holder, IO io, Object... args) {
        super(holder, GTValues.MAX, io, args);
    }

    @Override
    protected int getInventorySize() {
        return SLOT_COUNT;
    }

    @Override
    protected NotifiableItemStackHandler createInventory(Object... args) {
        return new NotifiableItemStackHandler(this, SLOT_COUNT, io, io,
                size -> new CreativeItemStorage(size, io == IO.IN));
    }

    /** OverPower 同様、 破壊時はインベントリ spill なし (= phantom 設定を NBT で保持) */
    @Override
    public void onMachineRemoved() {
        // skip clearInventory()
    }

    @Override
    public Widget createUIWidget() {
        // 10×10 grid of PhantomSlotWidget — drag JEI / inventory で登録、 右クリックでクリア
        int rowSize = 10;
        int colSize = 10;
        var group = new WidgetGroup(0, 0, 18 * rowSize + 16, 18 * colSize + 16);
        var container = new WidgetGroup(4, 4, 18 * rowSize + 8, 18 * colSize + 8);
        int index = 0;
        for (int y = 0; y < colSize; y++) {
            for (int x = 0; x < rowSize; x++) {
                container.addWidget(new PhantomSlotWidget(getInventory().storage, index++,
                        4 + x * 18, 4 + y * 18)
                        .setClearSlotOnRightClick(true)
                        .setMaxStackSize(1)
                        .setBackgroundTexture(GuiTextures.SLOT));
            }
        }
        container.setBackground(GuiTextures.BACKGROUND_INVERSE);
        group.addWidget(container);
        return group;
    }

    /**
     * ファントムストレージ。
     * <ul>
     *   <li>各スロットは {@code count=1} のファントムアイテムを保持 (= UI 表示用)</li>
     *   <li>IN モード: extractItem は要求量を copy で返す (= 無限供給)、 setStackInSlot で登録</li>
     *   <li>OUT モード: insertItem は何でも受け取り void、 extract はゼロ (= 無限 sink)</li>
     * </ul>
     */
    public static class CreativeItemStorage extends CustomItemStackHandler {
        private final boolean isInput;

        public CreativeItemStorage(int size, boolean isInput) {
            super(size);
            this.isInput = isInput;
        }

        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            // 常に count=1 で正規化 (= phantom)
            if (stack.isEmpty()) {
                super.setStackInSlot(slot, ItemStack.EMPTY);
            } else {
                super.setStackInSlot(slot, stack.copyWithCount(1));
            }
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!isInput) return ItemStack.EMPTY;  // output: nothing to extract
            ItemStack phantom = stacks.get(slot);  // 直接アクセス (= count=1 raw)
            if (phantom.isEmpty()) return ItemStack.EMPTY;
            return phantom.copyWithCount(amount);  // 要求量を返す、 実態は減らない
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (!isInput) {
                // output: void everything
                return ItemStack.EMPTY;
            }
            // input: phantom 登録は基本 setStackInSlot 経由 (= PhantomSlotWidget が呼ぶ)
            // 直接 insert は拒否してプレイヤー手中に残す
            return stack;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return true;
        }
    }
}
