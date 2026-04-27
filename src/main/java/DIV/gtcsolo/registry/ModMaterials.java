package DIV.gtcsolo.registry;

import DIV.gtcsolo.Gtcsolo;
import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlag;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.BlastProperty;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;

/**
 * EMallforone.js から移行した Material 登録。
 * Gtcsolo.addMaterials() (MaterialEvent) から呼ばれる。
 */
public class ModMaterials {

    // ── カスタム IconSet（専用テクスチャ使用） ──
    public static final MaterialIconSet ICON_SINGULARITY = new MaterialIconSet("singularity", MaterialIconSet.BRIGHT);
    public static final MaterialIconSet ICON_INFINITY    = new MaterialIconSet("infinity",    MaterialIconSet.BRIGHT);
    public static final MaterialIconSet ICON_ANTIMATTER  = new MaterialIconSet("antimatter",  MaterialIconSet.BRIGHT);

    // ── 共通フラグ ──
    private static final MaterialFlag[] COMMON_FLAGS = {
            MaterialFlags.GENERATE_PLATE,
            MaterialFlags.GENERATE_DENSE,
            MaterialFlags.GENERATE_ROD,
            MaterialFlags.GENERATE_LONG_ROD,
            MaterialFlags.GENERATE_BOLT_SCREW,
            MaterialFlags.GENERATE_RING,
            MaterialFlags.GENERATE_ROUND,
            MaterialFlags.GENERATE_GEAR,
            MaterialFlags.GENERATE_SMALL_GEAR,
            MaterialFlags.GENERATE_SPRING,
            MaterialFlags.GENERATE_FRAME,
            MaterialFlags.DISABLE_DECOMPOSITION
    };

    // ── 全素材フィールド（他クラスから参照可能にする） ──

    // 前提素材
    public static Material SKYSTONE;

    // 一般素材（ワイヤー付き）
    public static Material REFINED_GLOWSTONE;
    public static Material REFINED_OBSIDIAN;
    public static Material ENDNIUM;
    public static Material FRACTALINE;
    public static Material JUPITATE;
    public static Material HYPERX_NEUTRONIUM;
    public static Material ORIGINALIUM;
    public static Material MONEL;
    public static Material TARITON;
    public static Material INFUSED_STAINLESS_STEEL;
    public static Material OBLIVION;
    public static Material HSSX;
    public static Material PURE_NAQUADAH;
    public static Material IMPURE_OSMIUM;

    // 一般素材（シンプル）
    public static Material PROSPERITY;
    public static Material BETTER_GOLD;
    public static Material PLUSLITHERITE;
    public static Material SINGULARITY;
    public static Material SINGULARITY_TRITANIUM;
    public static Material FLAURITE;
    public static Material EXPORONIUM;
    public static Material HAFHNIUM;
    public static Material ENIGMA;
    public static Material STELLARIUM;
    public static Material STELLARIUM_ENIGMA;
    public static Material SINGULARITY_BISMATH;
    public static Material FRACTAL;
    public static Material TIME_MORY;
    public static Material SINGULARITY_IRON;
    public static Material SINGULARITY_NAQUADAH;
    public static Material SINGULARITY_SILVER;
    public static Material SINGULARITY_TUNGSTEN;
    public static Material SINGULARITY_OSMIUM;
    public static Material SINGULARITY_SAMARIUM;
    public static Material SINGULARITY_GOLD;
    public static Material SINGULARITY_DIAMOND;
    public static Material STAGNANTED_NEUTRONIUM;
    public static Material VALINIUM;
    public static Material BEDROCKIUM;

    // 合金素材
    public static Material SKYSTONE_TITANIUM;
    public static Material HAFHNIUM_DIBORIDE;
    public static Material HAFHNIUM_CARBIDE;
    public static Material ZIRCONIUM_DIBORIDE;
    public static Material KOVER;
    public static Material ULTRA_ALLOY;
    public static Material HE_BEDROCKIUM;

