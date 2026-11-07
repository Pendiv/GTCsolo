package DIV.gtcsolo.integration.jei.performance;

import DIV.gtcsolo.Gtcsolo;
import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.gui.widget.PatternPreviewWidget;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.gregtechceu.gtceu.integration.jei.multipage.MultiblockInfoCategory;
import com.gregtechceu.gtceu.integration.jei.multipage.MultiblockInfoWrapper;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.jei.ModularUIRecipeCategory;
import com.lowdragmc.lowdraglib.jei.ModularWrapper;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import com.mojang.blaze3d.systems.RenderSystem;
import cpw.mods.modlauncher.api.INameMappingService;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Native recipe/widget types are retained. Only render-world construction is postponed.
 * Search metadata uses native clone/drop/sort code for the first valid native page.
 */
public final class LazyNativeMultiblockPreviews {
    private static final int REGION_STRIDE = 500;
    private static final int MAX_METADATA_CELLS = 1_000_000;
    private static final Field LEVEL = field(PatternPreviewWidget.class, "LEVEL");
    private static final Field NEXT_REGION = field(PatternPreviewWidget.class, "LAST_POS");
    private static final Field CACHE = field(PatternPreviewWidget.class, "CACHE");
    private static final Field PARTS = partsField();
    private static final Field INFO_TAG = field(BlockInfo.class, "tag");
    private static final Field INFO_ITEM = field(BlockInfo.class, "itemStack");
    private static final Field INFO_CALLBACK = field(BlockInfo.class, "postCreate");
    private static final Field INFO_ENTITY = field(BlockInfo.class, "lastEntity");
    private static final Field MACHINE_FACTORY = field(MachineDefinition.class, "machineSupplier");
    private static final Method INITIALIZE = initializeMethod();
    private static final ThreadLocal<Context> REGISTRATION = new ThreadLocal<>();
    private static final ThreadLocal<Descriptor> MATERIALIZING = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> METADATA = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> VERIFYING_INDEX = new ThreadLocal<>();
    private static final Map<MultiblockMachineDefinition, Descriptor> DESCRIPTORS = new IdentityHashMap<>();
    private static final Map<Class<?>, Boolean> CLONE_METHODS = new IdentityHashMap<>();

    private LazyNativeMultiblockPreviews() {}

    public static void runRegistration(IRecipeRegistration registration, Runnable action) {
        Context previous = REGISTRATION.get();
        if (previous == null) DESCRIPTORS.clear();
        if (!JeiOptimizationOptions.nativePreviewLazyEnabled()) {
            action.run();
            return;
        }
        Context context = new Context(registration.getIngredientManager());
        REGISTRATION.set(context);
        try {
            action.run();
        } finally {
            if (previous == null) REGISTRATION.remove();
            else REGISTRATION.set(previous);
            Gtcsolo.LOGGER.info("[JEI-preview] deferred={} unchangedNativeWidgets={} rawVerified={} indexVerified={} candidateNativeBuildsDuringRegistration={} mismatches={} fallback={}",
                    context.deferred, context.unchanged, context.verified, context.indexVerified, context.fullBuilds,
                    context.mismatches, context.fallbacks);
        }
    }

    public static boolean isMetadataPass() { return Boolean.TRUE.equals(METADATA.get()); }

    public static boolean shouldDefer(MultiblockMachineDefinition definition) {
        if (REGISTRATION.get() == null || MATERIALIZING.get() != null || isMetadataPass()
                || !RenderSystem.isOnRenderThread() || !available()
                || definition.getClass() != MultiblockMachineDefinition.class
                || !definition.getId().getNamespace().equals("gtceu")) return false;
        try {
            return !((Map<?, ?>) CACHE.get(null)).containsKey(definition);
        } catch (IllegalAccessException unavailable) {
            return false;
        }
    }

    /** Returns an actual MBPattern[] of length zero, without writing a partial native cache entry. */
    public static Object emptyNativePatterns() {
        return Array.newInstance(PARTS.getDeclaringClass(), 0);
    }

