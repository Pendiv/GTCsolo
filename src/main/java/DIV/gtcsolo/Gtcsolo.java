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
import DIV.gtcsolo.registry.ModEnchantments;
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
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEnchantments.ENCHANTMENTS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModCreativeTabs.applyTabOverrides();
        MinecraftForge.EVENT_BUS.register(new DIV.gtcsolo.common.AbsoluteKillHandler());
        MinecraftForge.EVENT_BUS.register(new DIV.gtcsolo.common.framealtar.FrameAltarHandler());
        MinecraftForge.EVENT_BUS.addListener(commandHandler::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(tooltipDisplayEvents::onItemTooltip);
        // AE2統合: WENワイヤレスエネルギーカードのポーリングハンドラ
        MinecraftForge.EVENT_BUS.register(DIV.gtcsolo.integration.ae2.WENAe2Integration.class);
        // クライアント専用セットアップ
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> modEventBus.addListener(Gtcsolo::clientSetup));
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.EXTEND_ENERGY_CUBE.get(), ExtendEnergyCubeScreen::new);
            MenuScreens.register(ModMenuTypes.WEN_DATA_MONITOR.get(), WENDataMonitorScreen::new);
            MenuScreens.register(ModMenuTypes.WEN_ID_SELECT.get(), WENIdSelectScreen::new);
        });
    }

    // CommonSetup
    private void commonSetup(net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            DIV.gtcsolo.common.framealtar.FrameAltarRecipes.init();
            // AE2 アップグレードカード登録 (対象AE2マシンへの互換登録)
            DIV.gtcsolo.integration.ae2.WENAe2Integration.registerUpgrades();
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