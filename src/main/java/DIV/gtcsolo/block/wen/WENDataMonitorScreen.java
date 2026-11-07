package DIV.gtcsolo.block.wen;

import DIV.gtcsolo.machine.wen.WENMonitorData;
import DIV.gtcsolo.machine.wen.WENNetworkData;
import DIV.gtcsolo.network.ModNetwork;
import DIV.gtcsolo.network.wen.*;
import DIV.gtcsolo.util.EnergyFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.math.BigInteger;
import java.util.Locale;

@OnlyIn(Dist.CLIENT)
public class WENDataMonitorScreen extends AbstractContainerScreen<WENDataMonitorMenu> {
    private static final int BG = 0xFF1A1A2E,
            BORDER = 0xFF3A3A5E,
            LABEL = 0xFFAAAAAA,
            TEXT = 0xFFFFFFFF,
            GREEN = 0xFF55FF55,
            RED = 0xFFFF5555,
            BLUE = 0xFF55FFFF;
    private int currentPage, requestedOffset;
    private EditBox idField, adminIdField, adminValueField;
    private String idDraft, adminIdDraft, adminValueDraft = "";
    private Button setId, upgrade, bulkUpgrade, downgrade, crystal, delete, setEnergy, setCapacity;
    private int displayedLevel = -1;
    private boolean displayedCrystal, displayedOperator;

    public WENDataMonitorScreen(WENDataMonitorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 300;
        imageHeight = 230;
    }

    private static Component text(String key, Object... args) {
        return Component.translatable("gui.gtcsolo.wen." + key, args);
    }

