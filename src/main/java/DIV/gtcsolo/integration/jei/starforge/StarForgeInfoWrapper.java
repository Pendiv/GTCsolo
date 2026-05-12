package DIV.gtcsolo.integration.jei.starforge;

import DIV.gtcsolo.machine.starforge.StarForgeTraceData;

/**
 * StarForge 自作 JEI ページの 1 軌跡分のデータ wrapper。
 * Category.draw が hold データを参照して描画する。
 */
public final class StarForgeInfoWrapper {
    public final StarForgeTraceData.TraceInfo info;

    public StarForgeInfoWrapper(StarForgeTraceData.TraceInfo info) {
        this.info = info;
    }
}
