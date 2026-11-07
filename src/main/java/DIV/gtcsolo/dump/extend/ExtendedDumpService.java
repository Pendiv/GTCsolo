package DIV.gtcsolo.dump.extend;

import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.file.Path;

public class ExtendedDumpService {
    private final OreVeinDumpService oreVeinDumpService = new OreVeinDumpService();

    public ExtendedDumpResult dumpOreVeins(MinecraftServer server) throws IOException {
        OreVeinDumpService.OreVeinDumpResult result = oreVeinDumpService.dump(server);
        return new ExtendedDumpResult("ore_vein", result.outputPath(), result.count());
    }

    public record ExtendedDumpResult(
            String target,
            Path outputPath,
            int count
    ) {
    }
}
