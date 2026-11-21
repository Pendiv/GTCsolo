package DIV.gtcsolo.integration.mekanism;

import mekanism.api.Upgrade;
import mekanism.common.tier.FactoryTier;
import mekanism.common.tile.factory.TileEntityFactory;

public final class SlotUpgrade {
    public static final int MAX = 8;
    public static final Upgrade TYPE = Upgrade.valueOf("GTCSOLO_SLOT");

    private SlotUpgrade() {}

    public static int processes(TileEntityFactory<?> factory) {
        return factory.supportsUpgrades() ? 1 + factory.getComponent().getUpgrades(TYPE) : 1;
    }

    public static FactoryTier layout(FactoryTier tier) {
        return tier.processes < 1 + MAX ? FactoryTier.ULTIMATE : tier;
    }
}
