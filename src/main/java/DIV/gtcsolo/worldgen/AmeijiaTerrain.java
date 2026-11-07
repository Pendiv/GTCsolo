package DIV.gtcsolo.worldgen;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.ticks.ProtoChunkTicks;
import net.minecraft.world.ticks.ScheduledTick;

import java.util.HashMap;
import java.util.Map;

/** A single material substitution after native decoration, before block-light propagation. */
public final class AmeijiaTerrain {
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION,
            new ResourceLocation("gtcsolo", "ameijia"));
    private static volatile Map<BlockState, BlockState> replacements;

    private AmeijiaTerrain() {}

    public static boolean shouldReplace(ResourceKey<Level> dimension, ChunkAccess chunk) {
        return dimension.equals(DIMENSION) && chunk instanceof ProtoChunk
                && !chunk.getStatus().isOrAfter(ChunkStatus.LIGHT);
    }

    public static void finishTerrain(ResourceKey<Level> dimension, ChunkAccess chunk) {
        if (!shouldReplace(dimension, chunk)) return;
        var mapping = replacements;
        if (mapping == null) {
            // Resolve optional KubeJS blocks only after registries have finished loading.
            var states = new HashMap<>(replacementStates(resolve("gtcsolo:ameijia_stone", Blocks.STONE),
                    resolve("gtcsolo:ameijia_deepslate", Blocks.DEEPSLATE),
                    resolve("kubejs:overheat_magma", Blocks.LAVA)));
            DIV.gtcsolo.registry.material.AmeijiaOres.addTerrainReplacements(states);
            mapping = Map.copyOf(states);
            replacements = mapping;
        }
        replaceMaterials(chunk, mapping);
    }

    private static Block resolve(String id, Block fallback) {
        return BuiltInRegistries.BLOCK.getOptional(new ResourceLocation(id)).orElseGet(() -> {
            LogUtils.getLogger().warn("Ameijia terrain: {} is missing; retaining {}", id,
                    BuiltInRegistries.BLOCK.getKey(fallback));
            return fallback;
        });
    }

    static Map<BlockState, BlockState> replacementStates(Block stone, Block deepslate, Block magma) {
        var states = new HashMap<BlockState, BlockState>();
        mapStates(states, Blocks.STONE, stone);
        mapStates(states, Blocks.DEEPSLATE, deepslate);
        mapStates(states, Blocks.LAVA, magma);
        return Map.copyOf(states);
    }

    private static void mapStates(Map<BlockState, BlockState> states, Block original, Block replacement) {
        if (original == replacement) return;
        for (var state : original.getStateDefinition().getPossibleStates()) {
            var target = replacement.defaultBlockState();
            for (var property : state.getProperties()) {
                if (target.hasProperty(property)) target = copyProperty(state, target, property);
            }
            states.put(state, target);
        }
    }

    private static <T extends Comparable<T>> BlockState copyProperty(BlockState from, BlockState to,
                                                                     Property<T> property) {
        return to.setValue(property, from.getValue(property));
    }

    static int replaceMaterials(ChunkAccess chunk, Map<BlockState, BlockState> mapping) {
        int changed = 0;
        for (var section : chunk.getSections()) {
            // Inspect the palette first; air, water and unrelated sections require no cell scan.
            if (section.hasOnlyAir() || !section.getStates().maybeHas(mapping::containsKey)) continue;
            section.acquire();
            try {
                for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                    var target = mapping.get(section.getBlockState(x, y, z));
                    if (target == null) continue;
                    section.setBlockState(x, y, z, target, false);
                    changed++;
                }
            } finally {
                section.release();
            }
        }
        if (changed > 0) {
            transferFluidTicks(chunk);
            chunk.initializeLightSources();
            chunk.setUnsaved(true);
        }
        return changed;
    }

    private static void transferFluidTicks(ChunkAccess chunk) {
        if (!(chunk.getFluidTicks() instanceof ProtoChunkTicks<Fluid> ticks)) return;
        long order = 0;
        for (var tick : ticks.scheduledTicks()) {
            if (tick.type() != Fluids.LAVA && tick.type() != Fluids.FLOWING_LAVA) continue;
            var fluid = chunk.getFluidState(tick.pos()).getType();
            if (fluid == Fluids.EMPTY || fluid == tick.type()) continue;
            ticks.schedule(new ScheduledTick<>(fluid, tick.pos(), tick.delay(), tick.priority(), order++));
        }
        // Native tick containers have no removal API here. Obsolete lava ticks fail the normal
        // fluid-type check once; only their replacements run. Do not schedule every lake cell.
    }
}
