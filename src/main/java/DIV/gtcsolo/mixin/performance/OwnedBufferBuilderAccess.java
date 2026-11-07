package DIV.gtcsolo.mixin.performance;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.nio.ByteBuffer;

/** Used only to detach storage from the private builder owned by the mega world-preview batches. */
@Mixin(BufferBuilder.class)
public interface OwnedBufferBuilderAccess {
    @Accessor("buffer")
    ByteBuffer gtcsolo$getOwnedStorage();

    @Accessor("buffer")
    void gtcsolo$setOwnedStorage(ByteBuffer storage);
}
