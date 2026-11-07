package DIV.gtcsolo.audit;

import DIV.gtcsolo.machine.IsmbfRules;

/** Boundary and conservation checks for the ISMBF rules, without a client or world. */
final class IsmbfRegressionTest {
    static void run() {
        double batched = IsmbfRules.cool(12000, 40);
        double ticked = 12000;
        for (int i = 0; i < 40; i++) ticked = IsmbfRules.cool(ticked);
        assert Math.abs(batched - ticked) < 1e-7 : "batched cooling preserves 7% per second";
        assert IsmbfRules.cool(12000, 0) == 12000 : "powered ticks do not cool";
        assert Math.abs(IsmbfRules.cool(12000, 20) - 11160) < 1e-7;
        assert IsmbfRules.cool(1, 40) == IsmbfRules.AMBIENT : "cooling reaches ambient";
        assert !IsmbfRules.canStartExtreme(10800, 12000, 10800) : "insufficient coil heat resistance";
        assert IsmbfRules.canStartExtreme(12600, 12000, 12600) : "start gate is independent of 16,000 F operation";
        assert !IsmbfRules.canStartExtreme(12600, 12000, 12000) : "existing full-heat requirement remains";
        assert IsmbfRules.canStartExtreme(14400, 12000, 14400);
        assert IsmbfRules.canStartExtreme(10800, 0, 10800) : "older recipes without a gate remain compatible";
        assert !IsmbfRules.canProcessExtreme(12600, 12000, 12600, 16000)
                : "passing the coil gate does not provide free operating heat";
        long extraHeatEU = IsmbfRules.heatingEU(12600, 16000);
        assert extraHeatEU == 1_700_000 : "500 EU/F applies above the coil limit too";
        double halfHeated = IsmbfRules.heatAfterPayment(12600, 16000, extraHeatEU / 2);
        assert halfHeated == 14300;
        assert !IsmbfRules.canProcessExtreme(12600, 12000, halfHeated, 16000);
        double fullyHeated = IsmbfRules.heatAfterPayment(halfHeated, 16000, extraHeatEU / 2);
        assert fullyHeated == 16000 && IsmbfRules.canProcessExtreme(12600, 12000, fullyHeated, 16000);
        assert IsmbfRules.heatingEU(fullyHeated, 16000) == 0 : "retained paid heat is not charged twice";
        assert IsmbfRules.heatAfterPayment(12600, 16000, 0) == 12600 : "no energy means no heating";
        assert IsmbfRules.heatingEU(0, 16000) == 8_000_000;
        assert IsmbfRules.heatAfterPayment(16000, 12600, 0) == 16000 : "target changes do not delete paid heat";
        assert IsmbfRules.smeltingBonus(10000, true) == 4096;
        assert IsmbfRules.smeltingBonus(10000.01, true) == 4096 * 4;
        assert IsmbfRules.smeltingBonus(12000, true) == 4096 * 4;
        assert IsmbfRules.smeltingBonus(12000.01, true) == 4096 * 16;
        assert IsmbfRules.smeltingBonus(14000, true) == 4096 * 16;
        assert IsmbfRules.smeltingBonus(14000.01, true) == 4096 * 64;
        assert IsmbfRules.smeltingBonus(14400, false) == 4096 : "concurrent smelting has no exclusive bonus";
        assert IsmbfRules.durationBonus(10000, false) == 1;
        assert IsmbfRules.durationBonus(10000.01, false) == 0.75;
        assert IsmbfRules.durationBonus(12000, true) == 0.8;
        assert IsmbfRules.durationBonus(12000.01, true) == 0.75;
        assert IsmbfRules.durationBonus(14000.01, true) == 0.66;
        double heat = 14000;
        for (int i = 0; i < 20; i++) heat = IsmbfRules.cool(heat);
        assert Math.abs(heat - 14000 * 0.93) < 1e-6 : "7% cooling per second, not per tick";
        for (int i = 0; i < 10000; i++) heat = IsmbfRules.cool(heat);
        assert heat == 0 : "ambient and overheat unlock must be reachable";
        double discount = Math.pow(0.95, 10);
        assert Math.abs(IsmbfRules.convertedDiscount(1800, 10800, false) - discount) < 1e-10;
        assert Math.abs(IsmbfRules.convertedDiscount(1800, 10800, true) - discount * discount) < 1e-10;
        double originalDiscountedEnergy = 120 * discount * 200;
        double convertedEnergy = 120 * (200 * IsmbfRules.convertedDiscount(1800, 10800, false));
        assert Math.abs(convertedEnergy - originalDiscountedEnergy) < 1e-8;
        assert 120 * 200 * IsmbfRules.convertedDiscount(1800, 10800, true) < originalDiscountedEnergy;
        var standard = IsmbfRules.overclock(32, 200, 2048, 0, false, 1);
        assert standard.eut() == 2048 && standard.duration() == 25 && standard.parallels() == 1;
        var perfect = IsmbfRules.overclock(32, 200, 2048, 3, false, 1);
        assert perfect.eut() == 2048 && perfect.duration() == 4 && perfect.stages() == 3;
        var subtick = IsmbfRules.overclock(32, 1, 2048, 3, true, 64);
        assert subtick.eut() == 2048 && subtick.duration() == 1 && subtick.parallels() == 64;
        var restricted = IsmbfRules.overclock(32, 1, 2048, 3, true, 15);
        assert restricted.parallels() == 4 && restricted.eut() == 128 : "never overconsume limited inputs";
        var extreme = IsmbfRules.overclock(32, 1, 2048, 0, false, 1);
        assert extreme.parallels() == 1 && extreme.stages() == 0 && extreme.eut() == 32;
        assert IsmbfRules.overclock(2049, 200, 2048, 0, true, 100) == null : "reject voltage above hatches";
        var huge = IsmbfRules.overclock(1, 1, Long.MAX_VALUE, 100, true, Integer.MAX_VALUE);
        assert huge.eut() > 0 && huge.parallels() > 0 && huge.duration() == 1 : "no overflow at extreme voltages";
        var fractional = IsmbfRules.overclock(120, 100.25, 120, 0, true, 1);
        assert fractional.duration() == 101 && fractional.lastTickEU() == 30;
        assert fractional.eut() * (fractional.duration() - 1L) + fractional.lastTickEU() == 12030
                : "whole-tick rounding must not add energy cost";
        var fractionalSubtick = IsmbfRules.overclock(120, 0.3, 120, 0, true, 3);
        assert fractionalSubtick.parallels() == 3 && fractionalSubtick.duration() == 1;
        assert fractionalSubtick.lastTickEU() == 108 : "partial sub-tick batches conserve EU";
        assert IsmbfRules.EXPLOSION_STRENGTH == 15 && IsmbfRules.EXPLOSION_COUNT == 3;
        double health = IsmbfRules.MAX_HEALTH;
        for (int tick = 0; tick < 99; tick++)
            health = IsmbfRules.healthAfterCooling(health, IsmbfRules.coolingPerTick(20000));
        assert health == 1000 : "20,000 F without cooling survives exactly 99 ticks";
        assert IsmbfRules.healthAfterCooling(health, IsmbfRules.coolingPerTick(20000)) == 0
                : "20,000 F without cooling overheats after 5 seconds";
        health = IsmbfRules.MAX_HEALTH;
        for (int tick = 0; tick < 20; tick++)
            health = IsmbfRules.healthAfterCooling(health, IsmbfRules.coolingPerTick(20000) - 500);
        assert health == 90000 : "10,000 cooling per second leaves a 10,000 per second deficit";
        assert IsmbfRules.healthAfterCooling(100000, -1) == 100000 : "excess coolant is not extra health";
    }
}
