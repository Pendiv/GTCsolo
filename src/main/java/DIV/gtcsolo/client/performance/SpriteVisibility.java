package DIV.gtcsolo.client.performance;

import DIV.gtcsolo.Gtcsolo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Objects;
import java.util.Set;

/** Keeps sprites used by cached custom geometry visible to optional animation tracking. */
public final class SpriteVisibility {
    private static final AnimationApi API = findApi();
    private static final Capture NO_CAPTURE = () -> {};
    private static volatile Thread captureThread;
    private static Set<TextureAtlasSprite> captureTarget;
    private static CaptureScope activeScope;

    private SpriteVisibility() {}

    public static boolean isAvailable() { return API != null; }

    /** One capture per baking batch, rather than a wrapper or allocation per quad. */
    public static synchronized Capture capture(Set<TextureAtlasSprite> target) {
        Objects.requireNonNull(target);
        if (API == null) return NO_CAPTURE;
        Thread thread = Thread.currentThread();
        if (captureThread != null && captureThread != thread)
            throw new IllegalStateException("Sprite capture is already active on another thread");
        CaptureScope scope = new CaptureScope(thread, target, activeScope);
        activeScope = scope;
        captureTarget = target;
        captureThread = thread;
        return scope;
    }

    /** Called by the quad accessor mixin; ordinary rendering does not retain any sprites. */
    public static void record(TextureAtlasSprite sprite) {
        Thread thread = captureThread;
        if (thread != null && thread == Thread.currentThread() && sprite != null) captureTarget.add(sprite);
    }

    public static void markVisible(Iterable<TextureAtlasSprite> sprites) {
        if (API == null) return;
        for (TextureAtlasSprite sprite : sprites) markVisible(sprite);
    }

    public static void markVisible(TextureAtlasSprite sprite) {
        // The optional mod may be present while its animation-tracking mixins are disabled.
        if (API == null || sprite == null || !API.contentsType.isInstance(sprite.contents())) return;
        try {
            API.mark.invokeExact(sprite);
        } catch (Throwable failure) {
            throw new IllegalStateException("Cannot mark a visible Embeddium sprite", failure);
        }
    }

    public static void markAtlasTexture(ResourceLocation spriteId) {
        if (API != null && spriteId != null) {
            // Resolve the current atlas rather than retaining a sprite across resource reloads.
            markVisible(Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(spriteId));
        }
    }

    private static AnimationApi findApi() {
        try {
            ClassLoader loader = SpriteVisibility.class.getClassLoader();
            Class<?> util = Class.forName("me.jellysquid.mods.sodium.client.render.texture.SpriteUtil", false, loader);
            Class<?> contents = Class.forName("me.jellysquid.mods.sodium.client.render.texture.SpriteContentsExtended", false, loader);
            MethodHandle mark = MethodHandles.publicLookup().findStatic(util, "markSpriteActive",
                    MethodType.methodType(void.class, TextureAtlasSprite.class));
            return new AnimationApi(mark, contents);
        } catch (ClassNotFoundException absent) {
            return null;
        } catch (ReflectiveOperationException | LinkageError failure) {
            Gtcsolo.LOGGER.warn("Optional sprite visibility API is unavailable", failure);
            return null;
        }
    }

    private record AnimationApi(MethodHandle mark, Class<?> contentsType) {}

    public interface Capture extends AutoCloseable {
        @Override void close();
    }

    /** Metadata attached only to textures originating from an atlas sprite. */
    public interface AtlasTexture {
        ResourceLocation gtcsolo$getSpriteSource();
        void gtcsolo$setSpriteSource(ResourceLocation source);
    }

    private static final class CaptureScope implements Capture {
        private final Thread owner;
        private final Set<TextureAtlasSprite> target;
        private final CaptureScope previous;
        private boolean closed;

        private CaptureScope(Thread owner, Set<TextureAtlasSprite> target, CaptureScope previous) {
            this.owner = owner;
            this.target = target;
            this.previous = previous;
        }

        @Override public void close() {
            synchronized (SpriteVisibility.class) {
                if (closed) return;
                if (Thread.currentThread() != owner)
                    throw new IllegalStateException("Sprite capture must close on its owning thread");
                if (activeScope != this)
                    throw new IllegalStateException("Sprite captures must close in reverse opening order");
                captureThread = null;
                activeScope = previous;
                captureTarget = previous == null ? null : previous.target;
                if (previous != null) captureThread = owner;
                closed = true;
            }
        }
    }
}
