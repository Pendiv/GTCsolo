package DIV.gtcsolo.machine;

import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Authored /aisle exports. Rows run bottom to top; the controller is on the first aisle. */
public final class RoomStructures {
    public static final Structure CULTIVATION_FORMATION_ROOM = new Structure("cultivation_formation_room", new String[][] {
            {"#ADA#", "#BCB#", "#BCB#", "#AAA#", "##A##"},
            {"AAAAA", "B###B", "B###B", "A###A", "#AAA#"},
            {"AAAAA", "C###C", "C###C", "A###A", "AAAAA"},
            {"AAAAA", "B###B", "B###B", "A###A", "#AAA#"},
            {"#AAA#", "#BCB#", "#BCB#", "#AAA#", "##A##"}
    }, 'D', 'B', 'C', Map.of('A', "gtceu:heatproof_machine_casing", 'B', "gtceu:lv_machine_casing",
            'C', "gtcsolo:voltage_glass_lv", 'D', "gtceu:multi_ebf", '#', "minecraft:air"));

    public static final Structure FAMING_ROOM = new Structure("faming_room", new String[][] {
            {"AAABAAA", "AACCCAA", "ACDDDCA", "ACDDDCA", "AACCCAA"},
            {"EAAAAAE", "AFGGGFA", "A#####A", "A#####A", "AAAAAAA"},
            {"EAAAAAE", "AFGGGFA", "A#####A", "A#####A", "AAAAAAA"},
            {"EAAAAAE", "AFFFFFA", "A#####A", "A#####A", "AAAAAAA"},
            {"AAAAAAA", "AAAAAAA", "AAAAAAA", "AAAAAAA", "AAAAAAA"}
    }, 'B', 'C', 'D', Map.of(
            'A', "gtceu:solid_machine_casing", 'B', "gtceu:mv_bedrock_ore_miner",
            'C', "gtceu:lv_machine_casing", 'D', "minecraft:glass",
            'E', "gtceu:steel_firebox_casing", 'F', "gtceu:steel_machine_casing",
            'G', "minecraft:grass_block", '#', "minecraft:air"));

    public static final Structure CONSTRUCTION_ROOM = new Structure("construction_room", new String[][] {
            {"ABCBA", "ADEDA", "AFFFA", "AFFFA", "AFFFA", "ADEDA", "ABBBA"},
            {"BAAAB", "DEGED", "B###B", "A###A", "A###A", "BEGEB", "BAAAB"},
            {"BAAAB", "EG#GE", "B###B", "A###A", "A###A", "EG#GE", "BAHAB"},
            {"BAAAB", "DEGED", "B###B", "A###A", "A###A", "BEGEB", "BAAAB"},
            {"ABBBA", "ADEDA", "AFFFA", "AFFFA", "AFFFA", "ADEDA", "AAAAA"}
    }, 'C', 'D', 'F', Map.of(
            'A', "gtceu:solid_machine_casing", 'B', "gtceu:steel_firebox_casing",
            'C', "gtceu:mv_bedrock_ore_miner", 'D', "gtceu:lv_machine_casing",
            'E', "gtceu:cupronickel_coil_block", 'F', "minecraft:glass",
            'G', "gtceu:steel_pipe_casing", 'H', "gtceu:iv_muffler_hatch", '#', "minecraft:air"));

    private RoomStructures() {}

    /** Coordinate ordering follows the export: aisle (world X), row (Y), character (world Z). */
    public record Cell(int aisle, int y, int column) {}

    public static final class Structure {
        private final String id;
        private final String[][] aisles;
        private final char coreSymbol;
        private final char casingSymbol;
        private final char glassSymbol;
        private final Map<Character, String> exportLegend;
        private final Cell core;

