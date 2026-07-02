package DIV.gtcsolo;

import DIV.gtcsolo.block.ExtendEnergyCubeScreen;
import DIV.gtcsolo.block.wen.WENDataMonitorScreen;
import DIV.gtcsolo.block.wen.WENIdSelectScreen;
import DIV.gtcsolo.command.GtcSoloCommand;
import DIV.gtcsolo.network.ModNetwork;
import DIV.gtcsolo.common.TooltipDisplayEvents;
import DIV.gtcsolo.data.ModRecipeProvider;
import DIV.gtcsolo.dump.RecipeDumpService;
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
        modEventBus.addListener(this::commonSetup);
        // レシピタイプはGTのレジストリが開いているタイミングで登録する
        modEventBus.addGenericListener(GTRecipeType.class, this::registerRecipeTypes);
        ModMachines.REGISTRATE.registerRegistrate();
        // マシン登録もGTのレジストリが開いているタイミングで行う
        modEventBus.addGenericListener(MachineDefinition.class, this::registerMachines);
        // データ生成（./gradlew runData でレシピJSONを生成する）
        modEventBus.addListener(this::gatherData);
        ModNetwork.register();
        // Apotheosis Gem skin (texture iconset + color マッピング)
        DIV.gtcsolo.apoth.gem.GemSkinRegistry.bootstrap();
        // カスタム Ingredient 登録 (LocusIngredient — StrictNBT の null vs {} 不一致を回避)
        net.minecraftforge.common.crafting.CraftingHelper.register(
                DIV.gtcsolo.api.ingredient.LocusIngredient.ID,
                DIV.gtcsolo.api.ingredient.LocusIngredient.SERIALIZER);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        DIV.gtcsolo.registry.ModRecipeSerializers.SERIALIZERS.register(modEventBus);
        // マニュアルUI (LDLib ModularUI) のファクトリ — アイテム右クリック / /gtcsolo manual 共通経路
        com.lowdragmc.lowdraglib.gui.factory.UIFactory.register(DIV.gtcsolo.manual.ManualUIFactory.INSTANCE);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModCreativeTabs.applyTabOverrides();
        MinecraftForge.EVENT_BUS.register(new DIV.gtcsolo.common.SleepTimeHandler());   // 就寝を夕方→夜(13000t)に繰り下げ
        MinecraftForge.EVENT_BUS.addListener(commandHandler::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(tooltipDisplayEvents::onItemTooltip);
        // AE2統合: WENワイヤレスエネルギーカードのポーリングハンドラ
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
        });
    }

    // CommonSetup
    private void commonSetup(net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // AE2 アップグレードカード登録 (対象AE2マシンへの互換登録)
            DIV.gtcsolo.integration.ae2.WENAe2Integration.registerUpgrades();
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
        LOGGER.info("[gtcsolo] addMaterials: registering materials...");
        ModMaterials.init();
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