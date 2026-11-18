package DIV.gtcsolo.registry;

import DIV.gtcsolo.block.FusionMk4CasingType;
import DIV.gtcsolo.block.FusionMk5CasingType;
import DIV.gtcsolo.machine.fusion.FusionVariantMachine;
import DIV.gtcsolo.machine.fusion.FusionVariantPattern;
import DIV.gtcsolo.machine.fusion.FusionVariantSettings;
import DIV.gtcsolo.machine.fusion.MegaFusionMachine;
import DIV.gtcsolo.render.FusionVariantRenderer;
import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.FusionReactorMachine;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.Map;

/** Additional controllers. Original GT and GTCsolo fusion controllers remain registered unchanged. */
public final class FusionVariantMachines {
    public static final Map<FusionVariantSettings,MultiblockMachineDefinition> DEFINITIONS =
            new EnumMap<>(FusionVariantSettings.class);
    private FusionVariantMachines() {}

    public static void init() {
        GTBlocks.ALL_FUSION_CASINGS.put(FusionMk5CasingType.CASING, ModBlocks.FUSION_CASING_MK5);
        GTBlocks.ALL_FUSION_CASINGS.put(FusionMk5CasingType.COIL, ModBlocks.ADVANCED_FUSION_COIL);
        for (var settings:FusionVariantSettings.values()) {
            boolean mk5 = settings == FusionVariantSettings.STANDARD_MK5;
            if (mk5 && !GTCEuAPI.isHighTier()) continue;
            if (mk5) FusionReactorMachine.registerFusionTier(settings.tier(), " (MK5)");
            String englishName=switch (settings.family) {
                case "advanced", "compact", "giant" -> Character.toUpperCase(settings.family.charAt(0))
                        + settings.family.substring(1) + " Fusion Reactor Computer MK "
                        + new String[]{"I","II","III","IV"}[settings.mk-1];
                case "mega" -> "Mega Fusion Reactor";
                case "standard" -> "Fusion Reactor MK5";
                default -> throw new IllegalArgumentException("Unknown fusion family: "+settings.family);
            };
            var casingTexture=mk5 ? FusionMk5CasingType.CASING.getTexture()
                    : settings.mk==4 ? FusionMk4CasingType.TEXTURE
                    : FusionReactorMachine.getCasingType(settings.tier()).getTexture();
            var builder=ModMachines.REGISTRATE.multiblock(settings.id(),holder->settings.isMega()
                            ? new MegaFusionMachine(holder) : new FusionVariantMachine(holder,settings))
                    .rotationState(RotationState.ALL).tier(settings.tier())
                    .langValue(englishName)
                    .recipeType(GTRecipeTypes.FUSION_RECIPES)
                    .recipeModifiers(GTRecipeModifiers.DEFAULT_ENVIRONMENT_REQUIREMENT,
                            (machine,recipe)->settings.isMega() ? MegaFusionMachine.recipeModifier(machine,recipe)
                                    : FusionVariantMachine.recipeModifier(machine,recipe))
                    .appearanceBlock(()->FusionVariantPattern.casing(settings))
                    .pattern(definitionValue->settings.pattern().create(definitionValue,settings))
                    .tooltips(Component.translatable("gtceu.machine.fusion_reactor.capacity",settings.maximumStartupCapacity()/1_000_000L),
                            Component.translatable(mk5 ? "gtcsolo.machine.fusion_reactor_mk5.ports"
                                    : settings.isMega() ? "gtcsolo.mega_fusion.throughput" : settings.fusionOverclocking() ? "gtceu.machine.fusion_reactor.overclocking"
                                    : "gtcsolo.machine.fusion_reactor_mk4.no_overclock"))
                    .renderer(()->new FusionVariantRenderer(casingTexture,new ResourceLocation("gtceu","block/multiblock/fusion_reactor"),settings.isMega()?512:64))
                    .hasTESR(true);
            if (settings.isMega()) builder.multiblockPreviewRenderer(true,false)
                    .rotationState(RotationState.NON_Y_AXIS)
                    .allowExtendedFacing(false).allowFlip(false)
                    .tooltips(Component.translatable("gtcsolo.mega_fusion.heat"),Component.translatable("gtcsolo.mega_fusion.cooling"),
                            Component.translatable("gtcsolo.mega_fusion.ports"));
            else builder.shapeInfos(definitionValue->settings.pattern().previews(definitionValue,settings));
            var definition=builder.register();
            DEFINITIONS.put(settings,definition);
        }
    }
}
