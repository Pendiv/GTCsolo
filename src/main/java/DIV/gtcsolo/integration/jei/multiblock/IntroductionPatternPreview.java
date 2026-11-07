package DIV.gtcsolo.integration.jei.multiblock;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.PatternPreviewWidget;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.SceneWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraft.client.gui.GuiGraphics;
import java.util.List;

/** Keep the native scene and controls, with its part slots in a separate vertical sidebar. */
final class IntroductionPatternPreview extends WidgetGroup {
    static final int PARTS_WIDTH = 26, PARTS_X = MultiblockIntroductionWidget.WIDTH - PARTS_WIDTH - 2;
    static final int PARTS_Y = 16, PARTS_HEIGHT = 140, ROW_HEIGHT = 20;
    private final DraggableScrollableWidgetGroup parts;
    private List<SlotWidget> arranged = List.of();

    IntroductionPatternPreview(MultiblockMachineDefinition definition) {
        this(definition, true);
    }

    IntroductionPatternPreview(MultiblockMachineDefinition definition, boolean showTitle) {
        super(0, 0, MultiblockIntroductionWidget.WIDTH, 160);
        setClientSideWidget();
        var preview = PatternPreviewWidget.getPatternWidget(definition);
        if (!showTitle) preview.widgets.stream().filter(ImageWidget.class::isInstance)
                .filter(w -> w.getSelfPosition().x == 3 && w.getSelfPosition().y == 3 && w.getSize().height == 10)
                .forEach(w -> w.setVisible(false));
        preview.setSize(getSize());
        // Give the removed name column back to the scene; keep the native P/L controls beside it.
        preview.widgets.stream().filter(SceneWidget.class::isInstance)
                .forEach(w -> w.setSize(PARTS_X - 7, w.getSize().height));
        preview.widgets.stream().filter(w -> w.getSelfPosition().x == 138 && w.getSize().width == 18)
                .forEach(w -> w.setSelfPositionX(PARTS_X - 26));
        parts = preview.widgets.stream().filter(DraggableScrollableWidgetGroup.class::isInstance)
                .map(DraggableScrollableWidgetGroup.class::cast).findFirst().orElseThrow();
        parts.setSelfPosition(PARTS_X, PARTS_Y);
        parts.setSize(PARTS_WIDTH, PARTS_HEIGHT);
        parts.setXScrollBarHeight(0).setYScrollBarWidth(4)
                .setYBarStyle(GuiTextures.SLIDER_BACKGROUND, GuiTextures.BUTTON)
                .setScrollable(true).setDraggable(false).setUseScissor(true);
        parts.setScrollWheelDirection(DraggableScrollableWidgetGroup.ScrollWheelDirection.VERTICAL);
        addWidget(preview);
        arrangeParts();
    }

    private void arrangeParts() {
        var slots = parts.widgets.stream().filter(SlotWidget.class::isInstance).map(SlotWidget.class::cast).toList();
        if (slots.equals(arranged)) return;
        parts.setScrollXOffset(0);
        parts.setScrollYOffset(0);
        for (int i = 0; i < slots.size(); i++) {
            SlotWidget slot = slots.get(i);
            slot.setSelfPosition(0, i * ROW_HEIGHT);
        }
        parts.computeMax();
        arranged = slots;
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        // Native P changes replace the slots; reflow before the next input or draw.
        arrangeParts();
        return handled;
    }

    @Override public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        arrangeParts();
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
    }
}
