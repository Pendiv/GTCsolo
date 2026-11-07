package DIV.gtcsolo.torcherino;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import torcherino.api.TierSupplier;

public enum Profile {
    LV("LV", 2), MV("MV", 3), HV("HV", 5), EV("EV", 7), IV("IV", 11),
    LUV("LuV", 13), ZPM("ZPM", 17), UV("UV", 19), UHV("UHV", 23), UEV("UEV", 29),
    UIV("UIV", 31), UXV("UXV", 37), OPV("OpV", 41), MAX("MAX", 43);
    public final String label;
    public final int defaultPower;
    Profile(String label, int power) { this.label = label; this.defaultPower = power; }
    public String path() { return name().toLowerCase(java.util.Locale.ROOT) + "_torcherino"; }
    public String lanternPath() { return name().toLowerCase(java.util.Locale.ROOT) + "_lanterino"; }
    public String pumpkinPath() { return name().toLowerCase(java.util.Locale.ROOT) + "_jack_o_lanterino"; }
    public ResourceLocation id() { return new ResourceLocation(TieredTorcherino.ID, path()); }
    public static Profile from(ResourceLocation id) {
        if (!TieredTorcherino.ID.equals(id.getNamespace())) return null;
        for (Profile profile : values()) {
            if (profile.path().equals(id.getPath()) || profile.lanternPath().equals(id.getPath())
                    || profile.pumpkinPath().equals(id.getPath())) return profile;
        }
        return null;
    }
    public static Profile from(BlockState state) {
        return state.getBlock() instanceof TierSupplier supplier ? from(supplier.getTier()) : null;
    }
}