    // FEC 幻想元素
    public static Material MITHRIL;
    public static Material NOCTURNIUM;
    public static Material ETHERIUM;
    public static Material NEBULITE;
    public static Material ORICHALCUM;
    public static Material AXIOM_STEEL;
    public static Material MIALINEUM;
    public static Material NETHERA_MIALINEUM;
    public static Material AURORALIUM;
    public static Material VELZENIUM;
    public static Material ADAMANTITE;
    public static Material VILIRIA_STEEL;
    public static Material DILITHIUM;
    public static Material VESKER;
    public static Material HARMONIUM;
    public static Material URUMETAL;
    public static Material REFINED_NETHERITE;
    public static Material HE_NETHERITE;
    public static Material HE_FRIULTAIL;

    // 化合物
    public static Material CHLORINE_TRIFLUORIDE;
    public static Material HYDROGEN_PEROXIDE;
    public static Material HYDRAZINE;
    public static Material ANTIMONY_PENTAFLUORIDE;

    // SpaceForge 系素材
    public static Material STASIS;
    public static Material BARYON;

    // 上位素材
    public static Material INFINITY;
    public static Material ANTIMATTER;

    // 疑似プラズマ (擬似再現 — MaterialFlag plasma 不具合回避)
    public static Material STASIS_PLASMA;
    public static Material BARYON_PLASMA;
    public static Material BEDROCKIUM_PLASMA;
    public static Material JUPITATE_PLASMA;
    public static Material AURORALIUM_PLASMA;
    public static Material TIN_PLASMA;

