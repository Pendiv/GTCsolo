package DIV.gtcsolo.registry;

import com.gregtechceu.gtceu.api.data.chemical.Element;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

/**
 * EMallforone.js から移行した Element 登録。
 * 合金（components 指定）以外の素材に対応する Element を登録する。
 * GtcSoloAddon.registerElements() から呼ばれる。
 */
public class ModElements {

    // ── 前提素材（GTCEu に存在しないもの） ──
    public static Element E_SKYSTONE;

    // ── 一般素材 ──
    public static Element E_REFINED_GLOWSTONE;
    public static Element E_PROSPERITY;
    public static Element E_REFINED_OBSIDIAN;
    public static Element E_BETTER_GOLD;
    public static Element E_PLUSLITHERITE;
    public static Element E_SINGULARITY;
    public static Element E_SINGULARITY_TRITANIUM;
    public static Element E_FLAURITE;
    public static Element E_EXPORONIUM;
    public static Element E_ENDNIUM;
    public static Element E_HAFHNIUM;
    public static Element E_ENIGMA;
    public static Element E_STELLARIUM;
    public static Element E_SINGULARITY_BISMATH;
    public static Element E_FRACTAL;
    public static Element E_TIME_MORY;
    public static Element E_FRACTALINE;
    public static Element E_SINGULARITY_IRON;
    public static Element E_SINGULARITY_NAQUADAH;
    public static Element E_SINGULARITY_SILVER;
    public static Element E_SINGULARITY_TUNGSTEN;
    public static Element E_SINGULARITY_OSMIUM;
    public static Element E_SINGULARITY_SAMARIUM;
    public static Element E_SINGULARITY_GOLD;
    public static Element E_SINGULARITY_DIAMOND;
    public static Element E_JUPITATE;
    public static Element E_HYPERX_NEUTRONIUM;
    public static Element E_STAGNANTED_NEUTRONIUM;
    public static Element E_VALINIUM;
    public static Element E_ORIGINALIUM;
    public static Element E_BEDROCKIUM;

    // ── FEC 幻想元素 ──
    public static Element E_MITHRIL;
    public static Element E_NOCTURNIUM;
    public static Element E_ETHERIUM;
    public static Element E_NEBULITE;
    public static Element E_ORICHALCUM;
    public static Element E_AXIOM_STEEL;
    public static Element E_MIALINEUM;
    public static Element E_NETHERA_MIALINEUM;
    public static Element E_AURORALIUM;
    public static Element E_VELZENIUM;
    public static Element E_ADAMANTITE;
    public static Element E_VILIRIA_STEEL;
    public static Element E_DILITHIUM;
    public static Element E_VESKER;
    public static Element E_HARMONIUM;
    public static Element E_URUMETAL;
    public static Element E_REFINED_NETHERITE;
    public static Element E_HE_NETHERITE;
    public static Element E_HE_FRIULTAIL;

    // SpaceForge 系素材
    public static Element E_STASIS;
    public static Element E_BARYON;

    // 上位素材
    public static Element E_INFINITY;
    public static Element E_ANTIMATTER;

