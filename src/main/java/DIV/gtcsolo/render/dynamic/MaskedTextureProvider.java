package DIV.gtcsolo.render.dynamic;

import com.google.gson.JsonObject;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconType;
import com.gregtechceu.gtceu.data.pack.GTDynamicResourcePack;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * mask PNG × GTCEu base iconset PNG を per-pixel 乗算合成し、
 * GTDynamicResourcePack に runtime PNG として inject する。
 *
 * 合成式:
 *   out.rgb   = base.rgb × mask.rgb / 255   (= multiply blend)
 *   out.alpha = base.a   × mask.a   / 255   (= alpha AND、 透明部分の保持)
 *
 * 結果 sprite path: gtceu:textures/item/material_sets/<iconSetName>/<type>.png
 * → GTCEu の MaterialIconType.getItemTexturePath が ResourceHelper check で
 *   存在を確認 → fallback せず私たちの inject sprite を使う。
 */
public final class MaskedTextureProvider {
    private static final Logger LOGGER = LogUtils.getLogger();

    public record Entry(String iconSetName, String baseIconSetName, ResourceLocation maskPath) {}

    public static final List<Entry> ENTRIES = new ArrayList<>();

    private MaskedTextureProvider() {}

    public static void register(String iconSetName, String baseIconSetName, ResourceLocation maskPath) {
        ENTRIES.add(new Entry(iconSetName, baseIconSetName, maskPath));
        LOGGER.info("[MaskedTex] Entry registered: iconset='{}', base='{}', mask={}",
                iconSetName, baseIconSetName, maskPath);
    }

    /** 共通 mask ResourceLocation 構築ヘルパ (= gtcsolo:item/gem_masks/<file>) */
    private static ResourceLocation maskLoc(String file) {
        return new ResourceLocation("gtcsolo", "item/gem_masks/" + file);
    }

    public static void bootstrap() {
        LOGGER.info("[MaskedTex] bootstrap() — registering masked iconset entries");
        register("paleblue_star", "metallic", maskLoc("masked_1"));
        register("red_star",      "bright",   maskLoc("masked_2"));
        // test_a..w : 全 iconset 網羅検証 (fluid のみ skip)
        // マスク 7 種 (masked_1/2/3/4/5/6a/8) を循環割当。 各 entry の base iconset 指定で
        // parent chain walk → 必ず dull に fallback して全 iconType 合成成功する
        register("test_a", "bright",         maskLoc("masked_3"));
        register("test_b", "metallic",       maskLoc("masked_4"));
        register("test_c", "rough",          maskLoc("masked_5"));
        register("test_d", "shiny",          maskLoc("masked_6a"));
        register("test_e", "dull",           maskLoc("masked_1"));
        register("test_f", "magnetic",       maskLoc("masked_2"));
        register("test_g", "diamond",        maskLoc("masked_3"));
        register("test_h", "emerald",        maskLoc("masked_4"));
        register("test_i", "gem_horizontal", maskLoc("masked_5"));
        register("test_j", "gem_vertical",   maskLoc("masked_6a"));
        register("test_k", "ruby",           maskLoc("masked_7"));
        register("test_l", "opal",           maskLoc("masked_1"));
        register("test_m", "glass",          maskLoc("masked_2"));
        register("test_n", "netherstar",     maskLoc("masked_3"));
        register("test_o", "fine",           maskLoc("masked_4"));
        register("test_p", "sand",           maskLoc("masked_5"));
        register("test_q", "wood",           maskLoc("masked_6a"));
        register("test_r", "flint",          maskLoc("masked_7"));
        register("test_s", "lignite",        maskLoc("masked_1"));
        register("test_t", "quartz",         maskLoc("masked_2"));
        register("test_u", "certus",         maskLoc("masked_3"));
        register("test_v", "lapis",          maskLoc("masked_4"));
        register("test_w", "radioactive",    maskLoc("masked_5"));
        // gtcsolo:aurum_stellis_gold (Java 移植済) 用
        register("aurum_stellis_gold", "shiny", maskLoc("masked_7"));
        // gtcsolo:nether_star — Apotheosis Gem 用、 GTCEu netherstar base × 白寄り cyan mask
        register("nether_star", "netherstar", maskLoc("nether_star"));
        LOGGER.info("[MaskedTex] bootstrap() done — {} entries registered", ENTRIES.size());
    }

