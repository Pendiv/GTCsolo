package DIV.gtcsolo.render.material.portal;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;

/** Keeps the original silhouette for the world portal pass without placing vertices in chunk buffers. */
public final class GalacticaBlockModel extends BakedModelWrapper<BakedModel> {
    private final EnumMap<Direction, List<BakedQuad>> faces = new EnumMap<>(Direction.class);
    private final List<BakedQuad> unculled;
    private final boolean frame;

    public GalacticaBlockModel(BakedModel original, BlockState state, boolean frame) {
        super(original);
        this.frame = frame;
        for (Direction face : Direction.values()) {
            faces.put(face, silhouette(original.getQuads(state, face, RandomSource.create(42))));
        }
        unculled = silhouette(original.getQuads(state, null, RandomSource.create(42)));
    }

    private static List<BakedQuad> silhouette(List<BakedQuad> quads) {
        return PortalItemModel.silhouette(quads.stream()
                .filter(q -> !q.getSprite().contents().name().getPath().endsWith("_secondary"))
                .toList());
    }

    public List<BakedQuad> portalFaces(@Nullable Direction side) {
        return side == null ? unculled : faces.getOrDefault(side, List.of());
    }

    public boolean isFrame() { return frame; }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
        return GalacticaRenderContext.isPreview() ? originalModel.getQuads(state, side, random) : List.of();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random,
                                    ModelData data, @Nullable RenderType renderType) {
        if (GalacticaRenderContext.isPreview()) return originalModel.getQuads(state, side, random, data, renderType);
        // The breaking overlay is a vanilla model pass with a null layer.
        return renderType == null ? portalFaces(side) : List.of();
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data) {
        return ChunkRenderTypeSet.of(RenderType.cutout());
    }
}
