package DIV.gtcsolo.block.copy;

import DIV.gtcsolo.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.UUID;

public final class PasteBlockEntity extends BlockEntity {
    public static final String COPY_ID = "gtcsolo_copy_id", COPY_NAME = "gtcsolo_copy_name";
    private UUID copyId;
    private String copyName = "";
    public PasteBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.PASTE_BLOCK.get(), pos, state); }
    public UUID copyId() { return copyId; }
    public String copyName() { return copyName; }
    public static void bind(ItemStack stack, UUID id, String name) {
        stack.getOrCreateTag().putUUID(COPY_ID, id);
        stack.getOrCreateTag().putString(COPY_NAME, name);
        stack.setHoverName(net.minecraft.network.chat.Component.literal(name));
    }
    public void readItem(ItemStack stack) {
        var tag = stack.getTag();
        copyId = tag != null && tag.hasUUID(COPY_ID) ? tag.getUUID(COPY_ID) : null;
        copyName = tag == null ? "" : tag.getString(COPY_NAME);
        if (copyName.length() > 80) copyName = copyName.substring(0, 80);
        setChanged();
    }
    public ItemStack bindItem(ItemStack stack) {
        if (copyId != null) bind(stack, copyId, copyName);
        return stack;
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        copyId = tag.hasUUID(COPY_ID) ? tag.getUUID(COPY_ID) : null;
        copyName = tag.getString(COPY_NAME);
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (copyId != null) tag.putUUID(COPY_ID, copyId);
        tag.putString(COPY_NAME, copyName);
    }
}
