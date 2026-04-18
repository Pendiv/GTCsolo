package DIV.gtcsolo.machine;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.common.machine.multiblock.part.EnergyHatchPartMachine;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

/**
 * SpaceForge Energy Hatch (SEHatch).
 *
 * SpaceForge マルチブロックの根幹ハッチ。UV/UHV/UEV のみ。
 * - マルチブロック構造の Tier 制約を決定する
 * - レシピ稼働時のメインエネルギー供給源
 * - OC の通常/不完全境界 Tier
 *
 * 通常エネルギーハッチは本ハッチの Tier − 3 のみ許可される。
 */
public class SpaceforgeEnergyHatchMachine extends EnergyHatchPartMachine {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            SpaceforgeEnergyHatchMachine.class, EnergyHatchPartMachine.MANAGED_FIELD_HOLDER);

    public SpaceforgeEnergyHatchMachine(IMachineBlockEntity holder, int tier, int amperage) {
        super(holder, tier, IO.IN, amperage);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    public static SpaceforgeEnergyHatchMachine create(IMachineBlockEntity holder, int tier, int amperage) {
        return new SpaceforgeEnergyHatchMachine(holder, tier, amperage);
    }
}