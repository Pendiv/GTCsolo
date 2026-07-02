package DIV.gtcsolo.dump.extend;

import DIV.gtcsolo.Gtcsolo;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.gregtechceu.gtceu.api.data.worldgen.GTOreDefinition;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * GTCEu の鉱脈定義 (GTRegistries.ORE_VEINS) を直接 JSON へ書き出す。
 * 旧 OreVeinJeiDumpService の JEI ランタイム経由リフレクションを廃し、
 * GTOreDefinition.FULL_CODEC で GT のデータパック形式そのままにエンコードする。
 * サーバー側で完結し、クライアント/JEI に依存しない。
 */
public class OreVeinDumpService {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .serializeNulls()
            .create();

    public OreVeinDumpResult dump(MinecraftServer server) throws IOException {
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());

        List<Map.Entry<ResourceLocation, GTOreDefinition>> entries =
                new ArrayList<>(GTRegistries.ORE_VEINS.entries());
        entries.sort(Comparator.comparing(e -> e.getKey().toString()));

        JsonObject veins = new JsonObject();
        int encoded = 0;
        for (Map.Entry<ResourceLocation, GTOreDefinition> entry : entries) {
            ResourceLocation id = entry.getKey();
            JsonElement json = GTOreDefinition.FULL_CODEC
                    .encodeStart(ops, entry.getValue())
                    .resultOrPartial(err -> Gtcsolo.LOGGER.warn("[ore_vein_dump] failed to encode {}: {}", id, err))
                    .orElse(null);
            if (json == null) {
                continue;
            }
            veins.add(id.toString(), json);
            encoded++;
        }

        JsonObject root = new JsonObject();
        root.addProperty("type", "gtcsolo:ore_vein_definition");
        root.addProperty("source", "GTRegistries.ORE_VEINS");
        root.addProperty("count", encoded);
        root.add("veins", veins);

        Path outputPath = FMLPaths.GAMEDIR.get()
                .resolve("kubejs")
                .resolve("exports")
                .resolve("gtcsolo")
                .resolve("extended")
                .resolve("ore_veins.json");

        Files.createDirectories(outputPath.getParent());
        try (Writer writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        }

        Gtcsolo.LOGGER.info("Dumped {} ore vein definitions to {}", encoded, outputPath.toAbsolutePath());
        return new OreVeinDumpResult(outputPath, encoded);
    }

    public record OreVeinDumpResult(
            Path outputPath,
            int count
    ) {
    }
}
