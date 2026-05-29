package DIV.gtcsolo.registry;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.l2.effect.CertainKillEffect;
import DIV.gtcsolo.l2.effect.ConcealmentEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * gtcsolo の MobEffect 登録 manager。
 * 隠蔽 (concealment) を皮切りに、 時空タイプ trait 等が利用する effect を登録する。
 */
public class ModEffects {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Gtcsolo.MODID);

    public static final RegistryObject<ConcealmentEffect> CONCEALMENT =
            MOB_EFFECTS.register("concealment", ConcealmentEffect::new);

    public static final RegistryObject<CertainKillEffect> CERTAIN_KILL =
            MOB_EFFECTS.register("certain_kill", CertainKillEffect::new);

    private ModEffects() {}
}
