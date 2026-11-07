package DIV.gtcsolo.machine.fusion;

import DIV.gtcsolo.registry.ModBlocks;
import com.google.gson.JsonParser;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.FusionReactorMachine;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.gregtechceu.gtceu.api.pattern.Predicates.*;

/** Atlas coordinates are RIGHT / UP / BACK, with a NORTH-facing controller. */
public final class FusionVariantPattern {
    private final String id;
    private volatile Layout layout;
    FusionVariantPattern(String id) { this.id = id; }

    public static final class Layout {
        private final String[][] rows;
        public final int width, height, depth, anchorX, anchorY, anchorZ;
        public final double radius;
        private Layout(String[][] rows, int[] size, int[] anchor, double radius) {
            this.rows = rows;
            width=size[0]; height=size[1]; depth=size[2];
            anchorX=anchor[0]; anchorY=anchor[1]; anchorZ=anchor[2]; this.radius=radius;
        }
        private char at(int x,int y,int z) {
            return x<0 || y<0 || z<0 || x>=width || y>=height || z>=depth ? '#' : rows[z][y].charAt(x);
        }
    }

    public synchronized Layout layout() {
        if (layout != null) return layout;
        String resource = "/data/gtcsolo/multiblocks/fusion/" + id + ".json";
        try (var input = FusionVariantPattern.class.getResourceAsStream(resource)) {
            if (input == null) throw new IllegalStateException("Missing fusion layout " + resource);
            var json = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            if (json.get("schema").getAsInt()!=1) throw new IllegalStateException("Invalid fusion layout schema");
            int[] size=new int[3], anchor=new int[3];
            String[] axes={"RIGHT","UP","BACK"};
            for (int i=0;i<3;i++) {
                size[i]=json.getAsJsonArray("size").get(i).getAsInt();
                anchor[i]=json.getAsJsonArray("controller").get(i).getAsInt();
                if (size[i]<1 || anchor[i]<0 || anchor[i]>=size[i]
                        || !axes[i].equals(json.getAsJsonArray("axes").get(i).getAsString()))
                    throw new IllegalStateException("Invalid fusion layout header");
            }
            var aisles=json.getAsJsonArray("aisles");
            if (aisles.size()!=size[2]) throw new IllegalStateException("Invalid fusion depth");
            String[][] rows=new String[size[2]][size[1]];
            int controllers=0;
            for (int z=0;z<size[2];z++) {
                var aisle=aisles.get(z).getAsJsonArray();
                if (aisle.size()!=size[1]) throw new IllegalStateException("Invalid fusion height");
                for (int y=0;y<size[1];y++) {
                    String row=aisle.get(y).getAsString();
                    if (row.length()!=size[0]) throw new IllegalStateException("Invalid fusion width");
                    rows[z][y]=row;
                    for (int x=0;x<size[0];x++) {
                        char symbol=row.charAt(x);
                        if ("CGKEIiOoS.#".indexOf(symbol)<0) throw new IllegalStateException("Unknown fusion symbol");
                        if (symbol=='S') {
                            controllers++;
                            if (x!=anchor[0] || y!=anchor[1] || z!=anchor[2])
                                throw new IllegalStateException("Fusion anchor differs from controller");
                        }
                    }
                }
            }
            if (controllers!=1) throw new IllegalStateException("Expected one fusion controller");
            return layout=new Layout(rows,size,anchor,json.get("radius").getAsDouble());
        } catch (Exception e) { throw new IllegalStateException("Cannot load fusion layout " + id,e); }
    }

    public static Block casing(FusionVariantSettings settings) {
        return settings.mk==4 ? ModBlocks.FUSION_CASING_MK4.get() : FusionReactorMachine.getCasingState(settings.tier());
    }