    public static void init() {

        // ================================================================
        // 前提素材（GTCEu に存在しない、他素材の成分として必要）
        // ================================================================

        SKYSTONE = simpleMaterial("skystone", 0x404040, ModElements.E_SKYSTONE, false, false);

        // ================================================================
        // ワイヤー素材（conductorSuper 相当: ingot + blastTemp + cableProperties）
        // ================================================================

        // refined_glowstone: tier=MV, amp=4A, plasma
        REFINED_GLOWSTONE = wireMaterial("refined_glowstone", 0xE6C76A, ModElements.E_REFINED_GLOWSTONE,
                GTValues.MV, 4, true);

        // refined_obsidian: tier=EV, amp=16A
        REFINED_OBSIDIAN = wireMaterial("refined_obsidian", 0xC58BFF, ModElements.E_REFINED_OBSIDIAN,
                GTValues.EV, 16, false);

        // endnium: tier=EV, amp=32A, ore
        ENDNIUM = wireMaterialBuilder("endnium", 0x050007, ModElements.E_ENDNIUM,
                GTValues.EV, 32)
                .ore(1, 1)
                .buildAndRegister();

        // fractaline: tier=OpV, amp=1111111A (UXV から OpV へ昇格)
        FRACTALINE = wireMaterial("fractaline", 0x123456, ModElements.E_FRACTALINE,
                GTValues.OpV, 1111111, false);

        // jupitate: tier=UEV, amp=1024A, ore
        JUPITATE = wireMaterialBuilder("jupitate", 0x50C878, ModElements.E_JUPITATE,
                GTValues.UEV, 1024)
                .ore(1, 1)
                .buildAndRegister();

        // hyperx_neutronium: tier=UXV, amp=524888A, ore, plasma
        HYPERX_NEUTRONIUM = wireMaterialBuilder("hyperx_neutronium", 0x8B8589, ModElements.E_HYPERX_NEUTRONIUM,
                GTValues.UXV, 524888)
                .plasma()
                .ore(1, 1)
                .buildAndRegister();

        // originalium: tier=UV, amp=128A, ore
        ORIGINALIUM = wireMaterialBuilder("originalium", 0x505050, ModElements.E_ORIGINALIUM,
                GTValues.UV, 128)
                .ore(1, 1)
                .buildAndRegister();

        // monel: 通常合金 (MV超電導は refined_glowstone に一本化したのでワイヤー剥がし)
        MONEL = new Material.Builder(id("monel"))
                .ingot().dust().fluid()
                .color(0xFFE4E1)
                .iconSet(MaterialIconSet.METALLIC)
                .flags(COMMON_FLAGS)
                .components("copper", 1, "nickel", 2)
                .blastTemp(21600, BlastProperty.GasTier.HIGHEST, GTValues.VA[GTValues.MV], 4000)
                .buildAndRegister();

        // tariton: tier=LV, amp=32A, alloy (red_alloy 2 + blue_alloy 3)
        TARITON = alloyWireBuilder("tariton", 0x5D3FD3, GTValues.LV, 32)
                .components("red_alloy", 2, "blue_alloy", 3)
                .buildAndRegister();

        // infused_stainless_steel: tier=HV, amp=32A, alloy (作成方法は別途)
        INFUSED_STAINLESS_STEEL = alloyWireBuilder("infused_stainless_steel", 0xB4C4DF,
                GTValues.HV, 32)
                .buildAndRegister();

        // oblivion: tier=IV, amp=32A, 幻想元素系合金
        OBLIVION = alloyWireBuilder("oblivion", 0x1A0933, GTValues.IV, 32)
                .buildAndRegister();

        // hssx: tier=LuV, amp=32A, alloy (HSSG9+HSSS9+HSSE9+RoseGold5+StainlessSteel9)
        // EBF焼成用に blastTemp 付き (LuVゲート)
        HSSX = alloyWireBuilder("hssx", 0x5E4068, GTValues.LuV, 32)
                .components(GTMaterials.HSSG, 9, GTMaterials.HSSS, 9, GTMaterials.HSSE, 9,
                            GTMaterials.RoseGold, 5, GTMaterials.StainlessSteel, 9)
                .blastTemp(5400, BlastProperty.GasTier.HIGHEST, GTValues.VA[GTValues.LuV], 4000)
                .buildAndRegister();

        // pure_naquadah: tier=ZPM, amp=32A, 幻想元素 ΦNqa
        // レシピ保留、色はデフォルト Naquadah より明るめ
        PURE_NAQUADAH = wireMaterialBuilder("pure_naquadah", 0x4A4A4A, ModElements.E_PURE_NAQUADAH,
                GTValues.ZPM, 32)
                .buildAndRegister();

        // impure_osmium: Mek産出 osmium の差し替え先。低〜中tier Mekレシピで代替可能。
        // blastTemp 無し (EBF焼き回避)、plate等の COMMON_FLAGS も無し (精製前の粗素材)
        IMPURE_OSMIUM = new Material.Builder(id("impure_osmium"))
                .ingot().dust()
                .color(0x5A6B4D)
                .iconSet(MaterialIconSet.METALLIC)
                .flags(MaterialFlags.DISABLE_DECOMPOSITION)
                .element(ModElements.E_IMPURE_OSMIUM)
                .buildAndRegister();

        // ================================================================
        // 一般素材（非ワイヤー、非合金）
        // ================================================================

        PROSPERITY              = simpleMaterial("prosperity",              0x5FB8D6, ModElements.E_PROSPERITY,              false, false);
        BETTER_GOLD             = simpleMaterial("better_gold",             0xFFC300, ModElements.E_BETTER_GOLD,             false, false);
        SINGULARITY = new Material.Builder(id("singularity"))
                .ingot().dust().fluid()
                .color(0x2E0854)
                .iconSet(ICON_SINGULARITY)
                .flags(COMMON_FLAGS)
                .element(ModElements.E_SINGULARITY)
                .buildAndRegister();
        SINGULARITY_TRITANIUM   = simpleMaterial("singularity_tritanium",   0xB35A6B, ModElements.E_SINGULARITY_TRITANIUM,   false, false);
        SINGULARITY_BISMATH     = simpleMaterial("singularity_bismath",     0x7FC8CF, ModElements.E_SINGULARITY_BISMATH,     false, false);
        TIME_MORY               = simpleMaterial("time_mory",               0x32FF21, ModElements.E_TIME_MORY,               false, false);
        SINGULARITY_IRON        = simpleMaterial("singularity_iron",        0xE0E0E0, ModElements.E_SINGULARITY_IRON,        false, false);
        SINGULARITY_NAQUADAH    = simpleMaterial("singularity_naquadah",    0x4A4A4A, ModElements.E_SINGULARITY_NAQUADAH,    false, false);
        SINGULARITY_SILVER      = simpleMaterial("singularity_silver",      0xE6E6E6, ModElements.E_SINGULARITY_SILVER,      false, false);
        SINGULARITY_TUNGSTEN    = simpleMaterial("singularity_tungsten",    0x3A3A38, ModElements.E_SINGULARITY_TUNGSTEN,    false, false);
        SINGULARITY_OSMIUM      = simpleMaterial("singularity_osmium",      0x5A8FD6, ModElements.E_SINGULARITY_OSMIUM,      false, false);
        SINGULARITY_SAMARIUM    = simpleMaterial("singularity_samarium",    0xB7C98A, ModElements.E_SINGULARITY_SAMARIUM,    false, false);
        SINGULARITY_GOLD        = simpleMaterial("singularity_gold",        0xF9C834, ModElements.E_SINGULARITY_GOLD,        false, false);
        SINGULARITY_DIAMOND     = simpleMaterial("singularity_diamond",     0x33E0E0, ModElements.E_SINGULARITY_DIAMOND,     false, false);

        // ore 付き素材
        PLUSLITHERITE            = simpleMaterial("pluslitherite",            0x9FA3A7, ModElements.E_PLUSLITHERITE,            true,  false);
        FLAURITE                = simpleMaterial("flaurite",                0xB35A6B, ModElements.E_FLAURITE,                true,  false);
        EXPORONIUM              = simpleMaterial("exporonium",              0xADFF2F, ModElements.E_EXPORONIUM,              true,  false);
        HAFHNIUM                = simpleMaterial("hafhnium",                0x9FA3A7, ModElements.E_HAFHNIUM,                true,  false);
        ENIGMA                  = simpleMaterial("enigma",                  0xA3182F, ModElements.E_ENIGMA,                  true,  false);
        STELLARIUM              = simpleMaterial("stellarium",              0x9FA3A7, ModElements.E_STELLARIUM,              true,  false);
        // stellarium_enigma は合金セクションで登録（ore + components）
        // fractal: tier=UXV, amp=111111A, ore (UXV 超電導へ昇格)
        FRACTAL = wireMaterialBuilder("fractal", 0x123456, ModElements.E_FRACTAL,
                GTValues.UXV, 111111)
                .ore(1, 1)
                .buildAndRegister();
        STAGNANTED_NEUTRONIUM   = simpleMaterial("stagnanted_neutronium",   0xDFBFDF, ModElements.E_STAGNANTED_NEUTRONIUM,   true,  false);
        VALINIUM                = simpleMaterial("valinium",                0x8F8F8F, ModElements.E_VALINIUM,                true,  false);
        BEDROCKIUM              = simpleMaterial("bedrockium",              0x000000, ModElements.E_BEDROCKIUM,              true,  false);

        // ================================================================
        // 合金素材（components 指定）
        // ================================================================

        // stellarium_enigma: ore + 合金（自MOD素材を参照）
        STELLARIUM_ENIGMA = new Material.Builder(id("stellarium_enigma"))
                .ingot().dust().fluid()
                .color(0x9FA3A7)
                .iconSet(MaterialIconSet.METALLIC)
                .flags(COMMON_FLAGS)
                .components(STELLARIUM, 3, ENIGMA, 2)
                .ore(1, 1)
                .buildAndRegister();

        SKYSTONE_TITANIUM = new Material.Builder(id("skystone_titanium"))
                .ingot().dust().fluid()
                .color(0xC8A2FF)
                .iconSet(MaterialIconSet.METALLIC)
                .flags(COMMON_FLAGS)
                .components(SKYSTONE, 3, "titanium", 1)
                .ore(1, 1)
                .buildAndRegister();

        // hafhnium は自MOD素材なので gtcsolo: 名前空間を明示
        HAFHNIUM_DIBORIDE = new Material.Builder(id("hafhnium_diboride"))
                .ingot().dust().fluid()
                .color(0x5E6064)
                .iconSet(MaterialIconSet.METALLIC)
                .flags(COMMON_FLAGS)
                .components(HAFHNIUM, 1, "boron", 2)
                .buildAndRegister();

        HAFHNIUM_CARBIDE = new Material.Builder(id("hafhnium_carbide"))
                .ingot().dust().fluid()
                .color(0xB8A07E)
                .iconSet(MaterialIconSet.METALLIC)
                .flags(COMMON_FLAGS)
                .components(HAFHNIUM, 1, "carbon", 1)
                .buildAndRegister();

        ZIRCONIUM_DIBORIDE = new Material.Builder(id("zirconium_diboride"))
                .ingot().dust().fluid()
                .color(0xD1D1D1)
                .iconSet(MaterialIconSet.METALLIC)
                .flags(COMMON_FLAGS)
                .components("zirconium", 1, "boron", 2)
                .buildAndRegister();

        KOVER = new Material.Builder(id("kover"))
                .ingot().dust().fluid()
                .color(0x33E0E0)
                .iconSet(MaterialIconSet.METALLIC)
                .flags(COMMON_FLAGS)
                .components("iron", 1, "nickel", 1, "cobalt", 1)
                .buildAndRegister();

        ULTRA_ALLOY = new Material.Builder(id("ultra_alloy"))
                .ingot().dust().fluid()
                .color(0xFF00FF)
                .iconSet(MaterialIconSet.METALLIC)
                .flags(COMMON_FLAGS)
                .components("gold", 1, "silver", 1, "copper", 1, "tin", 1)
                .buildAndRegister();

        // bedrockium, netherite は自MOD素材なので Material 参照を使う
        HE_BEDROCKIUM = new Material.Builder(id("he_bedrockium"))
                .ingot().dust().fluid()
                .color(0xAFAFAF)
                .iconSet(MaterialIconSet.METALLIC)
                .flags(COMMON_FLAGS)
                .flags(MaterialFlags.GENERATE_FINE_WIRE)
                .components(BEDROCKIUM, 3, "netherite", 3, "tungsten", 3, "neutronium", 1)
                .ore(1, 1)
                .plasma()
                .blastTemp(21600, BlastProperty.GasTier.HIGHEST, GTValues.VA[GTValues.UHV], 4000)
                .cableProperties(GTValues.V[GTValues.UHV], 32768, 0, true)
                .buildAndRegister();

        // ================================================================
        // FEC 幻想元素
        // ================================================================

        MITHRIL             = simpleMaterial("mithril",             0xBFC9D9, ModElements.E_MITHRIL,             false, false);
        NOCTURNIUM          = simpleMaterial("nocturnium",          0x1C2238, ModElements.E_NOCTURNIUM,          false, false);
        ETHERIUM            = simpleMaterial("etherium",            0xD7E6F5, ModElements.E_ETHERIUM,            false, false);
        NEBULITE            = simpleMaterial("nebulite",            0x7A6FA8, ModElements.E_NEBULITE,            false, false);
        ORICHALCUM          = simpleMaterial("orichalcum",          0xC88A3D, ModElements.E_ORICHALCUM,          false, false);
        AXIOM_STEEL         = simpleMaterial("axiom_steel",         0x6E7684, ModElements.E_AXIOM_STEEL,         false, false);
        MIALINEUM           = simpleMaterial("mialineum",           0xE7F6FF, ModElements.E_MIALINEUM,           false, false);
        NETHERA_MIALINEUM   = simpleMaterial("nethera_mialineum",   0xB85A73, ModElements.E_NETHERA_MIALINEUM,   false, false);
        AURORALIUM          = simpleMaterial("auroralium",          0x69D4C3, ModElements.E_AURORALIUM,          false, false);
        VELZENIUM           = simpleMaterial("velzenium",           0x6A1B3A, ModElements.E_VELZENIUM,           false, false);
        ADAMANTITE          = simpleMaterial("adamantite",          0x7E8A96, ModElements.E_ADAMANTITE,          false, false);
        VILIRIA_STEEL       = simpleMaterial("viliria_steel",       0x4A4A4F, ModElements.E_VILIRIA_STEEL,       false, false);
        DILITHIUM           = simpleMaterial("dilithium",           0x74A7FF, ModElements.E_DILITHIUM,           false, false);
        VESKER              = simpleMaterial("vesker",              0x8A8F99, ModElements.E_VESKER,              false, false);
        HARMONIUM           = simpleMaterial("harmonium",           0xF1D36B, ModElements.E_HARMONIUM,           false, false);
        URUMETAL            = simpleMaterial("urumetal",            0xD7B45A, ModElements.E_URUMETAL,            false, false);
        REFINED_NETHERITE   = simpleMaterial("refined_netherite",   0x746772, ModElements.E_REFINED_NETHERITE,   false, false);
        HE_NETHERITE        = simpleMaterial("he_netherite",        0x4D434A, ModElements.E_HE_NETHERITE,        false, false);
        HE_FRIULTAIL        = simpleMaterial("he_friultail",        0x8FC9C7, ModElements.E_HE_FRIULTAIL,        false, false);

        // ================================================================
        // 化合物（components 指定、Element 不要）
        // ================================================================

        CHLORINE_TRIFLUORIDE = new Material.Builder(id("chlorine_trifluoride"))
                .liquid()
                .color(0xC8F0C8)
                .components(GTMaterials.Chlorine, 1, GTMaterials.Fluorine, 3)
                .flags(MaterialFlags.DISABLE_DECOMPOSITION)
                .buildAndRegister();

        // H₂O₂
        HYDROGEN_PEROXIDE = new Material.Builder(id("hydrogen_peroxide"))
                .liquid()
                .color(0xD1EAFF)
                .components(GTMaterials.Hydrogen, 2, GTMaterials.Oxygen, 2)
                .flags(MaterialFlags.DISABLE_DECOMPOSITION)
                .buildAndRegister();

        // N₂H₄
        HYDRAZINE = new Material.Builder(id("hydrazine"))
                .liquid()
                .color(0xE0E0F0)
                .components(GTMaterials.Nitrogen, 2, GTMaterials.Hydrogen, 4)
                .flags(MaterialFlags.DISABLE_DECOMPOSITION)
                .buildAndRegister();

        // SbF₅
        ANTIMONY_PENTAFLUORIDE = new Material.Builder(id("antimony_pentafluoride"))
                .liquid()
                .color(0xD2D2A0)
                .components(GTMaterials.Antimony, 1, GTMaterials.Fluorine, 5)
                .flags(MaterialFlags.DISABLE_DECOMPOSITION)
                .buildAndRegister();

        // ================================================================
        // 上位素材
        // ================================================================

        INFINITY = new Material.Builder(id("infinity"))
                .ingot().dust().fluid()
                .color(0xFFFFFF)
                .iconSet(ICON_INFINITY)
                .flags(MaterialFlags.GENERATE_PLATE,
                        MaterialFlags.GENERATE_DENSE,
                        MaterialFlags.GENERATE_ROD,
                        MaterialFlags.GENERATE_LONG_ROD,
                        MaterialFlags.GENERATE_BOLT_SCREW,
                        MaterialFlags.GENERATE_RING,
                        MaterialFlags.GENERATE_ROUND,
                        MaterialFlags.GENERATE_GEAR,
                        MaterialFlags.GENERATE_SMALL_GEAR,
                        MaterialFlags.DISABLE_DECOMPOSITION)
                .element(ModElements.E_INFINITY)
                .blastTemp(321600, BlastProperty.GasTier.HIGHEST, GTValues.VA[GTValues.OpV], 4000)
                .buildAndRegister();

        ANTIMATTER = new Material.Builder(id("antimatter"))
                .ingot().dust().fluid()
                .color(0x8B00B8)
                .iconSet(ICON_ANTIMATTER)
                .flags(MaterialFlags.GENERATE_PLATE,
                        MaterialFlags.GENERATE_DENSE,
                        MaterialFlags.GENERATE_ROD,
                        MaterialFlags.GENERATE_LONG_ROD,
                        MaterialFlags.GENERATE_BOLT_SCREW,
                        MaterialFlags.GENERATE_RING,
                        MaterialFlags.GENERATE_ROUND,
                        MaterialFlags.GENERATE_GEAR,
                        MaterialFlags.GENERATE_SMALL_GEAR,
                        MaterialFlags.DISABLE_DECOMPOSITION)
                .element(ModElements.E_ANTIMATTER)
                .buildAndRegister();

        // ================================================================
        // SpaceForge 系素材 (ingot/dust/fluid のみ、wire/cable/ore なし)
        // ================================================================

        STASIS = simpleMaterial("stasis", 0xC2E7FF, ModElements.E_STASIS, false, false);
        BARYON = simpleMaterial("baryon", 0x8A2BE2, ModElements.E_BARYON, false, false);

        // ================================================================
        // 疑似プラズマ素材 — liquid のみ、温度 1億F 指定
        // MaterialFlag.plasma() は挙動不安定のため、別素材として登録
        // forge:plasma タグで識別（data/forge/tags/fluids/plasma.json）
        // ================================================================

        STASIS_PLASMA     = pseudoPlasma("stasis_plasma",     0xC2E7FF, ModElements.E_STASIS);
        BARYON_PLASMA     = pseudoPlasma("baryon_plasma",     0x8A2BE2, ModElements.E_BARYON);
        BEDROCKIUM_PLASMA = pseudoPlasma("bedrockium_plasma", 0x000000, ModElements.E_BEDROCKIUM);
        JUPITATE_PLASMA   = pseudoPlasma("jupitate_plasma",   0x50C878, ModElements.E_JUPITATE);
        AURORALIUM_PLASMA = pseudoPlasma("auroralium_plasma", 0x69D4C3, ModElements.E_AURORALIUM);
        TIN_PLASMA        = pseudoPlasma("tin_plasma",        0xDCDCDC, com.gregtechceu.gtceu.common.data.GTElements.Sn);
    }

