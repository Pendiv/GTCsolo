package DIV.gtcsolo.mixin.mekanism;

import DIV.gtcsolo.integration.mekanism.SlotUpgrade;
import mekanism.common.tier.FactoryTier;
import mekanism.common.tile.factory.TileEntityItemToItemFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = TileEntityItemToItemFactory.class, remap = false)
public abstract class FactoryItemLayoutMixin {
    @Redirect(method = {"addSlots"}, at = @At(value = "FIELD",
            target = "Lmekanism/common/tile/factory/TileEntityItemToItemFactory;tier:Lmekanism/common/tier/FactoryTier;"))
    private FactoryTier gtcsolo$layoutTier(TileEntityItemToItemFactory<?> factory) {
        return SlotUpgrade.layout(factory.tier);
    }
}
