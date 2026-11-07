package DIV.gtcsolo.integration.jei.multiblock;

import DIV.gtcsolo.Gtcsolo;
import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.api.GTValues;
import com.lowdragmc.lowdraglib.gui.texture.*;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.zip.GZIPInputStream;

/** Sparse scene with bounded loading/baking and native LDLib camera and picking. */
final class MegaPreviewWidget extends WidgetGroup {
    static final int WIDTH = 260, HEIGHT = 255;
    private static final int PARTS_WIDTH = 90, PARTS_X = WIDTH - PARTS_WIDTH - 2;
    private static final long FRAME_BUDGET = 8_000_000L;
    private final MegaPreviewRecipe recipe;
    private final BiConsumer<ItemStack, Boolean> openRecipe;
    private final Map<Character, BlockInfo> palette = new HashMap<>();
    private final Set<Character> optionalSymbols = new HashSet<>();
    private final Map<Character, Integer> counts = new LinkedHashMap<>();
    private final List<BlockState> coils = new ArrayList<>();
    private final List<Character> portExamples = new ArrayList<>();
    private final List<ButtonWidget> controls = new ArrayList<>();
    private final List<ImageWidget> lines = new ArrayList<>();
    private final DraggableScrollableWidgetGroup parts;
    private MegaPreviewCursor cursor;
    private PreviewWorld world;
    private SparseScene scene;
    private MegaPreviewMesh mesh;
    private int layer = -1, coilIndex;
    private int portsSeen;
    private boolean showOptional = true, loaded;
    private String failure, selected = "";

