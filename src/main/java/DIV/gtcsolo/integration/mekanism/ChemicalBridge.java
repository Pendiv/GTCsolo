package DIV.gtcsolo.integration.mekanism;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.api.fluids.FluidBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.*;

/**
 * Mekanism Chemical Bridge
 *
 * Phase 1: MaterialEvent で静的リストからGT液体素材を生成。
 * Phase 2: FMLCommonSetupEvent で Mekanism レジストリと照合・検証。
 *
 * Mekanismのレジストリは MaterialEvent 時点で未作成のため、
 * ソースから読み取った静的データで登録する。
 */
public class ChemicalBridge {

    private static final Logger LOGGER = LogUtils.getLogger();

    public enum ChemType { GAS, INFUSION, PIGMENT, SLURRY }

    public record ChemDef(ChemType type, String namespace, String path, int color) {}

    private static final Map<String, String> chemKeyToMaterial = new LinkedHashMap<>();
    private static final Map<String, String> materialToChemKey = new LinkedHashMap<>();
    private static int createdCount = 0;

    // =========================================================================
    //  静的データ（Mekanismソースから抽出）
    // =========================================================================

    private static List<ChemDef> buildStaticList() {
        List<ChemDef> list = new ArrayList<>();
        String mek = "mekanism";

        // --- Gas (24種) ---
        list.add(new ChemDef(ChemType.GAS, mek, "hydrogen",            0xFFFFFF));
        list.add(new ChemDef(ChemType.GAS, mek, "oxygen",              0x6CE2FF));
        list.add(new ChemDef(ChemType.GAS, mek, "steam",               0xC4C4C4));
        list.add(new ChemDef(ChemType.GAS, mek, "water_vapor",         0xC4C4C4));
        list.add(new ChemDef(ChemType.GAS, mek, "chlorine",            0xCFE800));
        list.add(new ChemDef(ChemType.GAS, mek, "sulfur_dioxide",      0xA99D90));
        list.add(new ChemDef(ChemType.GAS, mek, "sulfur_trioxide",     0xCE6C6C));
        list.add(new ChemDef(ChemType.GAS, mek, "sulfuric_acid",       0x82802B));
        list.add(new ChemDef(ChemType.GAS, mek, "hydrogen_chloride",   0xA8F1E9));
        list.add(new ChemDef(ChemType.GAS, mek, "hydrofluoric_acid",   0x809960));
        list.add(new ChemDef(ChemType.GAS, mek, "uranium_oxide",       0xE1F573));
        list.add(new ChemDef(ChemType.GAS, mek, "uranium_hexafluoride",0x809960));
        list.add(new ChemDef(ChemType.GAS, mek, "ethene",              0xEACCF9));
        list.add(new ChemDef(ChemType.GAS, mek, "sodium",              0xE9FEF4));
        list.add(new ChemDef(ChemType.GAS, mek, "superheated_sodium",  0xD19469));
        list.add(new ChemDef(ChemType.GAS, mek, "brine",               0xFEEF9C));
        list.add(new ChemDef(ChemType.GAS, mek, "lithium",             0xEBA400));
        list.add(new ChemDef(ChemType.GAS, mek, "osmium",              0x52BDCA));
        list.add(new ChemDef(ChemType.GAS, mek, "fissile_fuel",        0x2E332F));
        list.add(new ChemDef(ChemType.GAS, mek, "nuclear_waste",       0x4F412A));
        list.add(new ChemDef(ChemType.GAS, mek, "spent_nuclear_waste", 0x262015));
        list.add(new ChemDef(ChemType.GAS, mek, "plutonium",           0x1F919C));
        list.add(new ChemDef(ChemType.GAS, mek, "polonium",            0x1B9E7B));
        list.add(new ChemDef(ChemType.GAS, mek, "antimatter",          0xA464B3));

        // --- InfuseType (8種) ---
        list.add(new ChemDef(ChemType.INFUSION, mek, "carbon",           0x2C2C2C));
        list.add(new ChemDef(ChemType.INFUSION, mek, "redstone",         0xB30505));
        list.add(new ChemDef(ChemType.INFUSION, mek, "diamond",          0x6CEDD8));
        list.add(new ChemDef(ChemType.INFUSION, mek, "refined_obsidian", 0x7C00ED));
        list.add(new ChemDef(ChemType.INFUSION, mek, "gold",             0xF2CD67));
        list.add(new ChemDef(ChemType.INFUSION, mek, "tin",              0xCCCCD9));
        list.add(new ChemDef(ChemType.INFUSION, mek, "fungi",            0x74656A));
        list.add(new ChemDef(ChemType.INFUSION, mek, "bio",              0x5A4630));

        // --- Pigment (18色 — EnumColor.registryPrefix がレジストリ名) ---
        addPigment(list, mek, "black",       0x404040);
        addPigment(list, mek, "blue",        0x3670CF);
        addPigment(list, mek, "green",       0x2D6A00);
        addPigment(list, mek, "cyan",        0x218A8A);
        addPigment(list, mek, "dark_red",    0x8C1717);
        addPigment(list, mek, "purple",      0x7E30A5);
        addPigment(list, mek, "orange",      0xCC6F0A);
        addPigment(list, mek, "light_gray",  0x8C8C8C);
        addPigment(list, mek, "gray",        0x565656);
        addPigment(list, mek, "light_blue",  0x2580D8);
        addPigment(list, mek, "lime",        0x3DC93D);
        addPigment(list, mek, "aqua",        0x21C1C1);
        addPigment(list, mek, "red",         0xD82121);
        addPigment(list, mek, "magenta",     0xD8A9D8);
        addPigment(list, mek, "yellow",      0xD8D821);
        addPigment(list, mek, "white",       0xF0F0F0);
        addPigment(list, mek, "brown",       0x6F4224);
        addPigment(list, mek, "pink",        0xD8A9D8);

        // --- Slurry (7鉱石 × dirty/clean = 14種) ---
        String[] ores = {"iron", "gold", "osmium", "copper", "tin", "lead", "uranium"};
        int[] oreTints = {0xAF8E77, 0xF2CD67, 0x1E79C3, 0xAA4B19, 0xCCCCD9, 0x3A404A, 0x46664F};
        for (int i = 0; i < ores.length; i++) {
            list.add(new ChemDef(ChemType.SLURRY, mek, "dirty_" + ores[i], oreTints[i]));
            list.add(new ChemDef(ChemType.SLURRY, mek, "clean_" + ores[i], oreTints[i]));
        }

        return list;
    }

