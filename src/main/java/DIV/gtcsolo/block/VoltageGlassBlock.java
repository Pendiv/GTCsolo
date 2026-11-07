package DIV.gtcsolo.block;

import com.gregtechceu.gtceu.api.GTValues;
import net.minecraft.world.level.block.GlassBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Immutable GT voltage tier. No block entity or item NBT is required. */
public final class VoltageGlassBlock extends GlassBlock {
    private final int tier;

    public VoltageGlassBlock(Properties properties, int tier) {
        // Properties.copy(GLASS) does not copy these predicates in Minecraft 1.20.1.
        super(properties.noOcclusion()
                .isValidSpawn((state, level, pos, type) -> false)
                .isRedstoneConductor((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false));
        if (tier < GTValues.LV || tier > GTValues.OpV) {
            throw new IllegalArgumentException("Voltage glass tier must be LV..OpV: " + tier);
        }
        this.tier = tier;
    }

    public int getTier() { return tier; }

    /** -1 means this is not voltage glass; ordinary glass is never silently treated as LV. */
    public static int tierOf(BlockState state) {
        return state.getBlock() instanceof VoltageGlassBlock glass ? glass.getTier() : -1;
    }
}
