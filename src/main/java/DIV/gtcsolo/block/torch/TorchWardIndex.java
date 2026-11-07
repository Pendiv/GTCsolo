package DIV.gtcsolo.block.torch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Spatial lookup only; no world access, ticking, or chunk loading. Owned by one server dimension. */
public final class TorchWardIndex {
    public record Position(int x, int y, int z) {}
    public record Ward(Position position, int tier) {
        public int radius() { return VoltageTorchTier.radiusForLevel(tier); }

        /** Distances use the torch block centre and the attempted spawn's feet position. */
        public boolean contains(double x, double y, double z) {
            double dx = x - (position.x + 0.5);
            double dy = y - (position.y + 0.5);
            double dz = z - (position.z + 0.5);
            return dx * dx + dy * dy + dz * dz <= (double) radius() * radius();
        }
    }
    private record Cell(int x, int y, int z) {}
    private record Chunk(int x, int z) {}

    private final Map<Position, Ward> wards = new HashMap<>();
    // Each ward occupies one cell, rather than duplicating MAX wards into thousands of chunks.
    // Cell width >= diameter, so a query visits at most eight cells in each occupied size band.
    private final Map<Integer, Map<Cell, Map<Position, Ward>>> bands = new HashMap<>();
    private final Map<Chunk, Map<Position, Ward>> sourceChunks = new HashMap<>();

    public boolean put(int x, int y, int z, int tier) {
        VoltageTorchTier.radiusForLevel(tier); // Reject invalid saved/input tiers before mutating.
        Position position = new Position(x, y, z);
        Ward previous = wards.get(position);
        if (previous != null && previous.tier == tier) return false;
        if (previous != null) remove(x, y, z);
        Ward ward = new Ward(position, tier);
        wards.put(position, ward);
        int width = width(ward.radius());
        bands.computeIfAbsent(width, ignored -> new HashMap<>())
                .computeIfAbsent(cell(position, width), ignored -> new HashMap<>()).put(position, ward);
        sourceChunks.computeIfAbsent(chunk(position), ignored -> new HashMap<>()).put(position, ward);
        return true;
    }

    public boolean remove(int x, int y, int z) {
        Position position = new Position(x, y, z);
        Ward previous = wards.remove(position);
        if (previous == null) return false;
        int width = width(previous.radius());
        var band = bands.get(width);
        Cell cell = cell(position, width);
        var entries = band.get(cell);
        entries.remove(position);
        if (entries.isEmpty()) band.remove(cell);
        if (band.isEmpty()) bands.remove(width);
        Chunk chunk = chunk(position);
        var sources = sourceChunks.get(chunk);
        sources.remove(position);
        if (sources.isEmpty()) sourceChunks.remove(chunk);
        return true;
    }

    /** Snapshot permits stale-source repairs while examining candidates. */
    public List<Ward> covering(double x, double y, double z) {
        List<Ward> result = new ArrayList<>();
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return result;
        // Stored coordinates are ints. Reject impossible queries before double-to-int saturation
        // could overflow a cell loop; the margin includes any valid ward at either int boundary.
        double limit = (double) Integer.MAX_VALUE + 1 + VoltageTorchTier.radiusForLevel(VoltageTorchTier.MAX_LEVEL);
        if (Math.abs(x) > limit || Math.abs(y) > limit || Math.abs(z) > limit) return result;
        for (var entry : bands.entrySet()) {
            int width = entry.getKey();
            int half = width / 2;
            int minX = coordinate(x - half, width), maxX = coordinate(x + half, width);
            int minY = coordinate(y - half, width), maxY = coordinate(y + half, width);
            int minZ = coordinate(z - half, width), maxZ = coordinate(z + half, width);
            for (int cx = minX; cx <= maxX; cx++) {
                for (int cy = minY; cy <= maxY; cy++) {
                    for (int cz = minZ; cz <= maxZ; cz++) {
                        var candidates = entry.getValue().get(new Cell(cx, cy, cz));
                        if (candidates == null) continue;
                        for (Ward ward : candidates.values()) if (ward.contains(x, y, z)) result.add(ward);
                    }
                }
            }
        }
        return result;
    }

    public List<Ward> inChunk(int x, int z) {
        var entries = sourceChunks.get(new Chunk(x, z));
        return entries == null ? List.of() : List.copyOf(entries.values());
    }

    public Collection<Ward> all() { return List.copyOf(wards.values()); }
    public int size() { return wards.size(); }

    private static int width(int radius) { return Integer.highestOneBit(radius * 2 - 1) << 1; }
    private static int coordinate(double centre, int width) { return (int) Math.floor(centre / width); }
    private static Cell cell(Position position, int width) {
        return new Cell(Math.floorDiv(position.x, width), Math.floorDiv(position.y, width),
                Math.floorDiv(position.z, width));
    }
    private static Chunk chunk(Position position) {
        return new Chunk(Math.floorDiv(position.x, 16), Math.floorDiv(position.z, 16));
    }
}
