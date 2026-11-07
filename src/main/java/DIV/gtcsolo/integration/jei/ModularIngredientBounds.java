package DIV.gtcsolo.integration.jei;

import DIV.gtcsolo.mixin.jei.ScrollableWidgetBoundsAccess;

import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.Widget;

import net.minecraft.client.renderer.Rect2i;

/** Recipe-local bounds of the currently visible part of an LDLib ingredient slot. */
public final class ModularIngredientBounds {
    private ModularIngredientBounds() {}

    public static Rect2i visibleBounds(Widget widget) {
        if (widget.getParent() == null) return new Rect2i(0, 0, 0, 0);
        int left = widget.getPositionX(), top = widget.getPositionY();
        int right = left + widget.getSizeWidth(), bottom = top + widget.getSizeHeight();
        for (Widget current = widget; current != null; current = current.getParent()) {
            if (!current.isVisible() || !current.isActive()) return new Rect2i(0, 0, 0, 0);
            var parent = current.getParent();
            // LDLib retains the parent reference after removal; removed slots must not remain
            // clickable.
            if (parent != null && !parent.widgets.contains(current)) return new Rect2i(0, 0, 0, 0);
            if (current instanceof DraggableScrollableWidgetGroup scroll && scroll.isUseScissor()) {
                int xBar = 0, yBar = 0;
                if (scroll instanceof ScrollableWidgetBoundsAccess access) {
                    xBar = Math.max(0, access.gtcsolo$xBarHeight());
                    yBar = Math.max(0, access.gtcsolo$yBarWidth());
                }
                left = Math.max(left, scroll.getPositionX());
                top = Math.max(top, scroll.getPositionY());
                right =
                        Math.min(
                                right,
                                scroll.getPositionX() + Math.max(0, scroll.getSizeWidth() - yBar));
                bottom =
                        Math.min(
                                bottom,
                                scroll.getPositionY() + Math.max(0, scroll.getSizeHeight() - xBar));
            }
        }
        return new Rect2i(left, top, Math.max(0, right - left), Math.max(0, bottom - top));
    }
}
