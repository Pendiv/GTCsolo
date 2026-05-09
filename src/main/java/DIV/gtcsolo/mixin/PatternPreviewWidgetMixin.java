package DIV.gtcsolo.mixin;

import DIV.gtcsolo.api.tier.TieredBlockSet;
import DIV.gtcsolo.api.tier.TieredMultiblockRegistry;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.gui.widget.PatternPreviewWidget;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

/**
 * tiered マルチブロックの JEI プレビュー画面で、現行表示中の電圧tierを
 * P ボタンの上にラベル表示する。
 *
 * 安全策: {@link TieredMultiblockRegistry} に登録された definition のみ
 * ラベルを追加する。未登録 (= 通常の MB) は早期 return で何もしない。
 */
@Mixin(PatternPreviewWidget.class)
public abstract class PatternPreviewWidgetMixin {

    @Shadow @Final
    public MultiblockMachineDefinition controllerDefinition;

    @Shadow
    public int index;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void gtcsolo$injectTierLabel(MultiblockMachineDefinition def, CallbackInfo ci) {
        TieredBlockSet set = TieredMultiblockRegistry.get(this.controllerDefinition);
        if (set == null) return;

        // tier 番号配列 (TreeMap 由来で昇順)。shape index N → tiers[N] が voltage tier。
        Integer[] tiers = set.getTiers().toArray(new Integer[0]);
        if (tiers.length == 0) return;

        // P ボタン (138, 30, 18, 18) の上に小さい label を置く。タイトルが y=3..13 に
        // あるので y=16 から高さ 12 で配置 (P ボタンの直上)。
        Widget tierLabel = new ImageWidget(138, 16, 18, 12,
                (IGuiTexture) new GuiTextureGroup(new IGuiTexture[]{
                        ColorPattern.T_GRAY.rectTexture(),
                        new TextTexture("LV").setSupplier(() -> {
                            int idx = this.index;
                            if (idx < 0 || idx >= tiers.length) return "?";
                            // VNF は GT 純正色付き短縮名 (例: HV→§6HV)
                            int tier = tiers[idx];
                            if (tier < 0 || tier >= GTValues.VNF.length) return "?";
                            return GTValues.VNF[tier];
                        })
                }));

        // PatternPreviewWidget は WidgetGroup を継承
        ((WidgetGroup) (Object) this).addWidget(tierLabel);
    }
}
