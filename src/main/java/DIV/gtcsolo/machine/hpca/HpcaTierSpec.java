package DIV.gtcsolo.machine.hpca;

/** Component values consumed directly by the native HPCA grid. */
public enum HpcaTierSpec {
    UHV("uhv", 64, 8, 30_720, 491_520, 8, 30_720, 32),
    UEV("uev", 256, 32, 122_880, 1_966_080, 32, 122_880, 128);

    private final String path;
    private final int computationPerTick;
    private final int computationCoolingDemand;
    private final int computationUpkeepEUt;
    private final int computationMaxEUt;
    private final int coolingPerTick;
    private final int coolerEUt;
    private final int coolantPerTick;

    HpcaTierSpec(String path, int computationPerTick, int computationCoolingDemand,
                 int computationUpkeepEUt, int computationMaxEUt, int coolingPerTick,
                 int coolerEUt, int coolantPerTick) {
        this.path = path;
        this.computationPerTick = computationPerTick;
        this.computationCoolingDemand = computationCoolingDemand;
        this.computationUpkeepEUt = computationUpkeepEUt;
        this.computationMaxEUt = computationMaxEUt;
        this.coolingPerTick = coolingPerTick;
        this.coolerEUt = coolerEUt;
        this.coolantPerTick = coolantPerTick;
    }

    public String path() { return path; }
    public int computationPerTick() { return computationPerTick; }
    public int computationCoolingDemand() { return computationCoolingDemand; }
    public int computationUpkeepEUt() { return computationUpkeepEUt; }
    public int computationMaxEUt() { return computationMaxEUt; }
    public int coolingPerTick() { return coolingPerTick; }
    public int coolerEUt() { return coolerEUt; }
    public int coolantPerTick() { return coolantPerTick; }

    String iconPath(String component) {
        return "gtcsolo:textures/gui/widget/hpca/" + path + "_" + component + ".png";
    }
}
