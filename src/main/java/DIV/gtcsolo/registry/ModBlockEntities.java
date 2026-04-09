package DIV.gtcsolo.registry;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.block.ExtendEnergyCubeBlockEntity;
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
}