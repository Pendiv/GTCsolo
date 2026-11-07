package DIV.gtcsolo.mixin;

import DIV.gtcsolo.worldgen.AmeijiaTerrain;
import com.mojang.datafixers.util.Either;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

@Mixin(ChunkStatus.class)
public abstract class AmeijiaTerrainMixin {
    @Inject(method = "generate", at = @At("HEAD"))
    private void gtcsolo$finishAmeijiaTerrain(Executor executor, ServerLevel level, ChunkGenerator generator,
            StructureTemplateManager structures, ThreadedLevelLightEngine lighting,
            Function<ChunkAccess, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> fullChunk,
            List<ChunkAccess> neighbors,
            CallbackInfoReturnable<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> cir) {
        // LIGHT depends on neighboring INITIALIZE_LIGHT: their decoration has finished too.
        // Run before source propagation so the replacement fluid's actual luminosity is used.
        if ((Object) this == ChunkStatus.LIGHT && level.dimension().equals(AmeijiaTerrain.DIMENSION))
            AmeijiaTerrain.finishTerrain(level.dimension(), neighbors.get(neighbors.size() / 2));
    }
}
