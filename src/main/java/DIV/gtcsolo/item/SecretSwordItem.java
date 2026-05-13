package DIV.gtcsolo.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * secret_sword: 8 モード切替可能なソード。
 *
 *  Mode 1: TP — 視線方向 raycast ヒット地点に、 ヒット無ければ 32m 先に
 *  Mode 2: 装備中は永続的に火炎耐性 (10秒分を tick ごとに refresh)
 *  Mode 3: 通常近接攻撃時に上方へ ~15m 吹き飛ばし (hurtEnemy で実装)
 *  Mode 4: 氷ブロック投擲 (IceProjectile): 着弾でブロック設置、 mob 命中で Slow 3 / 120t + 20 ダメージ
 *  Mode 5: 食べると満腹度 3/3 + 耐久値 -10
 *  Mode 6: アメジスト射出 (AmethystProjectile): 32m 飛翔、 地形・モブ貫通、 ヒット mob 20 ダメージ
 *  Mode 7: ファイアチャージ射出: 爆発半径 10
 *  Mode 8: 自身が直上 ~15m に打ち上がり、 slow_falling 10秒で着地保護
 *
 *  shift+左クリックで mode 切替 (1→2→...→8→1)
 *  耐久値消費: Mode 5 は -10、 他は使用ごと -1
 */
public class SecretSwordItem extends SwordItem {

    public static final int MAX_DAMAGE = 6200;
    public static final int MIN_MODE = 1;
    public static final int MAX_MODE = 8;
    public static final String MODE_TAG = "secret_sword_mode";

    public static final int MODE5_FOOD_NUTRITION = 3;
    public static final float MODE5_FOOD_SATURATION = 3.0f;
    public static final int MODE5_DURABILITY_COST = 10;

    public SecretSwordItem(Properties properties) {
        // durability(N) は内部で stackSize=1 をセットする。 stacksTo(1) を明示すると
        // "Unable to have damage AND stack" RuntimeException で起動こける。
        super(Tiers.NETHERITE, 3, -2.4F, properties.durability(MAX_DAMAGE));
    }

    public static int getMode(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(MODE_TAG)) return MIN_MODE;
        int mode = tag.getInt(MODE_TAG);
        if (mode < MIN_MODE || mode > MAX_MODE) return MIN_MODE;
        return mode;
    }

    public static void setMode(ItemStack stack, int mode) {
        if (mode < MIN_MODE) mode = MAX_MODE;
        if (mode > MAX_MODE) mode = MIN_MODE;
        stack.getOrCreateTag().putInt(MODE_TAG, mode);
    }

    /** 次の mode に進めて、 新しい mode 値を返す */
    public static int cycleMode(ItemStack stack) {
        int next = (getMode(stack) % MAX_MODE) + 1;
        setMode(stack, next);
        return next;
    }

    // ── Mode 2: 装備中の継続効果 ────────────────────────────────────────

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || !selected) return;
        if (!(entity instanceof Player player)) return;
        if (getMode(stack) != 2) return;
        // 火炎耐性 10秒 (= 200 tick) を毎 tick refresh。 amplifier 0、 hidden アイコン、 効果のみ
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0, false, false, true));
    }

    // ── Mode 5: 食べる ─────────────────────────────────────────────

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (getMode(stack) == 5 && player.canEat(false)) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        return super.use(level, player, hand);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return getMode(stack) == 5 ? 32 : super.getUseDuration(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return getMode(stack) == 5 ? UseAnim.EAT : super.getUseAnimation(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (getMode(stack) == 5 && entity instanceof Player player) {
            if (!level.isClientSide) {
                player.getFoodData().eat(MODE5_FOOD_NUTRITION, MODE5_FOOD_SATURATION);
                stack.hurtAndBreak(MODE5_DURABILITY_COST, player,
                        e -> e.broadcastBreakEvent(player.getUsedItemHand()));
            }
            return stack;
        }
        return super.finishUsingItem(stack, level, entity);
    }

    // ── Mode 3: 攻撃時の上方吹き飛ばし ────────────────────────────────────

    /**
     * 対象を上方 ~15m に吹き飛ばす。 v = 1.55 → max height ≈ 15.0 m (= v² / (2*g)、 g=0.08)。
     * 秘剣 Mode 3 と TropicalCandyItem (= キャンディの近接攻撃) で共有。
     */
    public static void applyKnockup(LivingEntity target) {
        target.setDeltaMovement(target.getDeltaMovement().x, 1.55, target.getDeltaMovement().z);
        target.hurtMarked = true;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean dealt = super.hurtEnemy(stack, target, attacker);
        if (getMode(stack) == 3) {
            applyKnockup(target);
        }
        return dealt;
    }

    // ── Tooltip & enchant gold ──────────────────────────────────────

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        int mode = getMode(stack);
        tooltip.add(Component.translatable("item.gtcsolo.secret_sword.mode", mode).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.gtcsolo.secret_sword.mode." + mode).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.gtcsolo.secret_sword.cycle_hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
