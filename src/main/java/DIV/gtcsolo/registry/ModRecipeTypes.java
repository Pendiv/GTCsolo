package DIV.gtcsolo.registry;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.recipe.GTRecipeSerializer;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import static com.lowdragmc.lowdraglib.gui.texture.ProgressTexture.FillDirection.*;

public class ModRecipeTypes {

    // fantasy_element_constructor の略
    public static GTRecipeType FEC;

    /** Mega mixer: 6 item + 12 fluid inputs; 3 item + 9 fluid outputs. Initially empty. */
    public static GTRecipeType MULTI_MIXING;
    public static GTRecipeType ORE_WASHING_PROCESS;
    public static GTRecipeType ORE_CHEMICAL_WASHING_PROCESS;
    public static GTRecipeType ORE_SEPARATION_PROCESS;
    public static GTRecipeType FAMING_ROOM;
    public static GTRecipeType CONSTRUCTION_ROOM;

    // WEN (Wireless Energy Network) — ダミーレシピタイプ
    // マルチブロック登録に必要だが、実際のレシピは持たない
    public static GTRecipeType WEN_STORAGE;

    // Space Forge — 仮値 IO (16, 3, 3, 3)
    public static GTRecipeType SPACEFORGE;

    // Chemical Combustion Generator — 液体入力3、発電
    public static GTRecipeType CHEMICAL_COMBUSTION_GENERATOR;

    // Fantasia Forge — 幻想元素鍛造マルチブロック、items 3/3, fluids 1/1
    public static GTRecipeType FANTASIA_FORGE;

    // Mekanism Infuser — 吹込み合金生成マルチ (item 3/3, fluid 2/2, EU in)
    public static GTRecipeType MEKANISM_INFUSER;

    // Conversion — Mekanism infusion_conversion 模倣 (item 3/3, INFUSION 1/1, output 1.5x)
    public static GTRecipeType CONVERSION;

    // Industrial Infusion Conversion — 工業的吹き込みタイプ変換 (Conversion機械が扱う追加レシピタイプ)
    // duration = output_amount × 8 ticks (= 4s/10mb), EUt = LV固定, output = Mekanism元の1.5倍
    public static GTRecipeType INDUSTRIAL_INFUSION_CONVERSION;

    // WEN Integration — items 9/32, fluids 3/0, EU IN
    public static GTRecipeType WEN_INTEGRATION;

    // WEN Nexus Assembler — items 9/32, fluids 3/2, EU IN
    public static GTRecipeType WEN_NEXUS_ASSEMBLER;

    // Singularity Maker — items 16/1, no fluid, EU IN。Singularity を 16 個まとめて 1 個へ統合
    public static GTRecipeType SINGULARITY_MAKER;

    // Singularity Compresser — items 1/1, no fluid, EU IN。素材を圧縮して singularity を 1 個生成
    public static GTRecipeType SINGULARITY_COMPRESSER;

    // Locus Simulation Builder — items 3/1, no fluid, EU IN。空 star_locus に Trace NBT を書き込む
    // (decaying_star_locus 触媒で次 tier 解放、消費しない)
    public static GTRecipeType LOCUS_SIMULATION_BUILDER;

    // StarForge — items 1/15, fluids 0/12, EU IN。実態は GT 標準レシピで賄えない (フェイズ管理 + 累計消費)。
    // ここに登録するレシピは JEI 表示専用のダミー (8 軌跡分)、実稼働は別ロジック。
    public static GTRecipeType STARFORGE;

    // Armor Build — items 1/4, no fluid, EU IN。素材から防具一式をひねり無く生成 (例: 鉄26 → 鉄装備4点)。
    public static GTRecipeType ARMOR_BUILD;

    /** Researched UHV+ circuits; used alongside the ordinary GT circuit assembler type. */
    public static GTRecipeType CIRCUIT_ASSEMBLY_LINE;
    public static GTRecipeType CIRCUIT_DATA_ANALYSIS;

    /** ISMBF extreme-temperature processing: 9 item / 3 fluid inputs, 1 item / 1 fluid outputs. */
    public static GTRecipeType EXTREME_TEMPERATURE_BLAST_FURNACE;

    // Fantasy Armor Build — items 15/4, no fluid, EU IN。Fantasy 装備の構築。
    public static GTRecipeType FANTASY_ARMOR_BUILD;

    /**
     * GTCEu 標準 {@code GTRecipeTypes.register()} と同じく RECIPE_TYPE / RECIPE_SERIALIZER /
     * RECIPE_TYPES の 3 レジストリへ登録する。 id は type の registryName から取得。
     */
    private static GTRecipeType reg(GTRecipeType type) {
        ResourceLocation id = type.registryName;
        GTRegistries.register(BuiltInRegistries.RECIPE_TYPE, id, type);
        GTRegistries.register(BuiltInRegistries.RECIPE_SERIALIZER, id, new GTRecipeSerializer());
        GTRegistries.RECIPE_TYPES.register(id, type);
        return type;
    }