    public static void finishConstruction(MultiblockInfoWrapper wrapper) {
        Context context = REGISTRATION.get();
        if (context == null) return;
        Widget widget = raw(wrapper);
        if (!(widget instanceof DeferredPatternPreviewWidget marker) || !marker.gtcsolo$isDeferredPatternPreview()) {
            context.unchanged++;
            return;
        }
        Descriptor descriptor = null;
        try {
            if (wrapper.getClass() != MultiblockInfoWrapper.class) throw new UnsupportedOperationException("wrapper-subclass");
            descriptor = DESCRIPTORS.get(wrapper.definition);
            if (descriptor == null) descriptor = prepare((PatternPreviewWidget) widget, wrapper.definition);
            State state = new State(wrapper.definition, descriptor);
            ((LazyNativePreviewRecipe) wrapper).gtcsolo$setLazyNativePreview(state);
            if (JeiOptimizationOptions.nativePreviewVerificationEnabled()) {
                PatternPreviewWidget nativeWidget = materialize(wrapper.definition, descriptor);
                List<ItemStack> actual = nativeCachedInputs(wrapper.definition).orElseThrow();
                if (!sameStacks(descriptor.inputs, actual)) {
                    context.mismatches++;
                    Gtcsolo.LOGGER.warn("[JEI-preview] metadata mismatch definition={}; using native UI/index", wrapper.definition.getId());
                    DESCRIPTORS.remove(wrapper.definition);
                } else {
                    context.verified++;
                    state.metadataMatched = true;
                }
                install(wrapper, widget, nativeWidget);
                state.loaded = true;
                descriptor.shapes = null;
            } else {
                DESCRIPTORS.put(wrapper.definition, descriptor);
                context.deferred++;
            }
        } catch (ReflectiveOperationException | RuntimeException failure) {
            if (failure instanceof ShapeProviderFailure provider) throw provider.original;
            Exception detail = failure;
            if (failure instanceof MetadataFailure metadata) {
                descriptor = metadata.fallback;
                detail = metadata.reason;
            }
            String reason = detail instanceof UnsupportedOperationException ? detail.getMessage() : detail.getClass().getSimpleName();
            context.fallbacks.merge(reason, 1L, Long::sum);
            DESCRIPTORS.remove(wrapper.definition);
            State state = new State(wrapper.definition, descriptor);
            ((LazyNativePreviewRecipe) wrapper).gtcsolo$setLazyNativePreview(state);
            // Unknown metadata takes the complete original path before JEI builds its index.
            PatternPreviewWidget nativeWidget = materialize(wrapper.definition, descriptor);
            install(wrapper, widget, nativeWidget);
            state.loaded = true;
            if (descriptor != null) descriptor.shapes = null;
        }
    }

