package DIV.gtcsolo.block;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.math.BigInteger;

@OnlyIn(Dist.CLIENT)
public class ExtendEnergyCubeScreen extends AbstractContainerScreen<ExtendEnergyCubeMenu> {

    private static final int BG_COLOR     = 0xFF1A1A2E;
    private static final int BORDER_COLOR = 0xFF3A3A5E;
    private static final int LABEL_COLOR  = 0xFFAAAAAA;
    private static final int VALUE_COLOR  = 0xFFFFFFFF;
    private static final int SCI_COLOR    = 0xFF888888;
    private static final int STORED_COLOR = 0xFF55FF55;
    private static final int STORED_SCI   = 0xFF228822;
    private static final int RATE_COLOR   = 0xFFFFAA00;
    private static final int RATE_SCI     = 0xFF886600;

    public ExtendEnergyCubeScreen(ExtendEnergyCubeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth  = 270;
        this.imageHeight = 138;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos, w = imageWidth, h = imageHeight;
        graphics.fill(x,     y,     x + w,     y + h,     BORDER_COLOR);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, BG_COLOR);
        // 区切り線
        graphics.fill(x + 6, y + 50, x + w - 6, y + 51, BORDER_COLOR);
        graphics.fill(x + 6, y + 94, x + w - 6, y + 95, BORDER_COLOR);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // タイトル
        String title = getTitle().getString();
        graphics.drawString(font, title,
                (imageWidth - font.width(title)) / 2, 6, VALUE_COLOR, false);

        // --- 上段: 容量 ---
        graphics.drawString(font, "容量", 8, 18, LABEL_COLOR, false);
        BigInteger cap = ExtendEnergyCubeBlockEntity.MAX_CAPACITY;
        graphics.drawString(font, cap.toString() + " FE",
                8, 28, VALUE_COLOR, false);
        graphics.drawString(font, "(" + toSciStr(cap) + " FE)",
                8, 38, SCI_COLOR, false);

        // --- 中段: 蓄積 ---
        graphics.drawString(font, "蓄積", 8, 58, LABEL_COLOR, false);
        BigInteger stored = menu.getStored();
        graphics.drawString(font, stored.toString() + " FE",
                8, 68, STORED_COLOR, false);
        graphics.drawString(font, "(" + toSciStr(stored) + " FE)",
                8, 78, STORED_SCI, false);

        // --- 下段: 搬出速度 ---
        graphics.drawString(font, "搬出速度", 8, 102, LABEL_COLOR, false);
        BigInteger rate = menu.getOutputRate();
        graphics.drawString(font, rate.toString() + " FE/t",
                8, 112, RATE_COLOR, false);
        graphics.drawString(font, "(" + toSciStr(rate) + " FE/t)",
                8, 122, RATE_SCI, false);
    }

    /**
     * BigInteger を科学記数法で表示する。
     * 例: 340282366920938463463374607431768211455 → "3.402×10^38"
     *     12345 → "1.234×10^4"
     *     0 → "0"
     */
    private static String toSciStr(BigInteger val) {
        if (val.signum() == 0) return "0";
        String s = val.toString();
        int exp = s.length() - 1;
        if (exp == 0) return s;
        String mantissa = s.charAt(0) + "." + s.substring(1, Math.min(4, s.length()));
        return mantissa + "×10^" + exp;
    }
}