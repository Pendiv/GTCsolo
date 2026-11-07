package DIV.gtcsolo.audit;

import DIV.gtcsolo.recipe.orefactory.OreRouteCompiler;
import java.util.*;
import static DIV.gtcsolo.recipe.orefactory.OreRouteCompiler.*;
import DIV.gtcsolo.recipe.orefactory.OreRouteCompiler.Process;

/** Small deterministic graph fixture; actual ore names and yields remain in source recipes. */
final class OreRouteCompilerTest {
    static Item item(String id,Form form,int amount){return new Item(id,"test:ore",form,amount,10000,10000,0);}
    static Node recipe(String id,Process process,String input,Item... outputs){
        return new Node(id,process,Set.of(input),1,List.of(outputs),0,null);
    }
    static void run(){
        var raw=item("raw",Form.RAW,1);
        var nodes=List.of(
            recipe("root",Process.MACERATOR,"raw",item("crushed",Form.CRUSHED,2)),
            recipe("wash",Process.WASHER,"crushed",item("purified",Form.PURIFIED,1)),
            recipe("bath",Process.CHEMICAL_BATH,"crushed",item("purified",Form.PURIFIED,1),item("dust",Form.DUST,1)),
            recipe("thermal",Process.THERMAL_CENTRIFUGE,"purified",item("refined",Form.REFINED,1)),
            recipe("grind",Process.MACERATOR,"refined",item("dust",Form.DUST,1)),
            recipe("sift",Process.SIFTER,"purified",item("gem",Form.OTHER,1)));
        var result=new OreRouteCompiler(nodes,Map.of("raw",raw)).compile();
        assert result.plans().size()==3:result.skipped();
        for(var plan:result.plans()){
            assert plan.steps().get(0).runs()==1;
            assert plan.steps().stream().skip(1).allMatch(s->s.runs()==2);
            assert plan.steps().get(plan.steps().size()-1).forwardedOutput()==-1;
            assert plan.steps().size()==(plan.route()==Route.SEPARATION?3:4);
        }
        // A 100% dust side product in a bath must not displace purified ore as the main stream.
        var bath=result.plans().stream().filter(p->p.route()==Route.CHEMICAL_WASHING).findFirst().orElseThrow();
        assert bath.steps().get(1).forwardedOutput()==0;
        assert scaled(1,2)==8192&&scaled(1000,2)==8192000;
        assert rebaseChance(1400,10000,850,0)==8200;
        assert rebaseChance(1000,10000,300,0)==3400;
        assert rebaseChance(7000,10000,580,1)==10000;
        assert rebaseChance(3333,9999,0,1)==3333;
        for(int source=0;source<=9;source++)for(int target=9;target<=14;target++){
            int rebased=rebaseChance(1400,10000,850,source);
            assert Math.min(10000,rebased+850*(target-9))==Math.min(10000,1400+850*(target-source-(source==0?1:0)));
        }
        var branched=new ArrayList<>(nodes);
        branched.set(0,recipe("root",Process.MACERATOR,"raw",item("crushed",Form.CRUSHED,2),
                new Item("crushed","test:ore",Form.CRUSHED,1,1400,10000,850)));
        assert new OreRouteCompiler(branched,Map.of("raw",raw)).compile().plans().isEmpty();
        boolean rejected=false;try{scaled(Integer.MAX_VALUE,2);}catch(IllegalArgumentException expected){rejected=true;}
        assert rejected;
        System.out.println("Ore-route regressions passed: three automatic routes, upstream doubling, terminal handling, guaranteed byproducts, chance rebasing, branch rejection and amount limits.");
    }
}
