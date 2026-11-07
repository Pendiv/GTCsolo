package DIV.gtcsolo.machine.mega.dismantle;

import DIV.gtcsolo.performance.PatternCoordinateTransform;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.predicates.SimplePredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/** Read-only GT 1.6.4 pattern metadata; never evaluate predicates or mutate a match context. */
public final class DismantleShape {
    private final BlockPattern pattern;
    private final TraceabilityPredicate[][][] cells;
    private final int[] anchor;
    private final Map<TraceabilityPredicate,Set<Block>> candidates=new IdentityHashMap<>();
    public final int width,height,depth;

    public DismantleShape(BlockPattern pattern) {
        this.pattern=pattern;
        for(int[] repeat:pattern.aisleRepetitions)
            if(repeat[0]!=1 || repeat[1]!=1) throw new IllegalArgumentException("Only fixed mega structures can be dismantled");
        try {
            Field matrix=BlockPattern.class.getDeclaredField("blockMatches"), center=BlockPattern.class.getDeclaredField("centerOffset");
            matrix.setAccessible(true);center.setAccessible(true);
            cells=(TraceabilityPredicate[][][])matrix.get(pattern);anchor=((int[])center.get(pattern)).clone();
        } catch(ReflectiveOperationException failure) { throw new IllegalStateException("GT 1.6.4 pattern metadata unavailable",failure); }
        depth=cells.length;height=cells[0].length;width=cells[0][0].length;
    }

    public int volume() { return Math.multiplyExact(Math.multiplyExact(width,height),depth); }
    public Set<Block> allowed(int index) {
        int x=index%width,y=index/width%height,z=index/width/height;
        return candidates.computeIfAbsent(cells[z][y][x],DismantleShape::candidateBlocks);
    }
    public BlockPos position(int index,BlockPos core,Direction front,Direction up,boolean flipped) {
        int x=index%width,y=index/width%height,z=index/width/height;
        return core.offset(PatternCoordinateTransform.offset(pattern.structureDir,x-anchor[0],y-anchor[1],z-anchor[2],front,up,flipped));
    }
    public static Set<Block> candidateBlocks(TraceabilityPredicate predicate) {
        // ANY remains untouched even when combined with another predicate/candidate list.
        if(predicate.common.contains(SimplePredicate.ANY)) return Set.of();
        Set<Block> result=Collections.newSetFromMap(new IdentityHashMap<>());
        for(var list:java.util.List.of(predicate.common,predicate.limited)) for(var part:list)
            if(part.candidates!=null) for(var candidate:part.candidates.get())
                if(!candidate.getBlockState().isAir()) result.add(candidate.getBlockState().getBlock());
        return Set.copyOf(result);
    }
}
