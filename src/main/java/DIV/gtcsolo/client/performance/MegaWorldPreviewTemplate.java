package DIV.gtcsolo.client.performance;

import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Runs the real native preview selector on the small sequence of port sites, without the huge geometry. */
public record MegaWorldPreviewTemplate(BlockPattern facingHelper, List<BlockState> ports, BlockState coil) {
    public static MegaWorldPreviewTemplate create(TraceabilityPredicate core, TraceabilityPredicate port,
                                                  TraceabilityPredicate coil, int portCount) {
        // The shipped layouts have 43/48/67 sites. Malformed exports must not create a huge template.
        if (portCount < 1 || portCount > 512) throw new IllegalArgumentException("Unexpected mega preview port count");
        for (var predicate : port.limited) {
            if (predicate.minLayerCount != -1 || predicate.maxLayerCount != -1)
                throw new IllegalArgumentException("Layer-limited ports require a layer-aware preview");
        }
        String row = "K" + (coil == null ? "" : "E") + "P".repeat(portCount);
        var builder = FactoryBlockPattern.start(RelativeDirection.RIGHT, RelativeDirection.UP, RelativeDirection.BACK)
                .aisle(row).where('K', core).where('P', port);
        if (coil != null) builder.where('E', coil);
        BlockPattern pattern = builder.build();
        var blocks = pattern.getPreview(new int[]{1});
        int firstPort = coil == null ? 1 : 2;
        List<BlockState> ports = new ArrayList<>(portCount);
        for (int index = 0; index < portCount; index++)
            ports.add(blocks[firstPort + index][0][0].getBlockState());
        return new MegaWorldPreviewTemplate(pattern, List.copyOf(ports),
                coil == null ? null : blocks[1][0][0].getBlockState());
    }

    /** Fully specified layouts still need the native orientation helper, but no port selection. */
    public static MegaWorldPreviewTemplate fixed(TraceabilityPredicate core) {
        var pattern=FactoryBlockPattern.start(RelativeDirection.RIGHT,RelativeDirection.UP,RelativeDirection.BACK)
                .aisle("K").where('K',core).build();
        return new MegaWorldPreviewTemplate(pattern,List.of(),null);
    }
}
