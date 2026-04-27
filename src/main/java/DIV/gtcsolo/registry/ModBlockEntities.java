package DIV.gtcsolo.registry;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.block.ExtendEnergyCubeBlockEntity;
import DIV.gtcsolo.block.wen.WENAePortBlockEntity;
import DIV.gtcsolo.block.wen.WENDataMonitorBlockEntity;
import DIV.gtcsolo.block.wen.WENFePortBlockEntity;
import DIV.gtcsolo.block.wen.WENPortBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Gtcsolo.MODID);

    public static final RegistryObject<BlockEntityType<ExtendEnergyCubeBlockEntity>> EXTEND_ENERGY_CUBE =
            BLOCK_ENTITIES.register("extend_energy_cube",
                    () -> BlockEntityType.Builder
                            .of(ExtendEnergyCubeBlockEntity::new, ModBlocks.EXTEND_ENERGY_CUBE.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<WENDataMonitorBlockEntity>> WEN_DATA_MONITOR =
            BLOCK_ENTITIES.register("wen_data_monitor",
                    () -> BlockEntityType.Builder
                            .of(WENDataMonitorBlockEntity::new, ModBlocks.WEN_DATA_MONITOR.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<WENPortBlockEntity>> WEN_PORT =
            BLOCK_ENTITIES.register("wen_port",
                    () -> BlockEntityType.Builder
                            .of(WENPortBlockEntity::new,
                                    ModBlocks.WEN_MAINSTORAGE_INPUT_PORT.get(),
                                    ModBlocks.WEN_MAINSTORAGE_OUTPUT_PORT.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<WENAePortBlockEntity>> WEN_AE_PORT =
            BLOCK_ENTITIES.register("wen_ae_port",
                    () -> BlockEntityType.Builder
                            .of(WENAePortBlockEntity::new,
                                    ModBlocks.WEN_AE_INPUT_PORT.get(),
                                    ModBlocks.WEN_AE_OUTPUT_PORT.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<WENFePortBlockEntity>> WEN_FE_PORT =
            BLOCK_ENTITIES.register("wen_fe_port",
                    () -> BlockEntityType.Builder
                            .of(WENFePortBlockEntity::new,
                                    ModBlocks.WEN_FE_INPUT_PORT.get(),
                                    ModBlocks.WEN_FE_OUTPUT_PORT.get())
                            .build(null));
}