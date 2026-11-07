package DIV.gtcsolo.client.gleam;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.mixin.performance.OwnedBufferBuilderAccess;
import DIV.gtcsolo.registry.UtilityEffects;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Reuses model vertices and vanilla's outline shaders, with a private depth-tested mask. */
@Mod.EventBusSubscriber(modid = Gtcsolo.MODID, value = Dist.CLIENT)
public final class GleamRayMobOutlines {
    private static final ResourceLocation SHADER = new ResourceLocation("minecraft", "shaders/post/entity_outline.json");
    private static ClientLevel level;
    private static PostChain chain;
    private static RenderTarget mask;
    private static MaskBuffers buffers;
    private static boolean failed;
    private static int width, height;

    private GleamRayMobOutlines() {}

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || chain == null) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !mc.player.hasEffect(UtilityEffects.GLEAM_RAY.get())) release();
    }

    public static void beginFrame() {
        var mc = Minecraft.getInstance();
        if (level != mc.level || mc.player == null || !mc.player.hasEffect(UtilityEffects.GLEAM_RAY.get())) {
            release();
            level = mc.level;
        } else if (buffers != null) {
            buffers.discard();
        }
    }

    public static MultiBufferSource wrap(Entity entity, float partialTick, MultiBufferSource original) {
        var mc = Minecraft.getInstance();
        if (!(entity instanceof Mob mob) || !mob.isAlive() || mc.player == null || mc.level == null
                || mob.isInvisibleTo(mc.player) || mc.shouldEntityAppearGlowing(entity) || entity.hasCustomOutlineRendering(mc.player)
                || !mc.levelRenderer.shouldShowEntityOutlines()) return original;
        var effect = mc.player.getEffect(UtilityEffects.GLEAM_RAY.get());
        if (effect == null) return original;
        var eye = mc.player.getEyePosition(partialTick);
        var box = mob.getBoundingBox();
        if (!GleamRayMobRules.inRange(effect.getAmplifier(), mc.level.getMaxLocalRawBrightness(mob.blockPosition()),
                mc.level.getMaxLocalRawBrightness(BlockPos.containing(mob.getEyePosition(partialTick))),
                eye.x, eye.y, eye.z, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ)
                || !prepare()) return original;
        return type -> {
            VertexConsumer normal = original.getBuffer(type);
            Optional<RenderType> outline = type.outline();
            if (outline.isEmpty()) return normal;
            VertexConsumer silhouette = buffers.capture(outline.get());
            return silhouette == null ? normal : VertexMultiConsumer.create(new GleamRayMobMask(silhouette), normal);
        };
    }

    private static boolean prepare() {
        if (failed) return false;
        var mc = Minecraft.getInstance();
        var main = mc.getMainRenderTarget();
        if (main.width <= 0 || main.height <= 0) return false;
        try {
            if (chain == null) {
                chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), main, SHADER);
                mask = chain.getTempTarget("final");
                if (mask == null) throw new IllegalStateException("Missing native outline final target");
                buffers = new MaskBuffers();
                width = height = 0;
            }
            if (width != main.width || height != main.height) {
                width = main.width; height = main.height;
                chain.resize(width, height);
            }
            return true;
        } catch (Exception error) {
            release();
            failed = true;
            LogUtils.getLogger().warn("Gleam Ray mob outlines unavailable until resource reload", error);
            return false;
        } finally {
            main.bindWrite(false);
        }
    }

    public static void finishFrame(float partialTick) {
        if (buffers == null || !buffers.pending()) return;
        var mc = Minecraft.getInstance();
        var main = mc.getMainRenderTarget();
        RenderSystem.backupProjectionMatrix();
        try {
            // All world geometry has finished writing depth before the captured silhouette is drawn.
            mask.clear(Minecraft.ON_OSX);
            mask.copyDepthFrom(main);
            main.bindWrite(false);
            buffers.endBatch();
            chain.process(partialTick);
            main.bindWrite(false);
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ZERO,
                    GlStateManager.DestFactor.ONE);
            mask.blitToScreen(width, height, false);
        } finally {
            main.bindWrite(false);
            RenderSystem.restoreProjectionMatrix();
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            RenderSystem.colorMask(true, true, true, true);
        }
    }

    public static void release() {
        if (chain != null) chain.close();
        if (buffers != null) buffers.close();
        chain = null; mask = null; buffers = null; level = null;
        width = height = 0;
    }

    private static final class MaskBuffers extends MultiBufferSource.BufferSource {
        private static final int MAX_TYPES = 256;
        private final Map<RenderType, RenderType> types = new IdentityHashMap<>();

        MaskBuffers() { super(new BufferBuilder(256), new LinkedHashMap<>()); }

        VertexConsumer capture(RenderType nativeType) {
            RenderType type = types.get(nativeType);
            if (type == null) {
                if (types.size() >= MAX_TYPES) return null;
                type = new VisibleOutline(nativeType);
                types.put(nativeType, type);
                fixedBuffers.put(type, new BufferBuilder(256));
            }
            return super.getBuffer(type);
        }

        boolean pending() { return !startedBuffers.isEmpty(); }

        void discard() {
            for (BufferBuilder buffer : startedBuffers) buffer.discard();
            startedBuffers.clear();
            lastState = Optional.empty();
        }

        void close() {
            discard();
            for (BufferBuilder buffer : fixedBuffers.values()) free(buffer);
            free(builder);
            fixedBuffers.clear(); types.clear();
        }

        private static void free(BufferBuilder builder) {
            var owned = (OwnedBufferBuilderAccess) (Object) builder;
            var memory = owned.gtcsolo$getOwnedStorage();
            if (memory != null) {
                owned.gtcsolo$setOwnedStorage(null);
                MemoryUtil.getAllocator(false).free(MemoryUtil.memAddress0(memory));
            }
        }
    }

    private static final class VisibleOutline extends RenderType {
        VisibleOutline(RenderType nativeType) {
            super("gtcsolo_visible_mob_outline", nativeType.format(), nativeType.mode(), 256, false, false,
                    () -> {
                        nativeType.setupRenderState();
                        mask.bindWrite(false);
                        RenderSystem.enableDepthTest();
                        RenderSystem.depthFunc(GL11.GL_LEQUAL);
                        RenderSystem.depthMask(false);
                    }, () -> {
                        nativeType.clearRenderState();
                        Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
                    });
        }
    }

    @Mod.EventBusSubscriber(modid = Gtcsolo.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Reload {
        @SubscribeEvent public static void register(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((ResourceManagerReloadListener) manager -> {
                release();
                failed = false;
            });
        }
    }
}