    private static void addPigment(List<ChemDef> list, String ns, String name, int color) {
        list.add(new ChemDef(ChemType.PIGMENT, ns, name, color));
    }

    // =========================================================================
    //  MaterialEvent で呼ぶ
    // =========================================================================

    public static void registerAllChemicalsAsMaterials() {
        LOGGER.info("[ChemBridge] === Starting Chemical → GT Material registration ===");

        List<ChemDef> defs = buildStaticList();
        LOGGER.info("[ChemBridge] Static list: {} definitions", defs.size());

        createdCount = 0;
        int skipped = 0;

        for (ChemDef def : defs) {
            String materialName = buildMaterialName(def);
            String chemKey = buildChemKey(def);

            if (materialToChemKey.containsKey(materialName)) {
                LOGGER.warn("[ChemBridge]   Skip (dup): {} → {}", chemKey, materialName);
                skipped++;
                continue;
            }

            try {
                ResourceLocation matId = new ResourceLocation("gtcsolo", materialName);
                new Material.Builder(matId)
                        .liquid(new FluidBuilder())
                        .color(def.color)
                        .flags(MaterialFlags.DISABLE_DECOMPOSITION)
                        .buildAndRegister();

                chemKeyToMaterial.put(chemKey, materialName);
                materialToChemKey.put(materialName, chemKey);
                createdCount++;

                LOGGER.info("[ChemBridge]   OK: {} → gt:{} (0x{})",
                        chemKey, materialName, Integer.toHexString(def.color & 0xFFFFFF));

            } catch (Exception e) {
                LOGGER.error("[ChemBridge]   FAIL: {} → {}: {}", chemKey, materialName, e.getMessage());
                skipped++;
            }
        }

        LOGGER.info("[ChemBridge] === Phase 1 Complete ===");
        LOGGER.info("[ChemBridge]   Defined: {}  Created: {}  Skipped: {}  Mappings: {}",
                defs.size(), createdCount, skipped, chemKeyToMaterial.size());
    }

