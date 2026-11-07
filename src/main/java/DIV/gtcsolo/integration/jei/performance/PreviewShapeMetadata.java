package DIV.gtcsolo.integration.jei.performance;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Snapshots provider arrays while preserving shared BlockInfo identities across all pages. */
public final class PreviewShapeMetadata {
    private PreviewShapeMetadata() {}

    @FunctionalInterface
    public interface Copier<T, E extends Exception> { T copy(T value) throws E; }

    public static <T, E extends Exception> List<T[][][]> freeze(List<T[][][]> originals,
                                                              Copier<T, E> copier,
                                                              int maximumCells, int regionStride) throws E {
        List<T[][][]> result = new ArrayList<>(originals.size());
        Map<T, T> copied = new IdentityHashMap<>();
        int cells = 0;
        for (T[][][] source : originals) {
            if (source.length > regionStride) throw new UnsupportedOperationException("wide-metadata");
            T[][][] blocks = source.clone();
            for (int x = 0; x < source.length; x++) {
                blocks[x] = source[x].clone();
                for (int y = 0; y < source[x].length; y++) {
                    if (source[x][y].length > regionStride) throw new UnsupportedOperationException("wide-metadata");
                    cells = Math.addExact(cells, source[x][y].length);
                    if (cells > maximumCells) throw new UnsupportedOperationException("large-metadata");
                    blocks[x][y] = source[x][y].clone();
                    for (int z = 0; z < source[x][y].length; z++) {
                        T original = source[x][y][z];
                        T clone = copied.get(original);
                        if (clone == null) {
                            clone = copier.copy(original);
                            copied.put(original, clone);
                        }
                        blocks[x][y][z] = clone;
                    }
                }
            }
            result.add(blocks);
        }
        return List.copyOf(result);
    }
}
