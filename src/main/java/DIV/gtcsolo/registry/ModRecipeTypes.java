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

    public static void init() {
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

        FEC = new GTRecipeType(
                new ResourceLocation("gtcsolo", "fec"), "multiblock")
                // アイテム入力6, アイテム出力1, 液体入力1, 液体出力0
                .setMaxIOSize(6, 1, 1, 1)
                .setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT);

        // WEN Storage ダミー
        WEN_STORAGE = new GTRecipeType(
                new ResourceLocation("gtcsolo", "wen_storage"), "multiblock")
                .setMaxIOSize(0, 0, 0, 0)
                .setEUIO(IO.IN);
        ResourceLocation wenId = new ResourceLocation("gtcsolo", "wen_storage");
        GTRegistries.register(BuiltInRegistries.RECIPE_TYPE, wenId, WEN_STORAGE);
        GTRegistries.register(BuiltInRegistries.RECIPE_SERIALIZER, wenId, new GTRecipeSerializer());
        GTRegistries.RECIPE_TYPES.register(wenId, WEN_STORAGE);

        ResourceLocation fecId = new ResourceLocation("gtcsolo", "fec");
        // GTCEu自身のGTRecipeTypes.register()と同様に、3つ全てに登録する
        GTRegistries.register(BuiltInRegistries.RECIPE_TYPE, fecId, FEC);
        GTRegistries.register(BuiltInRegistries.RECIPE_SERIALIZER, fecId, new GTRecipeSerializer());
        GTRegistries.RECIPE_TYPES.register(fecId, FEC);

        // Space Forge
        SPACEFORGE = new GTRecipeType(
                new ResourceLocation("gtcsolo", "spaceforge"), "multiblock")
                .setMaxIOSize(16, 3, 3, 3)
                .setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT);
        ResourceLocation spaceforgeId = new ResourceLocation("gtcsolo", "spaceforge");
        GTRegistries.register(BuiltInRegistries.RECIPE_TYPE, spaceforgeId, SPACEFORGE);
        GTRegistries.register(BuiltInRegistries.RECIPE_SERIALIZER, spaceforgeId, new GTRecipeSerializer());
        GTRegistries.RECIPE_TYPES.register(spaceforgeId, SPACEFORGE);

        // Chemical Combustion Generator
        CHEMICAL_COMBUSTION_GENERATOR = new GTRecipeType(
                new ResourceLocation("gtcsolo", "chemical_combustion_generator"), "multiblock")
                .setMaxIOSize(0, 0, 3, 3)
                .setEUIO(IO.OUT)
                .setProgressBar(GuiTextures.PROGRESS_BAR_GAS_COLLECTOR, LEFT_TO_RIGHT);
        ResourceLocation ccgId = new ResourceLocation("gtcsolo", "chemical_combustion_generator");
        GTRegistries.register(BuiltInRegistries.RECIPE_TYPE, ccgId, CHEMICAL_COMBUSTION_GENERATOR);
        GTRegistries.register(BuiltInRegistries.RECIPE_SERIALIZER, ccgId, new GTRecipeSerializer());
        GTRegistries.RECIPE_TYPES.register(ccgId, CHEMICAL_COMBUSTION_GENERATOR);

        // Fantasia Forge
        FANTASIA_FORGE = new GTRecipeType(
                new ResourceLocation("gtcsolo", "fantasia_forge"), "multiblock")
                .setMaxIOSize(3, 3, 1, 1)
                .setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT);
        ResourceLocation ffId = new ResourceLocation("gtcsolo", "fantasia_forge");
        GTRegistries.register(BuiltInRegistries.RECIPE_TYPE, ffId, FANTASIA_FORGE);
        GTRegistries.register(BuiltInRegistries.RECIPE_SERIALIZER, ffId, new GTRecipeSerializer());
        GTRegistries.RECIPE_TYPES.register(ffId, FANTASIA_FORGE);

        // Mekanism Infuser
        MEKANISM_INFUSER = new GTRecipeType(
                new ResourceLocation("gtcsolo", "mekanism_infuser"), "multiblock")
                .setMaxIOSize(3, 3, 2, 2)
                .setEUIO(IO.IN)
                // chemical capability (INFUSION 最大2入力) — 新方式migration
                .setMaxSize(IO.IN,
                        DIV.gtcsolo.integration.mekanism.capability.ChemicalCapabilities.INFUSION, 2)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT);
        ResourceLocation miId = new ResourceLocation("gtcsolo", "mekanism_infuser");
        GTRegistries.register(BuiltInRegistries.RECIPE_TYPE, miId, MEKANISM_INFUSER);
        GTRegistries.register(BuiltInRegistries.RECIPE_SERIALIZER, miId, new GTRecipeSerializer());
        GTRegistries.RECIPE_TYPES.register(miId, MEKANISM_INFUSER);

        // Conversion — infusion_conversion 模倣 (1.5倍 output)
        CONVERSION = new GTRecipeType(
                new ResourceLocation("gtcsolo", "conversion"), "multiblock")
                .setMaxIOSize(3, 3, 0, 0)
                .setEUIO(IO.IN)
                .setMaxSize(IO.IN,
                        DIV.gtcsolo.integration.mekanism.capability.ChemicalCapabilities.INFUSION, 1)
                .setMaxSize(IO.OUT,
                        DIV.gtcsolo.integration.mekanism.capability.ChemicalCapabilities.INFUSION, 1)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT);
        ResourceLocation convId = new ResourceLocation("gtcsolo", "conversion");
        GTRegistries.register(BuiltInRegistries.RECIPE_TYPE, convId, CONVERSION);
        GTRegistries.register(BuiltInRegistries.RECIPE_SERIALIZER, convId, new GTRecipeSerializer());
        GTRegistries.RECIPE_TYPES.register(convId, CONVERSION);

        // Industrial Infusion Conversion
        INDUSTRIAL_INFUSION_CONVERSION = new GTRecipeType(
                new ResourceLocation("gtcsolo", "industrial_infusion_conversion"), "multiblock")
                .setMaxIOSize(3, 3, 0, 0)
                .setEUIO(IO.IN)
                .setMaxSize(IO.IN,
                        DIV.gtcsolo.integration.mekanism.capability.ChemicalCapabilities.INFUSION, 1)
                .setMaxSize(IO.OUT,
                        DIV.gtcsolo.integration.mekanism.capability.ChemicalCapabilities.INFUSION, 1)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT);
        ResourceLocation iicId = new ResourceLocation("gtcsolo", "industrial_infusion_conversion");
        GTRegistries.register(BuiltInRegistries.RECIPE_TYPE, iicId, INDUSTRIAL_INFUSION_CONVERSION);
        GTRegistries.register(BuiltInRegistries.RECIPE_SERIALIZER, iicId, new GTRecipeSerializer());
        GTRegistries.RECIPE_TYPES.register(iicId, INDUSTRIAL_INFUSION_CONVERSION);

        // Conversion 系: レシピ EUt から構造tier 要求を自動で data に焼き、JEI に "Required Tier" 行を追加
        CONVERSION.onRecipeBuild(DIV.gtcsolo.api.tier.TierRecipeLogic.stampRequiredTierFromEUt());
        INDUSTRIAL_INFUSION_CONVERSION.onRecipeBuild(DIV.gtcsolo.api.tier.TierRecipeLogic.stampRequiredTierFromEUt());
        DIV.gtcsolo.api.tier.TierRecipeLogic.addRequiredTierDisplay(CONVERSION);
        DIV.gtcsolo.api.tier.TierRecipeLogic.addRequiredTierDisplay(INDUSTRIAL_INFUSION_CONVERSION);

        // Singularity Maker
        SINGULARITY_MAKER = new GTRecipeType(
                new ResourceLocation("gtcsolo", "singularity_maker"), "multiblock")
                .setMaxIOSize(16, 1, 0, 0)
                .setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT);
        ResourceLocation smId = new ResourceLocation("gtcsolo", "singularity_maker");
        GTRegistries.register(BuiltInRegistries.RECIPE_TYPE, smId, SINGULARITY_MAKER);
        GTRegistries.register(BuiltInRegistries.RECIPE_SERIALIZER, smId, new GTRecipeSerializer());
        GTRegistries.RECIPE_TYPES.register(smId, SINGULARITY_MAKER);
        SINGULARITY_MAKER.onRecipeBuild(DIV.gtcsolo.api.tier.TierRecipeLogic.stampRequiredTierFromEUt());
        DIV.gtcsolo.api.tier.TierRecipeLogic.addRequiredTierDisplay(SINGULARITY_MAKER);

        // Singularity Compresser
        SINGULARITY_COMPRESSER = new GTRecipeType(
                new ResourceLocation("gtcsolo", "singularity_compresser"), "multiblock")
                .setMaxIOSize(1, 1, 0, 0)
                .setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT);
        ResourceLocation scId = new ResourceLocation("gtcsolo", "singularity_compresser");
        GTRegistries.register(BuiltInRegistries.RECIPE_TYPE, scId, SINGULARITY_COMPRESSER);
        GTRegistries.register(BuiltInRegistries.RECIPE_SERIALIZER, scId, new GTRecipeSerializer());
        GTRegistries.RECIPE_TYPES.register(scId, SINGULARITY_COMPRESSER);
        SINGULARITY_COMPRESSER.onRecipeBuild(DIV.gtcsolo.api.tier.TierRecipeLogic.stampRequiredTierFromEUt());
        DIV.gtcsolo.api.tier.TierRecipeLogic.addRequiredTierDisplay(SINGULARITY_COMPRESSER);

        // Locus Simulation Builder
        LOCUS_SIMULATION_BUILDER = new GTRecipeType(
                new ResourceLocation("gtcsolo", "locus_simulation_builder"), "multiblock")
                .setMaxIOSize(3, 1, 0, 0)
                .setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT);
        ResourceLocation lsbId = new ResourceLocation("gtcsolo", "locus_simulation_builder");
        GTRegistries.register(BuiltInRegistries.RECIPE_TYPE, lsbId, LOCUS_SIMULATION_BUILDER);
        GTRegistries.register(BuiltInRegistries.RECIPE_SERIALIZER, lsbId, new GTRecipeSerializer());
        GTRegistries.RECIPE_TYPES.register(lsbId, LOCUS_SIMULATION_BUILDER);
        LOCUS_SIMULATION_BUILDER.onRecipeBuild(DIV.gtcsolo.api.tier.TierRecipeLogic.stampRequiredTierFromEUt());
        DIV.gtcsolo.api.tier.TierRecipeLogic.addRequiredTierDisplay(LOCUS_SIMULATION_BUILDER);

        // StarForge — JEI 表示専用 (実態ロジックは別系統、構築/成熟/崩壊フェイズ管理)
        //   注意: maxIO 入力 7 は JEI ダミー表示の都合 (= 構築フェイズの進捗テーブル全 item を
        //   並べるため)。 実態 machine 側は GT recipe processor を使わず自前 tick で別ハッチ
        //   から item を取るので、 maxIO 値は表示限界のみに作用する。
        STARFORGE = new GTRecipeType(
                new ResourceLocation("gtcsolo", "starforge"), "multiblock")
                .setMaxIOSize(6, 16, 0, 8)
                .setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT);
        // 情報欠落表記 (ver.0.5 §9.5 Phase 2): GT 標準 JEI ページの条件枠の直下に
        // 「JEI は概要のみ、 詳細は Trace 情報ページ参照」 を 1 行差し込む。
        // dataInfo は client 側で LabelWidget 化されるので getString() で OK
        STARFORGE.addDataInfo(tag -> net.minecraft.network.chat.Component
                .translatable("gtcsolo.jei.starforge.dummy_notice").getString());
        ResourceLocation sfId = new ResourceLocation("gtcsolo", "starforge");
        GTRegistries.register(BuiltInRegistries.RECIPE_TYPE, sfId, STARFORGE);
        GTRegistries.register(BuiltInRegistries.RECIPE_SERIALIZER, sfId, new GTRecipeSerializer());
        GTRegistries.RECIPE_TYPES.register(sfId, STARFORGE);

        // WEN Integration
        WEN_INTEGRATION = new GTRecipeType(
                new ResourceLocation("gtcsolo", "wen_integration"), "multiblock")
                .setMaxIOSize(9, 32, 3, 0)
                .setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT);
        ResourceLocation wiId = new ResourceLocation("gtcsolo", "wen_integration");
        GTRegistries.register(BuiltInRegistries.RECIPE_TYPE, wiId, WEN_INTEGRATION);
        GTRegistries.register(BuiltInRegistries.RECIPE_SERIALIZER, wiId, new GTRecipeSerializer());
        GTRegistries.RECIPE_TYPES.register(wiId, WEN_INTEGRATION);

        // WEN Nexus Assembler
        WEN_NEXUS_ASSEMBLER = new GTRecipeType(
                new ResourceLocation("gtcsolo", "wen_nexus_assembler"), "multiblock")
                .setMaxIOSize(9, 2, 3, 0)
                .setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT);
        ResourceLocation wnaId = new ResourceLocation("gtcsolo", "wen_nexus_assembler");
        GTRegistries.register(BuiltInRegistries.RECIPE_TYPE, wnaId, WEN_NEXUS_ASSEMBLER);
        GTRegistries.register(BuiltInRegistries.RECIPE_SERIALIZER, wnaId, new GTRecipeSerializer());
        GTRegistries.RECIPE_TYPES.register(wnaId, WEN_NEXUS_ASSEMBLER);
    }
}