    // ── クリエイティブタブ登録 ──
    // gtcsolo レジストリの全素材 × 全TagPrefix を自動で吸い込む

    private static final TagPrefix[] CREATIVE_PREFIXES = {
            TagPrefix.ingot, TagPrefix.dust, TagPrefix.nugget,
            TagPrefix.plate, TagPrefix.plateDense,
            TagPrefix.rod, TagPrefix.rodLong,
            TagPrefix.bolt, TagPrefix.screw,
            TagPrefix.ring, TagPrefix.round,
            TagPrefix.gear, TagPrefix.gearSmall,
            TagPrefix.spring, TagPrefix.springSmall,
            TagPrefix.foil, TagPrefix.wireGtSingle,
            TagPrefix.block, TagPrefix.rawOre,
            // ore の 15 stone 変種 (存在する material のみ自動で ChemicalHelper.get が返す)
            TagPrefix.ore, TagPrefix.oreGranite, TagPrefix.oreDiorite,
            TagPrefix.oreAndesite, TagPrefix.oreRedGranite, TagPrefix.oreMarble,
            TagPrefix.oreDeepslate, TagPrefix.oreTuff, TagPrefix.oreSand,
            TagPrefix.oreRedSand, TagPrefix.oreGravel, TagPrefix.oreBasalt,
            TagPrefix.oreNetherrack, TagPrefix.oreBlackstone, TagPrefix.oreEndstone,
            TagPrefix.gem, TagPrefix.lens,
            TagPrefix.frameGt
    };

