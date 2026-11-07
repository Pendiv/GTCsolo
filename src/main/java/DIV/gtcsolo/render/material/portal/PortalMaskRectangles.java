package DIV.gtcsolo.render.material.portal;

import java.util.ArrayList;
import java.util.List;

/** Covers a binary silhouette exactly once. Runs are joined vertically only when their spans match. */
public final class PortalMaskRectangles {
    public record Rectangle(int x, int y, int width, int height) {}

    private PortalMaskRectangles() {}

    public static List<Rectangle> cover(boolean[][] mask) {
        if (mask.length == 0) return List.of();
        int width = mask[0].length;
        for (boolean[] row : mask) {
            if (row.length != width) throw new IllegalArgumentException("Unequal mask rows");
        }
        List<Rectangle> result = new ArrayList<>();
        List<Integer> previous = List.of();
        for (int y = 0; y < mask.length; y++) {
            List<Integer> current = new ArrayList<>();
            for (int x = 0; x < width;) {
                if (!mask[y][x]) { x++; continue; }
                int start = x;
                while (x < width && mask[y][x]) x++;
                int span = x - start;
                int index = -1;
                for (int candidate : previous) {
                    Rectangle above = result.get(candidate);
                    if (above.x == start && above.width == span) { index = candidate; break; }
                }
                if (index < 0) {
                    index = result.size();
                    result.add(new Rectangle(start, y, span, 1));
                } else {
                    Rectangle above = result.get(index);
                    result.set(index, new Rectangle(start, above.y, span, above.height + 1));
                }
                current.add(index);
            }
            previous = current;
        }
        return List.copyOf(result);
    }
}
