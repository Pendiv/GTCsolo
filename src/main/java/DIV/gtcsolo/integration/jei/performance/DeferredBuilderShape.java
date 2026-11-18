package DIV.gtcsolo.integration.jei.performance;

import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.lowdragmc.lowdraglib.utils.BlockInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Holds a native shape builder's rows and symbols until its block array is needed. */
final class DeferredBuilderShape extends MultiblockShapeInfo {
    private final List<String[]> aisles;
    private final Map<Character, BlockInfo> symbols;
    private final BlockInfo empty;
    private BlockInfo[][][] blocks;

    static DeferredBuilderShape capture(List<String[]> source, Map<Character, BlockInfo> symbols) {
        if (source.isEmpty() || source.get(0) == null || source.get(0).length == 0
                || source.get(0)[0] == null || source.get(0)[0].isEmpty()) return null;
        int height = source.get(0).length;
        int width = source.get(0)[0].length();
        List<String[]> aisles = new ArrayList<>(source.size());
        for (String[] aisle : source) {
            if (aisle == null || aisle.length != height) return null;
            for (String row : aisle) {
                if (row == null || row.length() != width) return null;
            }
            aisles.add(aisle.clone());
        }
        return new DeferredBuilderShape(aisles, new LinkedHashMap<>(symbols), BlockInfo.EMPTY);
    }

    private DeferredBuilderShape(List<String[]> aisles, Map<Character, BlockInfo> symbols, BlockInfo empty) {
        super(null);
        this.aisles = aisles;
        this.symbols = symbols;
        this.empty = empty;
    }

    DeferredBuilderShape freeze(PreviewShapeMetadata.Snapshot<BlockInfo, ReflectiveOperationException> snapshot)
            throws ReflectiveOperationException {
        snapshot.countBox(aisles.get(0)[0].length(), aisles.get(0).length, aisles.size());
        Map<Character, BlockInfo> frozenSymbols = new LinkedHashMap<>();
        for (var entry : symbols.entrySet()) frozenSymbols.put(entry.getKey(), snapshot.copy(entry.getValue()));
        return new DeferredBuilderShape(aisles, frozenSymbols, snapshot.copy(empty));
    }

    boolean isBaked() {
        return blocks != null;
    }

    @Override
    public BlockInfo[][][] getBlocks() {
        if (blocks == null) blocks = new CapturedBuilder(aisles, symbols).bakeArray(BlockInfo.class, empty);
        return blocks;
    }

    private static final class CapturedBuilder extends ShapeInfoBuilder {
        private CapturedBuilder(List<String[]> aisles, Map<Character, BlockInfo> symbols) {
            this.shape = aisles;
            this.symbolMap = symbols;
        }
    }
}
