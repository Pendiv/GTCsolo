package DIV.gtcsolo.mixin;

import com.gregtechceu.gtceu.client.renderer.MultiblockInWorldPreviewRenderer;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import java.util.Collection;

/** Feed a copied layout to GT's renderer without registering a fake GT machine. Client-only. */
@Mixin(value = MultiblockInWorldPreviewRenderer.class, remap = false)
public interface CopyPreviewRendererAccess {
    @Accessor("LEVEL") static void gtcsolo$setPreviewLevel(TrackedDummyWorld level) { throw new AssertionError(); }
    @Accessor("LEVEL") static TrackedDummyWorld gtcsolo$getPreviewLevel() { throw new AssertionError(); }
    @Invoker("prepareBuffers") static void gtcsolo$prepareBuffers(TrackedDummyWorld level, Collection<BlockPos> positions, int duration) { throw new AssertionError(); }
}
