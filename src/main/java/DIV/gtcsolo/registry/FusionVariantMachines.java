package DIV.gtcsolo.registry;

import DIV.gtcsolo.block.FusionMk4CasingType;
import DIV.gtcsolo.machine.fusion.FusionVariantMachine;
import DIV.gtcsolo.machine.fusion.FusionVariantPattern;
import DIV.gtcsolo.machine.fusion.FusionVariantSettings;
import DIV.gtcsolo.machine.fusion.MegaFusionMachine;
import DIV.gtcsolo.render.FusionVariantRenderer;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
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
        for (var settings:FusionVariantSettings.values()) {
            String roman=new String[]{"I","II","III","IV"}[settings.mk-1];
            String familyName=switch (settings.family) {
                case "advanced" -> "Advanced";
                case "compact" -> "Compact";
                case "giant" -> "Giant";
                case "mega" -> "Mega";
                default -> throw new IllegalArgumentException("Unknown fusion family: "+settings.family);
            };
            var casingTexture=settings.mk==4 ? FusionMk4CasingType.TEXTURE
                    : FusionReactorMachine.getCasingType(settings.tier()).getTexture();
            var builder=ModMachines.REGISTRATE.multiblock(settings.id(),holder->settings.isMega()
                            ? new MegaFusionMachine(holder) : new FusionVariantMachine(holder,settings))
                    .rotationState(RotationState.ALL).tier(settings.tier())
                    .langValue(settings.isMega() ? "Mega Fusion Reactor" : familyName+" Fusion Reactor Computer MK "+roman)
                    .recipeType(GTRecipeTypes.FUSION_RECIPES)
                    .recipeModifiers(GTRecipeModifiers.DEFAULT_ENVIRONMENT_REQUIREMENT,
                            (machine,recipe)->settings.isMega() ? MegaFusionMachine.recipeModifier(machine,recipe)
                                    : FusionVariantMachine.recipeModifier(machine,recipe))
                    .appearanceBlock(()->FusionVariantPattern.casing(settings))
                    .pattern(definitionValue->settings.pattern().create(definitionValue,settings))
                    .tooltips(Component.translatable("gtceu.machine.fusion_reactor.capacity",settings.maximumStartupCapacity()/1_000_000L),
                            Component.translatable(settings.isMega() ? "gtcsolo.mega_fusion.throughput" : settings.fusionOverclocking() ? "gtceu.machine.fusion_reactor.overclocking"
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