    MegaPreviewWidget(MegaPreviewRecipe recipe, BiConsumer<ItemStack, Boolean> openRecipe) {
        super(0, 0, WIDTH, availableHeight());
        this.recipe = recipe; this.openRecipe = openRecipe; setClientSideWidget();
        addWidget(new ImageWidget(1, 1, WIDTH - 2, 11, new TextTexture(recipe.definition().getDescriptionId())
                .setWidth(WIDTH - 4).setType(TextTexture.TextType.ROLL)));
        button(16, "R", "reset", this::resetCamera);
        button(39, "C", "core", () -> {
            if (scene != null && world.controller != null)
                scene.setCenter(world.controller.getCenter().toVector3f()).setZoom(14);
        });
        button(62, "+", "layer_up", () -> setLayer(layer < 0 ? 0 : layer + 1));
        button(85, "-", "layer_down", () -> setLayer(layer < 0 && cursor != null ? cursor.height() - 1 : layer - 1));
        button(108, "A", "all_layers", () -> setLayer(-1));
        button(131, "F", "optional", () -> { showOptional = !showOptional; rebuild(); });
        button(154, "P", "coil", () -> {
            if (coils.size() > 1) { coilIndex = (coilIndex + 1) % coils.size(); rebuild(); refreshParts(); }
        });
        line(0, this::status);
        line(0, () -> (selected.isEmpty() ? "" : selected + " / ") + text("layer", layer < 0 ? text("all") : Integer.toString(layer),
                showOptional ? text("shown") : text("hidden")));
        parts = new DraggableScrollableWidgetGroup(PARTS_X, 16, PARTS_WIDTH, HEIGHT - 90)
                .setYScrollBarWidth(4).setYBarStyle(GuiTextures.SLIDER_BACKGROUND, GuiTextures.BUTTON)
                .setScrollable(true).setDraggable(false).setUseScissor(true);
        parts.setScrollWheelDirection(DraggableScrollableWidgetGroup.ScrollWheelDirection.VERTICAL);
        parts.setClientSideWidget(); addWidget(parts);
        relayout();
    }
    static int availableHeight() {
        return Math.max(160, Math.min(HEIGHT, Minecraft.getInstance().getWindow().getGuiScaledHeight() - 80));
    }
    private void relayout() {
        int height = availableHeight(); setSize(WIDTH, height);
        for (int i = 0; i < controls.size(); i++) controls.get(i).setSelfPosition(2 + i * 36, height - 70);
        for (int i = 0; i < lines.size(); i++) lines.get(i).setSelfPosition(2, height - 52 + i * 13);
        parts.setSize(PARTS_WIDTH, height - 90);
        if (scene != null) scene.setSize(PARTS_X - 4, height - 86);
    }
    private void line(int y, java.util.function.Supplier<String> value) {
        var line = new ImageWidget(2, y, WIDTH - 4, 12, new TextTexture("").setSupplier(value)
                .setWidth(WIDTH - 4).setType(TextTexture.TextType.ROLL));
        lines.add(line); addWidget(line);
    }
    private void button(int y, String label, String tip, Runnable action) {
        var button = new ButtonWidget(222, y, 34, 16,
                new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture(label)), click -> action.run());
        button.setHoverTooltips(Component.translatable("gtcsolo.mega_preview." + tip)); controls.add(button); addWidget(button);
    }
    @Override public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        MegaPreviewLifecycle.touch(this);
        if (getSize().height != availableHeight()) relayout();
        if (world == null && failure == null && Minecraft.getInstance().level != null) start();
        if (cursor != null && !loaded && failure == null) {
            try {
                cursor.step(16_384, System.nanoTime() + FRAME_BUDGET, this::addCell);
                if (cursor.done()) { loaded = true; refreshParts(); rebuild(); }
            } catch (Exception exception) { fail(exception); }
        }
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
    }
    private void start() {
        try {
            var stream = MegaPreviewWidget.class.getResourceAsStream("/data/gtcsolo/multiblocks/" + recipe.resource() + ".json.gz");
            if (stream == null) throw new IllegalStateException("Missing mega preview " + recipe.resource());
            cursor = new MegaPreviewCursor(new InputStreamReader(new GZIPInputStream(stream), StandardCharsets.UTF_8));
            world = new PreviewWorld();
            for (var entry : cursor.palette().entrySet()) {
                char symbol = entry.getKey().charAt(0); var spec = entry.getValue().getAsJsonObject();
                var state = block(spec.get("block").getAsString());
                for (var property : spec.getAsJsonObject("state").entrySet())
                    state = property(state, property.getKey(), property.getValue().getAsString());
                palette.put(symbol, BlockInfo.fromBlockState(state));
                if (spec.has("optional") && spec.get("optional").getAsBoolean()) optionalSymbols.add(symbol);
            }
            palette.put('K', BlockInfo.fromBlockState(recipe.definition().getBlock().defaultBlockState()));
            palette.put(recipe.port(), BlockInfo.fromBlockState(block(recipe.portBlock())));
            if (recipe.port()!='\0') {
            int powerTier = recipe.resource().equals("ore_processing_factory") ? GTValues.UHV : GTValues.UV;
            example(GTMachines.ENERGY_INPUT_HATCH[powerTier].getBlock().defaultBlockState());
            example(GTMachines.ITEM_IMPORT_BUS[GTValues.ULV].getBlock().defaultBlockState());
            example(GTMachines.ITEM_EXPORT_BUS[GTValues.ULV].getBlock().defaultBlockState());
            example(GTMachines.FLUID_IMPORT_HATCH[GTValues.LV].getBlock().defaultBlockState());
            if (!recipe.resource().equals("ore_processing_factory"))
                example(GTMachines.FLUID_EXPORT_HATCH[GTValues.LV].getBlock().defaultBlockState());
            }
            if (recipe.variableCoils()) {
                GTCEuAPI.HEATING_COILS.entrySet().stream().sorted(Comparator.comparingInt(e -> e.getKey().getCoilTemperature()))
                        .forEach(e -> coils.add(e.getValue().get().defaultBlockState()));
                palette.put('E', BlockInfo.fromBlockState(GTBlocks.COIL_CUPRONICKEL.getDefaultState())); coilIndex = 0;
            }
            scene = new SparseScene(world); scene.setOnSelected((pos, face) -> inspect(pos));
            scene.setBeforeWorldRender(ignored -> {
                if (mesh == null || failure != null) return;
                try { mesh.step(System.nanoTime() + FRAME_BUDGET); mesh.draw(); }
                catch (Exception exception) { fail(exception); }
            });
            addWidget(scene); relayout(); resetCamera();
        } catch (Exception exception) { fail(exception); }
    }
    private static BlockState block(String id) {
        var location = new ResourceLocation(id);
        if (!ForgeRegistries.BLOCKS.containsKey(location)) throw new IllegalStateException("Missing block " + id);
        return ForgeRegistries.BLOCKS.getValue(location).defaultBlockState();
    }
    private static <T extends Comparable<T>> BlockState setProperty(BlockState state, Property<T> p, String value) {
        return state.setValue(p, p.getValue(value).orElseThrow(() -> new IllegalStateException("Invalid property " + value)));
    }
    private static BlockState property(BlockState state, String key, String value) {
        var p = state.getBlock().getStateDefinition().getProperty(key);
        if (p == null) throw new IllegalStateException("Unknown property " + key);
        return setProperty(state, p, value);
    }
    private void addCell(int x, int y, int z, char symbol) {
        boolean port = symbol == recipe.port();
        if (port && portsSeen < portExamples.size()) symbol = portExamples.get(portsSeen++);
        var info = palette.get(symbol);
        if (info == null) throw new IllegalStateException("Unknown preview symbol " + symbol);
        if (info.getBlockState().isAir()) return;
        var pos = new BlockPos(x, y, z); world.addBlock(pos, info); scene.append(pos);
        if (optionalSymbols.contains(symbol)) world.optional.add(pos);
        if (port) world.ports.add(pos);
        if (symbol == 'K') world.controller = pos;
        counts.merge(symbol, 1, Integer::sum);
    }
    private void example(BlockState state) {
        char key = (char) (256 + portExamples.size());
        portExamples.add(key); palette.put(key, BlockInfo.fromBlockState(state));
    }
    private void setLayer(int next) {
        if (cursor == null) return;
        layer = Math.max(-1, Math.min(cursor.height() - 1, next)); rebuild();
    }
    private void resetCamera() {
        if (failure != null) { failure = null; releasePreview(); return; }
        if (scene != null && cursor != null) {
            scene.setCenter(new Vector3f(cursor.width() / 2f, cursor.height() / 2f, cursor.depth() / 2f));
            scene.setZoom(Math.max(cursor.width(), Math.max(cursor.height(), cursor.depth())) * 1.7f);
            scene.setCameraYawAndPitch(25, -135);
        }
    }
    private void rebuild() {
        if (mesh != null) mesh.close(); mesh = null;
        if (loaded && scene != null) {
            Collection<BlockPos> positions = scene.getCore();
            if (layer >= 0) {
                var slice = new ArrayList<BlockPos>();
                for (var pos : positions) if (pos.getY() == layer) slice.add(pos);
                positions = slice;
            }
            mesh = new MegaPreviewMesh(scene.getDummyWorld(), positions);
        }
    }
    private void inspect(BlockPos pos) {
        if (world == null) return;
        selected = text("selected", world.getBlockState(pos).getBlock().getName().getString(), pos.getX(), pos.getY(), pos.getZ());
        if (world.ports.contains(pos)) selected += " / " + Component.translatable(recipe.portTooltip()).getString();
        if (world.optional.contains(pos)) selected += " / " + text("optional");
    }
    private void refreshParts() {
        parts.clearAllWidgets(); int row = 0;
        record Part(net.minecraft.world.item.Item item, boolean optional, boolean port, boolean coil) {}
        Map<Part, Integer> totals = new LinkedHashMap<>();
        for (var entry : counts.entrySet()) {
            char symbol = entry.getKey();
            BlockState state = recipe.variableCoils() && symbol == 'E' && !coils.isEmpty()
                    ? coils.get(coilIndex) : palette.get(symbol).getBlockState();
            var item = state.getBlock().asItem();
            var part = new Part(item, optionalSymbols.contains(symbol), symbol == recipe.port() || portExamples.contains(symbol),
                    recipe.variableCoils() && symbol == 'E');
            totals.merge(part, entry.getValue(), Integer::sum);
        }
        for (var entry : totals.entrySet()) {
            var part = entry.getKey(); var stack = part.item().getDefaultInstance();
            if (stack.isEmpty()) continue;
            int y = row++ * 20;
            var button = new ButtonWidget(0, y, 18, 18,
                    new GuiTextureGroup(GuiTextures.SLOT, new ItemStackTexture(stack)),
                    click -> openRecipe.accept(stack, click.button == 1));
            var tips = new ArrayList<Component>(); tips.add(stack.getHoverName());
            tips.add(Component.translatable("gtcsolo.mega_preview.count", entry.getValue()));
            if (part.optional()) tips.add(Component.translatable("gtcsolo.mega_preview.optional"));
            if (part.port()) tips.add(Component.translatable(recipe.portTooltip()));
            if (part.coil()) tips.add(Component.translatable("gtcsolo.mega_preview.coil"));
            button.setHoverTooltips(tips); parts.addWidget(button);
            var label = new ButtonWidget(20, y, PARTS_WIDTH - 26, 18,
                    new TextTexture(Integer.toString(entry.getValue()))
                            .setColor(0xff202020).setDropShadow(false)
                            .setWidth(PARTS_WIDTH - 26).setType(TextTexture.TextType.LEFT_HIDE),
                    click -> openRecipe.accept(stack, click.button == 1));
            label.setHoverTooltips(tips); parts.addWidget(label);
        }
    }
    private String status() {
        if (failure != null) return text("failed", failure);
        if (cursor == null) return text("waiting");
        if (!loaded) return text("loading", cursor.visited() * 100 / cursor.volume());
        if (mesh != null && !mesh.done()) return text("meshing", mesh.percent());
        return text("ready", cursor.width(), cursor.height(), cursor.depth(), scene == null ? 0 : scene.getCore().size());
    }
    private static String text(String key, Object... args) {
        // TextTexture performs a second I18n/String.format pass over supplied strings.
        return Component.translatable("gtcsolo.mega_preview." + key, args).getString().replace("%", "%%");
    }
    private void fail(Exception exception) {
        Gtcsolo.LOGGER.error("Mega preview failed for {}", recipe.resource(), exception);
        failure = exception.getClass().getSimpleName(); releasePreview();
    }
    void releasePreview() {
        if (cursor != null) { try { cursor.close(); } catch (Exception ignored) {} }
        if (mesh != null) mesh.close(); mesh = null;
        if (scene != null) { scene.getDummyWorld().clear(); scene.getCore().clear(); removeWidget(scene); }
        if (world != null) { world.clear(); world.optional.clear(); world.ports.clear(); }
        scene = null; world = null; cursor = null; loaded = false;
        palette.clear(); optionalSymbols.clear(); counts.clear(); coils.clear(); portExamples.clear(); portsSeen = 0; parts.clearAllWidgets();
    }
    private final class PreviewWorld extends TrackedDummyWorld {
        final Set<BlockPos> optional = new HashSet<>(), ports = new HashSet<>();
        BlockPos controller;
        boolean visible(BlockPos pos) { return (layer < 0 || pos.getY() == layer) && (showOptional || !optional.contains(pos)); }
        @Override public BlockState getBlockState(BlockPos pos) {
            if (!visible(pos)) return Blocks.AIR.defaultBlockState();
            var original = super.getBlockState(pos);
            if (recipe.variableCoils() && original.is(GTBlocks.COIL_CUPRONICKEL.get()) && !coils.isEmpty()) return coils.get(coilIndex);
            return original;
        }
    }
    private static final class SparseScene extends SceneWidget {
        SparseScene(PreviewWorld world) {
            super(2, 14, WIDTH - 4, availableHeight() - 86, world); dummyWorld.setRenderFilter(world::visible);
            setRenderFacing(false); setHoverTips(true);
            setOnAddedTooltips((scene, tooltips) -> tooltips.add(Component.translatable("gtcsolo.mega_preview.controls")));
        }
        void append(BlockPos pos) { core.add(pos); }
    }
}