    public static void addToCreativeTab(CreativeModeTab.Output output) {
        Collection<Material> allMats = GTCEuAPI.materialManager
                .getRegistry(Gtcsolo.MODID).getAllMaterials();
        for (Material mat : allMats) {
            for (TagPrefix prefix : CREATIVE_PREFIXES) {
                ItemStack stack = ChemicalHelper.get(prefix, mat);
                if (!stack.isEmpty()) {
                    output.accept(stack);
                }
            }
        }
    }

    // ── ケーブル専用タブ用 prefix（1倍〜16倍、裸線+絶縁両方） ──
    // 超電導素材は cableGt*（絶縁）が生成されないため wireGt* も含める
    private static final TagPrefix[] CABLE_PREFIXES = {
            TagPrefix.wireGtSingle,
            TagPrefix.wireGtDouble,
            TagPrefix.wireGtQuadruple,
            TagPrefix.wireGtOctal,
            TagPrefix.wireGtHex,
            TagPrefix.cableGtSingle,
            TagPrefix.cableGtDouble,
            TagPrefix.cableGtQuadruple,
            TagPrefix.cableGtOctal,
            TagPrefix.cableGtHex
    };

    public static void addCablesToCreativeTab(CreativeModeTab.Output output) {
        Collection<Material> allMats = GTCEuAPI.materialManager
                .getRegistry(Gtcsolo.MODID).getAllMaterials();
        for (Material mat : allMats) {
            for (TagPrefix prefix : CABLE_PREFIXES) {
                ItemStack stack = ChemicalHelper.get(prefix, mat);
                if (!stack.isEmpty()) {
                    output.accept(stack);
                }
            }
        }
    }

