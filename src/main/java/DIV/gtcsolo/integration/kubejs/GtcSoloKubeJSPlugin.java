package DIV.gtcsolo.integration.kubejs;

import DIV.gtcsolo.integration.mekanism.capability.ChemicalPartAbilities;
import DIV.gtcsolo.integration.mekanism.capability.ChemicalRecipeComponents;
import DIV.gtcsolo.item.AbstractLocusItem;
import DIV.gtcsolo.machine.starforge.PhaseProgressionTable;
import DIV.gtcsolo.machine.starforge.StarForgeTraceData;
import com.mojang.logging.LogUtils;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.recipe.schema.RecipeComponentFactoryRegistryEvent;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import org.slf4j.Logger;

/**
 * GTCsoloEU の KubeJS 拡張 entry point.
 * - RecipeComponent 登録 (ChemicalIngredient 用)
 * - Bindings 追加 (KubeJS スクリプト内で ChemicalPartAbilities 等を参照可能に)
 * 登録ファイル: src/main/resources/kubejs.plugins.txt
 */
public class GtcSoloKubeJSPlugin extends KubeJSPlugin {

    private static final Logger LOGGER = LogUtils.getLogger();

    public GtcSoloKubeJSPlugin() {
        LOGGER.info("[ChemCap] KubeJSPlugin constructed");
    }

    @Override
    public void registerRecipeComponents(RecipeComponentFactoryRegistryEvent event) {
        LOGGER.info("[ChemCap] KubeJSPlugin.registerRecipeComponents invoked");
        ChemicalRecipeComponents.registerRecipeComponents(event);
    }

    @Override
    public void registerBindings(BindingsEvent event) {
        LOGGER.info("[ChemCap] KubeJSPlugin.registerBindings invoked");
        event.add("GtcsoloChemicalPartAbilities", ChemicalPartAbilities.class);
        event.add("GtcsoloChemCapHelper", ChemCapKJSHelper.class);

        // StarForge 系 (Phase A 露出)。
        // 既存軌跡編集は GtcsoloStarForge.get(trace).toBuilder().xxx(...).build() → GtcsoloStarForge.register(info)。
        // 新規追加は GtcsoloStarLocus.registerTrace(name) + GtcsoloStarForge.register(builder.build())。
        // Builder / Kind / TraceInfo は nested class 経由 (GtcsoloStarForgeTraceInfo.builder, .Kind)。
        event.add("GtcsoloStarForge", StarForgeTraceData.class);
        event.add("GtcsoloStarForgeTraceInfo", StarForgeTraceData.TraceInfo.class);
        event.add("GtcsoloStarForgeOrdering", StarForgeTraceData.OrderingHint.class);
        event.add("GtcsoloStarForgePhaseTable", PhaseProgressionTable.class);
        event.add("GtcsoloStarLocus", AbstractLocusItem.class);
    }
}