package DIV.gtcsolo.torcherino.client;

import DIV.gtcsolo.torcherino.BudgetScreen;
import net.minecraft.client.gui.components.AbstractSliderButton;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

public final class SliderBindings {
    private record Binding(WeakReference<BudgetScreen> screen, int field) {}
    private static final Map<AbstractSliderButton, Binding> BINDINGS = new WeakHashMap<>();
    private SliderBindings() {}
    public static void bind(AbstractSliderButton slider, BudgetScreen screen, int field) {
        BINDINGS.put(slider, new Binding(new WeakReference<>(screen), field));
    }
    public static void reconcile(AbstractSliderButton slider) {
        Binding binding = BINDINGS.get(slider);
        if (binding == null) return;
        BudgetScreen screen = binding.screen.get();
        if (screen == null) return;
        double value = screen.tiered$afterSliderEdit(binding.field);
        if (Double.isNaN(value)) return;
        SliderAccess access = (SliderAccess) slider;
        access.tiered$setValue(value);
        access.tiered$updateMessage();
    }
}