    /**
     * 全 entry について全 MaterialIconType 分の合成 sprite を生成して inject。
     *
     * 呼び出しタイミング = mod constructor (client only)。 = atlas stitch / model load より前。
     * base/mask PNG は ResourceManager 経由ではなく **classpath から直接読む** (getResourceAsStream)。
     * → reload listener タイミング遅延問題を回避。
     */
    public static void generateAll() {
        LOGGER.info("[MaskedTex] generateAll() start — entries={}, iconTypes={}",
                ENTRIES.size(), MaterialIconType.ICON_TYPES.size());
        for (Entry entry : ENTRIES) {
            generateForEntry(entry);
        }
        injectBigPlateModel();
        LOGGER.info("[MaskedTex] generateAll() done");
    }

    /**
     * big_plate TagPrefix 用の model JSON を 1 つだけ inject (= dull iconset = root)。
     * GTCEu の getItemModelPath は iconset の parent chain を歩いて先頭に存在する .json を探すが、
     * 我々の big_plate type はどの iconset にも JSON が無いので必ず root (dull) まで歩いて
     * ここに inject した JSON を拾う。 結果として全 material が rough/plate_dense ベースの 2層
     * テクスチャを使い、 ItemColor が material.getLayerARGB(i) を適用して色 tint される。
     */
    private static void injectBigPlateModel() {
        JsonObject modelJson = new JsonObject();
        modelJson.addProperty("parent", "item/generated");
        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", "gtceu:item/material_sets/rough/plate_dense");
        textures.addProperty("layer1", "gtceu:item/material_sets/rough/plate_dense_secondary");
        modelJson.add("textures", textures);
        ResourceLocation loc = new ResourceLocation("gtceu", "material_sets/dull/big_plate");
        GTDynamicResourcePack.addItemModel(loc, modelJson);
        LOGGER.info("[MaskedTex] injected big_plate model at gtceu:material_sets/dull/big_plate (= rough/plate_dense base + secondary)");
    }

    private static void generateForEntry(Entry entry) {
        ResourceLocation maskTexPath = toTexturePath(entry.maskPath());
        LOGGER.info("[MaskedTex] === processing entry: iconset='{}', base='{}', mask={} ===",
                entry.iconSetName(), entry.baseIconSetName(), maskTexPath);

        NativeImage mask = loadPng(maskTexPath);
        if (mask == null) {
            LOGGER.error("[MaskedTex] ABORT entry '{}': mask not found at classpath:/assets/{}/{}",
                    entry.iconSetName(), maskTexPath.getNamespace(), maskTexPath.getPath());
            return;
        }
        LOGGER.info("[MaskedTex] mask loaded: {}x{} from {}",
                mask.getWidth(), mask.getHeight(), maskTexPath);

        // baseIconSetName 文字列 → MaterialIconSet instance 解決 (parent chain 走査用)
        MaterialIconSet baseSet = MaterialIconSet.getByName(entry.baseIconSetName());
        if (baseSet == null) {
            LOGGER.error("[MaskedTex] ABORT entry '{}': base iconset '{}' not registered in MaterialIconSet.ICON_SETS",
                    entry.iconSetName(), entry.baseIconSetName());
            mask.close();
            return;
        }

        int processed = 0;
        int skipped = 0;
        int failed = 0;
        for (MaterialIconType type : MaterialIconType.ICON_TYPES.values()) {
            // 各 iconType につき base iconset → parentIconset → ... の順で flat PNG 探索。
            // GTCEu の iconset 仕様:
            //   metallic (16件) は parent=dull、 dull (60件) が root の完全 base set。
            //   bright/shiny 等の他 iconset も最終的に dull に着地するため、 ring/gear/spring 等の
            //   "metallic にフラット PNG が無い" iconType も dull 経由で必ず合成可能になる。
            NativeImage base = null;
            String usedBaseSetName = null;
            MaterialIconSet current = baseSet;
            while (current != null) {
                ResourceLocation basePath = new ResourceLocation("gtceu",
                        "textures/item/material_sets/" + current.name + "/" + type.name() + ".png");
                base = loadPng(basePath);
                if (base != null) {
                    usedBaseSetName = current.name;
                    break;
                }
                // root に到達 / 自己参照 / null を踏んだら終端
                if (current.isRootIconset || current.parentIconset == null || current == current.parentIconset) break;
                current = current.parentIconset;
            }
            if (base == null) {
                skipped++;
                continue;
            }

            NativeImage composed;
            try {
                composed = compose(mask, base);
            } catch (Exception ex) {
                LOGGER.error("[MaskedTex] compose failed for {}/{} (base={}): {}",
                        entry.iconSetName(), type.name(), usedBaseSetName, ex.getMessage(), ex);
                base.close();
                failed++;
                continue;
            }
            base.close();

            byte[] pngBytes;
            try {
                pngBytes = composed.asByteArray();
            } catch (IOException ex) {
                LOGGER.error("[MaskedTex] PNG encode failed for {}/{}: {}",
                        entry.iconSetName(), type.name(), ex.getMessage(), ex);
                composed.close();
                failed++;
                continue;
            }
            composed.close();

            // GTDynamicResourcePack.addItemTexture が内部で textures/item/<path>.png に展開
            ResourceLocation injectLoc = new ResourceLocation("gtceu",
                    "material_sets/" + entry.iconSetName() + "/" + type.name());
            GTDynamicResourcePack.addItemTexture(injectLoc, pngBytes);

            // 並列で model JSON も inject (= GTCEu DelegatedModel の parent chain が物理 file fallback しないように)
            // 中身: {"parent":"item/generated","textures":{"layer0":"gtceu:item/material_sets/<set>/<type>"}}
            JsonObject modelJson = new JsonObject();
            modelJson.addProperty("parent", "item/generated");
            JsonObject textures = new JsonObject();
            textures.addProperty("layer0", "gtceu:item/material_sets/" + entry.iconSetName() + "/" + type.name());
            modelJson.add("textures", textures);
            GTDynamicResourcePack.addItemModel(injectLoc, modelJson);

            LOGGER.info("[MaskedTex] injected: png({} bytes) + model JSON for gtceu:material_sets/{}/{} (base={})",
                    pngBytes.length, entry.iconSetName(), type.name(), usedBaseSetName);
            processed++;
        }
        mask.close();
        LOGGER.info("[MaskedTex] entry '{}' summary: processed={}, skipped(no base)={}, failed={}",
                entry.iconSetName(), processed, skipped, failed);
    }

