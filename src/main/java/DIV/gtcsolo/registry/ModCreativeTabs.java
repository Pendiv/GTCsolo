package DIV.gtcsolo.registry;

import DIV.gtcsolo.Gtcsolo;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class
ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Gtcsolo.MODID);

    // 指定なしのアイテムはすべて GTCSOLO_TAB に自動で入る
    // 別タブに入れたいアイテムは assignTab() で登録する
    private static final Map<RegistryObject<? extends ItemLike>, RegistryObject<CreativeModeTab>> TAB_OVERRIDES
            = new HashMap<>();

    /**
     * デフォルトタブ以外に入れたいアイテムを登録する。
     * 使い方は ModItems.java のコメントを参照。
     */
    public static void assignTab(RegistryObject<? extends ItemLike> item,
                                  RegistryObject<CreativeModeTab> tab) {
        TAB_OVERRIDES.put(item, tab);
    }

    public static final RegistryObject<CreativeModeTab> GTCSOLO_TAB =
            CREATIVE_MODE_TABS.register("gtcsolo_tab", () ->
                    CreativeModeTab.builder()
                            .icon(() -> new ItemStack(ModItems.XCRYSTAL.get()))
                            .title(Component.translatable("itemGroup.gtcsolo"))
                            .displayItems((params, output) -> {
                                // TAB_OVERRIDES に登録されていないアイテムをすべて追加
                                ModItems.ITEMS.getEntries().stream()
                                        .filter(item -> !TAB_OVERRIDES.containsKey(item))
                                        .forEach(item -> output.accept(item.get()));
                                // GTRegistrate経由で登録したマルチブロックコントローラー
                                if (ModMachines.FEC != null) output.accept(ModMachines.FEC.asStack());
                                if (ModMachines.EEBF != null) output.accept(ModMachines.EEBF.asStack());
                            })
                            .build()
            );

    // 別タブを増やす場合の例:
    // public static final RegistryObject<CreativeModeTab> ANOTHER_TAB =
    //         CREATIVE_MODE_TABS.register("another_tab", () ->
    //                 CreativeModeTab.builder()
    //                         .icon(() -> new ItemStack(ModItems.PRIEST_AXE.get()))
    //                         .title(Component.translatable("itemGroup.gtcsolo.another"))
    //                         .displayItems((params, output) -> {
    //                             TAB_OVERRIDES.entrySet().stream()
    //                                     .filter(e -> e.getValue() == ANOTHER_TAB)
    //                                     .forEach(e -> output.accept(e.getKey().get()));
    //                         })
    //                         .build()
    //         );
}
