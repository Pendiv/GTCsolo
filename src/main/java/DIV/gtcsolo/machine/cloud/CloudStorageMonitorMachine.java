package DIV.gtcsolo.machine.cloud;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class CloudStorageMonitorMachine extends MultiblockPartMachine {
    public CloudStorageMonitorMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public boolean canShared() {
        return false;
    }

    @Override
    public ModularUI createUI(Player player) {
        var root = new WidgetGroup(0, 0, 176, 127);
        root.setBackground(GuiTextures.BACKGROUND);
        root.addWidget(new ComponentPanelWidget(8, 7, List.of(getBlockState().getBlock().getName())));
        root.addWidget(new ComponentPanelWidget(8, 23, this::addStatus).setMaxWidthLimit(160));

        for (DyeColor dye : DyeColor.values()) {
            int id = dye.getId();
            int x = 8 + id % 8 * 20;
            int y = 63 + id / 8 * 20;
            var swatch = new ButtonWidget(x, y, 18, 18, new ColorRectTexture(0xff000000 | dye.getTextColor()),
                    click -> {
                        if (!click.isRemote) changeColor(player, id);
                    });
            swatch.setHoverBorderTexture(1, 0xffffffff);
            swatch.setHoverTooltips(CloudStorageUi.dyeName(dye));
            root.addWidget(swatch);
        }
        root.addWidget(new ButtonWidget(8, 104, 160, 16,
                new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("gtcsolo.cloud.ui.disconnect")), click -> {
                    if (!click.isRemote) changeColor(player, CloudStorageData.UNCOLORED);
                }));
        return new ModularUI(root, this, player);
    }

    private void changeColor(Player player, int color) {
        if (!(player instanceof ServerPlayer) || !CloudStorageUi.isNear(player, this)) return;
        CloudStorageMachine machine = controller();
        if (machine == null || !machine.isOperational()) return;
        if (machine.getColor() != color && !machine.setColor(color)) {
            player.displayClientMessage(Component.translatable("gtcsolo.cloud.ui.change_blocked"), true);
        }
    }

    private void addStatus(List<Component> text) {
        if (isRemote()) return;
        CloudStorageMachine machine = controller();
        if (machine == null) {
            text.add(Component.translatable("gtcsolo.cloud.ui.unformed"));
            return;
        }
        int color = machine.getColor();
        if (!CloudStorageData.validColor(color)) {
            text.add(Component.translatable("gtcsolo.cloud.ui.unlinked"));
            return;
        }
        text.add(CloudStorageUi.channel(color));
        var data = machine.getCloudData();
        if (data == null) return;
        long capacity = data.capacity(machine.getKind(), color);
        if (machine.getKind() == CloudStorageData.Kind.ITEMS) {
            text.add(Component.translatable("gtcsolo.cloud.ui.item_usage",
                    data.usedItemSlots(color), capacity));
        } else {
            var fluid = data.fluid(color);
            text.add(fluid.isEmpty() ? Component.translatable("gtcsolo.cloud.ui.empty")
                    : fluid.getDisplayName());
            text.add(Component.translatable("gtcsolo.cloud.ui.fluid_usage",
                    data.fluidAmount(color), capacity));
        }
    }

    private @Nullable CloudStorageMachine controller() {
        var attached = getControllers();
        if (attached.size() == 1 && attached.first() instanceof CloudStorageMachine machine
                && machine.getLevel() == getLevel() && !machine.isInValid() && machine.isFormed()) {
            return machine;
        }
        return null;
    }
}