    private static ResourceLocation toTexturePath(ResourceLocation loc) {
        return new ResourceLocation(loc.getNamespace(), "textures/" + loc.getPath() + ".png");
    }

    /**
     * Cross-mod resource access via Forge ModList。 Class.getResourceAsStream は呼び出し元 mod 内
     * しか見ないため、他 mod (= gtceu) のリソース読みには ModList.findResource を使う必要がある。
     */
    private static NativeImage loadPng(ResourceLocation loc) {
        IModFileInfo modFileInfo = ModList.get().getModFileById(loc.getNamespace());
        if (modFileInfo == null) {
            LOGGER.debug("[MaskedTex] mod '{}' not found in ModList for {}", loc.getNamespace(), loc);
            return null;
        }
        String resourcePath = "assets/" + loc.getNamespace() + "/" + loc.getPath();
        Path path = modFileInfo.getFile().findResource(resourcePath);
        if (path == null || !Files.exists(path)) {
            LOGGER.debug("[MaskedTex] resource absent: {} (mod={}, path={})", loc, loc.getNamespace(), resourcePath);
            return null;
        }
        try (InputStream is = Files.newInputStream(path)) {
            return NativeImage.read(is);
        } catch (Exception e) {
            LOGGER.error("[MaskedTex] PNG load failed for {} (mod={}, path={}): {}",
                    loc, loc.getNamespace(), resourcePath, e.getMessage(), e);
            return null;
        }
    }

    /**
     * mask size に合わせて base を nearest-neighbor scale up し、 per-pixel multiply + alpha AND。
     */
    private static NativeImage compose(NativeImage mask, NativeImage base) {
        int w = mask.getWidth();
        int h = mask.getHeight();
        int bw = base.getWidth();
        int bh = base.getHeight();
        NativeImage out = new NativeImage(w, h, false);
        for (int y = 0; y < h; y++) {
            int sy = (int) ((long) y * bh / h);
            for (int x = 0; x < w; x++) {
                int sx = (int) ((long) x * bw / w);
                int basePixel = base.getPixelRGBA(sx, sy);
                int maskPixel = mask.getPixelRGBA(x, y);

                int br = basePixel & 0xFF;
                int bg = (basePixel >> 8) & 0xFF;
                int bb = (basePixel >> 16) & 0xFF;
                int ba = (basePixel >> 24) & 0xFF;

                int mr = maskPixel & 0xFF;
                int mg = (maskPixel >> 8) & 0xFF;
                int mb = (maskPixel >> 16) & 0xFF;
                int ma = (maskPixel >> 24) & 0xFF;

                int or = (br * mr) / 255;
                int og = (bg * mg) / 255;
                int ob = (bb * mb) / 255;
                int oa = (ba * ma) / 255;

                out.setPixelRGBA(x, y, (oa << 24) | (ob << 16) | (og << 8) | or);
            }
        }
        return out;
    }
}
