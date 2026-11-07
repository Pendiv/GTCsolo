package DIV.gtcsolo.audit;

import DIV.gtcsolo.machine.pump.PumpAssistLogic;
import DIV.gtcsolo.machine.pump.PumpAssistLogic.Power;

/** Production boost arithmetic with bounded inventories; no Minecraft process is started. */
public final class PumpAssistRegressionTest {
    private static int checks;

    public static void main(String[] args) {
        for (int base : new int[]{100, 170, 175, 250, 262, 350, 800, 1000, 4000, 6000}) {
            Fixture electric = new Fixture(100_000, 100_000, Integer.MAX_VALUE);
            PumpAssistLogic logic = new PumpAssistLogic();
            for (int tick = 0; tick < 20; tick++) logic.tick(base, electric);
            check(electric.water + base == base * 16, "electric total including passive water");
            check(electric.energy == 100_000 - 320, "electric consumes 16 EU/t");
            check(electric.steam == 100_000, "electric takes priority without steam consumption");

            Fixture steam = new Fixture(0, 100_000, Integer.MAX_VALUE);
            logic = new PumpAssistLogic();
            for (int tick = 0; tick < 20; tick++) logic.tick(base, steam);
            check(steam.water + base == base * 8, "steam total including passive water");
            check(steam.steam == 100_000 - 1280, "steam consumes 64 mB/t");
        }

        Fixture empty = new Fixture(0, 0, 1000);
        PumpAssistLogic logic = new PumpAssistLogic();
        for (int tick = 0; tick < 100; tick++) logic.tick(1000, empty);
        check(empty.water == 0 && empty.fills == 0, "no power leaves passive production alone");
        Fixture nether = new Fixture(1000, 1000, 1000);
        logic.tick(-1, nether);
        logic.tick(0, nether);
        check(nether.water == 0 && nether.energy == 1000 && nether.steam == 1000,
                "no boost for invalid or uninitialized biome");

        Fixture blocked = new Fixture(1000, 1000, 0);
        for (int tick = 0; tick < 100; tick++) logic.tick(1000, blocked);
        check(blocked.energy == 1000 && blocked.steam == 1000, "blocked hatch consumes nothing");
        blocked.capacity = 100_000;
        logic.tick(1000, blocked);
        check(blocked.water == 750 && blocked.energy == 984, "unblocking has no accumulated burst");

        Fixture partial = new Fixture(1000, 1000, 100);
        logic.tick(1000, partial);
        check(partial.water == 100 && partial.energy == 997, "partial fill costs rounded-up proportion");
        check(partial.steam == 1000, "partial electric fill does not consume steam");
        for (int tick = 0; tick < 20; tick++) {
            partial.water = 99;
            logic.tick(1000, partial);
        }
        check(partial.energy == 977, "one mB openings cannot bypass the power cost");

        Fixture switching = new Fixture(16, 128, 100_000);
        logic.tick(1000, switching);
        logic.tick(1000, switching);
        logic.tick(1000, switching);
        logic.tick(1000, switching);
        check(switching.water == 750 + 350 + 350, "power loss falls back to steam then passive");
        check(switching.energy == 0 && switching.steam == 0, "no negative inventories");
        Fixture insufficient = new Fixture(15, 63, 1000);
        logic.tick(1000, insufficient);
        check(insufficient.water == 0 && insufficient.energy == 15 && insufficient.steam == 63,
                "insufficient input is not consumed");

        Fixture changed = new Fixture(1000, 1000, 1000);
        changed.executionLimit = 100;
        logic.tick(1000, changed);
        check(changed.water == 100 && changed.energy == 997, "reduced actual fill refunds unused EU");
        Fixture rejected = new Fixture(1000, 1000, 1000);
        rejected.executionLimit = 0;
        logic.tick(1000, rejected);
        check(rejected.water == 0 && rejected.energy == 1000, "failed fill fully refunds EU");
        Fixture rejectedSteam = new Fixture(0, 1000, 1000);
        rejectedSteam.executionLimit = 0;
        logic.tick(1000, rejectedSteam);
        check(rejectedSteam.steam == 1000, "failed fill fully refunds steam");
        Fixture lostPower = new Fixture(1000, 1000, 1000);
        lostPower.rejectPayment = true;
        logic.tick(1000, lostPower);
        check(lostPower.water == 0 && lostPower.energy == 1000, "payment failure generates no water");

        Fixture split = new Fixture(1000, 0, 1000);
        logic.tick(175, split);
        check(split.water == 131, "fractional tick rounds down");
        logic.reset();
        split.water = 0;
        logic.tick(175, split);
        check(split.water == 131, "invalidation resets the fractional tick");

        Fixture large = new Fixture(1000, 0, Integer.MAX_VALUE);
        logic.tick(Integer.MAX_VALUE, large);
        check(large.water == 1_610_612_735 && large.energy == 984, "rate multiplication uses long arithmetic");
        System.out.println("PASS: " + checks + " pump boost checks (production logic, isolated inventories).");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static final class Fixture implements PumpAssistLogic.Ports {
        long energy;
        int steam;
        int water;
        int capacity;
        int fills;
        int executionLimit = Integer.MAX_VALUE;
        boolean rejectPayment;

        Fixture(long energy, int steam, int capacity) {
            this.energy = energy;
            this.steam = steam;
            this.capacity = capacity;
        }

        @Override public long storedEnergy() { return energy; }
        @Override public int storedSteam() { return steam; }

        @Override public int fillWater(int amount, boolean simulate) {
            fills++;
            int accepted = Math.min(amount, capacity - water);
            if (!simulate) {
                accepted = Math.min(accepted, executionLimit);
                water += accepted;
            }
            return accepted;
        }

        @Override public boolean consume(Power power, int amount) {
            if (rejectPayment) return false;
            if (power == Power.ELECTRIC) {
                if (energy < amount) return false;
                energy -= amount;
            } else {
                if (steam < amount) return false;
                steam -= amount;
            }
            return true;
        }

        @Override public void refund(Power power, int amount) {
            if (power == Power.ELECTRIC) energy += amount;
            else steam += amount;
        }
    }
}
