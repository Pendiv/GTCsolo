package DIV.gtcsolo.api.tier;

import DIV.gtcsolo.block.VoltageGlassBlock;
import DIV.gtcsolo.registry.VoltageGlass;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.error.PatternStringError;
import com.gregtechceu.gtceu.api.pattern.predicates.SimplePredicate;
import com.gregtechceu.gtceu.api.pattern.util.PatternMatchContext;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/** A shared voltage grade for the separate casing and window positions of a room. */
public final class RoomStructureTier {
    public static final String MISMATCH_ERROR = "gtcsolo.multiblock.error.room_tier_mismatch";
    private static final List<Integer> TIERS = IntStream.rangeClosed(GTValues.LV, GTValues.OpV).boxed().toList();
    private final String contextKey;

    public RoomStructureTier(String contextKey) {
        this.contextKey = Objects.requireNonNull(contextKey);
        if (contextKey.isBlank()) throw new IllegalArgumentException("Room tier context key must not be blank");
    }

    public String contextKey() { return contextKey; }

    public List<Integer> tiers() { return TIERS; }

    public TraceabilityPredicate casingPredicate() {
        return predicate("gtcsolo_room_casing", state -> casingTierOf(state.getBlockState()),
                () -> TIERS.stream().map(this::casingForTier).map(RoomStructureTier::blockInfo).toArray(BlockInfo[]::new));
    }

    public TraceabilityPredicate glassPredicate() {
        return predicate("gtcsolo_room_glass", state -> glassTierOf(state.getBlockState()), () -> {
            List<BlockInfo> candidates = new ArrayList<>();
            for (int tier : TIERS) {
                candidates.add(blockInfo(VoltageGlass.get(tier)));
                if (tier == GTValues.LV) candidates.add(blockInfo(GTBlocks.CASING_TEMPERED_GLASS.get()));
                if (tier == GTValues.HV) candidates.add(blockInfo(GTBlocks.CASING_LAMINATED_GLASS.get()));
            }
            return candidates.toArray(BlockInfo[]::new);
        });
    }

    private TraceabilityPredicate predicate(String type,
            java.util.function.ToIntFunction<MultiblockState> tierResolver,
            java.util.function.Supplier<BlockInfo[]> candidates) {
        return new TraceabilityPredicate(new SimplePredicate(type, state -> {
            int tier = tierResolver.applyAsInt(state);
            if (tier < GTValues.LV || tier > GTValues.OpV) return false;
            if (!matchTier(state.getMatchContext(), tier)) {
                state.setError(new PatternStringError(MISMATCH_ERROR));
                return false;
            }
            return true;
        }, candidates)).addTooltips(Component.translatable(MISMATCH_ERROR));
    }

    /** Shared by both predicates; a failed match cannot replace the grade already selected. */
    public boolean matchTier(PatternMatchContext context, int tier) {
        validateTier(tier);
        Object selected = context.getOrPut(contextKey, Integer.valueOf(tier));
        return selected instanceof Integer value && value.intValue() == tier;
    }

    public int tierFromContext(PatternMatchContext context) {
        Object selected = context.get(contextKey);
        return selected instanceof Integer value && TIERS.contains(value) ? value : -1;
    }

    public int getTierFromMachine(IMultiController machine) {
        return machine.isFormed() ? tierFromContext(machine.getMultiblockState().getMatchContext()) : -1;
    }

    public int casingTierOf(BlockState state) {
        for (int tier : TIERS) if (state.is(casingForTier(tier))) return tier;
        return -1;
    }

    public int glassTierOf(BlockState state) {
        int tier = VoltageGlassBlock.tierOf(state);
        if (tier >= GTValues.LV && tier <= GTValues.OpV) return tier;
        if (state.is(GTBlocks.CASING_TEMPERED_GLASS.get())) return GTValues.LV;
        if (state.is(GTBlocks.CASING_LAMINATED_GLASS.get())) return GTValues.HV;
        return -1;
    }

    /** One representative per grade keeps native JEI pages aligned with the casing grade. */
    public Block previewGlassForTier(int tier) {
        validateTier(tier);
        return VoltageGlass.get(tier);
    }

    public Block casingForTier(int tier) {
        validateTier(tier);
        return switch (tier) {
            case GTValues.LV -> GTBlocks.MACHINE_CASING_LV.get();
            case GTValues.MV -> GTBlocks.MACHINE_CASING_MV.get();
            case GTValues.HV -> GTBlocks.MACHINE_CASING_HV.get();
            case GTValues.EV -> GTBlocks.MACHINE_CASING_EV.get();
            case GTValues.IV -> GTBlocks.MACHINE_CASING_IV.get();
            case GTValues.LuV -> GTBlocks.MACHINE_CASING_LuV.get();
            case GTValues.ZPM -> GTBlocks.MACHINE_CASING_ZPM.get();
            case GTValues.UV -> GTBlocks.MACHINE_CASING_UV.get();
            case GTValues.UHV -> GTBlocks.MACHINE_CASING_UHV.get();
            case GTValues.UEV -> GTBlocks.MACHINE_CASING_UEV.get();
            case GTValues.UIV -> GTBlocks.MACHINE_CASING_UIV.get();
            case GTValues.UXV -> GTBlocks.MACHINE_CASING_UXV.get();
            case GTValues.OpV -> GTBlocks.MACHINE_CASING_OpV.get();
            default -> throw new IllegalArgumentException("Unsupported room tier: " + tier);
        };
    }

    private static void validateTier(int tier) {
        if (tier < GTValues.LV || tier > GTValues.OpV) {
            throw new IllegalArgumentException("Room tier must be within LV..OpV: " + tier);
        }
    }

    private static BlockInfo blockInfo(Block block) { return BlockInfo.fromBlockState(block.defaultBlockState()); }
}
