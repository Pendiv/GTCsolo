package DIV.gtcsolo.integration.mekanism.capability;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.lookup.AbstractMapIngredient;
import com.gregtechceu.gtceu.api.recipe.ui.GTRecipeTypeUI;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.slf4j.Logger;

import java.util.stream.Collectors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mekanism chemical を GTCEu レシピの第一級材料として扱う RecipeCapability.
 *
 * 4 種 (GAS/INFUSION/PIGMENT/SLURRY) それぞれ別 capability インスタンスとして登録される.
 * ChemicalCapabilities.GAS / INFUSION / PIGMENT / SLURRY で参照可.
 *
 * GT の FluidRecipeCapability を参考にしつつ、型判定を type field で内部識別する設計.
 */
public class ChemicalRecipeCapability extends RecipeCapability<ChemicalIngredient> {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final ChemicalIngredient.Type type;

    protected ChemicalRecipeCapability(ChemicalIngredient.Type type, String name, int color) {
        super(name, color, true, chemSortIndex(type), SerializerChemicalIngredient.forType(type));
        this.type = type;
    }

    public ChemicalIngredient.Type getType() {
        return type;
    }

    private static int chemSortIndex(ChemicalIngredient.Type t) {
        // Fluid (sortIndex=1) の直後に並ぶよう 2..5 を割り当て.
        switch (t) {
            case GAS:      return 2;
            case INFUSION: return 3;
            case PIGMENT:  return 4;
            case SLURRY:   return 5;
        }
        return 99;
    }

    @Override
    public ChemicalIngredient copyInner(ChemicalIngredient content) {
        return content.copy();
    }

    @Override
    public ChemicalIngredient copyWithModifier(ChemicalIngredient content, ContentModifier modifier) {
        if (content.isEmpty()) return content.copy();
        ChemicalIngredient copy = content.copy();
        copy.setAmount((long) modifier.apply(copy.getAmount()));
        return copy;
    }

    @Override
    public List<AbstractMapIngredient> convertToMapIngredient(Object obj) {
        List<AbstractMapIngredient> out = new ObjectArrayList<>(1);
        if (obj instanceof ChemicalIngredient ci) {
            if (ci.getType() == type) {
                out.add(new MapChemicalIngredient(ci));
                LOGGER.debug("[ChemCap] Cap[{}].convertToMapIngredient {} -> map", type, ci);
            } else {
                LOGGER.debug("[ChemCap] Cap[{}].convertToMapIngredient type mismatch (got {})", type, ci.getType());
            }
        } else {
            LOGGER.debug("[ChemCap] Cap[{}].convertToMapIngredient unknown obj type: {}",
                    type, obj == null ? "null" : obj.getClass().getName());
        }
        return out;
    }

    /**
     * 同じ (type, id) を持つ ingredient は amount を合算して圧縮する.
     */
    @Override
    public List<Object> compressIngredients(Collection<Object> ingredients) {
        LOGGER.debug("[ChemCap] Cap[{}].compressIngredients input={} ({}件)",
                type, ingredients, ingredients.size());
        // (id) -> accumulated ingredient
        Map<String, ChemicalIngredient> accum = new HashMap<>();
        List<Object> nonMatching = new ArrayList<>();
        for (Object o : ingredients) {
            if (!(o instanceof ChemicalIngredient ci)) {
                nonMatching.add(o);
                continue;
            }
            if (ci.getType() != type) {
                nonMatching.add(o);
                continue;
            }
            String key = ci.getId().toString();
            ChemicalIngredient existing = accum.get(key);
            if (existing == null) {
                accum.put(key, ci.copy());
            } else {
                existing.setAmount(existing.getAmount() + ci.getAmount());
            }
        }
        List<Object> result = new ArrayList<>(accum.size() + nonMatching.size());
        result.addAll(accum.values());
        result.addAll(nonMatching);
        LOGGER.debug("[ChemCap] Cap[{}].compressIngredients -> {} (merged={}, nonMatch={})",
                type, result, accum.size(), nonMatching.size());
        return result;
    }

