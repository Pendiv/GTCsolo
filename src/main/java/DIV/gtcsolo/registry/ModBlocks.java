package DIV.gtcsolo.registry;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.block.ExtendEnergyCubeBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Gtcsolo.MODID);

    public static final RegistryObject<Block> HIGH_STRENGTH_STEEL_CASING =
            BLOCKS.register("high_strength_steel_casing",
                    () -> new Block(BlockBehaviour.Properties.of()
                            .strength(5.0f, 12.0f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()
                    )
            );

    public static final RegistryObject<Block> EXTEND_ENERGY_CUBE =
            BLOCKS.register("extend_energy_cube", ExtendEnergyCubeBlock::new);
}