    private static GTRecipeType type(String name) {
        return new GTRecipeType(new ResourceLocation("gtcsolo", name), "multiblock");
    }

    private static GTRecipeType oreFactoryType(String name) {
        return reg(type(name).setMaxIOSize(1, 24, 4, 0).setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT)
                .addDataInfo(data -> com.lowdragmc.lowdraglib.utils.LocalizationUtils.format("gtcsolo.ore_factory.recipe_info")));
    }

    public static void init() {
        CombustionMiningRecipes.TYPE = reg(CombustionMiningRecipes.createType(new ResourceLocation("gtcsolo", "combustion_mining"))
                .setMaxIOSize(1, 12, 1, 0).setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT));
        BiochemicalMachines.initRecipeTypes();
        CultivationFormationRooms.initRecipeType();
        FAMING_ROOM = reg(type("faming_room").setMaxIOSize(2, 1, 1, 0).setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT));
        CONSTRUCTION_ROOM = reg(type("construction_room").setMaxIOSize(9, 4, 2, 2).setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT)
                .addDataInfo(data -> com.lowdragmc.lowdraglib.utils.LocalizationUtils.format(
                        "gtceu.recipe.temperature", data.getInt(DIV.gtcsolo.machine.RoomRecipeRules.REQUIRED_TEMPERATURE))));
        ORE_WASHING_PROCESS = oreFactoryType("ore_washing_process");
        ORE_CHEMICAL_WASHING_PROCESS = oreFactoryType("ore_chemical_washing_process");
        ORE_SEPARATION_PROCESS = oreFactoryType("ore_separation_process");
        MULTI_MIXING = reg(type("multi_mixing")
                .setMaxIOSize(6, 3, 12, 9).setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_MIXER, LEFT_TO_RIGHT));
        EXTREME_TEMPERATURE_BLAST_FURNACE = reg(type("extreme_temperature_blast_furnace")
                .setMaxIOSize(9, 1, 3, 1).setEUIO(IO.IN)
                .addDataInfo(data -> com.lowdragmc.lowdraglib.utils.LocalizationUtils.format(
                        "gtcsolo.ismbf.recipe.minimum_coil_temperature",
                        data.getInt(DIV.gtcsolo.machine.IsmbfRules.START_TEMPERATURE)))
                .addDataInfo(data -> com.lowdragmc.lowdraglib.utils.LocalizationUtils.format(
                        "gtcsolo.ismbf.recipe.operating_temperature", data.getInt("ebf_temp")))
                .setMaxTooltips(0)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT));
        CIRCUIT_ASSEMBLY_LINE = reg(type("circuit_assembly_line")
                .setMaxIOSize(16, 1, 4, 1).setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT)
                .setHasResearchSlot(true).setMaxTooltips(4)
                .onRecipeBuild(com.gregtechceu.gtceu.utils.ResearchManager::createDefaultResearchRecipe));
        CIRCUIT_DATA_ANALYSIS = reg(type("circuit_data_analysis")
                .setMaxIOSize(1, 0, 0, 0).setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT));
        // GTCEu 標準 BLAST_RECIPES に GAS capability 最大1入力 を後付け (EEBF でfissile_fuel等を受ける)
        com.gregtechceu.gtceu.common.data.GTRecipeTypes.BLAST_RECIPES
                .setMaxSize(IO.IN,
                        DIV.gtcsolo.integration.mekanism.capability.ChemicalCapabilities.GAS, 1);

        // Large Chemical Reactor: gas+gas→gas 系を受けるため GAS cap 入出力を追加
        com.gregtechceu.gtceu.common.data.GTRecipeTypes.LARGE_CHEMICAL_RECIPES
                .setMaxSize(IO.IN,
                        DIV.gtcsolo.integration.mekanism.capability.ChemicalCapabilities.GAS, 3)
                .setMaxSize(IO.OUT,
                        DIV.gtcsolo.integration.mekanism.capability.ChemicalCapabilities.GAS, 2);

        // アイテム入力6, アイテム出力1, 液体入力1, 液体出力0
        FEC = reg(type("fec")
                .setMaxIOSize(6, 1, 1, 1)
                .setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT));

        // WEN Storage ダミー
        WEN_STORAGE = reg(type("wen_storage")
                .setMaxIOSize(0, 0, 0, 0)
                .setEUIO(IO.IN));

        SPACEFORGE = reg(type("spaceforge")
                .setMaxIOSize(16, 3, 3, 3)
                .setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT));

        CHEMICAL_COMBUSTION_GENERATOR = reg(type("chemical_combustion_generator")
                .setMaxIOSize(0, 0, 3, 3)
                .setEUIO(IO.OUT)
                .setProgressBar(GuiTextures.PROGRESS_BAR_GAS_COLLECTOR, LEFT_TO_RIGHT));

        FANTASIA_FORGE = reg(type("fantasia_forge")
                .setMaxIOSize(3, 3, 1, 1)
                .setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT));

        MEKANISM_INFUSER = reg(type("mekanism_infuser")
                .setMaxIOSize(3, 3, 2, 2)
                .setEUIO(IO.IN)
                // chemical capability (INFUSION 最大2入力) — 新方式migration
                .setMaxSize(IO.IN,
                        DIV.gtcsolo.integration.mekanism.capability.ChemicalCapabilities.INFUSION, 2)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT));

        // Conversion — infusion_conversion 模倣 (1.5倍 output)
        CONVERSION = reg(type("conversion")
                .setMaxIOSize(3, 3, 0, 0)
                .setEUIO(IO.IN)
                .setMaxSize(IO.IN,
                        DIV.gtcsolo.integration.mekanism.capability.ChemicalCapabilities.INFUSION, 1)
                .setMaxSize(IO.OUT,
                        DIV.gtcsolo.integration.mekanism.capability.ChemicalCapabilities.INFUSION, 1)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT));

        INDUSTRIAL_INFUSION_CONVERSION = reg(type("industrial_infusion_conversion")
                .setMaxIOSize(3, 3, 0, 0)
                .setEUIO(IO.IN)
                .setMaxSize(IO.IN,
                        DIV.gtcsolo.integration.mekanism.capability.ChemicalCapabilities.INFUSION, 1)
                .setMaxSize(IO.OUT,
                        DIV.gtcsolo.integration.mekanism.capability.ChemicalCapabilities.INFUSION, 1)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT));

        // Conversion 系: レシピ EUt から構造tier 要求を自動で data に焼き、JEI に "Required Tier" 行を追加
        CONVERSION.onRecipeBuild(DIV.gtcsolo.api.tier.TierRecipeLogic.stampRequiredTierFromEUt());
        INDUSTRIAL_INFUSION_CONVERSION.onRecipeBuild(DIV.gtcsolo.api.tier.TierRecipeLogic.stampRequiredTierFromEUt());
        DIV.gtcsolo.api.tier.TierRecipeLogic.addRequiredTierDisplay(CONVERSION);
        DIV.gtcsolo.api.tier.TierRecipeLogic.addRequiredTierDisplay(INDUSTRIAL_INFUSION_CONVERSION);

        SINGULARITY_MAKER = reg(type("singularity_maker")
                .setMaxIOSize(16, 1, 0, 0)
                .setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT));
        SINGULARITY_MAKER.onRecipeBuild(DIV.gtcsolo.api.tier.TierRecipeLogic.stampRequiredTierFromEUt());
        DIV.gtcsolo.api.tier.TierRecipeLogic.addRequiredTierDisplay(SINGULARITY_MAKER);

        SINGULARITY_COMPRESSER = reg(type("singularity_compresser")
                .setMaxIOSize(1, 1, 0, 0)
                .setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT));
        SINGULARITY_COMPRESSER.onRecipeBuild(DIV.gtcsolo.api.tier.TierRecipeLogic.stampRequiredTierFromEUt());
        DIV.gtcsolo.api.tier.TierRecipeLogic.addRequiredTierDisplay(SINGULARITY_COMPRESSER);

        LOCUS_SIMULATION_BUILDER = reg(type("locus_simulation_builder")
                .setMaxIOSize(3, 1, 0, 0)
                .setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT));
        LOCUS_SIMULATION_BUILDER.onRecipeBuild(DIV.gtcsolo.api.tier.TierRecipeLogic.stampRequiredTierFromEUt());
        DIV.gtcsolo.api.tier.TierRecipeLogic.addRequiredTierDisplay(LOCUS_SIMULATION_BUILDER);

        // StarForge — JEI 表示専用 (実態ロジックは別系統、構築/成熟/崩壊フェイズ管理)
        //   注意: maxIO はJEIダミー表示の都合。 実態 machine は自前 tick で別ハッチから取るので表示限界のみに作用。
        STARFORGE = reg(type("starforge")
                .setMaxIOSize(6, 16, 0, 8)
                .setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT));
        // 情報欠落表記 (ver.0.5 §9.5 Phase 2): JEI ページに「詳細は Trace 情報ページ参照」 を 1 行差し込む。
        STARFORGE.addDataInfo(tag -> net.minecraft.network.chat.Component
                .translatable("gtcsolo.jei.starforge.dummy_notice").getString());

        WEN_INTEGRATION = reg(type("wen_integration")
                .setMaxIOSize(9, 32, 3, 0)
                .setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT));

        WEN_NEXUS_ASSEMBLER = reg(type("wen_nexus_assembler")
                .setMaxIOSize(9, 2, 3, 0)
                .setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT));

        // Fantasy Builder の 2 系統レシピ
        ARMOR_BUILD = reg(type("armor_build")
                .setMaxIOSize(1, 4, 0, 0)
                .setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT));

        FANTASY_ARMOR_BUILD = reg(type("fantasy_armor_build")
                .setMaxIOSize(15, 4, 0, 0)
                .setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT));
    }
}