        private Structure(String id, String[][] aisles, char coreSymbol, char casingSymbol,
                char glassSymbol, Map<Character, String> exportLegend) {
            this.id = id;
            this.aisles = copy(aisles);
            this.coreSymbol = coreSymbol;
            this.casingSymbol = casingSymbol;
            this.glassSymbol = glassSymbol;
            this.exportLegend = Map.copyOf(exportLegend);
            for (String[] aisle : aisles) {
                if (aisle.length != height()) throw new IllegalArgumentException("Unequal room aisle heights");
                for (String row : aisle) {
                    if (row.length() != width()) throw new IllegalArgumentException("Unequal room row widths");
                    for (char symbol : row.toCharArray()) {
                        if (!exportLegend.containsKey(symbol)) throw new IllegalArgumentException("Unknown room symbol");
                    }
                }
            }
            List<Cell> controllers = positions(coreSymbol);
            if (controllers.size() != 1) throw new IllegalArgumentException("A room requires one controller");
            core = controllers.get(0);
            if (core.aisle() != 0 || core.y() != 0) throw new IllegalArgumentException("Expected bottom front controller");
        }

        public String id() { return id; }
        public int width() { return aisles[0][0].length(); }
        public int height() { return aisles[0].length; }
        public int depth() { return aisles.length; }
        public char coreSymbol() { return coreSymbol; }
        public char casingSymbol() { return casingSymbol; }
        public char glassSymbol() { return glassSymbol; }
        public Cell core() { return core; }
        public Map<Character, String> exportLegend() { return exportLegend; }
        public String[][] aisles() { return copy(aisles); }

        /** +aisle points behind the controller; +character points right. No reversal is needed for shapes. */
        public FactoryBlockPattern patternBuilder() {
            var builder = FactoryBlockPattern.start(RelativeDirection.RIGHT, RelativeDirection.UP, RelativeDirection.BACK);
            for (String[] aisle : aisles) builder.aisle(aisle.clone());
            return builder;
        }

        public MultiblockShapeInfo.ShapeInfoBuilder shapeBuilder() { return shapeBuilder(Map.of()); }

        /** Optional substitutions place preview hatches without changing the formation pattern. */
        public MultiblockShapeInfo.ShapeInfoBuilder shapeBuilder(Map<Cell, Character> substitutions) {
            String[][] shape = aisles();
            substitutions.forEach((cell, symbol) -> {
                checkBounds(cell);
                char[] row = shape[cell.aisle()][cell.y()].toCharArray();
                row[cell.column()] = symbol;
                shape[cell.aisle()][cell.y()] = new String(row);
            });
            var builder = MultiblockShapeInfo.builder();
            for (String[] aisle : shape) builder.aisle(aisle);
            return builder;
        }

        public char symbolAt(Cell cell) {
            checkBounds(cell);
            return aisles[cell.aisle()][cell.y()].charAt(cell.column());
        }

        public List<Cell> positions(char symbol) {
            List<Cell> positions = new ArrayList<>();
            for (int a = 0; a < depth(); a++) for (int y = 0; y < height(); y++) {
                for (int column = 0; column < width(); column++) {
                    if (aisles[a][y].charAt(column) == symbol) positions.add(new Cell(a, y, column));
                }
            }
            return List.copyOf(positions);
        }

        /** Matches GT's RIGHT/UP/BACK pattern coordinates for upright horizontal machines. */
        public BlockPos relativePosition(Cell cell, Direction front) {
            checkBounds(cell);
            if (front.getAxis() == Direction.Axis.Y) throw new IllegalArgumentException("Room front must be horizontal");
            return BlockPos.ZERO.relative(front.getClockWise(), cell.column() - core.column())
                    .above(cell.y() - core.y()).relative(front.getOpposite(), cell.aisle() - core.aisle());
        }

        /** LDLib bakes native previews as [character][row][aisle], at positive X/Y/Z. */
        public BlockPos previewPosition(Cell cell) {
            checkBounds(cell);
            return new BlockPos(cell.column(), cell.y(), cell.aisle());
        }

        private void checkBounds(Cell cell) {
            if (cell.aisle() < 0 || cell.aisle() >= depth() || cell.y() < 0 || cell.y() >= height()
                    || cell.column() < 0 || cell.column() >= width()) {
                throw new IllegalArgumentException("Room cell is outside the exported structure: " + cell);
            }
        }

        private static String[][] copy(String[][] source) {
            String[][] result = new String[source.length][];
            for (int i = 0; i < source.length; i++) result[i] = source[i].clone();
            return result;
        }
    }
}
