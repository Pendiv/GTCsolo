package DIV.gtcsolo.mixin.mekanism;

import DIV.gtcsolo.integration.mekanism.SlotUpgrade;
import java.util.Arrays;
import mekanism.api.Upgrade;
import mekanism.api.text.APILang;
import mekanism.api.text.EnumColor;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Upgrade.class, priority = 1200, remap = false)
public abstract class SlotUpgradeTypeMixin {
    @Shadow @Mutable @Final private static Upgrade[] $VALUES;
    @Shadow @Mutable @Final private static Upgrade[] UPGRADES;

    @Invoker("<init>")
    public static Upgrade gtcsolo$newUpgrade(String enumName, int ordinal, String name, APILang lang,
                                               APILang description, int maximum, EnumColor color) {
        throw new AssertionError();
    }

    // Append after Evolved Mekanism. Existing upgrade ordinals are stored in worlds.
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void gtcsolo$appendSlotUpgrade(CallbackInfo ci) {
        Upgrade slot = gtcsolo$newUpgrade("GTCSOLO_SLOT", $VALUES.length, "slot", APILang.UPGRADE_SPEED,
                APILang.UPGRADE_SPEED_DESCRIPTION, SlotUpgrade.MAX, EnumColor.AQUA);
        $VALUES = Arrays.copyOf($VALUES, $VALUES.length + 1);
        $VALUES[slot.ordinal()] = slot;
        UPGRADES = $VALUES;
    }

    @Inject(method = "getTranslationKey", at = @At("HEAD"), cancellable = true)
    private void gtcsolo$name(CallbackInfoReturnable<String> result) {
        if ((Object) this == SlotUpgrade.TYPE) result.setReturnValue("upgrade.gtcsolo.slot");
    }

    @Inject(method = "getDescription", at = @At("HEAD"), cancellable = true)
    private void gtcsolo$description(CallbackInfoReturnable<Component> result) {
        if ((Object) this == SlotUpgrade.TYPE) result.setReturnValue(Component.translatable("upgrade.gtcsolo.slot.description"));
    }
}
