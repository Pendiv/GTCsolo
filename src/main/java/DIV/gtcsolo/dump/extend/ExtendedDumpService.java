package DIV.gtcsolo.dump.extend;

import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.file.Path;

public class ExtendedDumpService {
    private final OreVeinJeiDumpService oreVeinJeiDumpService = new OreVeinJeiDumpService();

    public ExtendedDumpResult dumpOreVeinsFromJei(MinecraftServer server) throws IOException {
        OreVeinJeiDumpService.OreVeinJeiDumpResult result = oreVeinJeiDumpService.dump(server);
        return new ExtendedDumpResult("ore_vein_jei", result.outputPath(), result.count());
    }

    public record ExtendedDumpResult(
            String target,
            Path outputPath,
            int count
    ) {
    }
}