    // =========================================================================
    //  FMLCommonSetupEvent で呼ぶ — Mekレジストリとの照合
    // =========================================================================

    public static void validateAgainstMekanismRegistry() {
        LOGGER.info("[ChemBridge] === Phase 2: Validating against Mekanism registries ===");

        try {
            int gasCount = validateType("GAS",
                    mekanism.api.MekanismAPI.gasRegistry(), ChemType.GAS);
            int infCount = validateType("INFUSION",
                    mekanism.api.MekanismAPI.infuseTypeRegistry(), ChemType.INFUSION);
            int pigCount = validateType("PIGMENT",
                    mekanism.api.MekanismAPI.pigmentRegistry(), ChemType.PIGMENT);
            int sluCount = validateType("SLURRY",
                    mekanism.api.MekanismAPI.slurryRegistry(), ChemType.SLURRY);

            int total = gasCount + infCount + pigCount + sluCount;
            LOGGER.info("[ChemBridge] Registry total: {} (gas={} inf={} pig={} slu={})",
                    total, gasCount, infCount, pigCount, sluCount);
            LOGGER.info("[ChemBridge] Our mappings: {}", chemKeyToMaterial.size());

            if (total > chemKeyToMaterial.size()) {
                LOGGER.warn("[ChemBridge] ⚠ {} in registry > {} mapped — addon chemicals missing!",
                        total, chemKeyToMaterial.size());
            }
        } catch (Exception e) {
            LOGGER.error("[ChemBridge] Validation error: {}", e.getMessage());
        }
    }

    private static <C extends mekanism.api.chemical.Chemical<C>> int validateType(
            String name, net.minecraftforge.registries.IForgeRegistry<C> reg, ChemType type) {
        if (reg == null) { LOGGER.warn("[ChemBridge] {} registry NULL", name); return 0; }

        int count = 0, missing = 0;
        for (var entry : reg.getEntries()) {
            if (entry.getValue().isEmptyType()) continue;
            ResourceLocation id = entry.getKey().location();
            if ("kubejs".equals(id.getNamespace())) continue;
            count++;
            String key = type.name().toLowerCase() + ":" + id.getNamespace() + ":" + id.getPath();
            if (!chemKeyToMaterial.containsKey(key)) {
                LOGGER.warn("[ChemBridge]   UNMAPPED: {} {}", name, id);
                missing++;
            }
        }
        LOGGER.info("[ChemBridge] {} — in registry: {}  unmapped: {}", name, count, missing);
        return count;
    }

    // =========================================================================
    //  ヘルパー/ルックアップ
    // =========================================================================

    private static String buildMaterialName(ChemDef def) {
        return def.type.name().toLowerCase() + "_" + def.namespace + "_"
                + def.path.replaceAll("[^a-z0-9_]", "_");
    }

    private static String buildChemKey(ChemDef def) {
        return def.type.name().toLowerCase() + ":" + def.namespace + ":" + def.path;
    }

    public static String getMaterialName(String chemKey) { return chemKeyToMaterial.get(chemKey); }
    public static String getChemKey(String materialName) { return materialToChemKey.get(materialName); }
    public static Map<String, String> getAllMappings() { return Collections.unmodifiableMap(chemKeyToMaterial); }
    public static int getCreatedCount() { return createdCount; }
}
