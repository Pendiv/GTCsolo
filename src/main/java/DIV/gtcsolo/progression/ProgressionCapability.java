package DIV.gtcsolo.progression;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;

/**
 * {@link PlayerProgression} を player に持たせる Forge capability のハンドル。
 * 登録は mod event bus の {@link RegisterCapabilitiesEvent} で行う。
 */
public class ProgressionCapability {

    public static final Capability<PlayerProgression> PLAYER =
            CapabilityManager.get(new CapabilityToken<>() {});

    private ProgressionCapability() {}

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(PlayerProgression.class);
    }
}
