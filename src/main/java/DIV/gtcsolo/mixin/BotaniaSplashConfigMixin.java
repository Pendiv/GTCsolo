package DIV.gtcsolo.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Botania 1.20.1-452-FORGE の dev env 限定 crash 回避。
 *
 * <p>ForgeBotaniaConfig$Client.splashesEnabled() が config load 完了前 (= SplashManager
 * の reload phase) に呼ばれて、 ConfigValue.get() が IllegalStateException
 * ("Cannot get config value before config is loaded") を投げて起動失敗する。
 *
 * <p>原因経路: botania_xplat.mixins.json の SplashManagerMixin が
 * SplashManager.addSplashes に inject、 reload number 1 の初期段階で
 * splashesEnabled() を読みに行く。 この時点で ForgeBotaniaConfig$Client の
 * ConfigValue は spec 構築済だが acceptConfig (= 実値 load) 未実行。
 *
 * <p>対処: method 本体実行を完全 bypass、 常に true (= default、 splashes 有効) を返す。
 * 副作用: user が splashes config を false にしても効かない、 常時 splashes ON。
 * → splashes off にしたい場合は別途 Mixin 精緻化 (= @WrapOperation で ConfigValue.get
 *   panic catch 等) が要るが、 crash 回避優先で簡易対処。
 *
 * <p>当初は @Redirect で ConfigValue.get() を wrap しようとしたが、 bytecode 上の
 * invoke target が BooleanValue.get() (= define() 戻り値の subclass) で resolve され、
 * "Critical injection failure ... failed injection check" でこける。
 * @Inject HEAD cancellable は signature が緩いので確実に当たる。
 *
 * <p>@Pseudo + remap=false で他 mod 内部 class への Mixin 明示。
 */
@Pseudo
@Mixin(targets = "vazkii.botania.forge.ForgeBotaniaConfig$Client", remap = false)
public class BotaniaSplashConfigMixin {

    @Inject(
            method = "splashesEnabled",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void gtcsolo$bypassConfigCheck(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }
}
