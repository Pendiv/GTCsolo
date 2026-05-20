package DIV.gtcsolo.mixin;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 全 FoodProperties 持ちアイテム (= バニラ + MOD 食料) を 1 tick で食べ切れるようにする。
 *
 * <p>{@link Item#getUseDuration(ItemStack)} を HEAD で横取りし、 食べ物なら 1 を返す。
 * 食事のエフェクト (satiation/buff/etc) は使い切り時に発火するので、 1 tick でも全部適用される。
 *
 * <p>適用外: FoodProperties を使わない独自食料 (= 自前 getUseDuration override)、 ポーション (= 飲み物扱い)、
 * ケーキ等のブロック食料 (= use animation を使わない経路)。
 */
@Mixin(Item.class)
public abstract class InstantEatMixin {

    @Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
    private void gtcsolo$instantEat(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (stack.getItem().isEdible()) {
            cir.setReturnValue(1);
        }
    }
}
