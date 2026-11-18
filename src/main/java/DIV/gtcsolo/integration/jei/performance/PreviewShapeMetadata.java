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
        var snapshot = new Snapshot<>(copier, maximumCells, regionStride);
        List<T[][][]> result = new ArrayList<>(originals.size());
        for (T[][][] source : originals) result.add(snapshot.freeze(source));
        return List.copyOf(result);
    }

    static final class Snapshot<T, E extends Exception> {
        private final Copier<T, E> copier;
        private final int maximumCells;
        private final int regionStride;
        private final Map<T, T> copied = new IdentityHashMap<>();
        private int cells;

        Snapshot(Copier<T, E> copier, int maximumCells, int regionStride) {
            this.copier = copier;
            this.maximumCells = maximumCells;
            this.regionStride = regionStride;
        }

        T[][][] freeze(T[][][] source) throws E {
            if (source.length > regionStride) throw new UnsupportedOperationException("wide-metadata");
            T[][][] blocks = source.clone();
            for (int x = 0; x < source.length; x++) {
                blocks[x] = source[x].clone();
                for (int y = 0; y < source[x].length; y++) {
                    if (source[x][y].length > regionStride) throw new UnsupportedOperationException("wide-metadata");
                    countCells(source[x][y].length);
                    blocks[x][y] = source[x][y].clone();
                    for (int z = 0; z < source[x][y].length; z++) {
                        blocks[x][y][z] = copy(source[x][y][z]);
                    }
                }
            }
            return blocks;
        }

        void countBox(int width, int height, int depth) {
            if (width > regionStride || depth > regionStride) throw new UnsupportedOperationException("wide-metadata");
            countCells(Math.multiplyExact(Math.multiplyExact(width, height), depth));
        }

        private void countCells(int amount) {
            cells = Math.addExact(cells, amount);
            if (cells > maximumCells) throw new UnsupportedOperationException("large-metadata");
        }

        T copy(T original) throws E {
            T clone = copied.get(original);
            if (clone == null) {
                clone = copier.copy(original);
                copied.put(original, clone);
            }
            return clone;
        }
    }
}