    public static void init() {
        // 前提素材
        E_SKYSTONE                 = create(110, 160, "e_skystone",                "Sk");

        // SpaceForge 系
        E_STASIS                   = create(110, 160, "e_stasis",                  "=ST=");
        E_BARYON                   = create(110, 160, "e_baryon",                  "=◇=");

        // 一般素材
        E_REFINED_GLOWSTONE        = create(110, 160, "e_refined_glowstone",       "OsGl");
        E_PROSPERITY               = create(110, 160, "e_prosperity",               "Ps");
        E_REFINED_OBSIDIAN         = create(110, 160, "e_refined_obsidian",         "OsMgFeSi2O4");
        E_BETTER_GOLD              = create(110, 160, "e_better_gold",              "⇑Au");
        E_PLUSLITHERITE             = create(110, 160, "e_pluslitherite",             "⇑NrLi3(C2H3ClW)");
        E_SINGULARITY              = create(110, 160, "e_singularity",              "◇");
        E_SINGULARITY_TRITANIUM    = create(110, 160, "e_singularity_tritanium",    "◇Tr");
        E_FLAURITE                 = create(110, 160, "e_flaurite",                 "Fa");
        E_EXPORONIUM               = create(169, 119, "e_exporonium",               "ExPr3");
        E_ENDNIUM                  = create(110, 160, "e_endnium",                  "θEn");
        E_HAFHNIUM                 = create(110, 160, "e_hafhnium",                 "Hf");
        E_ENIGMA                   = create(110, 160, "e_enigma",                   "Eg");
        E_STELLARIUM               = create(110, 160, "e_stellarium",               "St");
        E_SINGULARITY_BISMATH      = create(110, 160, "e_singularity_bismath",      "◇Bi");
        E_FRACTAL                  = create(110, 160, "e_fractal",                  "≡F");
        E_TIME_MORY                = create(110, 160, "e_time_mory",                "T=F");
        E_FRACTALINE               = create(110, 160, "e_fractaline",               "-F");
        E_SINGULARITY_IRON         = create(110, 160, "e_singularity_iron",         "◇Fe");
        E_SINGULARITY_NAQUADAH     = create(110, 160, "e_singularity_naquadah",     "◇Nq");
        E_SINGULARITY_SILVER       = create(110, 160, "e_singularity_silver",       "◇Ag");
        E_SINGULARITY_TUNGSTEN     = create(110, 160, "e_singularity_tungsten",     "◇W");
        E_SINGULARITY_OSMIUM       = create(110, 160, "e_singularity_osmium",       "◇Os");
        E_SINGULARITY_SAMARIUM     = create(110, 160, "e_singularity_samarium",     "◇Sm");
        E_SINGULARITY_GOLD         = create(110, 160, "e_singularity_gold",         "◇Au");
        E_SINGULARITY_DIAMOND      = create(110, 160, "e_singularity_diamond",      "◇H");
        E_JUPITATE                 = create(110, 160, "e_jupitate",                 "Ju");
        E_HYPERX_NEUTRONIUM        = create(110, 160, "e_hyperx_neutronium",        "Hy≡χNu");
        E_STAGNANTED_NEUTRONIUM    = create(110, 160, "e_stagnanted_neutronium",    "Nu??");
        E_VALINIUM                 = create(110, 160, "e_valinium",                 "Vl");
        E_ORIGINALIUM              = create(110, 160, "e_originalium",              "Or");
        E_BEDROCKIUM               = create(110, 160, "e_bedrockium",               "◇Br");

        // 上位素材
        E_INFINITY                 = create(110, 160, "e_infinity",                "∞");
        E_ANTIMATTER               = create(110, 160, "e_antimatter",              "⊘");

        // FEC 幻想元素
        E_MITHRIL                  = create(110, 160, "e_mithril",                  "ΦMit");
        E_NOCTURNIUM               = create(110, 160, "e_nocturnium",               "ΦNoc");
        E_ETHERIUM                 = create(110, 160, "e_etherium",                 "ΦEth");
        E_NEBULITE                 = create(110, 160, "e_nebulite",                 "ΦNeb");
        E_ORICHALCUM               = create(110, 160, "e_orichalcum",               "ΦOri");
        E_AXIOM_STEEL              = create(110, 160, "e_axiom_steel",              "FeΦAxi");
        E_MIALINEUM                = create(110, 160, "e_mialineum",                "ΦMia");
        E_NETHERA_MIALINEUM        = create(110, 160, "e_nethera_mialineum",        "Nr+ΦMia");
        E_AURORALIUM               = create(110, 160, "e_auroralium",               "ΦAur");
        E_VELZENIUM                = create(110, 160, "e_velzenium",                "ΦVel");
        E_ADAMANTITE               = create(110, 160, "e_adamantite",               "ΦAdm");
        E_VILIRIA_STEEL            = create(110, 160, "e_viliria_steel",            "FeΦVil");
        E_DILITHIUM                = create(110, 160, "e_dilithium",                "ΦDil");
        E_VESKER                   = create(110, 160, "e_vesker",                   "ΦVes");
        E_HARMONIUM                = create(110, 160, "e_harmonium",                "ΦHrm");
        E_URUMETAL                 = create(110, 160, "e_urumetal",                 "ΦUru");
        E_REFINED_NETHERITE        = create(110, 160, "e_refined_netherite",        "Nr+");
        E_HE_NETHERITE             = create(110, 160, "e_he_netherite",             "Φ");
        E_HE_FRIULTAIL             = create(110, 160, "e_he_friultail",             "AuAlBiHfRu<HE>");
    }

    private static Element create(long protons, long neutrons, String name, String symbol) {
        Element element = new Element(protons, neutrons, -1, null, name, symbol, false);
        GTRegistries.ELEMENTS.register(name, element);
        return element;
    }
}