    // ── ヘルパー ──

    private static ResourceLocation id(String path) {
        return new ResourceLocation(Gtcsolo.MODID, path);
    }

    /**
     * 疑似プラズマ素材 — liquid のみ、温度 1億F、元素シンボル付き。
     * forge:plasma タグで識別。
     */
    private static Material pseudoPlasma(String name, int color,
                                          com.gregtechceu.gtceu.api.data.chemical.Element element) {
        return new Material.Builder(id(name))
                .liquid(new com.gregtechceu.gtceu.api.fluids.FluidBuilder()
                        .temperature(100_000_000))
                .color(color)
                .iconSet(MaterialIconSet.SHINY)
                .flags(MaterialFlags.DISABLE_DECOMPOSITION)
                .element(element)
                .buildAndRegister();
    }

    /**
     * シンプルな ingot 素材（非ワイヤー、非合金）
     */
    private static Material simpleMaterial(String name, int color,
                                           com.gregtechceu.gtceu.api.data.chemical.Element element,
                                           boolean ore, boolean plasma) {
        Material.Builder builder = new Material.Builder(id(name))
                .ingot().dust().fluid()
                .color(color)
                .iconSet(MaterialIconSet.METALLIC)
                .flags(COMMON_FLAGS)
                .element(element);

        if (ore) builder.ore(1, 1);
        if (plasma) builder.plasma();

        return builder.buildAndRegister();
    }

