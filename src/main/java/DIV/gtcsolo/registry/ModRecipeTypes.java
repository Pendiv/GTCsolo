package DIV.gtcsolo.registry;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.block.ICoilType;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.recipe.GTRecipeSerializer;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.integration.xei.entry.item.ItemStackList;
import com.gregtechceu.gtceu.integration.xei.handlers.item.CycleItemEntryHandler;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ModRecipeTypes {

    // fantasy_element_constructor の略
    public static GTRecipeType FEC;

    public static void init() {
        FEC = new GTRecipeType(
                ResourceLocation.fromNamespaceAndPath("gtcsolo", "fec"), "multiblock")
                // アイテム入力6, アイテム出力1, 液体入力1, 液体出力0
                .setMaxIOSize(6, 1, 1, 0)
                .setEUIO(IO.IN)
                // ebf_temp: レシピに必要な最低コイル温度を表示
                .addDataInfo(data -> {
                    int temp = data.getInt("ebf_temp");
                    return LocalizationUtils.format("gtceu.recipe.temperature",
                            FormattingUtil.formatNumbers(temp) + "K");
                })
                // 必要なコイルの最小グレードを表示
                .addDataInfo(data -> {
                    int temp = data.getInt("ebf_temp");
                    ICoilType requiredCoil = ICoilType.getMinRequiredType(temp);
                    if (requiredCoil != null && !requiredCoil.getMaterial().isNull()) {
                        return LocalizationUtils.format("gtceu.recipe.coil.tier",
                                I18n.get(requiredCoil.getMaterial().getUnlocalizedName()));
                    }
                    return "";
                })
                // JEI/REIで使用可能コイルをスロット表示
                .setUiBuilder((recipe, widgetGroup) -> {
                    int temp = recipe.data.getInt("ebf_temp");
                    List<ItemStack> coilStacks = GTCEuAPI.HEATING_COILS.entrySet().stream()
                            .filter(coil -> coil.getKey().getCoilTemperature() >= temp)
                            .map(coil -> new ItemStack(coil.getValue().get()))
                            .toList();
                    CycleItemEntryHandler handler = new CycleItemEntryHandler(
                            List.of(ItemStackList.of(coilStacks)));
                    widgetGroup.addWidget(new SlotWidget(handler, 0,
                            widgetGroup.getSize().width - 25, widgetGroup.getSize().height - 32, false, false));
                });

        ResourceLocation fecId = ResourceLocation.fromNamespaceAndPath("gtcsolo", "fec");
        // GTCEu自身のGTRecipeTypes.register()と同様に、3つ全てに登録する
        GTRegistries.register(BuiltInRegistries.RECIPE_TYPE, fecId, FEC);
        GTRegistries.register(BuiltInRegistries.RECIPE_SERIALIZER, fecId, new GTRecipeSerializer());
        GTRegistries.RECIPE_TYPES.register(fecId, FEC);
    }
}