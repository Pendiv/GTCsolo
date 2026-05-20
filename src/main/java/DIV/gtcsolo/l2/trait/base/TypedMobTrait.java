package DIV.gtcsolo.l2.trait.base;

import dev.xkmc.l2damagetracker.contents.attack.AttackCache;
import dev.xkmc.l2damagetracker.contents.attack.CreateSourceEvent;
import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import dev.xkmc.l2hostility.content.logic.InheritContext;
import dev.xkmc.l2hostility.content.logic.TraitEffectCache;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2hostility.init.data.LHDamageTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * 特定 type にのみ適用される MobTrait の base class。
 *
 * <p>サブクラスは {@link #isValidTarget(LivingEntity)} と onValidXxx 群を override する。
 * 全 hook の冒頭で {@code isValidTarget} を呼び、 不適合 entity に対しては早期 return する
 * (= datapack/command/他 mod 経由で wrong type に付与されてもクラッシュしない安全保証)。
 *
 * <p>inherited も wrap — 不適合 child には 0 を返して継承拒否する
 * (= SummoningRitual 等で誤継承された場合に wrong type へ流れ込むのを防ぐ)。
 *
 * <p>各 hook で渡される LivingEntity は 「trait 保持者」 (= mob holder)。
 * onHurtTarget/postHurtImpl/onCreateSource の attacker、 onHurtByOthers/onAttackedByOthers
 * の entity、 tick/postInit/initialize/onDamaged/onDeath の mob — 全て trait 保持者を指す。
 */
public abstract class TypedMobTrait extends MobTrait {

    public TypedMobTrait(ChatFormatting style) {
        super(style);
    }

    /** この trait が対象 entity に対し有効か (= 全 hook の前段フィルタ) */
    protected abstract boolean isValidTarget(LivingEntity mob);

    // === all hooks: wrapper with isValidTarget guard ===

    @Override
    public final void initialize(LivingEntity mob, int level) {
        if (isValidTarget(mob)) onValidInitialize(mob, level);
    }

    @Override
    public final void postInit(LivingEntity mob, int lv) {
        if (isValidTarget(mob)) onValidPostInit(mob, lv);
    }

    @Override
    public final void tick(LivingEntity mob, int level) {
        if (isValidTarget(mob)) onValidTick(mob, level);
    }

    @Override
    public final void onHurtTarget(int level, LivingEntity attacker, AttackCache cache, TraitEffectCache traitCache) {
        if (isValidTarget(attacker)) onValidHurtTarget(level, attacker, cache, traitCache);
    }

    @Override
    public final void postHurtImpl(int level, LivingEntity attacker, LivingEntity target) {
        if (isValidTarget(attacker)) onValidPostHurtImpl(level, attacker, target);
    }

    @Override
    public final void onAttackedByOthers(int level, LivingEntity entity, LivingAttackEvent event) {
        if (isValidTarget(entity)) onValidAttackedByOthers(level, entity, event);
    }

    @Override
    public final void onHurtByOthers(int level, LivingEntity entity, LivingHurtEvent event) {
        if (isValidTarget(entity)) onValidHurtByOthers(level, entity, event);
    }

    @Override
    public final void onCreateSource(int level, LivingEntity attacker, CreateSourceEvent event) {
        if (isValidTarget(attacker)) onValidCreateSource(level, attacker, event);
    }

    @Override
    public final void onDamaged(int level, LivingEntity mob, AttackCache cache) {
        if (isValidTarget(mob)) onValidDamaged(level, mob, cache);
    }

    @Override
    public final void onDeath(int level, LivingEntity entity, LivingDeathEvent event) {
        if (isValidTarget(entity)) onValidDeath(level, entity, event);
    }

    @Override
    public final int inherited(MobTraitCap cap, int rank, InheritContext ctx) {
        if (!isValidTarget(ctx.child())) return 0;
        return onValidInherited(cap, rank, ctx);
    }

    // === default no-op hooks: サブクラスは必要なものだけ override する ===

    protected void onValidInitialize(LivingEntity mob, int level) {}

    protected void onValidPostInit(LivingEntity mob, int lv) {}

    protected void onValidTick(LivingEntity mob, int level) {}

    /**
     * Default: {@link MobTrait#onHurtTarget} と同じ filter cascade を再現
     * (= KILLER_AURA 除外 + 与ダメ > 0 で postHurtPlayer 呼び出し)。
     * サブクラスが super.onValidHurtTarget を呼ばずに override すれば cascade をスキップできる。
     */
    protected void onValidHurtTarget(int level, LivingEntity attacker, AttackCache cache, TraitEffectCache traitCache) {
        LivingHurtEvent e = cache.getLivingHurtEvent();
        if (e == null) return;
        if (e.getAmount() > 0 && !e.getSource().is(LHDamageTypes.KILLER_AURA)) {
            postHurtPlayer(level, attacker, traitCache);
        }
    }

    protected void onValidPostHurtImpl(int level, LivingEntity attacker, LivingEntity target) {}

    protected void onValidAttackedByOthers(int level, LivingEntity entity, LivingAttackEvent event) {}

    protected void onValidHurtByOthers(int level, LivingEntity entity, LivingHurtEvent event) {}

    protected void onValidCreateSource(int level, LivingEntity attacker, CreateSourceEvent event) {}

    protected void onValidDamaged(int level, LivingEntity mob, AttackCache cache) {}

    protected void onValidDeath(int level, LivingEntity entity, LivingDeathEvent event) {}

    /** Default: super と同じく rank をそのまま返す (= 制限なく継承)。 wrong type は親 wrapper で 0 になる。 */
    protected int onValidInherited(MobTraitCap cap, int rank, InheritContext ctx) {
        return rank;
    }
}
