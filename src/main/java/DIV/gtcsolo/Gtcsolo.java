package DIV.gtcsolo;

import DIV.gtcsolo.block.ExtendEnergyCubeScreen;
import DIV.gtcsolo.block.wen.WENDataMonitorScreen;
import DIV.gtcsolo.block.wen.WENIdSelectScreen;
import DIV.gtcsolo.command.GtcSoloCommand;
import DIV.gtcsolo.network.ModNetwork;
import DIV.gtcsolo.common.TooltipDisplayEvents;
import DIV.gtcsolo.data.ModRecipeProvider;
import DIV.gtcsolo.dump.RecipeDumpService;
import DIV.gtcsolo.integration.mekanism.ModGases;
import DIV.gtcsolo.registry.ModBlockEntities;
import DIV.gtcsolo.registry.ModBlocks;
import DIV.gtcsolo.registry.ModCreativeTabs;
import DIV.gtcsolo.registry.ModItems;
import DIV.gtcsolo.registry.ModMachines;
import DIV.gtcsolo.registry.ModMaterials;
import DIV.gtcsolo.registry.ModMenuTypes;
import DIV.gtcsolo.registry.ModRecipeTypes;
import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.material.event.MaterialEvent;
import com.gregtechceu.gtceu.api.data.chemical.material.event.MaterialRegistryEvent;
import com.gregtechceu.gtceu.api.data.chemical.material.event.PostMaterialEvent;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Gtcsolo.MODID)
public class Gtcsolo {
    public static final String MODID = "gtcsolo";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Gtcsolo() {
        // Forge 1.20.1 は mods.toml の [[mixins]] / jar manifest MixinConfigs を dev で読まないので
        // ここで明示的に Mixin config を登録する。target class (PatternPreviewWidget 等) のロード前に
        // 走るため、JEI 経由でロードされるクラスへの mixin は問題なく適用される。
        org.spongepowered.asm.mixin.Mixins.addConfiguration("gtcsolo.mixins.json");

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        RecipeDumpService recipeDumpService = new RecipeDumpService();
        GtcSoloCommand commandHandler = new GtcSoloCommand(recipeDumpService);
        TooltipDisplayEvents tooltipDisplayEvents = new TooltipDisplayEvents();
        // 素材登録
        modEventBus.addListener(this::addMaterialRegistries);
        modEventBus.addListener(this::addMaterials);
        modEventBus.addListener(this::modifyMaterials);
        modEventBus.addListener(this::commonSetup);
        // レシピタイプはGTのレジストリが開いているタイミングで登録する
        modEventBus.addGenericListener(GTRecipeType.class, this::registerRecipeTypes);
        DIV.gtcsolo.recipe.condition.GtcsoloRecipeConditions.register(modEventBus);
        ModMachines.REGISTRATE.registerRegistrate();
        // マシン登録もGTのレジストリが開いているタイミングで行う
        modEventBus.addGenericListener(MachineDefinition.class, this::registerMachines);
        // データ生成（./gradlew runData でレシピJSONを生成する）
        modEventBus.addListener(this::gatherData);
        ModNetwork.register();
        // Apotheosis Gem skin (texture iconset + color マッピング)
        DIV.gtcsolo.apoth.gem.GemSkinRegistry.bootstrap();
        // Material / TagPrefix 非依存の GT MaterialIcon アイテム・ブロック登録口。
        // GTRegistration.REGISTRATE を使うため成果物 ID は gtceu:* になる。
        DIV.gtcsolo.registry.freeform.FreeformMaterialIcons.init();
        // 既知GT Item画像へ色変換を注入した任意ID Item。
        // family宣言から shade × method を一括展開し、texture/modelはclient resource packへ動的注入する。
        DIV.gtcsolo.registry.iteminjection.KnownItemInjections.bootstrap();
        // 任意の導入済みMODをsourceにし、gtcsolo:* Itemとして登録する汎用画像注入。
        // GT専用系とはregistry・creative tab・番号帯を分離する。
        DIV.gtcsolo.registry.genericiteminjection.GenericItemInjections.bootstrap();
        // Re-Avaritia's public halo model loader is attached client-side to selected existing items.
        DIV.gtcsolo.registry.halo.HaloInjections.bootstrap();
        // カスタム Ingredient 登録 (LocusIngredient — StrictNBT の null vs {} 不一致を回避)
        net.minecraftforge.common.crafting.CraftingHelper.register(
                DIV.gtcsolo.api.ingredient.LocusIngredient.ID,
                DIV.gtcsolo.api.ingredient.LocusIngredient.SERIALIZER);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        DIV.gtcsolo.registry.UtilityEffects.EFFECTS.register(modEventBus);
        DIV.gtcsolo.registry.IronCastingMolds.register(modEventBus);
        net.minecraftforge.common.crafting.CraftingHelper.register(
                DIV.gtcsolo.recipe.mold.MoldIngredient.ID, DIV.gtcsolo.recipe.mold.MoldIngredient.SERIALIZER);
        DIV.gtcsolo.registry.VoltageTorches.register(modEventBus);
        DIV.gtcsolo.registry.VoltageClocks.register(modEventBus);
        DIV.gtcsolo.registry.VoltageGlass.register(modEventBus);
        ModGases.GASES.register(modEventBus);
        DIV.gtcsolo.registry.ModRecipeSerializers.SERIALIZERS.register(modEventBus);
        DIV.gtcsolo.integration.mekanism.generator.LiquidFuelRecipe.register(modEventBus);
        DIV.gtcsolo.integration.mekanism.generator.CombustionSupportRecipe.register(modEventBus);
        // マニュアルUI (LDLib ModularUI) のファクトリ — アイテム右クリック / /gtcsolo manual 共通経路
        com.lowdragmc.lowdraglib.gui.factory.UIFactory.register(DIV.gtcsolo.manual.ManualUIFactory.INSTANCE);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModCreativeTabs.applyTabOverrides();
        MinecraftForge.EVENT_BUS.register(new DIV.gtcsolo.common.SleepTimeHandler());   // 就寝を夕方→夜(13000t)に繰り下げ
        MinecraftForge.EVENT_BUS.addListener(commandHandler::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(tooltipDisplayEvents::onItemTooltip);

        MinecraftForge.EVENT_BUS.register(DIV.gtcsolo.integration.ae2.WENAe2Integration.class);
        // Masked texture provider — entry register のみ (両 dist 安全)。
        // generateAll は MaskedTextureClient が AddPackFindersEvent (LOWEST) で呼ぶ
        // = GTCEu の clearClient() の **後** に走らせて DATA を保持させる。
        DIV.gtcsolo.render.dynamic.MaskedTextureProvider.bootstrap();
        // クライアント専用セットアップ
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> modEventBus.addListener(Gtcsolo::clientSetup));
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.EXTEND_ENERGY_CUBE.get(), ExtendEnergyCubeScreen::new);
            MenuScreens.register(ModMenuTypes.WEN_DATA_MONITOR.get(), WENDataMonitorScreen::new);
            MenuScreens.register(ModMenuTypes.WEN_ID_SELECT.get(), WENIdSelectScreen::new);
            MenuScreens.register(ModMenuTypes.DATACHEST.get(), DIV.gtcsolo.block.datachest.DataChestScreen::new);
            for (var torch : DIV.gtcsolo.registry.VoltageTorches.ENTRIES) {
                net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                        torch.standing().get(), net.minecraft.client.renderer.RenderType.cutout());
                net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                        torch.wall().get(), net.minecraft.client.renderer.RenderType.cutout());
            }
            // LDLib's CTM wrapper falls back to the block's render layer rather
            // than forwarding the parent model's JSON render_type.
            for (var glass : DIV.gtcsolo.registry.VoltageGlass.ENTRIES) {
                net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                        glass.block().get(), net.minecraft.client.renderer.RenderType.cutoutMipped());
            }
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                    ModBlocks.MITHRILITE_FIREBOX_CASING.get(),
                    net.minecraft.client.renderer.RenderType.cutoutMipped());
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                    ModBlocks.STAINLESS_STEEL_FIREBOX_CASING.get(),
                    net.minecraft.client.renderer.RenderType.cutoutMipped());
        });
    }

    // CommonSetup
    private void commonSetup(net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // AE2 アップグレードカード登録 (対象AE2マシンへの互換登録)
            DIV.gtcsolo.integration.ae2.WENAe2Integration.registerUpgrades();
            DIV.gtcsolo.integration.mekanism.generator.GasGeneratorUpgrades.init();
            // StarForge 軌跡データ初期化 (全 material/fluid 登録完了後タイミング)
            DIV.gtcsolo.machine.starforge.StarForgeTraceData.init();
            // バニラ (minecraft 名前空間) の全 RangedAttribute の上限を Integer 最大へ拡張。
            // HP1024・攻撃力2048・防御30 等のバニラ上限を撤廃する。 mod 独自 attribute のレンジは保護のため対象外。
            final double cap = 2_147_483_647.0D;
            for (var entry : net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getEntries()) {
                if (!entry.getKey().location().getNamespace().equals("minecraft")) continue;
                if (entry.getValue() instanceof net.minecraft.world.entity.ai.attributes.RangedAttribute) {
                    ((DIV.gtcsolo.mixin.RangedAttributeAccessor) (Object) entry.getValue()).gtcsolo$setMaxValue(cap);
                }
            }
        });
    }

    // 素材レジストリ作成（自MODの名前空間）
    private void addMaterialRegistries(MaterialRegistryEvent event) {
        GTCEuAPI.materialManager.createRegistry(MODID);
    }

    // 素材登録
    private void addMaterials(MaterialEvent event) {
        LOGGER.debug("[gtcsolo] addMaterials: registering materials...");
        ModMaterials.init();
        // Full-custom tools must first touch GTToolType during GTCEu's material-registration window.
        DIV.gtcsolo.registry.tool.fullcustom.FullCustomGtTools.bootstrapAndRegister();
    }

    // 全MODのMaterial登録後・凍結前。既知Materialへツール生成宣言と性能補正を適用する。
    private void modifyMaterials(PostMaterialEvent event) {
        DIV.gtcsolo.registry.material.AmeijiaOreFlags.enableDefaults();
        DIV.gtcsolo.registry.material.NobleGasPlasmas.register();
        DIV.gtcsolo.block.DraconicCoilTypes.register();
        DIV.gtcsolo.registry.material.InfinityForms.enableParts();
        // Netheriteの主要派生prefixを後付けする。
        // IGTAddon.registerMaterials()から触るとKubeJS/L2Hostilityの構築と静的初期化が競合する。
        com.gregtechceu.gtceu.common.data.GTMaterials.Netherite.addFlags(
                com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags.GENERATE_PLATE,
                com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags.GENERATE_DENSE,
                com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags.GENERATE_ROD,
                com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags.GENERATE_LONG_ROD,
                com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags.GENERATE_GEAR,
                com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags.GENERATE_SMALL_GEAR,
                com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags.GENERATE_FOIL,
                com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags.GENERATE_RING,
                com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags.GENERATE_SPRING,
                com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags.GENERATE_ROTOR
        );
        // GTToolType は GTSoundEntries を参照するため、MODコンストラクタでは触らない。
        // GTCEuがsound登録を終えた後に発火する、このイベント内で初めて宣言を構築する。
        DIV.gtcsolo.registry.tool.GtcToolMaterialFlags.bootstrap();
        DIV.gtcsolo.registry.tool.GtcToolMaterialFlags.applyAll();
    }

    private void gatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(
                event.includeServer(),
                new ModRecipeProvider(event.getGenerator().getPackOutput())
        );
    }

    private void registerMachines(GTCEuAPI.RegisterEvent<ResourceLocation, MachineDefinition> event) {
        ModMachines.init();
    }

    private void registerRecipeTypes(GTCEuAPI.RegisterEvent<ResourceLocation, GTRecipeType> event) {
        ModRecipeTypes.init();
    }

}
