package DIV.gtcsolo.block;

import DIV.gtcsolo.network.EnergyCubeSwitchPacket;
import DIV.gtcsolo.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ExtendEnergyCubeScreen extends AbstractContainerScreen<ExtendEnergyCubeMenu> {

    private static final int BG_COLOR     = 0xFF1A1A2E;
    private static final int BORDER_COLOR = 0xFF3A3A5E;
    private static final int LABEL_COLOR  = 0xFFAAAAAA;
    private static final int VALUE_COLOR  = 0xFFFFFFFF;
    private static final int SCI_COLOR    = 0xFF888888;
    private static final int STORED_COLOR = 0xFF55FF55;
    private static final int EU_COLOR     = 0xFF55FFFF;
    private static final int BTN_OFF_BG   = 0xFF333355;
    private static final int BTN_ON_BG    = 0xFF225522;
    private static final int BTN_BORDER   = 0xFF555577;
    private static final int BTN_TEXT     = 0xFFFFFFFF;

    private static final int FE_PER_EU = ExtendEnergyCubeBlockEntity.FE_PER_EU;

    // スイッチボタン位置(UI座標系)
    private static final int SW1_X = 8, SW1_Y = 86, SW_W = 120, SW_H = 16;
    private static final int SW2_X = 8, SW2_Y = 106, SW2_W = 120, SW2_H = 16;

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
        graphics.fill(x + 6, y + 78, x + w - 6, y + 79, BORDER_COLOR);
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

        long storedFe = menu.getStored();
        long storedEu = storedFe / FE_PER_EU;

        // ---- 蓄積FE ----
        graphics.drawString(font, Component.translatable("gui.gtcsolo.energy_cube.stored_fe").getString(),
                8, 20, LABEL_COLOR, false);
        String feStr = formatNumber(storedFe) + " FE";
        graphics.drawString(font, feStr, 8, 32, STORED_COLOR, false);
        graphics.drawString(font, "(" + toSciStr(storedFe) + " FE)", 8, 42, SCI_COLOR, false);

        // ---- 蓄積EU ----
        graphics.drawString(font, Component.translatable("gui.gtcsolo.energy_cube.stored_eu").getString(),
                8, 56, LABEL_COLOR, false);
        String euStr = formatNumber(storedEu) + " EU";
        graphics.drawString(font, euStr, 8, 68, EU_COLOR, false);

        // ---- スイッチ1: 吸収/放出 ----
        boolean emit = menu.isEmitMode();
        String sw1Label = Component.translatable(emit
                ? "gui.gtcsolo.energy_cube.emit" : "gui.gtcsolo.energy_cube.absorb").getString();
        drawSwitch(graphics, SW1_X, SW1_Y, SW_W, SW_H, emit, sw1Label);

        // ---- スイッチ2: FE/EU ----
        boolean eu = menu.isEuMode();
        String sw2Label = Component.translatable(eu
                ? "gui.gtcsolo.energy_cube.eu_output" : "gui.gtcsolo.energy_cube.fe_output").getString();
        drawSwitch(graphics, SW2_X, SW2_Y, SW2_W, SW2_H, eu, sw2Label);
    }

    private void drawSwitch(GuiGraphics graphics, int x, int y, int w, int h,
                            boolean on, String label) {
        int bg = on ? BTN_ON_BG : BTN_OFF_BG;
        graphics.fill(x, y, x + w, y + h, BTN_BORDER);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, bg);
        graphics.drawString(font, label,
                x + (w - font.width(label)) / 2, y + (h - 8) / 2, BTN_TEXT, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int mx = (int) mouseX - leftPos;
            int my = (int) mouseY - topPos;

            if (isInBounds(mx, my, SW1_X, SW1_Y, SW_W, SW_H)) {
                boolean newVal = !menu.isEmitMode();
                sendSwitch(0, newVal);
                menu.updateData(menu.getStored(), newVal, menu.isEuMode());
                return true;
            }
            if (isInBounds(mx, my, SW2_X, SW2_Y, SW2_W, SW2_H)) {
                boolean newVal = !menu.isEuMode();
                sendSwitch(1, newVal);
                menu.updateData(menu.getStored(), menu.isEmitMode(), newVal);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isInBounds(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void sendSwitch(int switchId, boolean value) {
        ModNetwork.CHANNEL.sendToServer(
                new EnergyCubeSwitchPacket(menu.getBlockPos(), switchId, value));
    }

    private static String formatNumber(long val) {
        return String.format("%,d", val);
    }

    private static String toSciStr(long val) {
        if (val == 0) return "0";
        String s = Long.toString(val);
        int exp = s.length() - 1;
        if (exp == 0) return s;
        String mantissa = s.charAt(0) + "." + s.substring(1, Math.min(4, s.length()));
        return mantissa + "\u00d710^" + exp;
    }
}