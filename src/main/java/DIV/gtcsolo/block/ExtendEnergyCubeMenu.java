package DIV.gtcsolo.block;

import DIV.gtcsolo.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ExtendEnergyCubeMenu extends AbstractContainerMenu {

    private final BlockPos blockPos;
    private long stored;
    private boolean emitMode;
    private boolean euMode;

    /** サーバーサイドコンストラクタ */
    public ExtendEnergyCubeMenu(int id, BlockPos blockPos, long stored, boolean emitMode, boolean euMode) {
        super(ModMenuTypes.EXTEND_ENERGY_CUBE.get(), id);
        this.blockPos = blockPos;
        this.stored = stored;
        this.emitMode = emitMode;
        this.euMode = euMode;
    }

    /** クライアントサイドコンストラクタ */
    public ExtendEnergyCubeMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        super(ModMenuTypes.EXTEND_ENERGY_CUBE.get(), id);
        this.blockPos = buf.readBlockPos();
        this.stored = buf.readLong();
        this.emitMode = buf.readBoolean();
        this.euMode = buf.readBoolean();
    }

    public BlockPos getBlockPos() { return blockPos; }
    public long getStored() { return stored; }
    public boolean isEmitMode() { return emitMode; }
    public boolean isEuMode() { return euMode; }

    /** パケット受信時にデータを更新する */
    public void updateData(long stored, boolean emitMode, boolean euMode) {
        this.stored = stored;
        this.emitMode = emitMode;
        this.euMode = euMode;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        return ItemStack.EMPTY;
    }
}