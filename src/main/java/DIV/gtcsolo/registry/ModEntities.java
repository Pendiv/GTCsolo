package DIV.gtcsolo.registry;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.entity.AmethystProjectile;
import DIV.gtcsolo.entity.IceProjectile;
import DIV.gtcsolo.entity.OrangeProjectile;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Gtcsolo.MODID);

    public static final RegistryObject<EntityType<OrangeProjectile>> ORANGE_PROJECTILE =
            ENTITY_TYPES.register("orange_projectile",
                    () -> EntityType.Builder.<OrangeProjectile>of(OrangeProjectile::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(4)
                            .updateInterval(10)
                            .build("orange_projectile")
            );

    public static final RegistryObject<EntityType<IceProjectile>> ICE_PROJECTILE =
            ENTITY_TYPES.register("ice_projectile",
                    () -> EntityType.Builder.<IceProjectile>of(IceProjectile::new, MobCategory.MISC)
                            .sized(0.85f, 0.85f)
                            .clientTrackingRange(6)
                            .updateInterval(10)
                            .build("ice_projectile")
            );

    public static final RegistryObject<EntityType<AmethystProjectile>> AMETHYST_PROJECTILE =
            ENTITY_TYPES.register("amethyst_projectile",
                    () -> EntityType.Builder.<AmethystProjectile>of(AmethystProjectile::new, MobCategory.MISC)
                            .sized(0.3f, 0.3f)
                            .clientTrackingRange(8)
                            .updateInterval(2)
                            .build("amethyst_projectile")
            );
}
