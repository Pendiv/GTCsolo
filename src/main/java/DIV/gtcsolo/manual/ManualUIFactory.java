package DIV.gtcsolo.manual;

import com.lowdragmc.lowdraglib.gui.factory.UIFactory;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * マニュアル UI 用の LDLib {@link UIFactory}。
 *
 * <p>{@code HeldItemUIFactory} と違いプレイヤーのみで holder が成立する (= アイテムを手に持っていなくても
 * 開ける) ため、 マニュアル右クリックと {@code /gtcsolo manual} コマンドの両方をこの 1 経路で賄う。
 * {@link DIV.gtcsolo.Gtcsolo} の construct 時に {@code UIFactory.register(INSTANCE)} で登録する。
 */
public class ManualUIFactory extends UIFactory<ManualUIFactory.Holder> {

    public static final ManualUIFactory INSTANCE = new ManualUIFactory();

    public ManualUIFactory() {
        super(new ResourceLocation("gtcsolo", "manual"));
    }

    /** サーバ側からマニュアル UI を開く。 */
    public final boolean openUI(ServerPlayer player) {
        return openUI(new Holder(player), player);
    }

    @Override
    protected ModularUI createUITemplate(Holder holder, Player entityPlayer) {
        return holder.createUI(entityPlayer);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected Holder readHolderFromSyncData(FriendlyByteBuf syncData) {
        Player player = Minecraft.getInstance().player;
        return player == null ? null : new Holder(player);
    }

    @Override
    protected void writeHolderToSyncData(FriendlyByteBuf syncData, Holder holder) {
    }

    public static class Holder implements IUIHolder {
        public final Player player;

        public Holder(Player player) {
            this.player = player;
        }

        @Override
        public ModularUI createUI(Player entityPlayer) {
            return ManualUI.create(this, entityPlayer);
        }

        @Override
        public boolean isInvalid() {
            return !player.isAlive();
        }

        @Override
        public boolean isRemote() {
            return player.level().isClientSide;
        }

        @Override
        public void markAsDirty() {
        }
    }
}
