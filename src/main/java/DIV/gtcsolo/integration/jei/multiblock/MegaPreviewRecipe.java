package DIV.gtcsolo.integration.jei.multiblock;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;

/** JEI registration stores descriptors only, never an expanded GT shape or a dummy world. */
public record MegaPreviewRecipe(MultiblockMachineDefinition definition, String resource,
                                char port, String portBlock, boolean variableCoils, String portTooltip) {}
