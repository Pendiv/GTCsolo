package DIV.gtcsolo.registry.tool;

import com.gregtechceu.gtceu.api.item.tool.GTToolType;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * GTToolType の選択プリセット。
 *
 * <p>値はキャッシュせず、Material の PostMaterialEvent 中に解決する。
 * {@code ALL} は解決時点で登録済みの全 GTToolType を含む。</p>
 */
public enum GtcToolGroup {

    VANILLA,
    GT_MANUAL,
    GT_ELECTRIC,
    ALL;

    public Set<GTToolType> resolve() {
        return switch (this) {
            case VANILLA -> orderedSet(
                    GTToolType.SWORD,
                    GTToolType.PICKAXE,
                    GTToolType.SHOVEL,
                    GTToolType.AXE,
                    GTToolType.HOE);
            case GT_MANUAL -> orderedSet(
                    GTToolType.MINING_HAMMER,
                    GTToolType.SPADE,
                    GTToolType.SCYTHE,
                    GTToolType.SAW,
                    GTToolType.HARD_HAMMER,
                    GTToolType.SOFT_MALLET,
                    GTToolType.WRENCH,
                    GTToolType.FILE,
                    GTToolType.CROWBAR,
                    GTToolType.SCREWDRIVER,
                    GTToolType.MORTAR,
                    GTToolType.WIRE_CUTTER,
                    GTToolType.KNIFE,
                    GTToolType.BUTCHERY_KNIFE,
                    GTToolType.PLUNGER,
                    GTToolType.SHEARS);
            case GT_ELECTRIC -> orderedSet(
                    GTToolType.DRILL_LV,
                    GTToolType.DRILL_MV,
                    GTToolType.DRILL_HV,
                    GTToolType.DRILL_EV,
                    GTToolType.DRILL_IV,
                    GTToolType.CHAINSAW_LV,
                    GTToolType.WRENCH_LV,
                    GTToolType.WRENCH_HV,
                    GTToolType.WRENCH_IV,
                    GTToolType.WIRE_CUTTER_LV,
                    GTToolType.WIRE_CUTTER_HV,
                    GTToolType.WIRE_CUTTER_IV,
                    GTToolType.BUZZSAW,
                    GTToolType.SCREWDRIVER_LV);
            case ALL -> new LinkedHashSet<>(GTToolType.getTypes().values());
        };
    }

    private static Set<GTToolType> orderedSet(GTToolType... types) {
        return new LinkedHashSet<>(Arrays.asList(types));
    }
}
