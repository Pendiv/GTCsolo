package DIV.gtcsolo.integration.jei.multiblock;

import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Shared x1.0, shadowless text with native wrapping, hover and inline-button hit testing. */
public final class DocumentationTextPanel extends ComponentPanelWidget {
    public DocumentationTextPanel(int x, int y, List<Component> components) { super(x, y, components); }

    @Override public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        var font = Minecraft.getInstance().font;
        var position = getPosition();
        int y = position.y;
        for (var line : cacheLines) {
            int x = isCenter ? position.x + (getSize().width - font.width(line)) / 2 : position.x;
            graphics.drawString(font, line, x, y, 0xff000000, false);
            y += font.lineHeight + space;
        }
    }
}
