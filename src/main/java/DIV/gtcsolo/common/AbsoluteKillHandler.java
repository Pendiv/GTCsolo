package DIV.gtcsolo.common;

import DIV.gtcsolo.registry.ModEnchantments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class AbsoluteKillHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        Entity target = event.getTarget();

        if (player.level().isClientSide) return;
        if (!(target instanceof LivingEntity victim)) return;
        if (victim instanceof ArmorStand) return;

        ItemStack weapon = player.getMainHandItem();
        if (EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.ABSOLUTE_KILL.get(), weapon) <= 0) return;

        event.setCanceled(true);
        absoluteKill(victim, player.damageSources().playerAttack(player));
    }

    public static void absoluteKill(LivingEntity victim, DamageSource source) {
        if (victim.level().isClientSide) return;
        if (victim instanceof Player p && p.isCreative()) return;

        // 防御層を全て剥がす
        victim.invulnerableTime = 0;
        victim.setAbsorptionAmount(0);
        removeTotem(victim);

        // HP直接ゼロ
        victim.setHealth(0);

        // 正規死亡処理（ドロップ・統計・死亡メッセージ）
        victim.die(source);

        // それでも生きていたら強制処理
        if (!victim.isDeadOrDying()) {
            victim.setHealth(0);
            victim.kill();
        }
        if (!victim.isRemoved()) {
            victim.discard();
        }
    }

    private static void removeTotem(LivingEntity entity) {
        if (entity.getMainHandItem().is(Items.TOTEM_OF_UNDYING)) {
            entity.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
        if (entity.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
            entity.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }
    }
}