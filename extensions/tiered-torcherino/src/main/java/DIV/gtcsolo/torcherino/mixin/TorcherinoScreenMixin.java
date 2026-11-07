package DIV.gtcsolo.torcherino.mixin;

import DIV.gtcsolo.torcherino.Budget;
import DIV.gtcsolo.torcherino.BudgetScreen;
import DIV.gtcsolo.torcherino.Profile;
import DIV.gtcsolo.torcherino.client.SliderBindings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import torcherino.api.TorcherinoAPI;
import torcherino.client.screen.TorcherinoScreen;
import torcherino.client.screen.widgets.GradatedSliderWidget;

@Mixin(value = TorcherinoScreen.class, remap = false)
public abstract class TorcherinoScreenMixin extends Screen implements BudgetScreen {
    @Shadow private int xRange;
    @Shadow private int yRange;
    @Shadow private int zRange;
    @Shadow private int speed;
    @Shadow private int left;
    @Shadow private int top;
    @Unique private int tiered$power = -1;
    @Unique private Budget.State tiered$accepted;
    protected TorcherinoScreenMixin(Component title) { super(title); }
    @Inject(method = "<init>", at = @At("TAIL"))
    private void tiered$initialize(Component title, int x, int z, int y, int speed, int mode,
                                   BlockPos pos, ResourceLocation id, CallbackInfo ci) {
        if (Profile.from(id) == null) return;
        tiered$power = TorcherinoAPI.INSTANCE.getTier(id).maxSpeed() - 1;
        tiered$accepted = Budget.normalize(new Budget.State(xRange, yRange, zRange, this.speed), tiered$power);
        tiered$write(tiered$accepted);
    }
    @Inject(method = {"init", "m_7856_"}, at = @At("TAIL"))
    private void tiered$bindWidgets(CallbackInfo ci) {
        if (tiered$power < 0) return;
        int field = 0;
        for (var child : children()) {
            if (child instanceof GradatedSliderWidget slider) SliderBindings.bind(slider, this, field++);
        }
        if (field != 4) throw new IllegalStateException("Expected four Torcherino allocation sliders");
    }
    @Unique private void tiered$write(Budget.State state) {
        xRange = state.x(); yRange = state.y(); zRange = state.z(); speed = state.speed();
    }
    @Override public double tiered$afterSliderEdit(int field) {
        if (tiered$power < 0) return Double.NaN;
        tiered$accepted = Budget.edit(tiered$accepted, new Budget.State(xRange, yRange, zRange, speed), field, tiered$power);
        tiered$write(tiered$accepted);
        int value = switch (field) { case 0 -> speed - 1; case 1 -> xRange; case 2 -> zRange; case 3 -> yRange;
            default -> throw new IllegalArgumentException("Unknown slider"); };
        int limit = switch (field) { case 0 -> tiered$power; case 1, 2 -> Budget.MAX_XZ_RADIUS;
            case 3 -> Budget.MAX_Y_RADIUS; default -> throw new IllegalArgumentException("Unknown slider"); };
        return (double) value / limit;
    }
    @Inject(method = {"render", "m_88315_"}, at = @At("TAIL"))
    private void tiered$showBudget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (tiered$power < 0) return;
        long used = new Budget.State(xRange, yRange, zRange, speed).spent();
        graphics.drawString(font, Component.translatable("gtcsolo_torcherino.usage", used, tiered$power),
                left + 8, Math.max(3, top - 12), 0xFFE090, false);
    }
}
