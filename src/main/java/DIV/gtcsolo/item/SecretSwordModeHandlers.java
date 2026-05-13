package DIV.gtcsolo.item;

import DIV.gtcsolo.entity.AmethystProjectile;
import DIV.gtcsolo.entity.IceProjectile;
import DIV.gtcsolo.registry.ModEntities;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * SecretSwordItem の左クリック動作 (= server side mode action) の実行ハンドラ。
 * SecretSwordActionPacket から呼ばれる。
 *
 *   - Mode 1: TP (raycast)
 *   - Mode 4: IceProjectile 発射
 *   - Mode 6: AmethystProjectile 発射
 *   - Mode 7: LargeFireball (爆発半径 10) 発射
 *   - Mode 8: 自身を ~15m 上に launch + slow_falling
 *   - Mode 2/3/5: 左クリック特殊動作なし (Mode 2 passive / Mode 3 attack-time / Mode 5 right-click eat)
 */
public final class SecretSwordModeHandlers {

    private static final double MODE1_TP_DISTANCE = 32.0;
    private static final double MODE7_EXPLOSION_RADIUS = 10.0;
    private static final double MODE6_PROJECTILE_SPEED = 2.5;
    private static final double MODE4_PROJECTILE_SPEED = 1.8;
    private static final double MODE8_LAUNCH_Y = 1.55;
    private static final int MODE8_SLOW_FALL_TICKS = 200;
    private static final int PER_ACTION_DURABILITY = 1;

    private SecretSwordModeHandlers() {}

    public static void execute(ServerPlayer player, ItemStack stack, int mode) {
        switch (mode) {
            case 1 -> teleport(player, stack);
            case 4 -> throwIce(player, stack);
            case 6 -> shootAmethyst(player, stack);
            case 7 -> shootBigFireball(player, stack);
            case 8 -> launchSelf(player, stack);
            // 2, 3, 5: no left-click action
            default -> {}
        }
    }

    private static void damageStack(ServerPlayer player, ItemStack stack) {
        stack.hurtAndBreak(PER_ACTION_DURABILITY, player,
                p -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));
    }

    // ── Mode 1: TP ────────────────────────────────────────────────────

    private static void teleport(ServerPlayer player, ItemStack stack) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(MODE1_TP_DISTANCE));
        BlockHitResult hit = player.level().clip(new ClipContext(eye, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        Vec3 target;
        if (hit.getType() == HitResult.Type.MISS) {
            // 32m 先 (raycast ヒットなし = 空中 TP)
            target = end;
        } else {
            // ヒット位置の少し手前に立つ (= 壁にめり込まないよう look 方向に -1.0 オフセット)
            target = hit.getLocation().subtract(look.scale(1.0));
        }
        player.teleportTo(target.x, target.y, target.z);
        player.resetFallDistance();
        damageStack(player, stack);
    }

    // ── Mode 4: 氷ブロック投擲 ─────────────────────────────────────────

    private static void throwIce(ServerPlayer player, ItemStack stack) {
        IceProjectile ice = new IceProjectile(player.level(), player);
        Vec3 eye = player.getEyePosition();
        ice.setPos(eye.x, eye.y - 0.1, eye.z);
        Vec3 look = player.getLookAngle();
        ice.setDeltaMovement(look.scale(MODE4_PROJECTILE_SPEED));
        // shootFromRotation は inaccuracy 適用や視線同期で便利だが、 ここは直接 motion 指定で十分
        player.level().addFreshEntity(ice);
        damageStack(player, stack);
    }

    // ── Mode 6: アメジスト射出 ─────────────────────────────────────────

    private static void shootAmethyst(ServerPlayer player, ItemStack stack) {
        AmethystProjectile amethyst = new AmethystProjectile(
                ModEntities.AMETHYST_PROJECTILE.get(), player.level());
        amethyst.setShooter(player);
        Vec3 eye = player.getEyePosition();
        amethyst.setPos(eye.x, eye.y, eye.z);
        amethyst.shoot(player.getLookAngle(), MODE6_PROJECTILE_SPEED);
        player.level().addFreshEntity(amethyst);
        damageStack(player, stack);
    }

    // ── Mode 7: 大爆発ファイアチャージ ─────────────────────────────────

    private static void shootBigFireball(ServerPlayer player, ItemStack stack) {
        Vec3 look = player.getLookAngle();
        LargeFireball fireball = new LargeFireball(player.level(), player,
                look.x, look.y, look.z, (int) MODE7_EXPLOSION_RADIUS);
        Vec3 eye = player.getEyePosition();
        fireball.setPos(eye.x + look.x * 1.5, eye.y + look.y * 1.5, eye.z + look.z * 1.5);
        player.level().addFreshEntity(fireball);
        damageStack(player, stack);
    }

    // ── Mode 8: 自身を直上に launch ─────────────────────────────────────

    private static void launchSelf(ServerPlayer player, ItemStack stack) {
        Vec3 v = player.getDeltaMovement();
        player.setDeltaMovement(v.x, MODE8_LAUNCH_Y, v.z);
        player.hurtMarked = true;
        // 落下保護 (= ~10秒)、 落下中ダメージを完全に消す
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,
                MODE8_SLOW_FALL_TICKS, 0, false, false, true));
        player.resetFallDistance();
        damageStack(player, stack);
    }
}
