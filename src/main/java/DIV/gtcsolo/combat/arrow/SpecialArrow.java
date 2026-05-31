package DIV.gtcsolo.combat.arrow;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * 特殊な矢システムのレジストリ兼ディスパッチャ。
 *
 * <p>矢の {@code persistentData} の sub-compound {@value #NBT_ROOT} に
 * {@code behavior id → 性能(double)} を格納する。 {@link ArrowEventHandlers} が
 * spawn / hit / hurt の各イベントで対応 behavior へディスパッチする。
 *
 * <p>{@link ArrowBehaviors#registerAll()} を {@link #bootstrap()} 経由で起動時に呼んで
 * 組み込み behavior を登録する。
 */
public final class SpecialArrow {

    private SpecialArrow() {}

    public static final String NBT_ROOT = "gtcsolo_arrow";

    private static final Map<String, ArrowBehavior> REGISTRY = new HashMap<>();

    public static void register(ArrowBehavior behavior) {
        REGISTRY.put(behavior.id(), behavior);
    }

    @Nullable
    public static ArrowBehavior byId(String id) {
        return REGISTRY.get(id);
    }

    /** behavior を性能付きで矢へ付与する。 */
    public static void put(AbstractArrow arrow, ArrowBehavior behavior, double performance) {
        CompoundTag root = arrow.getPersistentData().getCompound(NBT_ROOT);
        root.putDouble(behavior.id(), performance);
        arrow.getPersistentData().put(NBT_ROOT, root);
    }

    /** behavior を初期値で矢へ付与する。 */
    public static void put(AbstractArrow arrow, ArrowBehavior behavior) {
        put(arrow, behavior, behavior.defaultPerformance());
    }

    /** 矢が 1 つ以上の特殊 behavior を持つか。 */
    public static boolean isSpecial(AbstractArrow arrow) {
        CompoundTag pd = arrow.getPersistentData();
        return pd.contains(NBT_ROOT) && !pd.getCompound(NBT_ROOT).isEmpty();
    }

    @FunctionalInterface
    private interface Action {
        void run(ArrowBehavior behavior, double performance);
    }

    private static void dispatch(AbstractArrow arrow, Action action) {
        CompoundTag pd = arrow.getPersistentData();
        if (!pd.contains(NBT_ROOT)) return;
        CompoundTag root = pd.getCompound(NBT_ROOT);
        for (String id : root.getAllKeys()) {
            ArrowBehavior behavior = REGISTRY.get(id);
            if (behavior != null) action.run(behavior, root.getDouble(id));
        }
    }

    public static void onSpawn(AbstractArrow arrow, @Nullable LivingEntity shooter) {
        dispatch(arrow, (b, p) -> b.onSpawn(arrow, shooter, p));
    }

    public static void onHitEntity(AbstractArrow arrow, LivingEntity target, @Nullable LivingEntity shooter) {
        dispatch(arrow, (b, p) -> b.onHitEntity(arrow, target, shooter, p));
    }

    public static void onHitBlock(AbstractArrow arrow, BlockHitResult hit, @Nullable LivingEntity shooter) {
        dispatch(arrow, (b, p) -> b.onHitBlock(arrow, hit, shooter, p));
    }

    public static void onHurt(AbstractArrow arrow, LivingHurtEvent event, @Nullable LivingEntity shooter) {
        dispatch(arrow, (b, p) -> b.onHurt(arrow, event, shooter, p));
    }

    /**
     * shooter から target へ照準した {@link Arrow} を生成して返す (= まだ world には追加しない)。
     * 呼び出し側で {@link #put} で behavior を付与し {@code level.addFreshEntity(arrow)} する。
     * shooter は mob / player どちらでもよい (= 将来の player 発射にも再利用可)。
     *
     * @param speed      初速 (vanilla 弓 ≈ 3.0、 スケルトン ≈ 1.6)
     * @param inaccuracy ばらつき (0 = 正確)
     */
    public static Arrow aimedArrow(LivingEntity shooter, Entity target, float speed, float inaccuracy) {
        Arrow arrow = new Arrow(shooter.level(), shooter);
        double dx = target.getX() - shooter.getX();
        double dy = target.getY(0.3333) - arrow.getY();
        double dz = target.getZ() - shooter.getZ();
        double horiz = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dy + horiz * 0.2, dz, speed, inaccuracy);  // horiz×0.2 弧補正 (= vanilla スケルトン同等)
        return arrow;
    }

    /** 組み込み behavior を登録する。 Gtcsolo 初期化から 1 度呼ぶ。 */
    public static void bootstrap() {
        ArrowBehaviors.registerAll();
    }
}
