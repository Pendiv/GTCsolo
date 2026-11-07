package DIV.gtcsolo.machine;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

public class UpgradeHatchMachine extends TieredPartMachine {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            UpgradeHatchMachine.class, MultiblockPartMachine.MANAGED_FIELD_HOLDER);

    private final int tierIndex;

    @Persisted
    private final NotifiableItemStackHandler inventory;

    public UpgradeHatchMachine(IMachineBlockEntity holder, int voltageTier, int tierIndex) {
        super(holder, voltageTier);
        this.tierIndex = tierIndex;
        int slots = (tierIndex + 1) * (tierIndex + 1);
        this.inventory = new NotifiableItemStackHandler(this, slots, IO.BOTH, IO.BOTH);
    }

    public int getTierIndex() {
        return tierIndex;
    }

    public NotifiableItemStackHandler getInventory() {
        return inventory;
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public Widget createUIWidget() {
        int side = tierIndex + 1;
        int guiW = side * 18 + 16;
        int guiH = side * 18 + 16;
        WidgetGroup group = new WidgetGroup(0, 0, guiW, guiH);
        for (int y = 0; y < side; y++) {
            for (int x = 0; x < side; x++) {
                int index = y * side + x;
                group.addWidget(new SlotWidget(inventory.storage, index,
                        8 + x * 18, 8 + y * 18, true, true));
            }
        }
        group.setBackground(GuiTextures.BACKGROUND_INVERSE);
        return group;
    }

    public static UpgradeHatchMachine create(IMachineBlockEntity holder, int voltageTier, int tierIndex) {
        return new UpgradeHatchMachine(holder, voltageTier, tierIndex);
    }
}