    /**
     * ワイヤー素材の Builder を返す（追加設定が必要な場合用）
     */
    private static Material.Builder wireMaterialBuilder(String name, int color,
                                                         com.gregtechceu.gtceu.api.data.chemical.Element element,
                                                         int tier, int amperage) {
        return new Material.Builder(id(name))
                .ingot().dust().fluid()
                .color(color)
                .iconSet(MaterialIconSet.METALLIC)
                .flags(COMMON_FLAGS)
                .flags(MaterialFlags.GENERATE_FINE_WIRE)
                .element(element)
                .blastTemp(21600, BlastProperty.GasTier.HIGHEST, GTValues.VA[tier], 4000)
                .cableProperties(GTValues.V[tier], amperage, 0, true);
    }

    /**
     * ワイヤー素材（追加設定不要の場合）
     */
    private static Material wireMaterial(String name, int color,
                                         com.gregtechceu.gtceu.api.data.chemical.Element element,
                                         int tier, int amperage, boolean plasma) {
        Material.Builder builder = wireMaterialBuilder(name, color, element, tier, amperage);
        if (plasma) builder.plasma();
        return builder.buildAndRegister();
    }

    /**
     * 合金ワイヤー素材の Builder (element無し、blastTemp無し)。超電導設定は cableProperties で isSuperconductor=true。
     */
    private static Material.Builder alloyWireBuilder(String name, int color, int tier, int amperage) {
        return new Material.Builder(id(name))
                .ingot().dust().fluid()
                .color(color)
                .iconSet(MaterialIconSet.METALLIC)
                .flags(COMMON_FLAGS)
                .flags(MaterialFlags.GENERATE_FINE_WIRE)
                .cableProperties(GTValues.V[tier], amperage, 0, true);
    }
}