    @Override
    public boolean isRecipeSearchFilter() {
        return true;
    }

    // ========================================================================
    //  JEI / XEI 描画統合 (Lv3 — Mek ChemicalStackRenderer 活用)
    // ========================================================================

    /** レシピの Content を ChemicalStack リストに変換 (Mek renderer が描画できる形式). */
    @Override
    public @NotNull List<Object> createXEIContainerContents(List<Content> contents, GTRecipe recipe, IO io) {
        List<Object> result = contents.stream()
                .map(c -> of(c.content))
                .map(ChemicalIngredient::toStack)
                .map(Object.class::cast)
                .collect(Collectors.toList());
        LOGGER.info("[ChemCap] createXEIContainerContents[{}] io={} recipe={} → {} stacks",
                type, io, recipe == null ? "?" : recipe.id, result.size());
        return result;
    }

    /** Content list をそのまま wrap. widget 側で index 指定して取り出す. */
    @Override
    public Object createXEIContainer(List<?> contents) {
        return contents;
    }

    @NotNull
    @Override
    public Widget createWidget() {
        LOGGER.info("[ChemCap] createWidget[{}] called", type);
        return new ChemicalStackWidget();
    }

    @NotNull
    @Override
    public Class<? extends Widget> getWidgetClass() {
        return ChemicalStackWidget.class;
    }

    /** widget に ChemicalStack をバインドし、tooltip用のIOラベルも設定. */
    @Override
    public void applyWidgetInfo(@NotNull Widget widget,
                                int index,
                                boolean isXEI,
                                IO io,
                                GTRecipeTypeUI.@UnknownNullability("null when storage == null") RecipeHolder recipeHolder,
                                @NotNull GTRecipeType recipeType,
                                @UnknownNullability("null when content == null") GTRecipe recipe,
                                @Nullable Content content,
                                @Nullable Object storage, int recipeTier, int chanceTier) {
        if (!(widget instanceof ChemicalStackWidget chemWidget)) return;

        // IO label (tooltip 先頭に [IN]/[OUT] を表示)
        chemWidget.setIngredientIO(io == IO.IN ? "IN" : "OUT");

        ChemicalStack<?> stackToShow = mekanism.api.chemical.gas.GasStack.EMPTY;

        // storage = createXEIContainer から渡ってくる. List<ChemicalStack> 前提.
        if (storage instanceof List<?> list) {
            if (index >= 0 && index < list.size() && list.get(index) instanceof ChemicalStack<?> cs) {
                stackToShow = cs;
            } else if (!list.isEmpty() && list.get(0) instanceof ChemicalStack<?> first) {
                stackToShow = first;
            }
        }
        // content が存在するなら、そこから ChemicalStack を再構築 (storage が null の場合用 fallback)
        if ((stackToShow == null || stackToShow.isEmpty()) && content != null) {
            ChemicalIngredient ci = of(content.content);
            stackToShow = ci.toStack();
        }

        chemWidget.setStack(stackToShow);
        LOGGER.debug("[ChemCap] applyWidgetInfo[{}] idx={} io={} stack={}",
                type, index, io,
                stackToShow == null || stackToShow.isEmpty()
                        ? "<empty>" : stackToShow.getTypeRegistryName() + "x" + stackToShow.getAmount());
    }

    /**
     * Lv1 で使ってた text ラベルはスロット描画に置き換わったので不要.
     * ただし perTick 情報など補助テキストは残したい場合ここで追加可.
     * 今は pass-through.
     */
    @Override
    public void addXEIInfo(WidgetGroup group, int xOffset, GTRecipe recipe,
                           List<Content> contents, boolean perTick, boolean isInput,
                           MutableInt yOffset) {
        // intentionally left blank — スロットwidget側で全て表示する
    }
}