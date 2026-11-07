package DIV.gtcsolo.machine;

import DIV.gtcsolo.api.pattern.GroupedBlockPattern;
import DIV.gtcsolo.registry.ModBlocks;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.common.data.GCYMBlocks;
import com.gregtechceu.gtceu.common.data.GTBlocks;

import static com.gregtechceu.gtceu.api.pattern.Predicates.*;

public final class IndustrialCircuitPattern {
    private IndustrialCircuitPattern() {}

    public static BlockPattern create(MultiblockMachineDefinition definition) {
        return GroupedBlockPattern.repeat(1, 6, modules -> fixed(definition, modules));
    }

    private static BlockPattern fixed(MultiblockMachineDefinition definition, int modules) {
        var casing = blocks(GCYMBlocks.CASING_LARGE_SCALE_ASSEMBLING.get());
        var fluidIn = abilities(PartAbility.IMPORT_FLUIDS).setPreviewCount(1);
        var pattern = FactoryBlockPattern.start(RelativeDirection.RIGHT, RelativeDirection.UP, RelativeDirection.BACK);
        for (String[] aisle : IndustrialCircuitStructure.aisles(modules)) pattern.aisle(aisle);
        return pattern
                .where('S', controller(blocks(definition.getBlock())))
                .where('A', casing.or(fluidIn)
                        .or(abilities(PartAbility.EXPORT_FLUIDS).setPreviewCount(1))
                        .or(abilities(PartAbility.DATA_ACCESS).setMaxGlobalLimited(1).setPreviewCount(1))
                        .or(abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1).setPreviewCount(1)))
                .where('F', fluidIn)
                .where('L', IndustrialCircuitLinePattern.bonusFluidInput())
                .where('J', IndustrialCircuitLinePattern.bonusInput())
                .where('I', IndustrialCircuitLinePattern.baseInput())
                .where('O', abilities(PartAbility.EXPORT_ITEMS))
                .where('E', abilities(PartAbility.INPUT_ENERGY, PartAbility.INPUT_LASER).setMinGlobalLimited(1))
                .where('M', casing.or(autoAbilities(true, false, false)))
                .where('G', blocks(GTBlocks.CASING_LAMINATED_GLASS.get()))
                .where('T', blocks(GCYMBlocks.CASING_ATOMIC.get()))
                .where('D', blocks(ModBlocks.DRACONIC_STEEL_GEARBOX.get()))
                .where('P', blocks(ModBlocks.DRACONIC_STEEL_PIPE_CASING.get()))
                .where('#', any())
                .build();
    }
}
