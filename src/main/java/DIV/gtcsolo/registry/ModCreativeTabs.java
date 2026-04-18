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
                                if (ModMachines.CC != null) output.accept(ModMachines.CC.asStack());
                                if (ModMachines.SPACEFORGE != null) output.accept(ModMachines.SPACEFORGE.asStack());
                                if (ModMachines.CHEMICAL_COMBUSTION_GENERATOR != null) output.accept(ModMachines.CHEMICAL_COMBUSTION_GENERATOR.asStack());
                                if (ModMachines.HIGHPRESSURE_ALLOY_BLAST_FURNACE != null) output.accept(ModMachines.HIGHPRESSURE_ALLOY_BLAST_FURNACE.asStack());
                                if (ModMachines.WIRE_MANUFACTURING_FACTORY != null) output.accept(ModMachines.WIRE_MANUFACTURING_FACTORY.asStack());
                                ModMachines.SPACEFORGE_ENERGY_HATCH.values()
                                        .forEach(ampMap -> ampMap.values()
                                                .forEach(def -> output.accept(def.asStack())));
                                // 化学変換液体搬入/搬出ハッチ
                                ModMachines.CONVERSION_FLUID_HATCH.values()
                                        .forEach(def -> output.accept(def.asStack()));
                                ModMachines.CONVERSION_FLUID_OUTPUT_HATCH.values()
                                        .forEach(def -> output.accept(def.asStack()));
                            })
                            .build()
            );

    public static final RegistryObject<CreativeModeTab> MATERIAL_TAB =
            CREATIVE_MODE_TABS.register("gtcsolo_material", () ->
                    CreativeModeTab.builder()
                            .icon(() -> new ItemStack(ModItems.FCORE.get()))
                            .title(Component.translatable("itemGroup.gtcsolo.material"))
                            .displayItems((params, output) -> {
                                ModMaterials.addToCreativeTab(output);
                            })
                            .build()
            );

    public static final RegistryObject<CreativeModeTab> WIRELESS_TAB =
            CREATIVE_MODE_TABS.register("gtcsolo_wireless_tab", () ->
                    CreativeModeTab.builder()
                            .icon(() -> new ItemStack(ModItems.WEN_MAINSTORAGE_CASING_ITEM.get()))
                            .title(Component.translatable("itemGroup.gtcsolo.wireless"))
                            .displayItems((params, output) -> {
                                // WENブロック群
                                output.accept(ModItems.WEN_MAINSTORAGE_CASING_ITEM.get());
                                output.accept(ModItems.WEN_MAINSTORAGE_OUTPUT_PORT_ITEM.get());
                                output.accept(ModItems.WEN_MAINSTORAGE_INPUT_PORT_ITEM.get());
                                output.accept(ModItems.WEN_DATA_MONITOR_ITEM.get());
                                output.accept(ModItems.WEN_BASIC_ENERGY_CELL_ITEM.get());
                                // AE2版ポート群
                                output.accept(ModItems.WEN_AE_INPUT_PORT_ITEM.get());
                                output.accept(ModItems.WEN_AE_OUTPUT_PORT_ITEM.get());
                                output.accept(ModItems.WEN_WIRELESS_ENERGYCARD.get());
                                // FEワイヤレスポート群
                                output.accept(ModItems.WEN_FE_INPUT_PORT_ITEM.get());
                                output.accept(ModItems.WEN_FE_OUTPUT_PORT_ITEM.get());
                                // WENコントローラー
                                if (ModMachines.WEN_MAIN_STORAGE != null)
                                    output.accept(ModMachines.WEN_MAIN_STORAGE.asStack());
                                // ワイヤレスIOマシン全種
                                WENMachines.WIRELESS_INPUT.values().forEach(ampMap ->
                                        ampMap.values().forEach(def -> output.accept(def.asStack())));
                                WENMachines.WIRELESS_OUTPUT.values().forEach(ampMap ->
                                        ampMap.values().forEach(def -> output.accept(def.asStack())));
                                WENMachines.ENERGY_HATCH.values().forEach(ampMap ->
                                        ampMap.values().forEach(def -> output.accept(def.asStack())));
                                WENMachines.ENERGY_OUTPUT_HATCH.values().forEach(ampMap ->
                                        ampMap.values().forEach(def -> output.accept(def.asStack())));
                            })
                            .build()
            );
}
