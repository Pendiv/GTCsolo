package DIV.gtcsolo.integration.ae2;

import DIV.gtcsolo.block.wen.WENDataMonitorBlockEntity;
import DIV.gtcsolo.machine.WENMainStorageMachine;
import appeng.items.materials.UpgradeCardItem;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * WEN ワイヤレスエネルギーカード
 *   UpgradeCardItem 継承 → AE2スロット (RestrictedInputSlot.UPGRADES) が受理する
 *   NBT 「wen_network_id」 に紐付けWEN網を保持
 *   Shift+右クリック on WEN Data Monitor / Main Storage controller → 自動バインド
 *   (バインド未対象なら super.onItemUseFirst → AE2 の「クリックでアップグレードスロットに挿入」へ委譲)
 */
public class WENEnergyCardItem extends UpgradeCardItem {
    public static final String TAG_NETWORK_ID = "wen_network_id";

    public WENEnergyCardItem(Properties props) {
        super(props);
    }

    public static String getBoundNetworkId(ItemStack stack) {
        if (!stack.hasTag()) return "";
        return stack.getTag().getString(TAG_NETWORK_ID);
    }

    public static void setBoundNetworkId(ItemStack stack, String id) {
        if (id == null || id.isEmpty()) {
            if (stack.hasTag()) stack.getTag().remove(TAG_NETWORK_ID);
        } else {
            stack.getOrCreateTag().putString(TAG_NETWORK_ID, id);
        }
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
        Player player = ctx.getPlayer();
        Level level = ctx.getLevel();
        if (player != null && player.isShiftKeyDown()) {
            BlockEntity be = level.getBlockEntity(ctx.getClickedPos());
            String networkId = extractNetworkId(be);
            if (networkId != null && !networkId.isEmpty()) {
                if (!level.isClientSide) {
                    setBoundNetworkId(stack, networkId);
                    player.displayClientMessage(
                            Component.translatable("gtcsolo.wen_energy_card.bound", networkId)
                                    .withStyle(ChatFormatting.AQUA),
                            true);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        // 非WENブロックへの shift+右クリックは AE2 のアップグレード挿入処理に委譲
        return super.onItemUseFirst(stack, ctx);
    }

    @Nullable
    private static String extractNetworkId(@Nullable BlockEntity be) {
        if (be == null) return null;
        if (be instanceof WENDataMonitorBlockEntity monitor) {
            WENMainStorageMachine ctrl = monitor.getController();
            return ctrl != null ? ctrl.getNetworkId() : null;
        }
        if (be instanceof IMachineBlockEntity mbe
                && mbe.getMetaMachine() instanceof WENMainStorageMachine wen) {
            return wen.getNetworkId();
        }
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        // AE2標準 (Supported by: ...) を先に表示
        super.appendHoverText(stack, level, tooltip, flag);

        // WEN紐付け状態
        String bound = getBoundNetworkId(stack);
        if (bound.isEmpty()) {
            tooltip.add(Component.translatable("gtcsolo.wen_energy_card.unlinked")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("gtcsolo.wen_energy_card.linked", bound)
                    .withStyle(ChatFormatting.AQUA));
        }
        tooltip.add(Component.translatable("gtcsolo.wen_energy_card.desc.1")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("gtcsolo.wen_energy_card.desc.2")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}