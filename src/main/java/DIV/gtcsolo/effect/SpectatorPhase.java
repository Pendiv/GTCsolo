package DIV.gtcsolo.effect;

import DIV.gtcsolo.registry.UtilityEffects;
import net.minecraft.world.entity.player.Player;

public interface SpectatorPhase {

    static boolean isActive(Player player) {
        var abilities = player.getAbilities();
        return abilities.mayfly && abilities.flying
                && !player.isSpectator() && !player.isPassenger() && !player.isSleeping()
                && player.isAlive() && player.hasEffect(UtilityEffects.SPECTATOR.get());
    }

    void gtcsolo$refreshSpectatorPhase();
}
