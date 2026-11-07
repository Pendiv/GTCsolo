package DIV.gtcsolo.api.tier;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 構造ブロック由来の電圧tierを保持するマルチブロック基底。
 *
 * - GTValues 互換の voltage tier (LV=1 .. MAX=14) を {@link #getStructureTier()} で公開
 * - 通常の {@link WorkableElectricMultiblockMachine#getTier()} (エネルギーハッチ tier) とは別軸
 * - 形成時に {@link TieredBlockSet} から tier を確定して NBT に永続化
 * - JEI レシピ表示の "Required Tier" との連携は {@link TierRecipeLogic} 側で行う
 */
public class TieredMultiblockMachine extends WorkableElectricMultiblockMachine {

    public static final String NBT_STRUCTURE_TIER_KEY = "gtcsolo_structure_tier";

    private final TieredBlockSet structureTierSet;
    private int structureTier = -1;

    public TieredMultiblockMachine(IMachineBlockEntity holder, TieredBlockSet structureTierSet, Object... args) {
        super(holder, args);
        this.structureTierSet = structureTierSet;
    }

    public TieredBlockSet getStructureTierSet() {
        return structureTierSet;
    }

    /** 構造由来の電圧tier (GTValues 互換)。未形成 / 未確定なら -1。 */
    public int getStructureTier() {
        return structureTier;
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        this.structureTier = structureTierSet.getTierFromMachine(this);
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        this.structureTier = -1;
    }

    @Override
    public void saveCustomPersistedData(@NotNull CompoundTag tag, boolean forDrop) {
        super.saveCustomPersistedData(tag, forDrop);
        tag.putInt(NBT_STRUCTURE_TIER_KEY, structureTier);
    }

    @Override
    public void loadCustomPersistedData(@NotNull CompoundTag tag) {
        super.loadCustomPersistedData(tag);
        if (tag.contains(NBT_STRUCTURE_TIER_KEY)) {
            this.structureTier = tag.getInt(NBT_STRUCTURE_TIER_KEY);
        }
    }

    @Override
    public void addDisplayText(@NotNull List<Component> textList) {
        super.addDisplayText(textList);
        if (isFormed() && structureTier >= 0 && structureTier < GTValues.VNF.length) {
            // VNF は GT 純正の tier 色付き短縮名 (例 HV→§6HV)
            textList.add(Component.translatable("gtcsolo.multiblock.structure_tier",
                    GTValues.VNF[structureTier]));
        }
    }
}