    public BlockPattern create(MultiblockMachineDefinition definition, FusionVariantSettings settings) {
        var data=layout();
        var builder=FactoryBlockPattern.start(RelativeDirection.RIGHT,RelativeDirection.UP,RelativeDirection.BACK);
        for (String[] rows:data.rows) builder.aisle(rows);
        var shell=blocks(casing(settings));
        var input=shell.or(abilities(PartAbility.IMPORT_FLUIDS).setMinGlobalLimited(2));
        var output=shell.or(abilities(PartAbility.EXPORT_FLUIDS));
        return builder.where('S',controller(blocks(definition.getBlock())))
                .where('C',shell).where('G',blocks(GTBlocks.FUSION_GLASS.get()).or(shell))
                .where('K',blocks(FusionReactorMachine.getCoilState(settings.tier())))
                .where('E',shell.or(blocks(PartAbility.INPUT_ENERGY.getBlockRange(settings.tier(),settings.maximumHatchTier())
                        .toArray(Block[]::new)).setMinGlobalLimited(settings.minimumEnergyHatches())
                        .setMaxGlobalLimited(settings.maximumEnergyHatches()).setPreviewCount(settings.maximumEnergyHatches())))
                .where('I',input).where('i',input).where('O',output).where('o',output)
                .where('.',any()).where('#',any()).build();
    }

    public List<MultiblockShapeInfo> previews(MultiblockMachineDefinition definition, FusionVariantSettings settings) {
        return List.of(preview(definition,settings,false),preview(definition,settings,true));
    }

    private MultiblockShapeInfo preview(MultiblockMachineDefinition definition, FusionVariantSettings settings, boolean glass) {
        var data=layout(); var builder=MultiblockShapeInfo.builder();
        Map<String,Character> ports=new HashMap<>();
        int energyPositions=0;
        for (int z=0;z<data.depth;z++) for (int y=0;y<data.height;y++) for (int x=0;x<data.width;x++)
            if (data.at(x,y,z)=='E' && exposedFace(data,x,y,z)!=null) energyPositions++;
        int energySeen=0, energyPlaced=0, required=settings.maximumEnergyHatches();
        for (int z=0;z<data.depth;z++) {
            String[] rows=new String[data.height];
            for (int y=0;y<data.height;y++) {
                StringBuilder row=new StringBuilder(data.rows[z][y]);
                for (int x=0;x<data.width;x++) {
                    char role=Character.toUpperCase(row.charAt(x));
                    if (role!='E' && role!='I' && role!='O') continue;
                    Direction facing=exposedFace(data,x,y,z);
                    if (facing==null) { row.setCharAt(x,'C'); continue; }
                    if (role=='E') {
                        boolean place=(energySeen++ * required / energyPositions)>=energyPlaced;
                        if (!place || energyPlaced==required) { row.setCharAt(x,'C'); continue; }
                        energyPlaced++;
                    }
                    String key=""+role+facing;
                    Character symbol=ports.get(key);
                    if (symbol==null) {
                        symbol=(char)(256+ports.size()); ports.put(key,symbol);
                        var machine=role=='E' ? GTMachines.ENERGY_INPUT_HATCH[settings.tier()]
                                : role=='I' ? GTMachines.FLUID_IMPORT_HATCH[settings.tier()]
                                : GTMachines.FLUID_EXPORT_HATCH[settings.tier()];
                        builder.where(symbol,machine,facing);
                    }
                    row.setCharAt(x,symbol);
                }
                rows[y]=row.toString();
            }
            builder.aisle(rows);
        }
        if (energyPlaced!=required) throw new IllegalStateException("Fusion preview lacks required energy ports");
        return builder.where('S',definition,Direction.NORTH).where('C',casing(settings))
                .where('G',glass ? GTBlocks.FUSION_GLASS.get() : casing(settings))
                .where('K',FusionReactorMachine.getCoilState(settings.tier()))
                .where('.',Blocks.AIR).where('#',Blocks.AIR).build();
    }

    private static Direction exposedFace(Layout data,int x,int y,int z) {
        // Show ports opening into exterior space, not into the sealed reaction channel.
        for (Direction direction:new Direction[]{Direction.UP,Direction.DOWN,Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST})
            if (data.at(x+direction.getStepX(),y+direction.getStepY(),z+direction.getStepZ())=='#') return direction;
        return null;
    }
}
