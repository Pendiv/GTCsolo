package DIV.gtcsolo.block.wen;

import DIV.gtcsolo.network.wen.WENSelectIdPacket;
import DIV.gtcsolo.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class WENIdSelectScreen extends AbstractContainerScreen<WENIdSelectMenu> {

    public WENIdSelectScreen(WENIdSelectMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 200;
        this.imageHeight = Math.min(50 + menu.getAvailableIds().size() * 22, 250);
    }

    @Override
    protected void init() {
        super.init();
        List<String> ids = menu.getAvailableIds();
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            boolean isCurrent = id.equals(menu.getCurrentId());
            String label = (isCurrent ? "\u00a7a> " : "  ") + id;
            int yy = topPos + 28 + i * 22;
            if (yy + 18 > topPos + imageHeight - 4) break; // 画面外防止
            addRenderableWidget(Button.builder(Component.literal(label), btn -> {
                ModNetwork.CHANNEL.sendToServer(new WENSelectIdPacket(menu.getMachinePos(), id));
                menu.setCurrentId(id);
                onClose();
            }).bounds(leftPos + 8, yy, imageWidth - 16, 18).build());
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float p, int mx, int my) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF3A3A5E);
        g.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFF1A1A2E);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float p) {
        renderBackground(g);
        super.render(g, mx, my, p);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, "Select Network ID", 8, 8, 0xFFFFFFFF, false);
        g.drawString(font, "Current: " + (menu.getCurrentId().isEmpty() ? "---" : menu.getCurrentId()),
                8, 18, 0xFF55FFFF, false);
    }
}