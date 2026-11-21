package DIV.gtcsolo.mixin.mekanism;

import DIV.gtcsolo.integration.mekanism.SlotUpgrade;
import mekanism.common.tier.FactoryTier;
import mekanism.common.tile.factory.TileEntitySawingFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = TileEntitySawingFactory.class, remap = false)
public abstract class FactorySawLayoutMixin {
    @Redirect(method = {"addSlots"}, at = @At(value = "FIELD",
            target = "Lmekanism/common/tile/factory/TileEntitySawingFactory;tier:Lmekanism/common/tier/FactoryTier;"))
    private FactoryTier gtcsolo$layoutTier(TileEntitySawingFactory factory) {
        return SlotUpgrade.layout(factory.tier);
    }
}
