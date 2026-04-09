package DIV.gtcsolo.block;

import DIV.gtcsolo.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;

public class ExtendEnergyCubeMenu extends AbstractContainerMenu {

    private final BlockPos blockPos;
    private BigInteger stored;
    private BigInteger outputRate;

    /** サーバーサイドコンストラクタ */
    public ExtendEnergyCubeMenu(int id, BlockPos blockPos, BigInteger stored, BigInteger outputRate) {
        super(ModMenuTypes.EXTEND_ENERGY_CUBE.get(), id);
        this.blockPos = blockPos;
        this.stored = stored;
        this.outputRate = outputRate;
    }

    /** クライアントサイドコンストラクタ（MenuType.create から呼ばれる） */
    public ExtendEnergyCubeMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        super(ModMenuTypes.EXTEND_ENERGY_CUBE.get(), id);
        this.blockPos = buf.readBlockPos();
        this.stored = new BigInteger(buf.readUtf(512));
        this.outputRate = new BigInteger(buf.readUtf(512));
    }

    public BlockPos getBlockPos() { return blockPos; }
    public BigInteger getStored() { return stored; }
    public BigInteger getOutputRate() { return outputRate; }

    /** パケット受信時にデータを更新する */
    public void updateData(BigInteger stored, BigInteger outputRate) {
        this.stored = stored;
        this.outputRate = outputRate;
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
