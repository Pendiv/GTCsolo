package DIV.gtcsolo.integration.mekanism.capability;

import com.gregtechceu.gtceu.GTCEu;
import com.lowdragmc.lowdraglib.jei.JEIPlugin;
import com.lowdragmc.lowdraglib.gui.ingredient.IRecipeIngredientSlot;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.jei.ClickableIngredient;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import com.mojang.logging.LogUtils;
import mekanism.api.chemical.ChemicalStack;
import mekanism.client.jei.ChemicalStackRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * JEI recipe slot として Mek chemical を表示する Widget.
 *
 * 描画方針: Mek が JEI に登録してる ChemicalStackRenderer の **デフォルト設定**
 * (BUCKET_VOLUME=1000 capacity, 16×16, ITEM_LIST tooltip mode) と同じものを利用することで、
 * Mek のネイティブ JEI 表示と完全一致 (量に応じた fill + swirl pattern) させる.
 *
 * IRecipeIngredientSlot で click → recipe/usage 遷移も実現.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ChemicalStackWidget extends Widget implements IRecipeIngredientSlot {

    private static final Logger LOGGER = LogUtils.getLogger();

    private ChemicalStack<?> stack = mekanism.api.chemical.gas.GasStack.EMPTY;
    private IngredientIO ingredientIO = IngredientIO.INPUT;
    // Mek renderer: capacity=stack.getAmount() にして 100% fill 表示させる.
    // amount が変わったときだけ作り直す (per-frame allocation を避ける).
    @SuppressWarnings("rawtypes")
    private transient ChemicalStackRenderer cachedRenderer;
    private transient long cachedRendererCapacity = 0L;

    public ChemicalStackWidget() {
        super(0, 0, 18, 18); // standard slot size
    }

    public ChemicalStackWidget setStack(ChemicalStack<?> stack) {
        this.stack = stack == null ? mekanism.api.chemical.gas.GasStack.EMPTY : stack;
        long cap = Math.max(1L, this.stack.getAmount());
        if (cachedRenderer == null || cachedRendererCapacity != cap) {
            cachedRenderer = new ChemicalStackRenderer(cap, 16, 16);
            cachedRendererCapacity = cap;
        }
        return this;
    }

    public ChemicalStackWidget setIngredientIO(String ioLabel) {
        if ("OUT".equals(ioLabel)) this.ingredientIO = IngredientIO.OUTPUT;
        else if ("IN".equals(ioLabel)) this.ingredientIO = IngredientIO.INPUT;
        return this;
    }

    public ChemicalStack<?> getStack() {
        return stack;
    }

    // ---- IRecipeIngredientSlot 実装 (JEI 連携) ----

    /**
     * JEI の addJEISlot は getXEIIngredients の中身を IClickableIngredient でラップされてるものだけ拾う.
     * 生の GasStack/InfusionStack 等を渡しても無視されるため、必ず ClickableIngredient に変換する.
     *
     * Mek が GasStack.class 等を IIngredientType として登録済みなので、
     * createTypedIngredient(stack) は自動で TYPE_GAS 等に解決してくれる.
     */
    @Override
    public List<Object> getXEIIngredients() {
        if (stack == null || stack.isEmpty()) return Collections.emptyList();
        if (!GTCEu.Mods.isJEILoaded()) {
            return List.of(stack);
        }
        try {
            Position pos = getPosition();
            Size size = getSize();
            Optional<?> typedIngredient = JEIPlugin.jeiHelpers.getIngredientManager()
                    .createTypedIngredient((Object) stack);
            if (typedIngredient.isPresent()) {
                mezz.jei.api.ingredients.ITypedIngredient<?> typed =
                        (mezz.jei.api.ingredients.ITypedIngredient<?>) typedIngredient.get();
                @SuppressWarnings({"unchecked"})
                Object clickable = new ClickableIngredient(
                        typed, pos.x, pos.y, size.width, size.height);
                return List.of(clickable);
            }
            LOGGER.warn("[ChemCap] JEI createTypedIngredient returned empty for stack={}",
                    stack.getTypeRegistryName());
        } catch (Throwable t) {
            LOGGER.warn("[ChemCap] getXEIIngredients JEI wrap failed: {}", t.toString(), t);
        }
        return Collections.emptyList();
    }

    @Override
    public Object getXEICurrentIngredient() {
        return stack == null || stack.isEmpty() ? null : stack;
    }

    @Override
    public Object getXEIIngredientOverMouse(double mouseX, double mouseY) {
        if (isMouseOverElement(mouseX, mouseY)) {
            return getXEICurrentIngredient();
        }
        return null;
    }

    @Override
    public IngredientIO getIngredientIO() {
        return ingredientIO;
    }

    // ---- drawInBackground: Mek ChemicalStackRenderer を直接使用 (capacity=amount で 100% fill) ----
    //
    // JEI IDrawable 経由だと default renderer = capacity=BUCKET_VOLUME (1000mb) が使われ、
    // amount=10 などの小さい値で 1% 高さ (1px) しか描画されず見えない.
    // Mek registered renderer 自体を直接 capacity=stack.getAmount() で構築し、比率=1.0 で full render.

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        if (stack == null || stack.isEmpty() || cachedRenderer == null) return;
        int x = getPosition().x + 1;
        int y = getPosition().y + 1;
        try {
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            cachedRenderer.render(graphics, stack);
            graphics.pose().popPose();
        } catch (Throwable t) {
            LOGGER.warn("[ChemCap] drawInBackground render failed: {}", t.toString());
        }
    }

    // ---- drawInForeground: tooltip 描画 (JEI $2 経路が発火しないため自前で描画) ----

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInForeground(graphics, mouseX, mouseY, partialTicks);
        if (stack == null || stack.isEmpty() || cachedRenderer == null) return;
        if (!isMouseOverElement(mouseX, mouseY)) return;
        try {
            TooltipFlag flag = Minecraft.getInstance().options.advancedItemTooltips
                    ? TooltipFlag.Default.ADVANCED
                    : TooltipFlag.Default.NORMAL;
            @SuppressWarnings({"rawtypes", "unchecked"})
            List<Component> tooltip = cachedRenderer.getTooltip(stack, flag);
            if (!tooltip.isEmpty()) {
                graphics.renderComponentTooltip(
                        Minecraft.getInstance().font, tooltip, mouseX, mouseY);
            }
        } catch (Throwable t) {
            LOGGER.warn("[ChemCap] drawInForeground tooltip render failed: {}", t.toString());
        }
    }

    // click-through: LDLib の ClickableIngredient 経由は JEI の hit test に届かないため、
    // widget が click を受けたら直接 JEI runtime を呼んで recipe/usage gui を開く.
    // 挙動は Fluid/Item の JEI native 挙動と同一:
    //   Left click  → IFocus(OUTPUT)  = この chemical を「作る」レシピ一覧
    //   Right click → IFocus(INPUT)   = この chemical を「使う」レシピ一覧 (usages)
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (stack == null || stack.isEmpty() || !isMouseOverElement(mouseX, mouseY)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (!GTCEu.Mods.isJEILoaded() || (button != 0 && button != 1)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        try {
            mezz.jei.api.recipe.RecipeIngredientRole role = (button == 0)
                    ? mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT
                    : mezz.jei.api.recipe.RecipeIngredientRole.INPUT;
            Optional<?> typedIngredient = JEIPlugin.jeiHelpers.getIngredientManager()
                    .createTypedIngredient((Object) stack);
            if (typedIngredient.isPresent()) {
                @SuppressWarnings({"unchecked", "rawtypes"})
                mezz.jei.api.recipe.IFocus<?> focus = JEIPlugin.jeiHelpers.getFocusFactory()
                        .createFocus(role, (mezz.jei.api.ingredients.ITypedIngredient) typedIngredient.get());
                JEIPlugin.jeiRuntime.getRecipesGui().show(focus);
                return true;
            } else {
                LOGGER.warn("[ChemCap] mouseClicked could not create typed ingredient for {}",
                        stack.getTypeRegistryName());
            }
        } catch (Throwable t) {
            LOGGER.warn("[ChemCap] mouseClicked JEI focus show failed: {}", t.toString(), t);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

}