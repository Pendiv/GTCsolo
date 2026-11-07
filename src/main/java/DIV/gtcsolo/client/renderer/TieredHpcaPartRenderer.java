package DIV.gtcsolo.client.renderer;

import DIV.gtcsolo.machine.hpca.HpcaTierSpec;
import com.gregtechceu.gtceu.client.renderer.machine.HPCAPartRenderer;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/** Native HPCA facing, active and damage handling on the supplied tier-specific casing model. */
public final class TieredHpcaPartRenderer extends HPCAPartRenderer {
    public TieredHpcaPartRenderer(HpcaTierSpec tier, String overlay, boolean computation) {
        super(true, overlay(tier, overlay, ""),
                overlay.equals("empty") ? null : overlay(tier, overlay, "_active"),
                overlay.equals("empty") ? null : overlay(tier, overlay, "_active_emissive"),
                computation ? overlay(tier, overlay, "_damaged") : null,
                computation ? overlay(tier, overlay, "_damaged_active") : null,
                computation ? overlay(tier, overlay, "_damaged_active_emissive") : null);
        String casing = tier == HpcaTierSpec.UHV ? "elite_computer_casing" : "ultimate_computer_casing";
        modelLocation = new ResourceLocation("gtcsolo", "block/hpca/" + casing);
        // Retain native particle lookup while the model supplies all six faces and its CTM metadata.
        String base = "block/casings/hpca/" + casing + "/";
        setTextureOverride(Map.of("side", new ResourceLocation("gtcsolo", base + "side"),
                "top", new ResourceLocation("gtcsolo", base + "top"),
                "bottom", new ResourceLocation("gtcsolo", base + "bottom")));
    }

    private static ResourceLocation overlay(HpcaTierSpec tier, String overlay, String suffix) {
        return new ResourceLocation("gtcsolo", "block/overlay/machine/hpca/" + tier.path() + "_" + overlay + suffix);
    }
}
