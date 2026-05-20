package DIV.gtcsolo.l2.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

/**
 * L2H trait / curtain call などで共有する entity 判定 util。
 */
public final class L2EntityUtil {

    private L2EntityUtil() {}

    /** Cataclysm ボス階級 (= 倒すべきギミック持ちボス) */
    private static final Set<String> CATACLYSM_BOSSES = Set.of(
            "cataclysm:netherite_monstrosity",
            "cataclysm:ender_guardian",
            "cataclysm:ignis",
            "cataclysm:the_harbinger",
            "cataclysm:the_leviathan",
            "cataclysm:ancient_remnant",
            "cataclysm:ender_golem"
    );

    private static final TagKey<EntityType<?>> FORGE_BOSSES = TagKey.create(
            Registries.ENTITY_TYPE, new ResourceLocation("forge", "bosses"));
    private static final TagKey<EntityType<?>> LH_SEMIBOSS = TagKey.create(
            Registries.ENTITY_TYPE, new ResourceLocation("l2hostility", "semiboss"));

    /**
     * ボス判定: vanilla EnderDragon/Wither、 forge:bosses tag、 l2hostility:semiboss tag、
     * Cataclysm のボス階級 hardcoded。
     */
    public static boolean isBoss(LivingEntity entity) {
        if (entity instanceof EnderDragon || entity instanceof WitherBoss) return true;
        EntityType<?> type = entity.getType();
        if (type.is(FORGE_BOSSES)) return true;
        if (type.is(LH_SEMIBOSS)) return true;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        return id != null && CATACLYSM_BOSSES.contains(id.toString());
    }
}
