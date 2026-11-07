package DIV.gtcsolo.api.pattern;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

/** Selects a real native pattern for each allowed controller position, without editing the world. */
public abstract class FlexibleCoreMultiblockMachine extends MultiblockControllerMachine {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            FlexibleCoreMultiblockMachine.class, MultiblockControllerMachine.MANAGED_FIELD_HOLDER);
    @Persisted @DescSynced private int corePosition;
    private volatile BlockPattern[] variants;
    private int matchedPosition = -1;

    protected FlexibleCoreMultiblockMachine(IMachineBlockEntity holder) { super(holder); }
    @Override public ManagedFieldHolder getFieldHolder() { return MANAGED_FIELD_HOLDER; }
    protected abstract BlockPattern[] createCorePatterns();

    private BlockPattern[] patterns() {
        if (variants == null) {
            synchronized (this) {
                if (variants == null) {
                    BlockPattern[] created = createCorePatterns();
                    if (created.length == 0) throw new IllegalStateException("No flexible core positions");
                    variants = created;
                }
            }
        }
        return variants;
    }

    public final int getCorePosition() { return Math.max(0, Math.min(corePosition, patterns().length - 1)); }
    @Override public BlockPattern getPattern() { return patterns()[getCorePosition()]; }

    @Override public boolean checkPattern() {
        BlockPattern[] candidates = patterns();
        int preferred = getCorePosition();
        matchedPosition = -1;
        if (candidates[preferred].checkPatternAt(getMultiblockState(), false)) {
            matchedPosition = preferred;
            return true;
        }
        for (int index = 0; index < candidates.length; index++) {
            if (index != preferred && candidates[index].checkPatternAt(getMultiblockState(), false)) {
                matchedPosition = index;
                return true;
            }
        }
        return false;
    }

    @Override public void onStructureFormed() {
        if (matchedPosition >= 0) corePosition = matchedPosition;
        super.onStructureFormed();
        onChanged();
    }

    @Override public void onStructureInvalid() {
        super.onStructureInvalid();
        matchedPosition = -1;
    }
}
