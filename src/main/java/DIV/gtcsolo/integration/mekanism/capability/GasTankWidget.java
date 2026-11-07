package DIV.gtcsolo.integration.mekanism.capability;

import mekanism.api.chemical.gas.GasStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/** Read-only gas display, synchronized while the machine UI is open. */
public final class GasTankWidget extends ChemicalStackWidget {
    private static final int CONTENT_UPDATE = 1;
    private final Supplier<GasStack> source;
    private final long capacity;
    private GasStack lastSent = GasStack.EMPTY;

    public GasTankWidget(Supplier<GasStack> source, long capacity) {
        this.source = source;
        this.capacity = capacity;
    }

    @Override
    public void writeInitialData(FriendlyByteBuf buffer) {
        super.writeInitialData(buffer);
        lastSent = source.get().copy();
        lastSent.writeToPacket(buffer);
    }

    @Override
    public void readInitialData(FriendlyByteBuf buffer) {
        super.readInitialData(buffer);
        setStack(GasStack.readFromPacket(buffer));
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        GasStack current = source.get();
        if (!current.isStackIdentical(lastSent)) {
            lastSent = current.copy();
            writeUpdateInfo(CONTENT_UPDATE, lastSent::writeToPacket);
        }
    }

    @Override
    public void readUpdateInfo(int id, FriendlyByteBuf buffer) {
        if (id == CONTENT_UPDATE) setStack(GasStack.readFromPacket(buffer));
        else super.readUpdateInfo(id, buffer);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (!isMouseOverElement(mouseX, mouseY)) return;
        Component name = getStack().isEmpty() ? Component.translatable("gtceu.fluid.empty")
                : getStack().getTextComponent();
        Component amount = Component.translatable("gtcsolo.gui.multi_chemical_bus.gas_amount",
                String.format(Locale.ROOT, "%,d", getStack().getAmount()),
                String.format(Locale.ROOT, "%,d", capacity));
        graphics.renderComponentTooltip(Minecraft.getInstance().font, List.of(name, amount), mouseX, mouseY);
    }
}
