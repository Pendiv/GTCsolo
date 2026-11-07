package DIV.gtcsolo.registry.material;

import DIV.gtcsolo.Gtcsolo;
import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlag;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;

import java.util.List;

/** Opt in existing materials; their composition, ordinary forms and ore properties stay intact. */
public final class AmeijiaOreFlags {
    public static final MaterialFlag GENERATE_AMEIJIA_ORE = new MaterialFlag.Builder("generate_ameijia_ore")
            .requireProps(PropertyKey.ORE).build();

    /** Current materials used by run/kubejs/server_scripts/Pendiv's_Server_code/vein_ameijia.js. */
    public static final List<String> DEFAULT_MATERIALS = List.of(
            "gtceu:aluminium", "gtceu:bastnasite", "gtceu:cobaltite", "gtceu:hafnium", "gtceu:ilmenite",
            "gtceu:molybdenite", "gtceu:naquadah", "gtceu:neodymium", "gtceu:platinum", "gtceu:pyrolusite",
            "gtcsolo:bedrockium", "gtcsolo:enigma", "gtcsolo:exporonium", "gtcsolo:flaurite", "gtcsolo:jupitate",
            "gtcsolo:stagnanted_neutronium", "gtcsolo:stellarium", "gtcsolo:stellarium_enigma", "gtcsolo:valinium");

    private AmeijiaOreFlags() {}

    public static void init() {}

    public static boolean enabled(Material material) {
        return material.hasProperty(PropertyKey.ORE) && material.hasFlag(GENERATE_AMEIJIA_ORE);
    }

    public static void enableDefaults() {
        for (String id : DEFAULT_MATERIALS) {
            Material material = GTCEuAPI.materialManager.getMaterial(id);
            if (material == null) {
                Gtcsolo.LOGGER.warn("Ameijia ore material {} is not present in this profile", id);
            } else if (!material.hasProperty(PropertyKey.ORE)) {
                Gtcsolo.LOGGER.warn("Ameijia ore material {} has no ore property; leaving it unchanged", id);
            } else {
                material.addFlags(GENERATE_AMEIJIA_ORE);
            }
        }
    }
}
