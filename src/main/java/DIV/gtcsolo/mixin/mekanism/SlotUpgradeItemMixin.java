package DIV.gtcsolo.mixin.mekanism;

import DIV.gtcsolo.integration.mekanism.SlotUpgrade;
import mekanism.api.Upgrade;
import mekanism.common.util.UpgradeUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = UpgradeUtils.class, remap = false)
public abstract class SlotUpgradeItemMixin {
    @Inject(method = "getStack(Lmekanism/api/Upgrade;I)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true)
    private static void gtcsolo$upgradeStack(Upgrade upgrade, int count, CallbackInfoReturnable<ItemStack> result) {
        if (upgrade == SlotUpgrade.TYPE) {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("gtcsolo", "slot_upgrade"));
            result.setReturnValue(item == null ? ItemStack.EMPTY : new ItemStack(item, count));
        }
    }
}
