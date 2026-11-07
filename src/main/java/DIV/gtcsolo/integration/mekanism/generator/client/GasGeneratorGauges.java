package DIV.gtcsolo.integration.mekanism.generator.client;

import mekanism.api.chemical.gas.IGasTank;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiFluidGauge;
import mekanism.client.gui.element.gauge.GuiGasGauge;
import mekanism.common.MekanismLang;
import mekanism.common.util.text.TextUtils;

import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Supplier;

/** Native gauge interactions with content-dependent, single-line tooltips. */
public final class GasGeneratorGauges {
    private GasGeneratorGauges() {}

    public static GuiGasGauge gas(
            Supplier<IGasTank> tank,
            Supplier<List<IGasTank>> tanks,
            GaugeType type,
            IGuiWrapper gui,
            int x,
            int y) {
        return new GasGauge(tank, tanks, type, gui, x, y);
    }

    public static GuiFluidGauge fluid(
            Supplier<IExtendedFluidTank> tank,
            Supplier<List<IExtendedFluidTank>> tanks,
            GaugeType type,
            IGuiWrapper gui,
            int x,
            int y) {
        return new FluidGauge(tank, tanks, type, gui, x, y);
    }

    private static final class GasGauge extends GuiGasGauge {
        private GasGauge(
                Supplier<IGasTank> tank,
                Supplier<List<IGasTank>> tanks,
                GaugeType type,
                IGuiWrapper gui,
                int x,
                int y) {
            super(tank, tanks, type, gui, x, y);
        }

        @Override
        public Component getLabel() {
            return null;
        }

        @Override
        public List<Component> getTooltipText() {
            var tank = getTank();
            if (tank == null || tank.isEmpty()) {
                return List.of(Component.translatable("gtcsolo.gas_generator.gas_slot"));
            }
            return List.of(
                    tank.getStored() == Long.MAX_VALUE
                            ? MekanismLang.GENERIC_STORED.translate(
                                    tank.getType(), MekanismLang.INFINITE)
                            : MekanismLang.GENERIC_STORED_MB.translate(
                                    tank.getType(), TextUtils.format(tank.getStored())));
        }
    }

    private static final class FluidGauge extends GuiFluidGauge {
        private FluidGauge(
                Supplier<IExtendedFluidTank> tank,
                Supplier<List<IExtendedFluidTank>> tanks,
                GaugeType type,
                IGuiWrapper gui,
                int x,
                int y) {
            super(tank, tanks, type, gui, x, y);
        }

        @Override
        public Component getLabel() {
            return null;
        }

        @Override
        public List<Component> getTooltipText() {
            var tank = getTank();
            if (tank == null || tank.isEmpty()) {
                return List.of(Component.translatable("gtcsolo.gas_generator.fluid_slot"));
            }
            return List.of(
                    tank.getFluidAmount() == Integer.MAX_VALUE
                            ? MekanismLang.GENERIC_STORED.translate(
                                    tank.getFluid(), MekanismLang.INFINITE)
                            : MekanismLang.GENERIC_STORED_MB.translate(
                                    tank.getFluid(), TextUtils.format(tank.getFluidAmount())));
        }
    }
}
