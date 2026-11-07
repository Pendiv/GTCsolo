package DIV.gtcsolo.render.material.portal;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/** Native portal shader on cached silhouette geometry; no per-frame texture generation or ticking. */
public final class PortalItemModel extends BakedModelWrapper<BakedModel> {
    private static final List<RenderType> PORTAL = List.of(RenderType.endPortal());
    private final List<BakedModel> passes = List.of(this);
    private final EnumMap<Direction, List<BakedQuad>> faces = new EnumMap<>(Direction.class);
    private final List<BakedQuad> unculled;

    public PortalItemModel(BakedModel original) {
        super(original);
        for (Direction face : Direction.values()) {
            faces.put(face, silhouette(original.getQuads(null, face, RandomSource.create(42))));
        }
        unculled = silhouette(original.getQuads(null, null, RandomSource.create(42)));
    }

    static List<BakedQuad> silhouette(List<BakedQuad> source) {
        List<BakedQuad> result = new ArrayList<>();
        for (BakedQuad quad : source) {
            var sprite = quad.getSprite();
            int[] data = quad.getVertices();
            int stride = data.length / 4;
            if (stride < 6) throw new IllegalArgumentException("Invalid item vertex format");
            float[][] vertex = new float[4][5];
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 3; j++) vertex[i][j] = Float.intBitsToFloat(data[i * stride + j]);
                vertex[i][3] = sprite.getUOffset(Float.intBitsToFloat(data[i * stride + 4])) / 16f;
                vertex[i][4] = sprite.getVOffset(Float.intBitsToFloat(data[i * stride + 5])) / 16f;
            }
            int width = sprite.contents().width(), height = sprite.contents().height();
            int columns = steps(vertex[0], vertex[3], width, height);
            int rows = steps(vertex[0], vertex[1], width, height);
            boolean[][] mask = new boolean[rows][columns];
            for (int y = 0; y < rows; y++) for (int x = 0; x < columns; x++) {
                float u = interpolate(vertex, 3, (x + 0.5f) / columns, (y + 0.5f) / rows);
                float v = interpolate(vertex, 4, (x + 0.5f) / columns, (y + 0.5f) / rows);
                int px = Math.max(0, Math.min(width - 1, (int) (u * width)));
                int py = Math.max(0, Math.min(height - 1, (int) (v * height)));
                // Native item alpha testing discards values below 0.1. Galactica uses static native masks.
                mask[y][x] = (sprite.getPixelRGBA(0, px, py) >>> 24) >= 26;
            }
            for (var rect : PortalMaskRectangles.cover(mask)) {
                float left = rect.x() / (float) columns, right = (rect.x() + rect.width()) / (float) columns;
                float top = rect.y() / (float) rows, bottom = (rect.y() + rect.height()) / (float) rows;
                float[][] positions = {{left, top}, {left, bottom}, {right, bottom}, {right, top}};
                int[] clipped = data.clone();
                for (int i = 0; i < 4; i++) {
                    float x = positions[i][0], y = positions[i][1];
                    for (int j = 0; j < 3; j++) clipped[i * stride + j] = Float.floatToRawIntBits(interpolate(vertex, j, x, y));
                    clipped[i * stride + 3] = -1;
                    clipped[i * stride + 4] = Float.floatToRawIntBits(sprite.getU(interpolate(vertex, 3, x, y) * 16));
                    clipped[i * stride + 5] = Float.floatToRawIntBits(sprite.getV(interpolate(vertex, 4, x, y) * 16));
                }
                result.add(new BakedQuad(clipped, -1, quad.getDirection(), sprite, false));
            }
        }
        return List.copyOf(result);
    }

    private static int steps(float[] start, float[] end, int width, int height) {
        return Math.max(1, (int) Math.ceil(Math.max(Math.abs(end[3] - start[3]) * width,
                Math.abs(end[4] - start[4]) * height) - 0.001f));
    }

    private static float interpolate(float[][] vertex, int coordinate, float x, float y) {
        return (1 - x) * ((1 - y) * vertex[0][coordinate] + y * vertex[1][coordinate])
                + x * ((1 - y) * vertex[3][coordinate] + y * vertex[2][coordinate]);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
        return side == null ? unculled : faces.get(side);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random,
                                     ModelData data, @Nullable RenderType type) {
        return getQuads(state, side, random);
    }

    @Override
    public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) { return passes; }

    @Override
    public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous) { return PORTAL; }

    @Override
    public BakedModel applyTransform(ItemDisplayContext context, PoseStack pose, boolean leftHand) {
        originalModel.applyTransform(context, pose, leftHand);
        return this;
    }

    @Override
    public boolean isCustomRenderer() { return false; }

    @Override
    public boolean usesBlockLight() { return false; }
}
