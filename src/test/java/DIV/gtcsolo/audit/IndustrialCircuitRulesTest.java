package DIV.gtcsolo.audit;
import DIV.gtcsolo.machine.IndustrialCircuitRules;

public final class IndustrialCircuitRulesTest {
    private static int checks;
    private static void check(boolean value) { checks++; if (!value) throw new AssertionError("check " + checks); }
    public static void main(String[] args) {
        check(IndustrialCircuitRules.ordinaryBonus(2,1)*4==128);
        var one=IndustrialCircuitRules.overclock(30,200,120);
        check(one.stages()==1&&one.eut()*4==480&&one.duration()==100);
        check(IndustrialCircuitRules.overclock(30,25,120).duration()==12);
        var fast=IndustrialCircuitRules.overclock(30,1,30*64);
        check(fast.stages()==3&&fast.duration()==1); // OC continues past the one-tick floor.
        check(IndustrialCircuitRules.overclock(31,10,30)==null);
        check(IndustrialCircuitRules.overclock(0,0,100).stages()==0);
        for(int modules=1;modules<=6;modules++) {
            check(1+IndustrialCircuitRules.extraLimit(modules)==1+2*modules);
            for(int stage=0;stage<20;stage++) check(IndustrialCircuitRules.ordinaryBonus(modules,stage)==8+8*modules+8*stage);
        }
        var edge=IndustrialCircuitRules.overclock(1,Integer.MAX_VALUE,Long.MAX_VALUE);
        check(edge.eut()>0&&edge.eut()<=Long.MAX_VALUE&&edge.stages()==31&&edge.duration()==1);
        check(IndustrialCircuitRules.extraLimit(6)==12);
        System.out.println("IndustrialCircuitRules: "+checks+" checks passed");
    }
}
