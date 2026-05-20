package DIV.gtcsolo.mixin;

import DIV.gtcsolo.l2.SoulDestructionGate;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Soul Destruction trait のための {@code LivingEntity.checkTotemDeathProtection} 介入。
 *
 * <p>1.20.1 では totem 判定は LivingEntity.checkTotemDeathProtection (private) にある
 * (= Player ではない、 1.21 で移動した可能性あり)。 mixin で HEAD 横取り、 攻撃者が
 * SoulDestruction trait 持ちなら false 返して totem 発火を skip。
 */
@Mixin(LivingEntity.class)
public abstract class PlayerTotemMixin {

    @Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)
    private void gtcsolo$skipTotemBySoulDestruction(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (!(source.getEntity() instanceof LivingEntity attacker)) return;
        if (SoulDestructionGate.shouldBypassTotem(attacker)) {
            cir.setReturnValue(false);
        }
    }
}
