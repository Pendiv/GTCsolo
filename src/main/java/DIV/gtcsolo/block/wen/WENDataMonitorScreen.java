package DIV.gtcsolo.block.wen;

import DIV.gtcsolo.util.EnergyFormat;
import DIV.gtcsolo.network.wen.WENAdminPacket;
import DIV.gtcsolo.network.wen.WENSetIdPacket;
import DIV.gtcsolo.network.wen.WENUpgradePacket;
import DIV.gtcsolo.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class WENDataMonitorScreen extends AbstractContainerScreen<WENDataMonitorMenu> {

    private static final int BG       = 0xFF1A1A2E;
    private static final int BORDER   = 0xFF3A3A5E;
    private static final int LABEL    = 0xFFAAAAAA;
    private static final int VALUE    = 0xFFFFFFFF;
    private static final int STORED   = 0xFF55FF55;
    private static final int WARN     = 0xFFFF5555;
    private static final int ID_COLOR = 0xFF55FFFF;
    private static final int GRAPH_BG = 0xFF222244;
    private static final int GRAPH_LINE = 0xFF44BB44;
    private static final int GRAPH_WARN = 0xFFBB4444;

    private int currentPage = 0;
    private static final int PAGE_COUNT = 5; // 1:Status 2:Upgrades 3:Graph 4:Networks 5:Admin

    // Page 1 widgets
    private EditBox idField;
    // Page 4 widgets
    private EditBox adminIdField;
    private EditBox adminValueField;

    public WENDataMonitorScreen(WENDataMonitorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 280;
        this.imageHeight = 220;
    }

    @Override
    protected void init() {
        super.init();
        buildPageWidgets();
    }

    private void buildPageWidgets() {
        clearWidgets();

        // ページ切替ボタン（常に表示）
        addRenderableWidget(Button.builder(Component.literal("<"), btn -> prevPage())
                .bounds(leftPos + 4, topPos + imageHeight - 20, 20, 16).build());
        addRenderableWidget(Button.builder(Component.literal(">"), btn -> nextPage())
                .bounds(leftPos + imageWidth - 24, topPos + imageHeight - 20, 20, 16).build());

        switch (currentPage) {
            case 0 -> buildPage1();      // Status + ID
            case 1 -> buildPage2Upgrades(); // Upgrades
            case 2 -> {}                 // Graph
            case 3 -> {}                 // Networks
            case 4 -> buildPageAdmin();  // Admin
        }
    }

    private void buildPage1() {
        if (!menu.isFormed()) return;
        idField = new EditBox(font, leftPos + 8, topPos + 58, 170, 16, Component.literal("ID"));
        idField.setMaxLength(32);
        idField.setValue(menu.getNetworkId());
        idField.setFilter(s -> s.matches("[a-zA-Z0-9]*"));
        addRenderableWidget(idField);
        addRenderableWidget(Button.builder(
                Component.translatable("gui.gtcsolo.wen_monitor.set_id"), btn -> {
                    if (idField != null && !idField.getValue().trim().isEmpty())
                        ModNetwork.CHANNEL.sendToServer(new WENSetIdPacket(menu.getMonitorPos(), idField.getValue().trim()));
                }).bounds(leftPos + 184, topPos + 57, 88, 18).build());
    }

    private void buildPage2Upgrades() {
        if (!menu.isFormed()) return;

        int lv = menu.getStorageLevel();
        boolean canUp = lv < 36;

        // ストレージアップグレード — ボタン行
        String upLabel = canUp ? "Upgrade (+1)" : "MAX";
        addRenderableWidget(Button.builder(Component.literal(upLabel), btn -> {
            if (!canUp) return;
            int action = hasShiftDown() ? 3 : 2;
            ModNetwork.CHANNEL.sendToServer(new WENUpgradePacket(menu.getMonitorPos(), 0, action));
        }).bounds(leftPos + 8, topPos + 56, 120, 16).build());

        // 返却ボタン
        if (lv > 0) {
            addRenderableWidget(Button.builder(Component.literal("Downgrade"), btn -> {
                ModNetwork.CHANNEL.sendToServer(new WENUpgradePacket(menu.getMonitorPos(), 0, 4));
            }).bounds(leftPos + 134, topPos + 56, 80, 16).build());
        }

        // クロスディメンション結晶
        boolean hasCrystal = menu.isCrossDimensionEnabled();
        String cLabel = hasCrystal ? "Remove Crystal" : "Insert Crystal";
        addRenderableWidget(Button.builder(Component.literal(cLabel), btn -> {
            ModNetwork.CHANNEL.sendToServer(new WENUpgradePacket(menu.getMonitorPos(), 0, hasCrystal ? 1 : 0));
        }).bounds(leftPos + 8, topPos + 102, 120, 16).build());
    }

    @Override
    public void containerTick() {
        super.containerTick();
        // ページ2(アップグレード)表示中はウィジェットを定期リビルドしてラベル更新
        if (currentPage == 1) {
            buildPageWidgets();
        }
    }

    private void buildPageAdmin() {
        if (!menu.isOp()) return;
        adminIdField = new EditBox(font, leftPos + 8, topPos + 42, 140, 14, Component.literal("ID"));
        adminIdField.setMaxLength(32);
        addRenderableWidget(adminIdField);

        adminValueField = new EditBox(font, leftPos + 8, topPos + 62, 140, 14, Component.literal("Value"));
        adminValueField.setMaxLength(20);
        addRenderableWidget(adminValueField);

        addRenderableWidget(Button.builder(Component.literal("Delete"), btn -> sendAdmin(WENAdminPacket.Action.DELETE_NETWORK, 0))
                .bounds(leftPos + 154, topPos + 40, 58, 16).build());
        addRenderableWidget(Button.builder(Component.literal("Set EU"), btn -> sendAdmin(WENAdminPacket.Action.SET_ENERGY, parseValue()))
                .bounds(leftPos + 154, topPos + 60, 58, 16).build());
        addRenderableWidget(Button.builder(Component.literal("Set Cap"), btn -> sendAdmin(WENAdminPacket.Action.SET_CAPACITY, parseValue()))
                .bounds(leftPos + 216, topPos + 60, 58, 16).build());
    }

    private void sendAdmin(WENAdminPacket.Action action, long value) {
        if (adminIdField == null) return;
        String id = adminIdField.getValue().trim();
        if (id.isEmpty()) return;
        ModNetwork.CHANNEL.sendToServer(new WENAdminPacket(menu.getMonitorPos(), action, id, value));
    }

    private long parseValue() {
        if (adminValueField == null) return 0;
        try { return Long.parseLong(adminValueField.getValue().trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    private void prevPage() { currentPage = (currentPage - 1 + PAGE_COUNT) % PAGE_COUNT; buildPageWidgets(); }
    private void nextPage() { currentPage = (currentPage + 1) % PAGE_COUNT; buildPageWidgets(); }

    // =========================================================================
    //  レンダリング
    // =========================================================================

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        int x = leftPos, y = topPos, w = imageWidth, h = imageHeight;
        g.fill(x, y, x + w, y + h, BORDER);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, BG);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        super.render(g, mx, my, partial);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        // タイトル + ページ番号
        String title = getTitle().getString() + " [" + (currentPage + 1) + "/" + PAGE_COUNT + "]";
        g.drawString(font, title, (imageWidth - font.width(title)) / 2, 4, VALUE, false);

        switch (currentPage) {
            case 0 -> renderPage1(g);
            case 1 -> renderPageUpgrades(g);
            case 2 -> renderPageGraph(g);
            case 3 -> renderPageNetworks(g);
            case 4 -> renderPageAdmin(g);
        }
    }

    // --- Page 1: ステータス + ID設定 ---
    private void renderPage1(GuiGraphics g) {
        boolean formed = menu.isFormed();
        g.drawString(font, Component.translatable("gui.gtcsolo.wen_monitor.status").getString(),
                8, 18, LABEL, false);
        g.drawString(font, formed
                        ? Component.translatable("gui.gtcsolo.wen_monitor.formed").getString()
                        : Component.translatable("gui.gtcsolo.wen_monitor.not_formed").getString(),
                60, 18, formed ? STORED : WARN, false);

        String curId = menu.getNetworkId();
        g.drawString(font, "ID: " + (curId.isEmpty() ? "---" : curId), 8, 30, ID_COLOR, false);

        if (!formed) {
            g.drawString(font, Component.translatable("gui.gtcsolo.wen_monitor.build_first").getString(),
                    8, 50, WARN, false);
            return;
        }

        g.drawString(font, Component.translatable("gui.gtcsolo.wen_monitor.new_id").getString(), 8, 46, LABEL, false);
        // ID入力はウィジェットで描画

        // エネルギー情報 (BigInteger文字列対応)
        String sStr = menu.getStoredStr();
        String cStr = menu.getCapacityStr();
        g.drawString(font, "Energy: " + EnergyFormat.format(sStr) + " / " + EnergyFormat.format(cStr) + " EU",
                8, 84, STORED, false);
        // パーセンテージ (doubleで近似)
        long stored = menu.getStored();
        long cap = menu.getCapacity();
        double pct = cap > 0 ? (100.0 * stored / cap) : 0;
        g.drawString(font, String.format("%.1f%%", pct), 8, 96, LABEL, false);

        // バー
        int barX = 8, barY = 110, barW = imageWidth - 16, barH = 10;
        g.fill(barX, barY, barX + barW, barY + barH, GRAPH_BG);
        if (cap > 0) {
            int filled = (int) (barW * Math.min(1.0, (double) stored / cap));
            g.fill(barX, barY, barX + filled, barY + barH, GRAPH_LINE);
        }

        // IO速度
        g.drawString(font, String.format("In: %,d EU/s  Out: %,d EU/s",
                menu.getInputPerSec(), menu.getOutputPerSec()), 8, 126, LABEL, false);
    }

    // --- Page 2: アップグレード ---
    private void renderPageUpgrades(GuiGraphics g) {
        g.drawString(font, "Upgrades", 8, 18, VALUE, false);
        g.fill(8, 28, imageWidth - 8, 29, BORDER);

        if (!menu.isFormed()) {
            g.drawString(font, Component.translatable("gui.gtcsolo.wen_monitor.build_first").getString(),
                    8, 36, WARN, false);
            return;
        }

        // --- ストレージアップグレード (2行: 情報 + ボタン) ---
        int lv = menu.getStorageLevel();
        int cost = menu.getNextUpgradeCost();
        long mult = 1L << Math.min(lv, 62);
        // 情報行
        g.drawString(font, String.format("Storage: Lv %d/36  (x%s capacity)", lv,
                lv <= 62 ? String.valueOf(mult) : "2^" + lv),
                8, 36, ID_COLOR, false);
        g.drawString(font, lv < 36
                ? String.format("Next: %d Fantasy Core  (Click/Shift=Bulk)", cost)
                : "MAX LEVEL",
                8, 46, LABEL, false);
        // ボタン行はbuildPage2Upgradesで描画

        g.fill(8, 76, imageWidth - 8, 77, BORDER);

        // --- クロスディメンション (2行: 情報 + ボタン) ---
        g.drawString(font, "Cross-Dimension: " +
                (menu.isCrossDimensionEnabled() ? "\u00a7aENABLED" : "\u00a7cDISABLED"),
                8, 84, LABEL, false);
        g.drawString(font, "Requires: Evernight Crystal (hold in hand)",
                8, 94, 0xFF666666, false);
        // ボタン行はbuildPage2Upgradesで描画
    }

    // --- Page 3: エネルギーグラフ ---
    private void renderPageGraph(GuiGraphics g) {
        g.drawString(font, "Energy History (60s)", 8, 18, LABEL, false);

        List<Long> history = menu.getEnergyHistory();
        long cap = menu.getCapacity();

        int gx = 8, gy = 32, gw = imageWidth - 16, gh = 120;
        g.fill(gx, gy, gx + gw, gy + gh, GRAPH_BG);

        if (history.size() < 2 || cap <= 0) {
            g.drawString(font, "No data", gx + gw / 2 - 20, gy + gh / 2, LABEL, false);
            return;
        }

        // 最大値を自動スケール
        long maxVal = history.stream().mapToLong(Long::longValue).max().orElse(1);
        if (maxVal == 0) maxVal = 1;

        // 折れ線グラフ
        for (int i = 1; i < history.size(); i++) {
            int x1 = gx + (i - 1) * gw / history.size();
            int x2 = gx + i * gw / history.size();
            int y1 = gy + gh - (int) (gh * history.get(i - 1) / (double) maxVal);
            int y2 = gy + gh - (int) (gh * history.get(i) / (double) maxVal);
            // 簡易線描画（1px幅の矩形で近似）
            drawLine(g, x1, y1, x2, y2, history.get(i) > history.get(i - 1) ? GRAPH_LINE : GRAPH_WARN);
        }

        // 軸ラベル
        g.drawString(font, String.format("%,d EU", maxVal), gx + 2, gy + 2, LABEL, false);
        g.drawString(font, "0", gx + 2, gy + gh - 10, LABEL, false);
        g.drawString(font, "60s ago", gx + 2, gy + gh + 2, LABEL, false);
        g.drawString(font, "now", gx + gw - 20, gy + gh + 2, LABEL, false);

        // IO情報
        g.drawString(font, String.format("In: %,d EU/s  Out: %,d EU/s",
                menu.getInputPerSec(), menu.getOutputPerSec()), 8, gy + gh + 14, VALUE, false);
    }

    private void drawLine(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0) { g.fill(x1, y1, x1 + 1, y1 + 1, color); return; }
        for (int i = 0; i <= steps; i++) {
            int px = x1 + dx * i / steps;
            int py = y1 + dy * i / steps;
            g.fill(px, py, px + 1, py + 1, color);
        }
    }

    // --- Page 4: 全ネットワーク一覧 ---
    private void renderPageNetworks(GuiGraphics g) {
        g.drawString(font, "All WEN Networks", 8, 18, VALUE, false);
        g.fill(8, 28, imageWidth - 8, 29, BORDER);

        List<WENDataMonitorMenu.NetworkInfo> nets = menu.getAllNetworks();
        if (nets.isEmpty()) {
            g.drawString(font, "No networks registered", 8, 34, LABEL, false);
            return;
        }

        int y = 34;
        for (int i = 0; i < Math.min(nets.size(), 12); i++) { // 最大12行
            var n = nets.get(i);
            String status = n.formed() ? "§a[OK]" : "§c[--]";
            String line = status + " §f" + n.id();
            g.drawString(font, line, 8, y, VALUE, false);
            if (n.formed()) {
                double pct = n.capacity() > 0 ? (100.0 * n.stored() / n.capacity()) : 0;
                g.drawString(font, String.format("%.0f%%", pct), 180, y, LABEL, false);
                g.drawString(font, n.dimension(), 210, y, 0xFF666666, false);
            }
            y += 12;
        }
        if (nets.size() > 12) {
            g.drawString(font, "... +" + (nets.size() - 12) + " more", 8, y, LABEL, false);
        }
    }

    // --- Page 5: 管理者メニュー ---
    private void renderPageAdmin(GuiGraphics g) {
        if (!menu.isOp()) {
            g.drawString(font, "§cAdmin access required (OP)", 8, 18, WARN, false);
            return;
        }
        g.drawString(font, "§6Admin Panel", 8, 18, VALUE, false);
        g.fill(8, 28, imageWidth - 8, 29, BORDER);
        g.drawString(font, "Target ID:", 8, 32, LABEL, false);
        g.drawString(font, "Value:", 8, 52, LABEL, false);
        // ウィジェットは buildPage4() で追加済み

        g.drawString(font, "§7Delete: removes network + energy", 8, 84, LABEL, false);
        g.drawString(font, "§7Set EU: set stored energy", 8, 96, LABEL, false);
        g.drawString(font, "§7Set Cap: set max capacity", 8, 108, LABEL, false);
    }

    // =========================================================================
    //  キー入力
    // =========================================================================

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        // テキストフィールドにフォーカスがある時はEscのみ画面を閉じる
        if (key == 256) { this.onClose(); return true; }
        if (idField != null && idField.isFocused()) return idField.keyPressed(key, scanCode, modifiers);
        if (adminIdField != null && adminIdField.isFocused()) return adminIdField.keyPressed(key, scanCode, modifiers);
        if (adminValueField != null && adminValueField.isFocused()) return adminValueField.keyPressed(key, scanCode, modifiers);
        return super.keyPressed(key, scanCode, modifiers);
    }
}