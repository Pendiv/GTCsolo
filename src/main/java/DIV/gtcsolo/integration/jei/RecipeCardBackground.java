package DIV.gtcsolo.integration.jei;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.IntSupplier;

/** Paint the full JEI card while preserving GT's centering offset for its children. */
public final class RecipeCardBackground implements IGuiTexture {
    private final IGuiTexture texture;
    private final IntSupplier contentOffset;

    public RecipeCardBackground(IGuiTexture texture, IntSupplier contentOffset) {
        this.texture = texture;
        this.contentOffset = contentOffset;
    }

    @Override
    public void draw(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, int width, int height) {
        texture.draw(graphics, mouseX, mouseY, x - contentOffset.getAsInt(), y, width, height);
    }

    @Override
    public void updateTick() {
        texture.updateTick();
    }
}
