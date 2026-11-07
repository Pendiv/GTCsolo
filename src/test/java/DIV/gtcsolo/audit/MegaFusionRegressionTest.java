package DIV.gtcsolo.audit;

import DIV.gtcsolo.machine.fusion.MegaFusionRules;
import DIV.gtcsolo.client.performance.MegaWorldPreviewTemplate;
import DIV.gtcsolo.integration.jei.multiblock.MegaPreviewCursor;
import DIV.gtcsolo.machine.mega.MegaPatternResource;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.Items;
import net.minecraftforge.fluids.FluidStack;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

public final class MegaFusionRegressionTest {
    public static void run() throws Exception {
        // GTRecipeTypes opens its registries during the real GT mod's construction phase.
        // Supply only that owner identity in this isolated JVM; production registration is untouched.
        net.minecraftforge.fml.ModLoadingContext.get().setActiveContainer(new RegistryOwner());
        var mods=net.minecraftforge.fml.ModList.of(java.util.List.of(),java.util.List.of());
        var loaded=net.minecraftforge.fml.ModList.class.getDeclaredMethod("setLoadedMods",java.util.List.class);
        loaded.setAccessible(true); loaded.invoke(mods,java.util.List.of());
        assert MegaFusionRules.EU_PER_HEAT==100;
        assert MegaFusionRules.STORAGE==128_000_000_000L;
        assert MegaFusionRules.heatingCost(0,100_000_000)==10_000_000_000L;
        assert MegaFusionRules.heatingCost(80_000_000,100_000_000)==2_000_000_000L;
        assert MegaFusionRules.heatingCost(100_000_000,100_000_000)==0;
        assert MegaFusionRules.heatingCost(200_000_000,100_000_000)==0;
        assert MegaFusionRules.heatingCost(99_990_000,100_000_000)==1_000_000;
        assert MegaFusionRules.heatingCost(0,MegaFusionRules.MAX_HEAT)==MegaFusionRules.STORAGE;
        assert MegaFusionRules.heatingCost(0,Long.MAX_VALUE)==-1;
        assert MegaFusionRules.heatingCost(-1,100)==-1;

        var source=GTRecipeTypes.FUSION_RECIPES.recipeBuilder("mega_fusion_regression")
                .inputFluids(new FluidStack(Fluids.WATER,10)).outputFluids(new FluidStack(Fluids.LAVA,20))
                .notConsumable(Items.STICK).EUt(32768).duration(160).fusionStartEU(100_000_000).buildRawRecipe();
        var modified=MegaFusionRules.transform(source);
        assert modified!=null && modified!=source;
        assert modified.duration==10 && modified.ocLevel==source.ocLevel+2;
        assert !modified.tickInputs.containsKey(EURecipeCapability.CAP);
        assert source.tickInputs.containsKey(EURecipeCapability.CAP);
        assert FluidRecipeCapability.CAP.of(modified.inputs.get(FluidRecipeCapability.CAP).get(0).content).getAmount()==327680;
        assert FluidRecipeCapability.CAP.of(modified.outputs.get(FluidRecipeCapability.CAP).get(0).content).getAmount()==655360;
        assert FluidRecipeCapability.CAP.of(source.inputs.get(FluidRecipeCapability.CAP).get(0).content).getAmount()==10;
        assert modified.inputs.get(ItemRecipeCapability.CAP).get(0).chance==0;
        assert modified.data.getLong("eu_to_start")==100_000_000;
        assert !source.data.contains("gtcsolo_mega_fusion_applied");
        assert MegaFusionRules.transform(modified)==modified;
        var shortRecipe=source.copy(); shortRecipe.duration=1;
        assert MegaFusionRules.transform(shortRecipe).duration==1;
        var overflow=source.copy();
        overflow.inputs.get(FluidRecipeCapability.CAP).get(0).content=com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient.of(65536,Fluids.WATER);
        assert MegaFusionRules.transform(overflow)==null;

        var core=Predicates.controller(Predicates.blocks(Blocks.DIAMOND_BLOCK));
        var fixed=MegaWorldPreviewTemplate.fixed(core);
        assert fixed.ports().isEmpty() && fixed.coil()==null;
        assert fixed.facingHelper().getPreview(new int[]{1})[0][0][0].getBlockState().is(Blocks.DIAMOND_BLOCK);
        var resource=new MegaPatternResource(MegaFusionRegressionTest.class,"/data/gtcsolo/multiblocks/mega_fusion.json.gz","mega fusion test");
        var header=resource.header();
        assert header.width()==333 && header.height()==20 && header.depth()==333;
        var validated=resource.readLayout();
        long[] cells={0,0,0};
        var serviceFaces=new java.util.ArrayList<int[]>();
        var coilPositions=new java.util.HashSet<net.minecraft.core.BlockPos>();
        try(var input=MegaFusionRegressionTest.class.getResourceAsStream("/data/gtcsolo/multiblocks/mega_fusion.json.gz");
            var cursor=new MegaPreviewCursor(new InputStreamReader(new GZIPInputStream(input),StandardCharsets.UTF_8))) {
            while(!cursor.done()) cursor.step(16384,Long.MAX_VALUE,(x,y,z,symbol)-> {
                cells[0]++;
                if(symbol=='K') { cells[1]++; assert x==header.anchorX() && y==header.anchorY() && z==header.anchorZ(); }
                var material=cursor.palette().getAsJsonObject(String.valueOf(symbol));
                assert material!=null;
                if(material.get("block").getAsString().equals("gtceu:fusion_coil"))
                    coilPositions.add(new net.minecraft.core.BlockPos(x,y,z));
                if(material.get("block").getAsString().equals("gtceu:uhv_energy_input_hatch")) cells[2]++;
                if(symbol=='K' || material.get("block").getAsString().contains("hatch")) {
                    var facing=net.minecraft.core.Direction.byName(material.getAsJsonObject("state").get("facing").getAsString());
                    assert facing!=null;
                    serviceFaces.add(new int[]{x,y,z,facing.getStepX(),facing.getStepY(),facing.getStepZ()});
                }
            });
        }
        assert cells[1]==1 && cells[2]==128;
        assert serviceFaces.size()==145;
        for(var face:serviceFaces) for(int distance=1;distance<=333;distance++) for(int above=-1;above<=2;above++) {
            int x=face[0]+face[3]*distance+(face[4]!=0?above:0);
            int y=face[1]+face[4]*distance+(face[4]==0?above:0),z=face[2]+face[5]*distance;
            if(x>=0 && x<333 && y>=0 && y<20 && z>=0 && z<333)
                assert validated.getAsJsonArray("aisles").get(z).getAsJsonArray().get(y).getAsString().charAt(x)=='#'
                        : "Buried service face or blocked access corridor";
        }
        assert header.anchorX()==166 && header.anchorZ()==0;
        var remaining=new java.util.HashSet<>(coilPositions);int rings=0;
        while(!remaining.isEmpty()) {
            rings++;var queue=new java.util.ArrayDeque<net.minecraft.core.BlockPos>();
            var first=remaining.iterator().next();remaining.remove(first);queue.add(first);
            while(!queue.isEmpty()) {
                var pos=queue.removeFirst();
                assert Math.hypot(Math.hypot(pos.getX()-166,pos.getZ()-166)-156.5,pos.getY()-9.5)<8;
                for(var direction:net.minecraft.core.Direction.values()) {
                    var neighbor=pos.relative(direction);if(remaining.remove(neighbor))queue.add(neighbor);
                }
            }
        }
        assert rings==16 : rings;
        for(int ring=0;ring<16;ring++) for(int sample=0;sample<128;sample++) {
            double theta=ring*Math.PI*2/16,phi=sample*Math.PI*2/128;
            int x=(int)Math.round(166+(156.5+6*Math.cos(phi))*Math.sin(theta));
            int y=(int)Math.round(9.5+6*Math.sin(phi));
            int z=(int)Math.round(166+(156.5+6*Math.cos(phi))*Math.cos(theta));
            assert coilPositions.contains(new net.minecraft.core.BlockPos(x,y,z)) : "Gap in transverse coil ring";
        }
        assert cells[0]>100_000;
        System.out.println("PASS mega fusion: 32768 batch, 2 perfect OCs, truly zero EU, immutable source, 100 EU/heat, overflow rejection, sparse preview "+cells[0]+" blocks and 128 power hatches.");
    }

