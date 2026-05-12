package DIV.gtcsolo.apoth.gem;

import com.google.common.collect.ImmutableMap;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public final class GemSkinRegistry {
    private static final Map<ResourceLocation, GemSkin> SKINS = new HashMap<>();

    private GemSkinRegistry() {}

    public static void register(ResourceLocation id, GemSkin skin) {
        SKINS.put(id, skin);
    }

    @Nullable
    public static GemSkin get(ResourceLocation id) {
        return SKINS.get(id);
    }

    public static Map<ResourceLocation, GemSkin> all() {
        return ImmutableMap.copyOf(SKINS);
    }

    public static void bootstrap() {
        register(new ResourceLocation("gtcsolo", "risk_return"),
                 new GemSkin(0xCC99FF, "netherstar"));
    }
}