    private static String number(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    private int pageCount() {
        return menu.isOp() ? 5 : 4;
    }

    @Override
    protected void init() {
        super.init();
        buildWidgets();
    }

    private void buildWidgets() {
        if (idField != null) idDraft = idField.getValue();
        if (adminIdField != null) adminIdDraft = adminIdField.getValue();
        if (adminValueField != null) adminValueDraft = adminValueField.getValue();
        clearWidgets();
        idField = null;
        adminIdField = null;
        adminValueField = null;
        setId = null;
        upgrade = null;
        bulkUpgrade = null;
        downgrade = null;
        crystal = null;
        delete = null;
        setEnergy = null;
        setCapacity = null;
        displayedLevel = menu.getStorageLevel();
        displayedCrystal = menu.isCrossDimensionEnabled();
        displayedOperator = menu.isOp();
        currentPage = Math.min(currentPage, pageCount() - 1);
        button(Component.literal("<"), 4, imageHeight - 21, 22, () -> changePage(-1));
        button(Component.literal(">"), imageWidth - 26, imageHeight - 21, 22, () -> changePage(1));
        switch (currentPage) {
            case 0 -> {
                idField = edit(8, 57, 208, 32, text("network_id"));
                idField.setValue(idDraft == null ? menu.getNetworkId() : idDraft);
                setId =
                        button(
                                text("set_id"),
                                222,
                                57,
                                70,
                                () ->
                                        ModNetwork.CHANNEL.sendToServer(
                                                new WENSetIdPacket(
                                                        menu.getMonitorPos(), idField.getValue())));
                setId.setTooltip(Tooltip.create(text("rename_hint")));
            }
            case 1 -> {
                upgrade = button(text("upgrade_one"), 8, 73, 90, () -> sendUpgrade(2));
                bulkUpgrade = button(text("upgrade_all"), 104, 73, 136, () -> sendUpgrade(3));
                downgrade = button(text("downgrade"), 8, 98, 132, () -> sendUpgrade(4));
                downgrade.setTooltip(Tooltip.create(text("downgrade_hint")));
                crystal =
                        button(
                                text(
                                        menu.isCrossDimensionEnabled()
                                                ? "remove_crystal"
                                                : "insert_crystal"),
                                8,
                                163,
                                180,
                                () -> sendUpgrade(menu.isCrossDimensionEnabled() ? 1 : 0));
            }
            case 3 -> {
                button(
                        Component.literal("<"),
                        8,
                        185,
                        22,
                        () -> scrollNetworks(-WENMonitorData.NETWORK_ROWS));
                button(
                        Component.literal(">"),
                        270,
                        185,
                        22,
                        () -> scrollNetworks(WENMonitorData.NETWORK_ROWS));
            }
            case 4 -> {
                adminIdField = edit(8, 44, 210, 64, text("target_id"));
                adminIdField.setValue(adminIdDraft == null ? menu.getNetworkId() : adminIdDraft);
                adminValueField =
                        edit(8, 80, 284, WENMonitorUpdatePacket.MAX_NUMBER_DIGITS, text("value"));
                adminValueField.setValue(adminValueDraft);
                delete =
                        button(
                                text("delete"),
                                8,
                                112,
                                88,
                                () -> sendAdmin(WENAdminPacket.Action.DELETE_NETWORK));
                setEnergy =
                        button(
                                text("set_energy"),
                                104,
                                112,
                                88,
                                () -> sendAdmin(WENAdminPacket.Action.SET_ENERGY));
                setCapacity =
                        button(
                                text("set_capacity"),
                                200,
                                112,
                                92,
                                () -> sendAdmin(WENAdminPacket.Action.SET_CAPACITY));
            }
        }
    }

    private Button button(Component label, int x, int y, int width, Runnable action) {
        return addRenderableWidget(
                Button.builder(label, b -> action.run())
                        .bounds(leftPos + x, topPos + y, width, 18)
                        .build());
    }

    private EditBox edit(int x, int y, int width, int max, Component label) {
        var box = new EditBox(font, leftPos + x, topPos + y, width, 18, label);
        box.setMaxLength(max);
        addRenderableWidget(box);
        return box;
    }

    private void changePage(int direction) {
        currentPage = (currentPage + direction + pageCount()) % pageCount();
        buildWidgets();
        requestView();
    }

    private void requestView() {
        menu.setView(currentPage, requestedOffset);
        ModNetwork.CHANNEL.sendToServer(
                new WENMonitorViewPacket(
                        menu.containerId, menu.getMonitorPos(), currentPage, requestedOffset));
    }

    private void scrollNetworks(int delta) {
        if (menu.data().page() != 3) return;
        int max = Math.max(0, menu.data().networkCount() - WENMonitorData.NETWORK_ROWS);
        requestedOffset = Math.max(0, Math.min(max, requestedOffset + delta));
        requestView();
    }

    private void sendUpgrade(int action) {
        ModNetwork.CHANNEL.sendToServer(new WENUpgradePacket(menu.getMonitorPos(), action));
    }

    private BigInteger adminValue() {
        if (adminValueField == null || !adminValueField.getValue().matches("[0-9]+")) return null;
        return new BigInteger(adminValueField.getValue());
    }

    private void sendAdmin(WENAdminPacket.Action action) {
        BigInteger value =
                action == WENAdminPacket.Action.DELETE_NETWORK ? BigInteger.ZERO : adminValue();
        if (menu.isOp()
                && adminIdField != null
                && !adminIdField.getValue().isBlank()
                && value != null)
            ModNetwork.CHANNEL.sendToServer(
                    new WENAdminPacket(
                            menu.getMonitorPos(), action, adminIdField.getValue(), value));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (displayedOperator != menu.isOp()
                || (currentPage == 1
                        && (displayedLevel != menu.getStorageLevel()
                                || displayedCrystal != menu.isCrossDimensionEnabled()))) {
            int previousPage = currentPage;
            buildWidgets();
            if (currentPage != previousPage) requestView();
        }
        if (setId != null)
            setId.active = menu.isFormed() && WENNetworkData.isValidId(idField.getValue());
        if (upgrade != null) upgrade.active = menu.isFormed() && menu.getStorageLevel() < 36;
        if (bulkUpgrade != null) bulkUpgrade.active = upgrade.active;
        if (downgrade != null) downgrade.active = menu.isFormed() && menu.getStorageLevel() > 0;
        if (crystal != null) crystal.active = menu.isFormed();
        if (delete != null) {
            delete.active = menu.isOp() && !adminIdField.getValue().isBlank();
            boolean valid = adminValue() != null;
            setEnergy.active = delete.active && valid;
            setCapacity.active = setEnergy.active;
            adminValueField.setTextColor(
                    valid || adminValueField.getValue().isEmpty() ? TEXT : RED);
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        if (currentPage == 3
                && delta != 0
                && x >= leftPos
                && x < leftPos + imageWidth
                && y >= topPos
                && y < topPos + imageHeight) {
            scrollNetworks(delta < 0 ? 1 : -1);
            return true;
        }
        return super.mouseScrolled(x, y, delta);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == 256) {
            onClose();
            return true;
        }
        if (getFocused() instanceof EditBox box && box.isFocused()) {
            box.keyPressed(key, scan, modifiers);
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, BORDER);
        graphics.fill(
                leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, BG);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (currentPage == 3
                && menu.data().page() == 3
                && mouseX >= leftPos + 8
                && mouseX < leftPos + 292) {
            int row = (mouseY - topPos - 37) / 12;
            if (mouseY >= topPos + 37 && row >= 0 && row < menu.getAllNetworks().size()) {
                var value = menu.getAllNetworks().get(row);
                graphics.renderTooltip(
                        font,
                        text("network_location", value.id(), dimensionName(value.dimension())),
                        mouseX,
                        mouseY);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, getTitle(), imageWidth / 2, 5, TEXT);
        graphics.drawCenteredString(
                font, Component.literal((currentPage + 1) + "/" + pageCount() + " ").append(text("page." + currentPage)), imageWidth / 2, imageHeight - 16, LABEL);
        switch (currentPage) {
            case 0 -> status(graphics);
            case 1 -> upgrades(graphics);
            case 2 -> history(graphics);
            case 3 -> networks(graphics);
            case 4 -> admin(graphics);
        }
    }

    private void draw(GuiGraphics graphics, Component value, int y, int color) {
        graphics.drawString(font, value, 8, y, color, false);
    }

    private void flow(GuiGraphics graphics, int y) {
        draw(graphics, text("input_rate", number(menu.getInputPerSec())), y, TEXT);
        draw(graphics, text("output_rate", number(menu.getOutputPerSec())), y + 13, TEXT);
    }

    private void status(GuiGraphics graphics) {
        draw(
                graphics,
                text(menu.isFormed() ? "formed" : "unformed"),
                24,
                menu.isFormed() ? GREEN : RED);
        draw(
                graphics,
                text("current_id", menu.getNetworkId().isEmpty() ? "—" : menu.getNetworkId()),
                38,
                BLUE);
        draw(graphics, text("id_hint"), 79, LABEL);
        draw(graphics, text("stored", EnergyFormat.format(menu.data().stored())), 102, TEXT);
        draw(graphics, text("capacity", EnergyFormat.format(menu.data().capacity())), 116, TEXT);
        graphics.fill(8, 134, 292, 144, BORDER);
        graphics.fill(8, 134, 8 + (int) (284 * menu.getFillFraction()), 144, GREEN);
        draw(
                graphics,
                Component.literal(
                        String.format(Locale.ROOT, "%.1f%%", 100 * menu.getFillFraction())),
                149,
                LABEL);
        flow(graphics, 169);
    }

    private void upgrades(GuiGraphics graphics) {
        draw(graphics, text("storage_level", menu.getStorageLevel(), 36), 27, TEXT);
        draw(
                graphics,
                text("capacity_multiplier", number(1L << menu.getStorageLevel())),
                42,
                LABEL);
        draw(
                graphics,
                menu.getStorageLevel() < 36
                        ? text(
                                "upgrade_cost",
                                menu.getNextUpgradeCost(),
                                Component.translatable("item.gtcsolo.fcore"))
                        : text("maximum_level"),
                57,
                LABEL);
        graphics.fill(8, 127, 292, 128, BORDER);
        draw(
                graphics,
                text(
                        menu.isCrossDimensionEnabled()
                                ? "cross_dimension_enabled"
                                : "cross_dimension_disabled"),
                137,
                menu.isCrossDimensionEnabled() ? GREEN : LABEL);
        draw(
                graphics,
                text("crystal_hint", Component.translatable("item.gtcsolo.evernight_crystal")),
                150,
                LABEL);
        if (!menu.isFormed()) draw(graphics, text("build_first"), 188, RED);
    }

    private void history(GuiGraphics graphics) {
        draw(graphics, text("history_title"), 23, LABEL);
        int x = 8, y = 39, width = 284, height = 124;
        graphics.fill(x, y, x + width, y + height, 0xFF222244);
        var history = menu.getEnergyHistory();
        if (menu.data().page() != 2 || history.size() < 2) {
            graphics.drawCenteredString(
                    font, text("no_data"), imageWidth / 2, y + height / 2, LABEL);
            return;
        }
        for (int i = 1; i < history.size(); i++) {
            int x1 = x + (i - 1) * width / (history.size() - 1),
                    x2 = x + i * width / (history.size() - 1);
            int y1 = y + height - (int) (height * menu.getHistoryFraction(i - 1)),
                    y2 = y + height - (int) (height * menu.getHistoryFraction(i));
            line(
                    graphics,
                    x1,
                    y1,
                    x2,
                    y2,
                    history.get(i).compareTo(history.get(i - 1)) >= 0 ? GREEN : RED);
        }
        graphics.drawString(
                font,
                EnergyFormat.format(menu.getHistoryMaximum()) + " EU",
                x + 2,
                y + 2,
                LABEL,
                false);
        graphics.drawString(
                font, text("seconds_ago", history.size() - 1), x, y + height + 3, LABEL, false);
        graphics.drawString(font, text("now"), x + width - 24, y + height + 3, LABEL, false);
        flow(graphics, 184);
    }

    private void line(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        // One vertical span per screen column keeps cost bounded by graph width, even for sharp
        // changes.
        int dx = x2 - x1;
        if (dx <= 0) {
            graphics.fill(x1, Math.min(y1, y2), x1 + 1, Math.max(y1, y2) + 1, color);
            return;
        }
        for (int x = 0; x < dx; x++) {
            int a = y1 + (y2 - y1) * x / dx, b = y1 + (y2 - y1) * (x + 1) / dx;
            graphics.fill(x1 + x, Math.min(a, b), x1 + x + 1, Math.max(a, b) + 1, color);
        }
    }

    private void networks(GuiGraphics graphics) {
        draw(graphics, text("networks_title"), 23, TEXT);
        if (menu.data().page() != 3) {
            draw(graphics, text("loading"), 40, LABEL);
            return;
        }
        var values = menu.getAllNetworks();
        if (values.isEmpty()) {
            draw(graphics, text("no_networks"), 40, LABEL);
            return;
        }
        for (int i = 0; i < values.size(); i++) {
            var value = values.get(i);
            int y = 37 + i * 12;
            graphics.drawString(
                    font,
                    text(value.formed() ? "network_active" : "network_inactive"),
                    8,
                    y,
                    value.formed() ? GREEN : RED,
                    false);
            graphics.drawString(font, font.plainSubstrByWidth(value.id(), 108), 42, y, TEXT, false);
            graphics.drawString(
                    font,
                    String.format(Locale.ROOT, "%.0f%%", 100 * menu.getNetworkFraction(i)),
                    154,
                    y,
                    LABEL,
                    false);
            String dimension = dimensionName(value.dimension()).getString();
            graphics.drawString(font, font.plainSubstrByWidth(dimension, 90), 202, y, LABEL, false);
        }
        graphics.drawCenteredString(
                font,
                text(
                        "network_rows",
                        menu.data().networkOffset() + 1,
                        menu.data().networkOffset() + values.size(),
                        menu.data().networkCount()),
                imageWidth / 2,
                190,
                LABEL);
    }

    private static Component dimensionName(String id) {
        return switch (id) {
            case "minecraft:overworld" -> text("dimension.overworld");
            case "minecraft:the_nether" -> text("dimension.nether");
            case "minecraft:the_end" -> text("dimension.end");
            default -> {
                String key = "dimension." + id.replace(':', '.');
                yield net.minecraft.client.resources.language.I18n.exists(key)
                        ? Component.translatable(key)
                        : Component.literal(id);
            }
        };
    }

    private void admin(GuiGraphics graphics) {
        if (!menu.isOp()) {
            draw(graphics, text("admin_only"), 26, RED);
            return;
        }
        draw(graphics, text("target_id"), 30, LABEL);
        draw(graphics, text("value"), 66, LABEL);
        draw(graphics, text("admin_delete_hint"), 142, LABEL);
        draw(graphics, text("admin_energy_hint"), 156, LABEL);
        draw(graphics, text("admin_capacity_hint"), 170, LABEL);
        if (adminValueField != null
                && !adminValueField.getValue().isEmpty()
                && adminValue() == null) draw(graphics, text("invalid_number"), 188, RED);
    }
}
