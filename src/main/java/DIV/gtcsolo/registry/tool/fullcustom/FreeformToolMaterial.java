package DIV.gtcsolo.registry.tool.fullcustom;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

/**
 * An unregistered Material used only because GTCEu 1.6.4 stores tool stats and tint colors behind Material.
 * It never enters a MaterialRegistry, so it cannot generate material forms or material-derived tool IDs.
 */
final class FreeformToolMaterial extends Material {

    private final FullCustomGtToolDeclaration declaration;

    FreeformToolMaterial(FullCustomGtToolDeclaration declaration) {
        super(internalId(declaration.id()));
        this.declaration = declaration;

        getProperties().setMaterial(this);
        getProperties().setProperty(PropertyKey.TOOL, declaration.toolProperty());
        getProperties().verify();
        setMaterialARGB(declaration.primaryColor());
        setMaterialSecondaryARGB(declaration.secondaryColor());
    }

    FullCustomGtToolDeclaration declaration() {
        return declaration;
    }

    @Override
    public String getUnlocalizedName() {
        return declaration.id().toLanguageKey("item");
    }

    @Override
    public MutableComponent getLocalizedName() {
        return net.minecraft.network.chat.Component.literal(FullCustomGtTools.localizedName(this));
    }

    private static ResourceLocation internalId(ResourceLocation itemId) {
        return new ResourceLocation("gtcsolo", "__freeform_tool/" +
                itemId.getNamespace() + "/" + itemId.getPath());
    }
}
