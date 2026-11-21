package DIV.gtcsolo.integration.mekanism;

/** One packet allowance shared by all input faces and all machine ticks at the same world time. */
public final class InputAmperageBudget {
    private long worldTick = Long.MIN_VALUE;
    private long used;

    public long remaining(long gameTime, long limit) {
        if (worldTick != gameTime) {
            worldTick = gameTime;
            used = 0;
        }
        return Math.max(0, limit - used);
    }

    public void record(long accepted) {
        used += accepted;
    }
}
