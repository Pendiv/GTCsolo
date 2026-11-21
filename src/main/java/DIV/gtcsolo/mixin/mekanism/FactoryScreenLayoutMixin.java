package DIV.gtcsolo.mixin.mekanism;

import DIV.gtcsolo.integration.mekanism.SlotUpgrade;
import mekanism.client.gui.machine.GuiFactory;
import mekanism.common.tier.FactoryTier;
import mekanism.common.tile.factory.TileEntityFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = GuiFactory.class, remap = false)
public abstract class FactoryScreenLayoutMixin {
    @Redirect(method = {"<init>", "addGuiElements"}, at = @At(value = "FIELD",
            target = "Lmekanism/common/tile/factory/TileEntityFactory;tier:Lmekanism/common/tier/FactoryTier;"))
    private FactoryTier gtcsolo$layoutTier(TileEntityFactory<?> factory) {
        return SlotUpgrade.layout(factory.tier);
    }
}
