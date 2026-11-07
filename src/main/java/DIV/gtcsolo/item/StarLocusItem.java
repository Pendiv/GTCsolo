package DIV.gtcsolo.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * StarForge で星を構築するための軌跡アイテム。
 * 単一アイテム ID + NBT "Trace" で 8 種類の軌跡を識別する (DataStick 方式)。
 * Trace 未設定 = 空の軌跡。観測機構 (locus_simulation_builder) で書き込まれる。
 * NBT 仕様の詳細は {@link AbstractLocusItem} を参照。
 *
 * <p>右クリックで JEI の StarForge 軌跡情報ページが開く (client side、JEI 未ロード時は無効)。
 */
public class StarLocusItem extends AbstractLocusItem {

    public StarLocusItem(Properties props) {
        super(props);
    }

    @Override
    protected String getTranslationKeyPrefix() {
        return "item.gtcsolo.star_locus";
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            // client-only ハンドラを lazy-load (server サイドでは JEI クラスを参照させない)
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    DIV.gtcsolo.integration.jei.starforge.StarLocusJeiHandler.openJei(stack));
        }
        return InteractionResultHolder.success(stack);
    }
}
