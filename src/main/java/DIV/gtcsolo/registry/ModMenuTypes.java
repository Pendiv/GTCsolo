package DIV.gtcsolo.registry;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.block.ExtendEnergyCubeMenu;
import DIV.gtcsolo.block.wen.WENDataMonitorMenu;
import DIV.gtcsolo.block.wen.WENIdSelectMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, Gtcsolo.MODID);

    public static final RegistryObject<MenuType<ExtendEnergyCubeMenu>> EXTEND_ENERGY_CUBE =
            MENU_TYPES.register("extend_energy_cube",
                    () -> IForgeMenuType.create(ExtendEnergyCubeMenu::new));

    public static final RegistryObject<MenuType<WENDataMonitorMenu>> WEN_DATA_MONITOR =
            MENU_TYPES.register("wen_data_monitor",
                    () -> IForgeMenuType.create(WENDataMonitorMenu::new));

    public static final RegistryObject<MenuType<WENIdSelectMenu>> WEN_ID_SELECT =
            MENU_TYPES.register("wen_id_select",
                    () -> IForgeMenuType.create(WENIdSelectMenu::new));
}