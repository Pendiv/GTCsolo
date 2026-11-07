package DIV.gtcsolo.integration.jei.performance;

import com.lowdragmc.lowdraglib.gui.widget.Widget;

/** Raw access must not trigger the public getter's promotion hook. */
public interface ModularWrapperWidgetAccess {
    Widget gtcsolo$getRawWrappedWidget();
    void gtcsolo$setRawWrappedWidget(Widget widget);
}
