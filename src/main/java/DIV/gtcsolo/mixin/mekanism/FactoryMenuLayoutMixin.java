package DIV.gtcsolo.mixin.mekanism;

import DIV.gtcsolo.integration.mekanism.SlotUpgrade;
import mekanism.common.inventory.container.tile.FactoryContainer;
import mekanism.common.tier.FactoryTier;
import mekanism.common.tile.factory.TileEntityFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = FactoryContainer.class, remap = false)
public abstract class FactoryMenuLayoutMixin {
    @Redirect(method = {"getInventoryXOffset"}, at = @At(value = "FIELD",
            target = "Lmekanism/common/tile/factory/TileEntityFactory;tier:Lmekanism/common/tier/FactoryTier;"))
    private FactoryTier gtcsolo$layoutTier(TileEntityFactory<?> factory) {
        return SlotUpgrade.layout(factory.tier);
    }
}
