package DIV.gtcsolo.performance;

import DIV.gtcsolo.Gtcsolo;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** Explicitly started, bounded diagnostic windows. No world state or recipe behavior is changed. */
@Mod.EventBusSubscriber(modid = Gtcsolo.MODID)
public final class PerformanceDiagnostics {
    private static final Samples FRAMES = new Samples(16_384);
    private static final Samples TICKS = new Samples(4_096);
    private static volatile boolean enabled;
    private static volatile long started;
    private static volatile long stopped;
    private static volatile long lastFrame;
    private static volatile Map<String, Object> clientState = Map.of();
    private static long tickStart;

    private PerformanceDiagnostics() {}

    public static boolean isEnabled() { return enabled; }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("gtcsolo_perf").requires(source -> source.hasPermission(2))
                .then(Commands.literal("start").executes(context -> {
                    enabled = false;
                    net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                            () -> DIV.gtcsolo.client.performance.ClientFrameDiagnostics::reset);
                    FRAMES.reset(); TICKS.reset(); PatternCheckProfiler.reset(); PatternCheckProfiler.setEnabled(true);
                    started = System.nanoTime(); stopped = 0; lastFrame = 0; tickStart = 0; enabled = true;
                    context.getSource().sendSuccess(() -> Component.literal("Performance window started."), false);
                    return 1;
                }))
                .then(Commands.literal("stop").executes(context -> { stopped = System.nanoTime(); enabled = false; tickStart = 0; PatternCheckProfiler.setEnabled(false); return 1; }))
                .then(Commands.literal("chunkRetry").then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(context -> {
                            FixedPatternChunkRetry.setEnabled(BoolArgumentType.getBool(context, "enabled"));
                            FixedPatternChunkRetry.resetCounters();
                            return 1;
                        })))
                .then(Commands.literal("coordinates").then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(context -> {
                            PatternCoordinateTransform.setEnabled(BoolArgumentType.getBool(context, "enabled"));
                            return 1;
                        })))
                .then(Commands.literal("predicateLoops").then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(context -> {
                            PatternPredicateLoop.setEnabled(BoolArgumentType.getBool(context, "enabled"));
                            return 1;
                        })))
                .then(Commands.literal("category").then(Commands.argument("id", StringArgumentType.greedyString())
                        .executes(context -> {
                            String id = StringArgumentType.getString(context, "id");
                            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                                    () -> () -> ClientFrames.openCategory(id));
                            return 1;
                        })))
                .then(Commands.literal("recipe").then(Commands.argument("item", StringArgumentType.greedyString())
                        .executes(context -> {
                            String id = StringArgumentType.getString(context, "item");
                            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                                    () -> () -> ClientFrames.openRecipe(id));
                            return 1;
                        })))
                .then(Commands.literal("reloadResources").executes(context -> {
                    net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                            net.minecraft.client.Minecraft.getInstance().execute(() ->
                                    net.minecraft.client.Minecraft.getInstance().reloadResourcePacks()));
                    return 1;
                }))
                .then(Commands.literal("corePreview").then(Commands.argument("x", IntegerArgumentType.integer())
                        .then(Commands.argument("y", IntegerArgumentType.integer())
                        .then(Commands.argument("z", IntegerArgumentType.integer()).executes(context -> {
                            int x = IntegerArgumentType.getInteger(context, "x");
                            int y = IntegerArgumentType.getInteger(context, "y");
                            int z = IntegerArgumentType.getInteger(context, "z");
                            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                                    () -> () -> ClientFrames.corePreview(x, y, z));
                            return 1;
                        })))))
                .then(Commands.literal("clearPreview").executes(context -> {
                    net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                            net.minecraft.client.Minecraft.getInstance().execute(
                                    com.gregtechceu.gtceu.client.renderer.MultiblockInWorldPreviewRenderer::cleanPreview));
                    return 1;
                }))
                .then(Commands.literal("scroll").then(Commands.argument("x", IntegerArgumentType.integer(0))
                        .then(Commands.argument("y", IntegerArgumentType.integer(0))
                        .then(Commands.argument("delta", IntegerArgumentType.integer(-20, 20)).executes(context -> {
                            int x = IntegerArgumentType.getInteger(context, "x");
                            int y = IntegerArgumentType.getInteger(context, "y");
                            int delta = IntegerArgumentType.getInteger(context, "delta");
                            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                                    () -> () -> ClientFrames.scroll(x, y, delta));
                            return 1;
                        })))))
                .then(Commands.literal("click").then(Commands.argument("x", IntegerArgumentType.integer(0))
                        .then(Commands.argument("y", IntegerArgumentType.integer(0))
                        .then(Commands.argument("button", IntegerArgumentType.integer(0, 2)).executes(context -> {
                            int x = IntegerArgumentType.getInteger(context, "x");
                            int y = IntegerArgumentType.getInteger(context, "y");
                            int button = IntegerArgumentType.getInteger(context, "button");
                            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                                    () -> () -> ClientFrames.click(x, y, button));
                            return 1;
                        })))))
                .then(Commands.literal("window").then(Commands.argument("width", IntegerArgumentType.integer(640, 7680))
                        .then(Commands.argument("height", IntegerArgumentType.integer(480, 4320)).executes(context -> {
                            int width = IntegerArgumentType.getInteger(context, "width");
                            int height = IntegerArgumentType.getInteger(context, "height");
                            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                                    net.minecraft.client.Minecraft.getInstance().execute(() -> {
                                        var window = net.minecraft.client.Minecraft.getInstance().getWindow();
                                        org.lwjgl.glfw.GLFW.glfwSetWindowSize(window.getWindow(), width, height);
                                    }));
                            return 1;
                        }))))
                .then(Commands.literal("unconstrainedCells").then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(context -> {
                            UnconstrainedPatternCells.setEnabled(BoolArgumentType.getBool(context, "enabled"));
                            return 1;
                        })))
                .then(Commands.literal("quit").executes(context -> {
                    context.getSource().getServer().saveEverything(true, true, true);
                    net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                            () -> () -> net.minecraft.client.Minecraft.getInstance().execute(
                                    () -> net.minecraft.client.Minecraft.getInstance().stop()));
                    return 1;
                }))
                .then(Commands.literal("status").executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal(new GsonBuilder().create().toJson(snapshot())), false);
                    return 1;
                }))
                .then(Commands.literal("export").then(Commands.argument("label", StringArgumentType.word())
                        .executes(context -> export(context.getSource(), StringArgumentType.getString(context, "label"))))));
    }

    private static int export(CommandSourceStack source, String label) {
        if (!label.matches("[A-Za-z0-9_-]{1,80}")) return 0;
        try {
            var directory = FMLPaths.GAMEDIR.get().resolve("gtcsolo-performance");
            Files.createDirectories(directory);
            var target = directory.resolve(label + ".json");
            var data = new LinkedHashMap<>(snapshot());
            var worlds = new LinkedHashMap<String, Object>();
            for (var level : source.getServer().getAllLevels()) {
                int entities = 0;
                for (var entity : level.getAllEntities()) entities++;
                worlds.put(level.dimension().location().toString(), Map.of(
                        "chunk_holders", level.getChunkSource().getLoadedChunksCount(),
                        "forced_chunks", level.getForcedChunks().size(),
                        "entities", entities));
            }
            data.put("worlds", worlds);
            Files.writeString(target, new GsonBuilder().setPrettyPrinting().create().toJson(data));
            source.sendSuccess(() -> Component.literal("Performance data: " + target), false);
            return 1;
        } catch (Exception failure) {
            Gtcsolo.LOGGER.warn("Could not export performance diagnostics", failure);
            return 0;
        }
    }

    public static Map<String, Object> snapshot() {
        var result = new LinkedHashMap<String, Object>();
        result.put("enabled", enabled);
        result.put("elapsed_seconds", started == 0 ? 0 : ((stopped == 0 ? System.nanoTime() : stopped) - started) / 1_000_000_000.0);
        result.put("frames", FRAMES.snapshot()); result.put("server_ticks", TICKS.snapshot());
        result.put("client", clientState);
        result.put("frame_phases", net.minecraftforge.fml.DistExecutor.unsafeRunForDist(
                () -> DIV.gtcsolo.client.performance.ClientFrameDiagnostics::snapshot,
                () -> () -> Map.of()));
        result.put("mega_world_preview", net.minecraftforge.fml.DistExecutor.unsafeRunForDist(
                () -> DIV.gtcsolo.client.performance.MegaWorldPreview::snapshot,
                () -> () -> Map.of()));
        var runtime = Runtime.getRuntime();
        result.put("heap_used_bytes", runtime.totalMemory() - runtime.freeMemory());
        result.put("heap_committed_bytes", runtime.totalMemory()); result.put("heap_max_bytes", runtime.maxMemory());
        result.put("patterns", PatternCheckProfiler.snapshot());
        result.put("unconstrained_cells", UnconstrainedPatternCells.snapshot());
        result.put("fixed_pattern_chunk_retry", FixedPatternChunkRetry.snapshot());
        result.put("pattern_coordinates", PatternCoordinateTransform.snapshot());
        result.put("pattern_predicate_loops", PatternPredicateLoop.snapshot());
        return result;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (!enabled) return;
        if (event.phase == TickEvent.Phase.START) tickStart = System.nanoTime();
        else {
            if (tickStart >= started && tickStart != 0) TICKS.add(System.nanoTime() - tickStart);
            tickStart = 0;
        }
    }

    @Mod.EventBusSubscriber(modid = Gtcsolo.MODID, value = Dist.CLIENT)
    public static final class ClientFrames {
        private static long lastState;
        private static void corePreview(int x, int y, int z) {
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                var minecraft = net.minecraft.client.Minecraft.getInstance();
                if (minecraft.level == null) return;
                var position = new net.minecraft.core.BlockPos(x, y, z);
                if (minecraft.level.getBlockEntity(position) instanceof com.gregtechceu.gtceu.api.machine.IMachineBlockEntity holder
                        && holder.getMetaMachine() instanceof com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine controller) {
                    com.gregtechceu.gtceu.client.renderer.MultiblockInWorldPreviewRenderer.showPreview(position, controller, 1200);
                }
            });
        }
        private static void click(int x, int y, int button) {
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                var minecraft = net.minecraft.client.Minecraft.getInstance();
                var screen = minecraft.screen;
                if (screen == null || x >= screen.width || y >= screen.height) return;
                var window = minecraft.getWindow();
                org.lwjgl.glfw.GLFW.glfwSetCursorPos(window.getWindow(),
                        x * (double) window.getScreenWidth() / screen.width,
                        y * (double) window.getScreenHeight() / screen.height);
                if (!net.minecraftforge.client.ForgeHooksClient.onScreenMouseClickedPre(screen, x, y, button)) {
                    boolean handled = screen.mouseClicked(x, y, button);
                    net.minecraftforge.client.ForgeHooksClient.onScreenMouseClickedPost(screen, x, y, button, handled);
                }
                var released = minecraft.screen;
                if (released != null && !net.minecraftforge.client.ForgeHooksClient.onScreenMouseReleasedPre(released, x, y, button)) {
                    boolean handled = released.mouseReleased(x, y, button);
                    net.minecraftforge.client.ForgeHooksClient.onScreenMouseReleasedPost(released, x, y, button, handled);
                }
            });
        }
        private static void scroll(int x, int y, int delta) {
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                var screen = net.minecraft.client.Minecraft.getInstance().screen;
                if (screen == null || x >= screen.width || y >= screen.height) return;
                if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                        new net.minecraftforge.client.event.ScreenEvent.MouseScrolled.Pre(screen, x, y, delta))) return;
                if (!screen.mouseScrolled(x, y, delta)) net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                        new net.minecraftforge.client.event.ScreenEvent.MouseScrolled.Post(screen, x, y, delta));
            });
        }
        private static void openCategory(String id) {
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                var runtime = DIV.gtcsolo.integration.jei.GtcSoloJeiPlugin.getRuntimeOrNull();
                if (runtime == null) return;
                var category = runtime.getRecipeManager().createRecipeCategoryLookup().get()
                        .filter(value -> value.getRecipeType().getUid().toString().equals(id)).findFirst();
                category.ifPresent(value -> runtime.getRecipesGui().showTypes(java.util.List.of(value.getRecipeType())));
            });
        }
        private static void openRecipe(String id) {
            net.minecraft.client.Minecraft.getInstance().execute(() -> {
                var runtime = DIV.gtcsolo.integration.jei.GtcSoloJeiPlugin.getRuntimeOrNull();
                var location = net.minecraft.resources.ResourceLocation.tryParse(id);
                if (runtime == null || location == null || !net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(location)) return;
                var stack = new net.minecraft.world.item.ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(location));
                runtime.getRecipesGui().show(java.util.List.of(runtime.getJeiHelpers().getFocusFactory().createFocus(
                        mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT, mezz.jei.api.constants.VanillaTypes.ITEM_STACK, stack)));
            });
        }
        @SubscribeEvent
        public static void render(TickEvent.RenderTickEvent event) {
            if (!enabled || event.phase != TickEvent.Phase.END) return;
            long now = System.nanoTime();
            long previous = lastFrame; lastFrame = now;
            if (previous != 0) FRAMES.add(now - previous);
            if (now - lastState < 1_000_000_000L) return;
            lastState = now;
            var minecraft = net.minecraft.client.Minecraft.getInstance();
            var state = new LinkedHashMap<String, Object>();
            state.put("renderer", org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_RENDERER));
            state.put("window_active", minecraft.isWindowActive());
            state.put("screen", minecraft.screen == null ? "world" : minecraft.screen.getClass().getSimpleName());
            state.put("paused", minecraft.isPaused());
            state.put("render_distance", minecraft.options.renderDistance().get());
            state.put("simulation_distance", minecraft.options.simulationDistance().get());
            state.put("width", minecraft.getWindow().getWidth()); state.put("height", minecraft.getWindow().getHeight());
            if (minecraft.player != null) {
                state.put("x", minecraft.player.getX()); state.put("y", minecraft.player.getY());
                state.put("z", minecraft.player.getZ()); state.put("yaw", minecraft.player.getYRot());
                state.put("pitch", minecraft.player.getXRot());
            }
            clientState = Map.copyOf(state);
        }
    }

    private static final class Samples {
        private final long[] values;
        private int count, next;
        private long totalCount, totalNanos, maximum;
        private final java.util.ArrayDeque<Map<String, Object>> slowSamples = new java.util.ArrayDeque<>();
        Samples(int capacity) { values = new long[capacity]; }
        synchronized void reset() { count = next = 0; totalCount = totalNanos = maximum = 0; slowSamples.clear(); }
        synchronized void add(long nanos) {
            values[next] = nanos; next = (next + 1) % values.length;
            count = Math.min(count + 1, values.length); totalCount++; totalNanos += nanos; maximum = Math.max(maximum, nanos);
            if (nanos >= 100_000_000L) {
                if (slowSamples.size() == 128) slowSamples.removeFirst();
                slowSamples.addLast(Map.of("end_unix_ms", System.currentTimeMillis(), "duration_ms", nanos / 1_000_000.0));
            }
        }
        synchronized Map<String, Object> snapshot() {
            var copy = Arrays.copyOf(values, count); Arrays.sort(copy);
            var result = new LinkedHashMap<String, Object>();
            result.put("samples", totalCount); result.put("retained_samples", count);
            result.put("mean_ms", totalCount == 0 ? 0 : totalNanos / (totalCount * 1_000_000.0));
            result.put("p50_ms", percentile(copy, .50)); result.put("p95_ms", percentile(copy, .95));
            result.put("p99_ms", percentile(copy, .99)); result.put("max_ms", maximum / 1_000_000.0);
            result.put("slow_samples", java.util.List.copyOf(slowSamples));
            return result;
        }
        private static double percentile(long[] sorted, double quantile) {
            return sorted.length == 0 ? 0 : sorted[(int) Math.ceil(quantile * sorted.length) - 1] / 1_000_000.0;
        }
    }
}
