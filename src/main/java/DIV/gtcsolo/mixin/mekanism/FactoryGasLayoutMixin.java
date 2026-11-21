package DIV.gtcsolo.mixin.mekanism;

import DIV.gtcsolo.integration.mekanism.SlotUpgrade;
import mekanism.common.tier.FactoryTier;
import mekanism.common.tile.factory.TileEntityItemStackGasToItemStackFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = TileEntityItemStackGasToItemStackFactory.class, remap = false)
public abstract class FactoryGasLayoutMixin {
    @Redirect(method = {"<init>", "load", "m_142466_"}, at = @At(value = "FIELD",
            target = "Lmekanism/common/tile/factory/TileEntityItemStackGasToItemStackFactory;tier:Lmekanism/common/tier/FactoryTier;"))
    private FactoryTier gtcsolo$layoutTier(TileEntityItemStackGasToItemStackFactory factory) {
        return SlotUpgrade.layout(factory.tier);
    }
}
