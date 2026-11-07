package DIV.gtcsolo.mixin.performance;

import com.gregtechceu.gtceu.client.renderer.MultiblockInWorldPreviewRenderer;
import com.lowdragmc.lowdraglib.client.scene.WorldSceneRenderer;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Reuses the native render/rotation implementation without starting its unbounded worker. */
@Mixin(value = MultiblockInWorldPreviewRenderer.class, remap = false)
public interface MegaWorldPreviewAccess {
    @Accessor("LEVEL")
    static void gtcsolo$megaSetLevel(TrackedDummyWorld level) { throw new AssertionError(); }

    @Accessor("BLOCK_ENTITIES")
    static void gtcsolo$megaSetBlockEntities(Set<BlockPos> positions) { throw new AssertionError(); }

    @Accessor("CACHE_STATE")
    static AtomicReference<?> gtcsolo$megaCacheState() { throw new AssertionError(); }

    @Accessor("LEFT_TICK")
    static AtomicInteger gtcsolo$megaLeftTick() { throw new AssertionError(); }

    @Accessor("THREAD")
    static Thread gtcsolo$megaGetThread() { throw new AssertionError(); }

    @Accessor("THREAD")
    static void gtcsolo$megaSetThread(Thread thread) { throw new AssertionError(); }

    @Invoker("renderBlocks")
    static void gtcsolo$megaRenderBlocks(TrackedDummyWorld level, PoseStack pose,
                                       BlockRenderDispatcher dispatcher, RenderType layer,
                                       WorldSceneRenderer.VertexConsumerWrapper consumer,
                                       Collection<BlockPos> positions) { throw new AssertionError(); }

    @Invoker("rotateByFrontAxis")
    static BlockPos gtcsolo$megaRotateByFrontAxis(BlockPos position, Direction front, Rotation rotation) {
        throw new AssertionError();
    }
}
