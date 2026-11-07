package DIV.gtcsolo.block.datachest;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * DataChest GUI Screen — 9×9 chest grid + プレイヤーインベ。
 *
 * <p>各 chest slot に大量 count が入った場合、 vanilla の数字描画は "99" で切られる
 * (or 64 cap)。 本 Screen は overlay text で実 count を 4 桁 + k/m/g 形式
 * (= "1.111k" / "21.34k" / "324.3k" / "21.47m") で描画する
 * (= slot 描画後の post-process)。
 */
public class DataChestScreen extends AbstractContainerScreen<DataChestMenu> {

    // 動的生成された GUI 背景 (= 自前で色塗り、 texture 不要)
    private static final int BG_COLOR     = 0xFFC6C6C6;  // vanilla GUI gray
    private static final int BG_DARK      = 0xFF555555;
    private static final int SLOT_BG      = 0xFF8B8B8B;
    private static final int COUNT_COLOR  = 0xFFFFFF55;  // 鮮黄、 hover でも見やすい

    public DataChestScreen(DataChestMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        // 9×9 + プレイヤーインベ 4 段
        this.imageWidth  = 8 + 9 * 18 + 8;          // 176
        this.imageHeight = 18 + 9 * 18 + 14 + 4 * 18 + 4 + 8;  // 18 + 162 + 14 + 72 + 4 + 8 = 278
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        // 背景パネル
        g.fill(x, y, x + imageWidth, y + imageHeight, BG_COLOR);
        // 上下 border
        g.fill(x, y, x + imageWidth, y + 1, BG_DARK);
        g.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, BG_DARK);
        g.fill(x, y, x + 1, y + imageHeight, BG_DARK);
        g.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, BG_DARK);

        // chest slot 背景 (= 9×9)
        for (int row = 0; row < DataChestMenu.CHEST_ROWS; row++) {
            for (int col = 0; col < DataChestMenu.CHEST_COLS; col++) {
                int sx = x + 8 + col * 18;
                int sy = y + 18 + row * 18;
                g.fill(sx, sy, sx + 16, sy + 16, SLOT_BG);
                // 1px frame
                g.fill(sx - 1, sy - 1, sx + 17, sy, BG_DARK);
                g.fill(sx - 1, sy + 16, sx + 17, sy + 17, BG_DARK);
                g.fill(sx - 1, sy, sx, sy + 16, BG_DARK);
                g.fill(sx + 16, sy, sx + 17, sy + 16, BG_DARK);
            }
        }
        // プレイヤーインベ slot 背景 (= 3×9 + 1×9)
        int invY = y + 18 + DataChestMenu.CHEST_ROWS * 18 + 14;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int sx = x + 8 + col * 18;
                int sy = invY + row * 18;
                g.fill(sx, sy, sx + 16, sy + 16, SLOT_BG);
            }
        }
        // hotbar
        int hotbarY = invY + 3 * 18 + 4;
        for (int col = 0; col < 9; col++) {
            int sx = x + 8 + col * 18;
            g.fill(sx, hotbarY, sx + 16, hotbarY + 16, SLOT_BG);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        // vanilla の壊れた chest slot 描画を BE 経由で上書き
        // (AbstractContainerScreen.renderSlot は private で override 不可、
        //  super.render 後の上書き描画で対応)
        overrideChestSlots(g);
        renderTooltip(g, mouseX, mouseY);
    }

    /**
     * chest slot を BE 経由で再描画。 vanilla の slot 描画
     * (= byte wrap した count を表示する / count text が消えずに残る) を上書きで隠す。
     *
     * <p>vanilla は item を z≈100、 count text を z=200 に描く。 我々の {@code g.fill}
     * は pose 既定で z=0 なので、 そのままだと vanilla の数字が透けて手前に残る。
     * pose 全体を z=+300 ずらしてから描く事で、 vanilla 描画を完全に覆い隠す。
     */
    private void overrideChestSlots(GuiGraphics g) {
        DataChestBlockEntity be = resolveClientBe();
        if (be == null) return;
        g.pose().pushPose();
        g.pose().translate(0.0f, 0.0f, 300.0f);
        for (int i = 0; i < DataChestMenu.CHEST_SLOTS; i++) {
            Slot slot = menu.slots.get(i);
            int x = leftPos + slot.x;
            int y = topPos + slot.y;
            // 強制的に slot 中央 16×16 を背景色で塗りつぶし (= vanilla 描画を消す)
            g.fill(x, y, x + 16, y + 16, SLOT_BG);
            ItemStack actual = be.getItems().getStackInSlot(i);
            if (actual.isEmpty()) continue;
            g.renderItem(actual, x, y);
            // count text は 4 digit + k/m/g (= "1.111k" / "21.34k" / "324.3k" / "21.47m")。
            // count <= 1 は表示なし、 2-999 は raw、 1000+ は compact 表記
            String countText = actual.getCount() > 1 ? formatCount(actual.getCount()) : null;
            g.renderItemDecorations(font, actual, x, y, countText);
        }
        g.pose().popPose();
    }

    /** client 側の DataChestBlockEntity を解決 (= chunk unload 等で null の可能性あり) */
    private DataChestBlockEntity resolveClientBe() {
        var pos = menu.getBePos();
        if (pos == null) return null;
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level == null) return null;
        var be = level.getBlockEntity(pos);
        return be instanceof DataChestBlockEntity dc ? dc : null;
    }

    /**
     * count を 4 桁の数字部分 + 単位 (k/m/g) で format する。
     * <ul>
     *   <li>count &lt; 1000   → raw ("999" 等)</li>
     *   <li>1.000k - 9.999k  → "x.xxx" (= 3 桁小数)</li>
     *   <li>10.00k - 99.99k  → "xx.xx" (= 2 桁小数)</li>
     *   <li>100.0k - 999.9k  → "xxx.x" (= 1 桁小数)</li>
     *   <li>以降 m, g も同形式</li>
     * </ul>
     * 切り捨てで桁落ち overflow (= "999.95k" → "1000.0k" 化) を防ぐ。
     */
    private static String formatCount(int count) {
        if (count < 1000) return Integer.toString(count);
        String unit;
        double value;
        if (count < 1_000_000) {
            unit = "k";
            value = count / 1_000.0;
        } else if (count < 1_000_000_000) {
            unit = "m";
            value = count / 1_000_000.0;
        } else {
            unit = "g";
            value = count / 1_000_000_000.0;
        }
        // 切り捨てて指定桁に揃える (= 上位 4 digits 保持)
        String numStr;
        if (value < 10.0) {
            value = Math.floor(value * 1000.0) / 1000.0;
            numStr = String.format(Locale.ROOT, "%.3f", value);
        } else if (value < 100.0) {
            value = Math.floor(value * 100.0) / 100.0;
            numStr = String.format(Locale.ROOT, "%.2f", value);
        } else {
            value = Math.floor(value * 10.0) / 10.0;
            numStr = String.format(Locale.ROOT, "%.1f", value);
        }
        return numStr + unit;
    }
}
