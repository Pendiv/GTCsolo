package DIV.gtcsolo.registry.tool.fullcustom;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Locale;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
final class FullCustomGtToolClientNames {

    private FullCustomGtToolClientNames() {}

    static String resolve(Map<String, String> localizedNames, String fallback) {
        String locale = Minecraft.getInstance().getLanguageManager().getSelected()
                .toLowerCase(Locale.ROOT)
                .replace('-', '_');
        return localizedNames.getOrDefault(locale, fallback);
    }
}
