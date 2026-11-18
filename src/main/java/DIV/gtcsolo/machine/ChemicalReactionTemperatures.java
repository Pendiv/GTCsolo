package DIV.gtcsolo.machine;

import com.gregtechceu.gtceu.api.block.ICoilType;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.SimpleTieredMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.data.machines.GTMultiMachines;
import net.minecraft.nbt.Tag;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ChemicalReactionTemperatures {
    public static final String RECOMMENDED_TEMPERATURE = "gtcsolo_recommended_temperature_kelvin";
    private static final int SMALL_REACTOR_BASE_KELVIN = 300;
    private static final int MAX_HEATING_OVERCLOCKS = 3;
    private static final int[] COIL_BASE_KELVIN_BY_TIER = {
            320, 385, 465, 550, 650, 775, 925, 1060, // GregTech tiers 0–7
            1200, 1375, // Draconium and Awakened Draconium
            1550, 1750 // Reserved tier 10 and tier 11
    };
    private static final Map<String, Integer> GREGTECH_RECIPES = gregTechTemperatures();
    private static final Map<String, Integer> PACK_RECIPES = Map.ofEntries(
            Map.entry("advanced_rubbers/acrylonitrile_synthesis", 725),
            Map.entry("advanced_rubbers/nbr_hydrogenation", 375),
            Map.entry("advanced_rubbers/ffkm_polymerization", 350),
            Map.entry("chi/growth_promoter", 425),
            Map.entry("nihonium/diethyl_ether", 425),
            Map.entry("nihonium/zinc_activated_rubber", 425),
            Map.entry("nihonium/zinc_activated_styrene_butadiene_rubber", 425),
            Map.entry("peroxide_processing/anthraquinone_cyclization", 410),
            Map.entry("polyimide_processing/durene_oxidation", 675),
            Map.entry("polyimide_processing/pyromellitic_dehydration", 525),
            Map.entry("polyimide_processing/maleic_anhydride", 675),
            Map.entry("polyimide_processing/n_methyl_2_pyrrolidone", 525),
            Map.entry("polyimide_processing/polyimide_preparation", 575),
            Map.entry("polyimide_processing/composite_preparation", 575));

    private ChemicalReactionTemperatures() {}

    public static boolean supports(GTRecipeType type) {
        return type == GTRecipeTypes.CHEMICAL_RECIPES || type == GTRecipeTypes.LARGE_CHEMICAL_RECIPES;
    }

    public static int recommendedKelvin(GTRecipe recipe) {
        if (!supports(recipe.recipeType)) return 0;
        if (recipe.data.contains(RECOMMENDED_TEMPERATURE, Tag.TAG_INT)) {
            return Math.max(0, recipe.data.getInt(RECOMMENDED_TEMPERATURE));
        }

        var id = recipe.getId();
        if (id == null) return 0;
        String prefix = recipe.recipeType.registryName.getPath() + "/";
        if (!id.getPath().startsWith(prefix)) return 0;
        // The large reactor can also receive a copy of a small-reactor recipe.
        String recipePath = id.getPath().substring(prefix.length());
        return switch (id.getNamespace()) {
            case "gtceu" -> GREGTECH_RECIPES.getOrDefault(recipePath, 0);
            case "gtcsolo" -> PACK_RECIPES.getOrDefault(recipePath, 0);
            default -> 0;
        };
    }

    public static int processingTicks(GTRecipe recipe, MetaMachine machine) {
        int recommended = recommendedKelvin(recipe);
        if (recommended <= 0 || operatingKelvin(machine, recipe.ocLevel) <= recommended) return recipe.duration;
        return Math.max(1, (int) (recipe.duration * 66L / 100));
    }

    private static int operatingKelvin(MetaMachine machine, int appliedOverclocks) {
        if (machine instanceof ReactionTemperatureSource source) {
            return source.getReactionTemperatureKelvin();
        }
        if (machine instanceof MultiblockControllerMachine controller
                && controller.getDefinition() == GTMultiMachines.LARGE_CHEMICAL_REACTOR
                && controller.isFormed()) {
            if (controller.getMultiblockState().getMatchContext().get("CoilType") instanceof ICoilType coil) {
                int tier = coil.getTier();
                if (tier >= 0 && tier < COIL_BASE_KELVIN_BY_TIER.length) {
                    return temperatureAfterOverclock(COIL_BASE_KELVIN_BY_TIER[tier], appliedOverclocks);
                }
            }
        } else if (machine instanceof SimpleTieredMachine
                && Arrays.asList(GTMachines.CHEMICAL_REACTOR).contains(machine.getDefinition())) {
            return temperatureAfterOverclock(SMALL_REACTOR_BASE_KELVIN, appliedOverclocks);
        }
        return 0;
    }

    private static int temperatureAfterOverclock(int baseKelvin, int appliedOverclocks) {
        long scaledKelvin = baseKelvin;
        int scale = 1;
        int heatingOverclocks = Math.min(appliedOverclocks, MAX_HEATING_OVERCLOCKS);
        // Apply 112/100 per stage and round only once.
        for (int stage = 0; stage < heatingOverclocks; stage++) {
            scaledKelvin *= 112;
            scale *= 100;
        }
        return (int) (scaledKelvin / scale);
    }

    private static Map<String, Integer> gregTechTemperatures() {
        var temperatures = new HashMap<>(Map.ofEntries(
                Map.entry("rubber", 425),
                Map.entry("styrene_butadiene_rubber", 425),
                Map.entry("methanol_from_dioxide", 500),
                Map.entry("methanol_from_monoxide", 500),
                Map.entry("sulfur_trioxide", 700),
                Map.entry("sulfur_trioxide_from_o2_so2", 700),
                Map.entry("ammonia_from_elements", 725),
                Map.entry("ammonia_shortcut", 1050),
                Map.entry("styrene_from_ethylbenzene", 900),
                Map.entry("nitric_oxide_from_ammonia", 1125),
                Map.entry("nitric_acid_from_ammonia", 1125),
                Map.entry("titanium_tetrachloride", 1175),
                Map.entry("elite_circuit_board_iron3", 315),
                Map.entry("elite_circuit_board_persulfate", 315),
                Map.entry("wetware_circuit_board_iron3", 315),
                Map.entry("wetware_circuit_board_persulfate", 315)));
        for (String feed : List.of("ethane", "ethylene", "propene", "propane", "butane", "butene", "butadiene")) {
            temperatures.put("hydro_crack_" + feed, 675);
            temperatures.put("steam_crack_" + feed, 1125);
        }
        for (String feed : List.of("heavy_fuel", "light_fuel", "naphtha", "refinery_gas")) {
            temperatures.put("lightly_hydro_crack_" + feed, 625);
            temperatures.put("severely_hydro_crack_" + feed, 725);
            temperatures.put("lightly_steam_crack_" + feed, 1075);
            temperatures.put("severely_steam_crack_" + feed, 1175);
        }
        return Map.copyOf(temperatures);
    }
}
