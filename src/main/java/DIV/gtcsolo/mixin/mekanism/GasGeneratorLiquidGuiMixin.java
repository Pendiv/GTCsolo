package DIV.gtcsolo.mixin.mekanism;

import DIV.gtcsolo.integration.mekanism.generator.GasGeneratorUpgrades;
import DIV.gtcsolo.integration.mekanism.generator.client.GasGeneratorGauges;

import mekanism.api.chemical.gas.IGasTank;
import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiGasGauge;
import mekanism.client.gui.element.slot.GuiSlot;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.tile.base.TileEntityMekanism;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Supplier;

@Pseudo
@Mixin(targets = "mekanism.generators.client.gui.GuiGasGenerator", remap = false)
public abstract class GasGeneratorLiquidGuiMixin
        extends GuiMekanismTile<TileEntityMekanism, MekanismTileContainer<TileEntityMekanism>> {
    protected GasGeneratorLiquidGuiMixin(
            MekanismTileContainer<TileEntityMekanism> container,
            Inventory inventory,
            Component title) {
        super(container, inventory, title);
    }

    @Redirect(
            method = "addGuiElements",
            at = @At(value = "NEW", target = "mekanism/client/gui/element/gauge/GuiGasGauge"),
            remap = false)
    private GuiGasGauge gtcsolo$gasGauge(
            Supplier<IGasTank> tank,
            Supplier<List<IGasTank>> tanks,
            GaugeType type,
            IGuiWrapper gui,
            int x,
            int y) {
        return GasGeneratorUpgrades.isTarget(tile)
                ? GasGeneratorGauges.gas(tank, tanks, GaugeType.SMALL_MED, gui, x, y)
                : new GuiGasGauge(tank, tanks, type, gui, x, y);
    }

    @Inject(method = "addGuiElements", at = @At("TAIL"), remap = false)
    private void gtcsolo$addLiquidGauge(CallbackInfo callback) {
        if (!GasGeneratorUpgrades.isTarget(tile)) return;
        addRenderableWidget(
                GasGeneratorGauges.fluid(
                        () -> tile.getFluidTanks(null).get(0),
                        () -> tile.getFluidTanks(null),
                        GaugeType.SMALL_MED,
                        this,
                        99,
                        18));
        for (var element : renderables) {
            if (element instanceof GuiSlot slot
                    && slot.getRelativeX() == 76
                    && slot.getRelativeY() == 34) {
                slot.hover(
                        (widget, graphics, mouseX, mouseY) -> {
                            var rotor =
                                    DIV.gtcsolo.integration.mekanism.generator.GasGeneratorRotor.of(
                                            tile);
                            if (rotor != null && rotor.slot().isEmpty()) {
                                widget.displayTooltips(
                                        graphics,
                                        mouseX,
                                        mouseY,
                                        Component.translatable("gtcsolo.gas_generator.rotor_slot"));
                            }
                        });
            }
        }
    }

    @ModifyVariable(method = "drawForegroundText", at = @At("STORE"), ordinal = 0, remap = false)
    private Component gtcsolo$fuelBurnRateLabel(Component original) {
        if (!GasGeneratorUpgrades.isTarget(tile)) return original;
        // The native value now covers either fuel, so keep its number and use a shared label.
        return original.getContents()
                        instanceof net.minecraft.network.chat.contents.TranslatableContents text
                ? Component.translatable("gtcsolo.gas_generator.burn_rate", text.getArgs())
                : original;
    }
}
