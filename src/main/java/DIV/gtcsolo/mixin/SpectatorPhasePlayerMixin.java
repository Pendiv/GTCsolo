package DIV.gtcsolo.mixin;

import DIV.gtcsolo.effect.SpectatorPhase;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class SpectatorPhasePlayerMixin implements SpectatorPhase {

    @Unique private boolean gtcsolo$phasing;
    @Unique private Level gtcsolo$clearLevel;
    @Unique private Vec3 gtcsolo$clearPosition;

    @Inject(method = "tick", at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/entity/player/Player;noPhysics:Z",
            opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void gtcsolo$setSpectatorPhaseAfterVanilla(CallbackInfo callback) {
        gtcsolo$updateSpectatorPhase(true);
    }

    @Inject(method = "travel", at = @At("HEAD"))
    private void gtcsolo$refreshBeforeTravel(CallbackInfo callback) {
        if (((Player) (Object) this).level().isClientSide) gtcsolo$refreshSpectatorPhase();
    }

    @Redirect(method = "updatePlayerPose", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;isSpectator()Z"))
    private boolean gtcsolo$useSpectatorPose(Player player) {
        return player.isSpectator() || SpectatorPhase.isActive(player);
    }

    @Override
    public void gtcsolo$refreshSpectatorPhase() {
        gtcsolo$updateSpectatorPhase(false);
    }

    @Unique
    private void gtcsolo$updateSpectatorPhase(boolean trackPosition) {
        Player player = (Player) (Object) this;
        if (SpectatorPhase.isActive(player)) {
            if (player instanceof ServerPlayer && (trackPosition || !gtcsolo$phasing)) {
                if (gtcsolo$clearLevel != player.level()) {
                    gtcsolo$clearLevel = player.level();
                    gtcsolo$clearPosition = null;
                }
                if (gtcsolo$isClear(player, player.getBoundingBox().deflate(1.0E-5))) {
                    gtcsolo$clearPosition = player.position();
                }
            }
            player.noPhysics = true;
            if (trackPosition || !gtcsolo$phasing) player.setOnGround(false);
            gtcsolo$phasing = true;
        } else if (gtcsolo$phasing) {
            gtcsolo$phasing = false;
            player.noPhysics = player.isSpectator();
            if (player instanceof ServerPlayer serverPlayer && !player.noPhysics
                    && player.isAlive() && !player.isPassenger() && !player.isSleeping()
                    && gtcsolo$clearLevel == player.level() && gtcsolo$clearPosition != null) {
                AABB box = player.getBoundingBox().deflate(1.0E-5);
                AABB exit = box.move(gtcsolo$clearPosition.subtract(player.position()));
                if (player.level().getBlockCollisions(player, box).iterator().hasNext()
                        && gtcsolo$isClear(player, exit)) {
                    serverPlayer.teleportTo(gtcsolo$clearPosition.x, gtcsolo$clearPosition.y, gtcsolo$clearPosition.z);
                }
            }
            gtcsolo$clearLevel = null;
            gtcsolo$clearPosition = null;
        }
    }

    @Unique
    private static boolean gtcsolo$isClear(Player player, AABB box) {
        Level level = player.level();
        return level.hasChunksAt(BlockPos.containing(box.minX, box.minY, box.minZ),
                BlockPos.containing(box.maxX, box.maxY, box.maxZ))
                && level.getWorldBorder().isWithinBounds(box)
                && !level.getBlockCollisions(player, box).iterator().hasNext();
    }
}