    private static final class RegistryOwner extends net.minecraftforge.fml.ModContainer {
        RegistryOwner() { super(info()); contextExtension=()->null; }
        @Override public boolean matches(Object value) { return false; }
        @Override public Object getMod() { return null; }
        private static net.minecraftforge.forgespi.language.IModInfo info() {
            var loader=MegaFusionRegressionTest.class.getClassLoader();
            Object config=java.lang.reflect.Proxy.newProxyInstance(loader,
                    new Class<?>[]{net.minecraftforge.forgespi.language.IConfigurable.class},(proxy,method,args)-> {
                        if(method.getName().equals("getConfigElement")) return java.util.Optional.of("IGNORE_ALL_VERSION");
                        if(method.getName().equals("getConfigList")) return java.util.List.of();
                        throw new UnsupportedOperationException(method.getName());
                    });
            return (net.minecraftforge.forgespi.language.IModInfo)java.lang.reflect.Proxy.newProxyInstance(loader,
                    new Class<?>[]{net.minecraftforge.forgespi.language.IModInfo.class},(proxy,method,args)-> {
                        if(method.getName().equals("getModId")) return "gtceu";
                        if(method.getName().equals("getConfig")) return config;
                        throw new UnsupportedOperationException(method.getName());
                    });
        }
    }
}
