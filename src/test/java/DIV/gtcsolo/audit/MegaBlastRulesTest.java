package DIV.gtcsolo.audit;

import DIV.gtcsolo.machine.mega.MegaBlastRules;
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic;

public final class MegaBlastRulesTest {
    public static void run() {
        assert MegaBlastRules.normalTemperature(7200) == 10800;
        assert MegaBlastRules.normalTemperature(Integer.MAX_VALUE) == Integer.MAX_VALUE;
        assert MegaBlastRules.heatingEU(10800, 20000, 1) == 288866200000L;
        assert MegaBlastRules.heatingEU(10800, 20000, 8) == 36108275000L;
        assert MegaBlastRules.heatingEU(20000, 10800, 1) == 0;
        assert MegaBlastRules.heatAfterPayment(10800, 20000, 8, 0) == 10800;
        double halfway = MegaBlastRules.heatAfterPayment(10800, 20000, 8, 18054137500L);
        assert halfway == 15400 : halfway;
        assert MegaBlastRules.heatAfterPayment(10800, 20000, 8, Long.MAX_VALUE) == 20000;
        double heat = 20000;
        for (int i=0;i<20;i++) heat = MegaBlastRules.cool(heat);
        assert Math.abs(heat - 19868) < 0.000001 : heat;
        for (int i=20;i<200;i++) heat = MegaBlastRules.cool(heat);
        assert Math.abs(heat - 20000 * Math.pow(0.9934,10)) < 0.000001;
        assert MegaBlastRules.sanitize(Double.NaN) == 0;
        assert MegaBlastRules.sanitize(Double.POSITIVE_INFINITY) == 0;
        assert MegaBlastRules.heatingEU(0, Integer.MAX_VALUE, 1) > 0;
        var ordinary = OverclockingLogic.heatingCoilOC(new OverclockingLogic.OCParams(32,320,4,1), 8192,900,900);
        var hot = OverclockingLogic.heatingCoilOC(new OverclockingLogic.OCParams(32,320,4,1), 8192,900,4500);
        assert ordinary.durationMultiplier() == 1.0/16;
        assert hot.durationMultiplier() == 1.0/64;
        assert Math.abs(OverclockingLogic.getCoilEUtDiscount(900,4500)-Math.pow(.95,4))<1e-10;
        var sub = OverclockingLogic.heatingCoilOC(new OverclockingLogic.OCParams(32,1,3,32), 2048,900,4500);
        assert sub.parallels()==32 && sub.ocLevel()==3;
        var bounded = OverclockingLogic.heatingCoilOC(new OverclockingLogic.OCParams(32,1,3,16), 2048,900,4500);
        assert bounded.parallels()==16 && bounded.ocLevel()==2;
        assert Double.isFinite(OverclockingLogic.getCoilEUtDiscount(900,Integer.MAX_VALUE));
        System.out.println("Mega blast rules passed: heating payment, coil efficiency, exact idle decay, temperature OC and bounded sub-tick parallels.");
    }
}
