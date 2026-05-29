package DIV.gtcsolo.l2.trait;

import DIV.gtcsolo.l2.trait.base.TypedMobTrait;
import DIV.gtcsolo.l2.util.L2EntityUtil;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;
import java.util.UUID;

/**
 * [20] Bushido Spirit — ボス専用。 ステータス全強化 + Innocence Battle 内部付与 + 周囲他 mob 排除。
 *
 * <p>除外: MASTER minion (= MobTraitCap で master 親リンク持ち) + Cataclysm ボス階級 (= hardcoded list)。
 *
 * <p>仕様上ボス専用 = {@link TypedMobTrait#isValidTarget} で {@link L2EntityUtil#isBoss} を必須化。
 * datapack/command/誤継承で雑魚に付いた場合は全 hook が no-op となり buff されない。
 */
public class BushidoSpiritTrait extends TypedMobTrait {

    private static final UUID MOD_HP = UUID.fromString("b1f3e9c7-3d44-6b1b-9a14-7b8e2b0c1e3a");
    private static final UUID MOD_ATK = UUID.fromString("b2f3e9c7-3d44-6b1b-9a14-7b8e2b0c1e3a");
    private static final UUID MOD_DEF = UUID.fromString("b3f3e9c7-3d44-6b1b-9a14-7b8e2b0c1e3a");

    /** Cataclysm ボス階級 (= 排除対象外、 倒すべきギミック持ちボス) */
    private static final Set<String> CATACLYSM_BOSSES = Set.of(
            "cataclysm:netherite_monstrosity",
            "cataclysm:ender_guardian",
            "cataclysm:ignis",
            "cataclysm:the_harbinger",
            "cataclysm:the_leviathan",
            "cataclysm:ancient_remnant",
            "cataclysm:ender_golem"
    );

    public BushidoSpiritTrait(ChatFormatting style) {
        super(style);
    }

    @Override
    protected boolean isValidTarget(LivingEntity mob) {
        return L2EntityUtil.isBoss(mob);
    }

    @Override
    protected void onValidPostInit(LivingEntity mob, int lv) {
        // ステータス強化 (= MAX_HEALTH +200%、 ATTACK_DAMAGE +100%、 ARMOR +20)
        applyMod(mob, Attributes.MAX_HEALTH, MOD_HP, 1.25 + 0.25 * lv, AttributeModifier.Operation.MULTIPLY_BASE);   // HP +(125 + 25n)%
        applyMod(mob, Attributes.ATTACK_DAMAGE, MOD_ATK, 0.25 + 0.25 * lv, AttributeModifier.Operation.MULTIPLY_BASE); // 攻撃 +(25 + 25n)%
        applyMod(mob, Attributes.ARMOR, MOD_DEF, 10.0 * lv, AttributeModifier.Operation.ADDITION);                    // 防具 +10n (実数)
        mob.setHealth(mob.getMaxHealth());

        // Innocence Battle を内部付与
        MobTraitCap cap = MobTraitCap.HOLDER.get(mob);
        if (!cap.hasTrait(DIV.gtcsolo.l2.ModL2Traits.INNOCENCE_BATTLE.get())) {
            cap.setTrait(DIV.gtcsolo.l2.ModL2Traits.INNOCENCE_BATTLE.get(), lv);
        }
    }

    @Override
    protected void onValidTick(LivingEntity mob, int level) {
        if (mob.level().isClientSide()) return;
        if (mob.tickCount % 40 != 0) return; // 2 秒おき

        // 周囲 mob 排除 (= 例外を除く)
        var area = mob.getBoundingBox().inflate(32);
        for (Mob other : mob.level().getEntitiesOfClass(Mob.class, area, e -> e != mob)) {
            if (isExcluded(other)) continue;
            other.discard();
        }
    }

    private static boolean isExcluded(Mob other) {
        // MASTER minion: MobTraitCap.minion フラグ持ち
        if (MobTraitCap.HOLDER.isProper(other)) {
            MobTraitCap cap = MobTraitCap.HOLDER.get(other);
            if (cap.minion) return true;
        }

        // Cataclysm ボス階級
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(other.getType());
        if (id != null && CATACLYSM_BOSSES.contains(id.toString())) return true;

        return false;
    }

    private static void applyMod(LivingEntity mob, net.minecraft.world.entity.ai.attributes.Attribute attr,
                                  UUID id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = mob.getAttribute(attr);
        if (inst == null) return;
        if (inst.getModifier(id) == null) {
            inst.addPermanentModifier(new AttributeModifier(id, "gtcsolo.bushido", amount, op));
        }
    }
}
