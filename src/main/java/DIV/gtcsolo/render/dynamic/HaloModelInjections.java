package DIV.gtcsolo.render.dynamic;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.registry.halo.HaloInjection;
import DIV.gtcsolo.registry.halo.HaloInjections;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gregtechceu.gtceu.data.pack.GTDynamicResourcePack;
import dev.latvian.mods.kubejs.item.ItemBuilder;
import dev.latvian.mods.kubejs.registry.BuilderBase;
import dev.latvian.mods.kubejs.registry.RegistryInfo;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Decorates arbitrary existing item models with a static or real-time-color Re-Avaritia halo.
 */
public final class HaloModelInjections {

    private static final String AVARITIA_MOD_ID = "avaritia";
    private static final String HALO_LOADER = "avaritia:halo";
    private static final String RAINBOW_HALO_LOADER = "gtcsolo:rainbow_halo";
    private static final String HALO_TEXTURE_KEY = "gtcsolo_halo";

    private HaloModelInjections() {}

    public static void generateAll() {
        if (!ModList.get().isLoaded(AVARITIA_MOD_ID)) {
            Gtcsolo.LOGGER.info(
                    "[HaloInjection] Re-Avaritia is not loaded; skipped {} declaration(s)",
                    HaloInjections.declarations().size());
            return;
        }

        int decorated = 0;
        int skipped = 0;
        for (HaloInjection declaration : HaloInjections.declarations()) {
            if (decorate(declaration)) {
                decorated++;
            } else {
                skipped++;
            }
        }
        Gtcsolo.LOGGER.info(
                "[HaloInjection] decorated {} item model(s), skipped={}",
                decorated,
                skipped);
    }

    private static boolean decorate(HaloInjection declaration) {
        ResourceLocation modelLocation = itemModelLocation(declaration.targetItemId());
        JsonObject model = loadModel(declaration.targetItemId(), modelLocation);
        if (model == null) {
            Gtcsolo.LOGGER.error(
                    "[HaloInjection] item model missing for {} at {}",
                    declaration.targetItemId(),
                    modelLocation);
            return false;
        }

        try {
            String targetLoader = declaration.colorMode() == HaloInjection.ColorMode.RAINBOW ?
                    RAINBOW_HALO_LOADER :
                    HALO_LOADER;
            if (model.has("loader")) {
                JsonElement loader = model.get("loader");
                if (!loader.isJsonPrimitive() || !targetLoader.equals(loader.getAsString())) {
                    Gtcsolo.LOGGER.warn(
                            "[HaloInjection] {} already uses an incompatible custom model loader; target skipped",
                            declaration.targetItemId());
                    return false;
                }
            }

            JsonObject textures;
            if (model.has("textures") && model.get("textures").isJsonObject()) {
                textures = model.getAsJsonObject("textures");
            } else {
                textures = new JsonObject();
                model.add("textures", textures);
            }
            textures.addProperty(HALO_TEXTURE_KEY, declaration.haloTexture().toString());
            model.addProperty("loader", targetLoader);

            JsonObject halo = new JsonObject();
            halo.addProperty("texture", "#" + HALO_TEXTURE_KEY);
            halo.addProperty("color", declaration.color());
            halo.addProperty("size", declaration.size());
            halo.addProperty("pulse", declaration.pulse());
            if (declaration.colorMode() == HaloInjection.ColorMode.RAINBOW) {
                halo.addProperty("cycleMillis", declaration.cycleMillis());
            }
            model.add("halo", halo);

            if (!declaration.layerColors().isEmpty()) {
                JsonArray layerColors = new JsonArray();
                declaration.layerColors().forEach(layerColors::add);
                model.add("layerColors", layerColors);
            }

            GTDynamicResourcePack.DATA.put(
                    modelLocation,
                    model.toString().getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (RuntimeException exception) {
            Gtcsolo.LOGGER.error(
                    "[HaloInjection] failed to decorate {} from {}: {}",
                    declaration.targetItemId(),
                    modelLocation,
                    exception.getMessage(),
                    exception);
            return false;
        }
    }

    private static JsonObject loadModel(
            ResourceLocation targetItemId,
            ResourceLocation modelLocation) {
        byte[] sourceBytes = GTDynamicResourcePack.DATA.get(modelLocation);
        if (sourceBytes == null) {
            sourceBytes = loadModResource(modelLocation);
        }
        if (sourceBytes != null) {
            try {
                JsonElement parsed =
                        JsonParser.parseString(new String(sourceBytes, StandardCharsets.UTF_8));
                if (parsed.isJsonObject()) {
                    return parsed.getAsJsonObject();
                }
                Gtcsolo.LOGGER.error(
                        "[HaloInjection] item model is not a JSON object: {}",
                        modelLocation);
                return null;
            } catch (RuntimeException exception) {
                Gtcsolo.LOGGER.error(
                        "[HaloInjection] item model JSON parse failed for {}: {}",
                        modelLocation,
                        exception.getMessage(),
                        exception);
                return null;
            }
        }
        return loadKubeJsGeneratedModel(targetItemId);
    }

    /**
     * Mirrors KubeJS 2001.6.5 ItemBuilder.generateAssetJsons without firing its asset events twice.
     */
    private static JsonObject loadKubeJsGeneratedModel(ResourceLocation targetItemId) {
        BuilderBase<?> builder = RegistryInfo.ITEM.objects.get(targetItemId);
        if (!(builder instanceof ItemBuilder itemBuilder)) {
            return null;
        }
        if (itemBuilder.modelJson != null) {
            return itemBuilder.modelJson.deepCopy();
        }

        JsonObject model = new JsonObject();
        model.addProperty(
                "parent",
                itemBuilder.parentModel.isEmpty() ?
                        "minecraft:item/generated" :
                        itemBuilder.parentModel);
        JsonObject textures = itemBuilder.textureJson.deepCopy();
        if (textures.size() == 0) {
            textures.addProperty(
                    "layer0",
                    targetItemId.getNamespace() + ":item/" + targetItemId.getPath());
        }
        model.add("textures", textures);
        return model;
    }

    private static ResourceLocation itemModelLocation(ResourceLocation itemId) {
        return new ResourceLocation(
                itemId.getNamespace(),
                "models/item/" + itemId.getPath() + ".json");
    }

    private static byte[] loadModResource(ResourceLocation location) {
        IModFileInfo modFileInfo = ModList.get().getModFileById(location.getNamespace());
        if (modFileInfo == null) {
            return null;
        }
        Path path = modFileInfo.getFile().findResource(
                "assets/" + location.getNamespace() + "/" + location.getPath());
        if (path == null || !Files.exists(path)) {
            return null;
        }
        try {
            return Files.readAllBytes(path);
        } catch (IOException exception) {
            Gtcsolo.LOGGER.error(
                    "[HaloInjection] resource read failed for {}: {}",
                    location,
                    exception.getMessage(),
                    exception);
            return null;
        }
    }
}
