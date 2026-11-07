package DIV.gtcsolo.audit;

import DIV.gtcsolo.machine.wen.WENNetworkData;
import DIV.gtcsolo.util.EnergyMath;
import java.math.BigInteger;

/** Offline regressions; run with -ea. No client or world is started. */
public final class IndustrialRegressionTest {
    public static void main(String[] args) {
        TorchWardIndexTest.main(new String[0]);
        PreviewFrameLeaseTest.run();
        try { MegaPreviewCursorTest.run(); }
        catch (Exception failure) { throw new AssertionError(failure); }
        OreRouteCompilerTest.run();
        MegaBlastRulesTest.run();
        IsmbfRegressionTest.run();
        IterativeAssemblyRegressionTest.run();
        RoomRecipeRulesTest.run();
        RoomPreviewRegressionTest.run();
        ForgeOfSunPatternTest.run();
        var data = new WENNetworkData();
        assert data.registerNetwork("test", "minecraft:overworld");
        data.updateCapacity("test", BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.TEN));
        assert data.addEnergy("test", Long.MAX_VALUE) == Long.MAX_VALUE;
        assert data.addEnergy("test", 1) == 1;
        var entry = data.getNetwork("test");
        assert entry.inputThisTick == Long.MAX_VALUE : "rate display must saturate, not wrap";
        data.recordHistory(0);
        assert entry.energyHistory.size() == 1 : "first history sample must not overflow sentinel";
        assert entry.inputLastPeriod == Long.MAX_VALUE;
        assert entry.inputThisTick == 0;
        data.recordHistory(0);
        data.recordHistory(19);
        assert entry.energyHistory.size() == 1 : "multiple monitors must not duplicate samples";
        data.recordHistory(20);
        assert entry.energyHistory.size() == 2;
        for (int i = 2; i < 100; i++) data.recordHistory(i * 20L);
        assert entry.energyHistory.size() == WENNetworkData.HISTORY_SIZE;
        data.recordHistory(0);
        data.recordHistory(20);
        assert entry.energyHistory.size() == WENNetworkData.HISTORY_SIZE : "clock rewind must resume";
        data.updateCapacity("test", BigInteger.valueOf(-1));
        assert entry.maxCapacity.equals(BigInteger.ZERO);
        assert entry.storedEnergy.signum() > 0 : "unformed storage preserves energy";
        data.registerNetwork("occupied", "minecraft:overworld");
        assert !data.renameNetwork("test", "occupied", "minecraft:overworld");
        assert data.getNetwork("test") == entry : "failed rename must preserve original network";
        assert data.renameNetwork("test", "renamed", "minecraft:the_nether");
        assert !data.hasNetwork("test") && data.getNetwork("renamed") == entry;
        assert data.renameNetwork("renamed", "renamed", "minecraft:the_nether");
        assert entry.storedEnergy.signum() > 0 : "rename must preserve stored energy";
        assert EnergyMath.clampEuChange(0, Long.MAX_VALUE, Long.MAX_VALUE, 4) == Long.MAX_VALUE / 4;
        assert EnergyMath.clampEuChange(Long.MAX_VALUE, Long.MAX_VALUE, 1, 4) == 0;
        assert EnergyMath.clampEuChange(Long.MAX_VALUE, Long.MAX_VALUE, Long.MIN_VALUE, 4) == -(Long.MAX_VALUE / 4);
        assert EnergyMath.clampEuChange(3, Long.MAX_VALUE, -1, 4) == 0 : "retain sub-EU remainder";
        assert EnergyMath.clampEuChange(9, Long.MAX_VALUE, -1, 4) == -1;
        double prepaid = 0;
        long spent = 0;
        for (int i = 0; i < 400; i++) {
            var transfer = EnergyMath.settleOutput(1, prepaid, 4);
            prepaid = transfer.prepaid(); spent += transfer.eu();
            assert spent * 4 == i + 1 + prepaid : "fractional FE must conserve energy";
        }
        assert spent == 100 && prepaid == 0;
        prepaid = 0; spent = 0;
        for (int i = 0; i < 800; i++) {
            var transfer = EnergyMath.settleOutput(0.25, prepaid, 2);
            prepaid = transfer.prepaid(); spent += transfer.eu();
            assert spent * 2 == (i + 1) * 0.25 + prepaid : "fractional AE must conserve energy";
        }
        assert spent == 100 && prepaid == 0;
        assert DIV.gtcsolo.util.RecipeMath.saturatedParallelCount(1.49) == 1;
        assert DIV.gtcsolo.util.RecipeMath.saturatedParallelCount(1.5) == 2;
        assert DIV.gtcsolo.util.RecipeMath.saturatedParallelCount(Math.pow(1.25, 200)) == Integer.MAX_VALUE;
        assert DIV.gtcsolo.util.RecipeMath.saturatedParallelCount(Double.POSITIVE_INFINITY) == Integer.MAX_VALUE;
        var reduction = new com.gregtechceu.gtceu.api.recipe.content.ContentModifier(0.75, 0.75);
        for (int n = 1; n <= 10000; n++) {
            assert (int) reduction.apply(n) == (3L * n + 3) / 4 : "assembly input ceil at " + n;
        }
        System.out.println("Industrial regressions passed: iterative AL, ISMBF, history, multi-monitor sampling, overflow, EU conversion.");
    }
}
