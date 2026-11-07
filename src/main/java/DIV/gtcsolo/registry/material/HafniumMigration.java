package DIV.gtcsolo.registry.material;

import DIV.gtcsolo.Gtcsolo;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlag;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.FluidProperty;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.IngotProperty;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.OreProperty;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.fluids.FluidBuilder;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.MissingMappingsEvent;

/** Retire the misspelled duplicate while retaining its ore, parts and saved stacks. */
@Mod.EventBusSubscriber(modid = Gtcsolo.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HafniumMigration {
    private HafniumMigration() {}

    public static Material enableNativeForms(MaterialFlag... flags) {
        Material material = GTMaterials.Hafnium;
        if (!material.hasProperty(PropertyKey.INGOT)) material.setProperty(PropertyKey.INGOT, new IngotProperty());
        if (!material.hasProperty(PropertyKey.ORE)) material.setProperty(PropertyKey.ORE, new OreProperty(1, 1));
        var fluids = material.getProperty(PropertyKey.FLUID);
        if (fluids == null) {
            material.setProperty(PropertyKey.FLUID, new FluidProperty(FluidStorageKeys.LIQUID, new FluidBuilder()));
        } else if (fluids.getEntry(FluidStorageKeys.LIQUID) == null
                && fluids.getQueuedBuilder(FluidStorageKeys.LIQUID) == null) {
            fluids.enqueueRegistration(FluidStorageKeys.LIQUID, new FluidBuilder());
        }
        material.addFlags(flags);
        return material;
    }

    /** Compounds are still independent materials and must never be renamed by this migration. */
    static ResourceLocation replacement(ResourceLocation oldId) {
        if (!oldId.getNamespace().equals(Gtcsolo.MODID)) return null;
        String path = oldId.getPath();
        if (path.contains("hafhnium_diboride") || path.contains("hafhnium_carbide")) return null;
        String replacement = path.replaceFirst("(^|_)hafhnium(?=_|$)", "$1hafnium");
        return replacement.equals(path) ? null : new ResourceLocation("gtceu", replacement);
    }

    @SubscribeEvent
    public static void onMissingMappings(MissingMappingsEvent event) {
        remap(event, ForgeRegistries.ITEMS);
        remap(event, ForgeRegistries.BLOCKS);
        remap(event, ForgeRegistries.FLUIDS);
        remap(event, ForgeRegistries.FLUID_TYPES.get());
    }

    private static <T> void remap(MissingMappingsEvent event, IForgeRegistry<T> registry) {
        for (var mapping : event.getMappings(registry.getRegistryKey(), Gtcsolo.MODID)) {
            ResourceLocation target = replacement(mapping.getKey());
            if (target != null && registry.containsKey(target)) mapping.remap(registry.getValue(target));
        }
    }
}