    public static boolean indexOnly(ModularUIRecipeCategory<?> category, IRecipeLayoutBuilder builder,
                                    Object recipe, IFocusGroup focuses) {
        if (Boolean.TRUE.equals(VERIFYING_INDEX.get())) return false;
        if (category.getClass() != MultiblockInfoCategory.class || !(recipe instanceof MultiblockInfoWrapper wrapper)
                || !(recipe instanceof LazyNativePreviewRecipe lazy)) return false;
        State state = lazy.gtcsolo$getLazyNativePreview();
        if (state == null || state.descriptor == null) return false;
        if (state.loaded) {
            if (JeiOptimizationOptions.nativePreviewVerificationEnabled() && state.metadataMatched && !state.indexChecked
                    && NativePreviewIngredientIndex.isIngredientIndexBuilder(builder)) {
                verifyIndex(category, builder, wrapper, focuses, state);
            }
            return false;
        }
        if (!NativePreviewIngredientIndex.isIngredientIndexBuilder(builder)) {
            ensureLoaded(wrapper);
            return false;
        }
        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStacks(copy(state.descriptor.inputs));
        return true;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void verifyIndex(ModularUIRecipeCategory category, IRecipeLayoutBuilder actual,
                                    MultiblockInfoWrapper recipe, IFocusGroup focuses, State state) {
        Context context = REGISTRATION.get();
        if (context == null) return;
        state.indexChecked = true;
        try {
            var constructor = actual.getClass().getConstructor(IIngredientManager.class);
            var nativeBuilder = (IRecipeLayoutBuilder) constructor.newInstance(context.manager);
            var fastBuilder = (IRecipeLayoutBuilder) constructor.newInstance(context.manager);
            VERIFYING_INDEX.set(Boolean.TRUE);
            try { category.setRecipe(nativeBuilder, recipe, focuses); }
            finally { VERIFYING_INDEX.remove(); }
            fastBuilder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStacks(copy(state.descriptor.inputs));
            if (NativeGtRecipeIngredientIndex.sameIngredientSuppliers(nativeBuilder, fastBuilder, context.manager)) {
                context.indexVerified++;
            } else {
                context.mismatches++;
                Gtcsolo.LOGGER.warn("[JEI-preview] ingredient index mismatch definition={}; using native index", recipe.definition.getId());
            }
        } catch (ReflectiveOperationException | RuntimeException failure) {
            context.fallbacks.merge("index-verification-exception", 1L, Long::sum);
            Gtcsolo.LOGGER.warn("[JEI-preview] index verification failed definition={} reason={}",
                    recipe.definition.getId(), failure.toString());
        }
    }

    public static void ensureLoaded(ModularWrapper<?> wrapper) {
        if (!(wrapper instanceof LazyNativePreviewRecipe lazy)) return;
        State state = lazy.gtcsolo$getLazyNativePreview();
        if (state == null || state.loaded) return;
        Widget previous = raw(wrapper);
        state.value.install(widget -> install(wrapper, previous, widget));
        state.loaded = true;
        if (state.descriptor != null) state.descriptor.shapes = null;
    }

    /** Also used when an introduction requests the native widget directly. */
    public static PatternPreviewWidget materializeReserved(MultiblockMachineDefinition definition) {
        if (MATERIALIZING.get() != null || isMetadataPass() || shouldDefer(definition)) return null;
        Descriptor descriptor = DESCRIPTORS.get(definition);
        return descriptor == null ? null : materialize(definition, descriptor);
    }

    public static List<MultiblockShapeInfo> shapesForNative(MultiblockMachineDefinition definition) {
        Descriptor descriptor = MATERIALIZING.get();
        if (descriptor != null && descriptor.definition == definition && descriptor.shapes != null) return descriptor.shapes;
        return definition.getMatchingShapes();
    }

    public static Optional<List<ItemStack>> cachedInputs(MultiblockMachineDefinition definition) {
        Descriptor descriptor = DESCRIPTORS.get(definition);
        return descriptor == null ? Optional.empty() : Optional.of(copy(descriptor.inputs));
    }

    private static Descriptor prepare(PatternPreviewWidget worker, MultiblockMachineDefinition definition)
            throws ReflectiveOperationException {
        List<MultiblockShapeInfo> originals;
        try { originals = List.copyOf(definition.getMatchingShapes()); }
        catch (RuntimeException failure) { throw new ShapeProviderFailure(failure); }
        BlockPos start = (BlockPos) NEXT_REGION.get(null);
        try { return prepareMetadata(worker, definition, originals, start); }
        catch (ReflectiveOperationException | RuntimeException failure) {
            // Even an unsupported provider is evaluated only once: fallback consumes its original DTOs.
            throw new MetadataFailure(failure, new Descriptor(definition, originals, List.of(), start, false));
        }
    }

    private static Descriptor prepareMetadata(PatternPreviewWidget worker, MultiblockMachineDefinition definition,
                                               List<MultiblockShapeInfo> originals, BlockPos start)
            throws ReflectiveOperationException {
        List<BlockInfo> ownedInfos = new ArrayList<>();
        List<MultiblockShapeInfo> shapes = freeze(originals, ownedInfos);
        if (shapes.isEmpty()) throw new UnsupportedOperationException("empty-shapes");
        int reserved = Math.multiplyExact(REGION_STRIDE, shapes.size());
        TrackedDummyWorld previousLevel = (TrackedDummyWorld) LEVEL.get(null);
        TrackedDummyWorld metadataLevel = new TrackedDummyWorld();
        Boolean previousMetadata = METADATA.get();
        boolean success = false;
        try {
            LEVEL.set(null, metadataLevel);
            METADATA.set(Boolean.TRUE);
            for (MultiblockShapeInfo shape : shapes) {
                Object pattern;
                try { pattern = INITIALIZE.invoke(worker, shape, new HashSet<>()); }
                catch (InvocationTargetException error) { throw nativeFailure(error); }
                if (pattern == null) continue;
                Object parts = PARTS.get(pattern);
                if (!(parts instanceof List<?> lists)) throw new UnsupportedOperationException("parts-layout");
                List<ItemStack> inputs = NativePreviewIngredientIndex.copyIndexedInputs(lists).orElseThrow();
                success = true;
                return new Descriptor(definition, shapes, inputs, start, true);
            }
            throw new UnsupportedOperationException("no-valid-controller-page");
        } finally {
            try {
                LEVEL.set(null, previousLevel);
                NEXT_REGION.set(null, success ? start.offset(reserved, 0, reserved) : start);
            } finally {
                if (previousMetadata == null) METADATA.remove();
                else METADATA.set(previousMetadata);
                // No display world, block map or live layout is retained by the metadata cache.
                metadataLevel.clear();
                // A metadata controller's MultiblockState captures its temporary level permanently.
                // Visual materialization must create fresh native entities in the real preview level.
                for (BlockInfo info : ownedInfos) info.clearBlockEntityCache();
            }
        }
    }

    private static PatternPreviewWidget materialize(MultiblockMachineDefinition definition, Descriptor descriptor) {
        Descriptor previous = MATERIALIZING.get();
        BlockPos next = null;
        boolean complete = false;
        try {
            next = (BlockPos) NEXT_REGION.get(null);
            if (descriptor != null) NEXT_REGION.set(null, descriptor.regionStart);
            MATERIALIZING.set(descriptor == null ? Descriptor.nativeFallback(definition) : descriptor);
            Context context = REGISTRATION.get();
            if (context != null && !((Map<?, ?>) CACHE.get(null)).containsKey(definition)) context.fullBuilds++;
            PatternPreviewWidget widget = PatternPreviewWidget.getPatternWidget(definition);
            complete = true;
            return widget;
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Native preview region unavailable: " + definition.getId(), failure);
        } finally {
            try {
                if (descriptor != null && descriptor.reserved && next != null) {
                    try { NEXT_REGION.set(null, next); }
                    catch (IllegalAccessException failure) { throw new IllegalStateException(failure); }
                }
            } finally {
                if (previous == null) MATERIALIZING.remove();
                else MATERIALIZING.set(previous);
                if (complete && descriptor != null) descriptor.shapes = null;
            }
        }
    }

    private static void install(ModularWrapper<?> wrapper, Widget previous, PatternPreviewWidget nativeWidget) {
        var group = wrapper.modularUI.mainGroup;
        if (previous != nativeWidget) {
            try {
                group.removeWidget(previous);
                group.addWidget(nativeWidget);
                ((ModularWrapperWidgetAccess) wrapper).gtcsolo$setRawWrappedWidget(nativeWidget);
            } catch (RuntimeException | Error failure) {
                try {
                    group.removeWidget(nativeWidget);
                    if (!group.widgets.contains(previous)) group.addWidget(previous);
                } catch (RuntimeException | Error rollback) { failure.addSuppressed(rollback); }
                throw failure;
            }
        }
    }

    private static Widget raw(ModularWrapper<?> wrapper) {
        return ((ModularWrapperWidgetAccess) wrapper).gtcsolo$getRawWrappedWidget();
    }

    private static List<MultiblockShapeInfo> freeze(List<MultiblockShapeInfo> originals, List<BlockInfo> ownedInfos)
            throws ReflectiveOperationException {
        List<BlockInfo[][][]> blocks = new ArrayList<>(originals.size());
        for (MultiblockShapeInfo shape : originals) {
            if (shape.getClass() != MultiblockShapeInfo.class) throw new UnsupportedOperationException("shape-subclass");
            blocks.add(shape.getBlocks());
        }
        return PreviewShapeMetadata.freeze(blocks, info -> {
                    BlockInfo clone = freezeInfo(info);
                    ownedInfos.add(clone);
                    return clone;
                },
                MAX_METADATA_CELLS, REGION_STRIDE).stream().map(MultiblockShapeInfo::new).toList();
    }

    @SuppressWarnings("unchecked")
    private static BlockInfo freezeInfo(BlockInfo info) throws ReflectiveOperationException {
        if (info == null || info.getClass() != BlockInfo.class || INFO_ENTITY.get(info) != null)
            throw new UnsupportedOperationException("shared-live-block-info");
        BlockState state = info.getBlockState();
        Block block = state.getBlock();
        var id = ForgeRegistries.BLOCKS.getKey(block);
        if (id == null || !(id.getNamespace().equals("minecraft") || id.getNamespace().equals("gtceu")))
            throw new UnsupportedOperationException("external-block");
        Boolean known = CLONE_METHODS.get(block.getClass());
        if (known == null) {
            String name = ObfuscationReflectionHelper.remapName(INameMappingService.Domain.METHOD, "m_7397_");
            Class<?> owner = block.getClass().getMethod(name, BlockGetter.class, BlockPos.class, BlockState.class).getDeclaringClass();
            known = owner == Block.class || owner == MetaMachineBlock.class || owner == LiquidBlock.class;
            CLONE_METHODS.put(block.getClass(), known);
        }
        if (!known) throw new UnsupportedOperationException("custom-pick-block");
        if (block instanceof MetaMachineBlock machine) {
            Object factory = MACHINE_FACTORY.get(machine.definition);
            if (!machine.definition.getId().getNamespace().equals("gtceu") || factory == null
                    || !factory.getClass().getName().startsWith("com.gregtechceu.gtceu."))
                throw new UnsupportedOperationException("custom-machine-factory");
        }
        Consumer<BlockEntity> callback = (Consumer<BlockEntity>) INFO_CALLBACK.get(info);
        if (callback != null) throw new UnsupportedOperationException("block-callback");
        ItemStack item = (ItemStack) INFO_ITEM.get(info);
        BlockInfo clone = new BlockInfo(state, info.hasBlockEntity(), item == null ? null : item.copy(), callback);
        CompoundTag tag = (CompoundTag) INFO_TAG.get(info);
        if (tag != null) clone.setTag(tag.copy());
        return clone;
    }

    private static Optional<List<ItemStack>> nativeCachedInputs(MultiblockMachineDefinition definition)
            throws IllegalAccessException {
        Object array = ((Map<?, ?>) CACHE.get(null)).get(definition);
        if (array == null || Array.getLength(array) == 0) return Optional.of(List.of());
        Object parts = PARTS.get(Array.get(array, 0));
        return parts instanceof List<?> lists ? NativePreviewIngredientIndex.copyIndexedInputs(lists) : Optional.empty();
    }

    private static boolean sameStacks(List<ItemStack> expected, List<ItemStack> actual) {
        if (expected.size() != actual.size()) return false;
        for (int i = 0; i < expected.size(); i++) {
            ItemStack one = expected.get(i), two = actual.get(i);
            if (one.getCount() != two.getCount() || !ItemStack.matches(one, two)
                    || !ItemHandlerHelper.canItemStacksStack(one, two)) return false;
        }
        return true;
    }

    private static List<ItemStack> copy(List<ItemStack> stacks) {
        return stacks.stream().map(ItemStack::copy).toList();
    }

    private static RuntimeException nativeFailure(InvocationTargetException error) {
        Throwable cause = error.getCause();
        if (cause instanceof RuntimeException runtime) return runtime;
        if (cause instanceof Error fatal) throw fatal;
        return new IllegalStateException(cause);
    }

    private static boolean available() {
        return LEVEL != null && NEXT_REGION != null && CACHE != null && PARTS != null && INITIALIZE != null
                && INFO_TAG != null && INFO_ITEM != null && INFO_CALLBACK != null && INFO_ENTITY != null && MACHINE_FACTORY != null;
    }

    private static Field field(Class<?> owner, String name) {
        try { Field field = owner.getDeclaredField(name); field.setAccessible(true); return field; }
        catch (ReflectiveOperationException | RuntimeException unavailable) { return null; }
    }

    private static Field partsField() {
        for (Class<?> nested : PatternPreviewWidget.class.getDeclaredClasses())
            if (nested.getSimpleName().equals("MBPattern")) return field(nested, "parts");
        return null;
    }

    private static Method initializeMethod() {
        try {
            Method method = PatternPreviewWidget.class.getDeclaredMethod("initializePattern", MultiblockShapeInfo.class, HashSet.class);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException | RuntimeException unavailable) { return null; }
    }

    public static final class State {
        private final Descriptor descriptor;
        private final DeferredPreviewValue<PatternPreviewWidget> value;
        private boolean loaded;
        private boolean metadataMatched;
        private boolean indexChecked;
        private State(MultiblockMachineDefinition definition, Descriptor descriptor) {
            this.descriptor = descriptor;
            this.value = new DeferredPreviewValue<>(() -> materialize(definition, descriptor));
        }
    }

    private static final class Descriptor {
        final MultiblockMachineDefinition definition;
        List<MultiblockShapeInfo> shapes;
        final List<ItemStack> inputs;
        final BlockPos regionStart;
        final boolean reserved;
        Descriptor(MultiblockMachineDefinition definition, List<MultiblockShapeInfo> shapes,
                   List<ItemStack> inputs, BlockPos regionStart, boolean reserved) {
            this.definition = definition; this.shapes = shapes; this.inputs = inputs; this.regionStart = regionStart;
            this.reserved = reserved;
        }
        static Descriptor nativeFallback(MultiblockMachineDefinition definition) {
            return new Descriptor(definition, null, List.of(), null, false);
        }
    }

    private static final class MetadataFailure extends RuntimeException {
        final Exception reason;
        final Descriptor fallback;
        MetadataFailure(Exception reason, Descriptor fallback) { this.reason = reason; this.fallback = fallback; }
    }

    private static final class ShapeProviderFailure extends RuntimeException {
        final RuntimeException original;
        ShapeProviderFailure(RuntimeException original) { this.original = original; }
    }

    private static final class Context {
        final IIngredientManager manager;
        long deferred, unchanged, verified, indexVerified, fullBuilds, mismatches;
        final Map<String, Long> fallbacks = new LinkedHashMap<>();
        Context(IIngredientManager manager) { this.manager = manager; }
    }
}
