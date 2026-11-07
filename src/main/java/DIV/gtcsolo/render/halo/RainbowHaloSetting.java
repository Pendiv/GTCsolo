package DIV.gtcsolo.render.halo;

import committee.nova.mods.avaritia.client.model.loader.base.HaloSetting;

/**
 * Re-Avaritia halo settings plus the duration of one complete HSV hue cycle.
 */
public record RainbowHaloSetting(HaloSetting halo, long cycleMillis) {}
