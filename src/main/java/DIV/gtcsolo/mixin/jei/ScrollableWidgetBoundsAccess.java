package DIV.gtcsolo.mixin.jei;

import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = DraggableScrollableWidgetGroup.class, remap = false)
public interface ScrollableWidgetBoundsAccess {
    @Accessor("xBarHeight")
    int gtcsolo$xBarHeight();

    @Accessor("yBarWidth")
    int gtcsolo$yBarWidth();
}
