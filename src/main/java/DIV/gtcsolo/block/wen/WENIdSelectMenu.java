package DIV.gtcsolo.block.wen;

import DIV.gtcsolo.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/** ワイヤレスハッチ用の簡易ID選択メニュー */
public class WENIdSelectMenu extends AbstractContainerMenu {

    private final BlockPos machinePos;
    private final List<String> availableIds = new ArrayList<>();
    private String currentId;

    public WENIdSelectMenu(int id, BlockPos machinePos, String currentId, List<String> ids) {
        super(ModMenuTypes.WEN_ID_SELECT.get(), id);
        this.machinePos = machinePos;
        this.currentId = currentId;
        this.availableIds.addAll(ids);
    }

    public WENIdSelectMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        super(ModMenuTypes.WEN_ID_SELECT.get(), id);
        this.machinePos = buf.readBlockPos();
        this.currentId = buf.readUtf(64);
        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) availableIds.add(buf.readUtf(64));
    }

    public BlockPos getMachinePos() { return machinePos; }
    public String getCurrentId() { return currentId; }
    public List<String> getAvailableIds() { return availableIds; }
    public void setCurrentId(String id) { this.currentId = id; }

    @Override public boolean stillValid(@NotNull Player p) { return true; }
    @Override public @NotNull ItemStack quickMoveStack(@NotNull Player p, int i) { return ItemStack.EMPTY; }
}