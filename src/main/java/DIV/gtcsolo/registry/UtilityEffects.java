package DIV.gtcsolo.registry;

import DIV.gtcsolo.effect.GleamRayEffect;
import DIV.gtcsolo.effect.SpectatorEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class UtilityEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, "gtcsolo");
    public static final RegistryObject<MobEffect> GLEAM_RAY = EFFECTS.register("gleam_ray", GleamRayEffect::new);
    public static final RegistryObject<MobEffect> SPECTATOR = EFFECTS.register("spectator", SpectatorEffect::new);
    private UtilityEffects() {}
}
