package DIV.gtcsolo.registry;

import DIV.gtcsolo.Gtcsolo;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * gtcsolo 独自 attribute 登録 manager。
 *
 * <p>Apotheosis / L2DamageTracker 既存の crit 系 attribute と
 * <b>意図的に重複して乗算される</b> エンドコンテンツ向けの「ラッキーヒット」 を提供する。
 *
 * <p>適用は {@link DIV.gtcsolo.combat.LuckyHitHandler} の LivingDamageEvent 経由
 * (= armor / absorb / 既存 crit 計算が全て終わった後の HP delta に倍率を掛ける)。
 */
public class ModAttributes {

    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, Gtcsolo.MODID);

    /** ラッキーヒット率: 0.0 - 1.0 (= 0%〜100%)、 default 0。 LivingDamageEvent で roll。 */
    public static final RegistryObject<Attribute> LUCKY_HIT_RATE = ATTRIBUTES.register("lucky_hit_rate",
            () -> new RangedAttribute("attribute.gtcsolo.lucky_hit_rate", 0.0, 0.0, 1.0).setSyncable(true));

    /**
     * ラッキーヒットダメージ倍率 (% 値): 1 - 1000、 default 100 (= 等倍 / no buff)。
     * 当選時 {@code event.amount × (value / 100)} で乗算される。
     */
    public static final RegistryObject<Attribute> LUCKY_HIT_DAMAGE = ATTRIBUTES.register("lucky_hit_damage",
            () -> new RangedAttribute("attribute.gtcsolo.lucky_hit_damage", 100.0, 1.0, 1000.0).setSyncable(true));

    public static void register(IEventBus bus) {
        ATTRIBUTES.register(bus);
    }
}
