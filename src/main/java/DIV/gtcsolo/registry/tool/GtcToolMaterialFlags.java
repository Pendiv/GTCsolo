package DIV.gtcsolo.registry.tool;

import DIV.gtcsolo.registry.ModMaterials;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 値付きMaterialFlagのように使う、既知Material向けツール宣言カタログ。
 */
public final class GtcToolMaterialFlags {

    private static final List<GtcToolMaterialDeclaration> DECLARATIONS = new ArrayList<>();
    private static final Set<String> NAMES = new HashSet<>();
    private static boolean bootstrapped;
    private static boolean applied;

    private GtcToolMaterialFlags() {}

    public static void bootstrap() {
        if (bootstrapped) return;
        bootstrapped = true;

        // 旧 IGTAddon.registerMaterials() はGTCEu 1.6.4で呼ばれないため、
        // Netheriteの派生形状宣言を正規のPostMaterialEvent経路へ移す。
        register(GtcToolMaterialDeclaration.builder("gtceu_netherite_forms", () -> GTMaterials.Netherite)
                .materialFlags(
                        MaterialFlags.GENERATE_PLATE,
                        MaterialFlags.GENERATE_DENSE,
                        MaterialFlags.GENERATE_ROD,
                        MaterialFlags.GENERATE_LONG_ROD,
                        MaterialFlags.GENERATE_GEAR,
                        MaterialFlags.GENERATE_SMALL_GEAR,
                        MaterialFlags.GENERATE_FOIL,
                        MaterialFlags.GENERATE_RING,
                        MaterialFlags.GENERATE_SPRING,
                        MaterialFlags.GENERATE_ROTOR)
                .build());

        // 第一段階の実証:
        // 既知Material refined_netherite + 既知GTToolType sword。
        // 統合性能値11.25から速度・攻撃・耐久等を導出する。
        // ID・モデル・色・GT固有の剣補正・標準レシピはGTの既存生成経路が担当する。
        register(GtcToolMaterialDeclaration.builder(
                "refined_netherite_sword",
                () -> ModMaterials.REFINED_NETHERITE)
                .abilityCount(11.25F)
                .toolTypes(GTToolType.SWORD)
                .standardRecipes()
                .build());

        // KubeJS registers Tyrose before PostMaterialEvent. Packs without that material keep loading.
        // Use Red Steel's complete metal-tool selection; wear resistance primarily improves durability.
        if (GTCEuAPI.materialManager.getMaterial("gtcsolo:tyrose") != null) {
            register(GtcToolMaterialDeclaration.builder("tyrose_tools",
                    () -> GTCEuAPI.materialManager.getMaterial("gtcsolo:tyrose"))
                    .inheritToolStats(() -> GTMaterials.RedSteel)
                    .harvestSpeed(8.0F)
                    .attackDamage(6.5F)
                    .durability(3200)
                    .enchantability(22)
                    // The native buzzsaw-blade recipe additionally needs a gear.
                    .materialFlags(MaterialFlags.GENERATE_GEAR)
                    .standardRecipes()
                    .build());
        }
    }

    public static void applyAll() {
        if (!bootstrapped) {
            throw new IllegalStateException("GtcToolMaterialFlags.bootstrap() must run before applyAll()");
        }
        if (applied) return;
        applied = true;
        DECLARATIONS.forEach(GtcToolMaterialDeclaration::apply);
    }

    public static void register(GtcToolMaterialDeclaration declaration) {
        if (applied) {
            throw new IllegalStateException("Cannot register tool material declarations after PostMaterialEvent");
        }
        if (!NAMES.add(declaration.name())) {
            throw new IllegalArgumentException("Duplicate tool material declaration: " + declaration.name());
        }
        DECLARATIONS.add(declaration);
    }

    public static void addToCreativeTab(CreativeModeTab.Output output) {
        DECLARATIONS.forEach(declaration -> declaration.addGeneratedToolsToCreativeTab(output));
    }

    public static ItemStack iconStack() {
        for (GtcToolMaterialDeclaration declaration : DECLARATIONS) {
            ItemStack stack = declaration.firstGeneratedToolStack();
            if (!stack.isEmpty()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
