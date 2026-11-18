package DIV.gamacrafting.content.crafting.desk;

import DIV.gamacrafting.Gamacrafting;
import DIV.gamacrafting.helper.Renders;
import DIV.gamacrafting.network.DeskNetwork;
import DIV.gamacrafting.network.desk.ClientboundDeskMinecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber(modid = Gamacrafting.MOD_ID)
public final class DeskHandler {
    private DeskHandler() {
    }

    @SubscribeEvent
    public static void logIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DeskSavedData.get(player.level()).sync2Client(player);
        }
    }

    @SubscribeEvent
    public static void changeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DeskSavedData.get(player.level()).sync2Client(player);
        }
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DeskSavedData.get(player.level()).sync2Client(player);
        }
    }

    @SubscribeEvent
    public static void minecrafting(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            DeskSavedData.get(event.level).tick(event.level);
        }
    }

    public static void minecraftAt(Level level, BlockPos pos, List<ItemStack> ingredients, ItemStack result) {
        if (!level.isClientSide()) {
            DeskSavedData.Entry entry = DeskSavedData.get(level).add(pos, ingredients, result);
            DeskNetwork.CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension), new ClientboundDeskMinecraft(level.dimension(), entry));
        }
    }

    @Mod.EventBusSubscriber(modid = Gamacrafting.MOD_ID, value = Dist.CLIENT)
    public static final class Client {
        private static final Random RANDOM = new Random();
        private static final int[] COLORS = {0xFF0000, 0x00FF00, 0x0000FF, 0xFFFF00, 0xFF00FF, 0x00FFFF, 0x000000, 0xFFFFFF};

        private Client() {
        }

        public static void addAnimation(ResourceKey<Level> dimension, DeskSavedData.Entry entry) {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null && dimension.equals(level.dimension())) {
                DeskSavedData.get(level).getEntries().add(entry);
            }
        }

        public static void syncSave(ResourceKey<Level> dimension, CompoundTag tag) {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null && dimension.equals(level.dimension())) {
                DeskSavedData.get(level).load(tag);
            }
        }

        @SubscribeEvent
        public static void render(RenderLevelStageEvent event) {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
                return;
            }

            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) {
                return;
            }
            PoseStack poseStack = event.getPoseStack();
            Camera camera = event.getCamera();

            DeskSavedData.get(mc.level).getEntries().forEach(entry -> Renders.with(poseStack, () -> {
                poseStack.translate(-camera.getPosition().x(), -camera.getPosition().y(), -camera.getPosition().z());
                Vec3 center = Vec3.atCenterOf(entry.pos);
                if (entry.shouldRenderOnes()) {
                    Renders.with(poseStack, () -> {
                        float size = Mth.lerp(event.getPartialTick(), entry.oldItemSize, entry.itemSize);
                        float zRot = Mth.rotLerp(event.getPartialTick(), entry.oldRot.z(), entry.rot.z());
                        float yRot = Mth.rotLerp(event.getPartialTick(), entry.oldRot.y(), entry.rot.y());
                        float xRot = Mth.rotLerp(event.getPartialTick(), entry.oldRot.x(), entry.rot.x());
                        BakedModel model = mc.getItemRenderer().getModel(entry.result, mc.level, null, 0);

                        poseStack.translate(center.x(), center.y(), center.z());

                        Renders.with(poseStack, () -> {
                            poseStack.scale(0.8f, 0.8f, 0.8f);
                            poseStack.mulPose(Axis.ZN.rotationDegrees(zRot));
                            poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
                            poseStack.mulPose(Axis.XN.rotationDegrees(xRot));

                            float transY = model.getTransforms().getTransform(ItemDisplayContext.GROUND).translation.y();

                            poseStack.translate(0, -transY * size, 0);

                            mc.getItemRenderer().render(entry.result, ItemDisplayContext.GROUND, false, poseStack, Renders.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, model);
                        });

                        poseStack.scale(size, size, size);

                        Renders.with(poseStack, () -> {
                            float cube = 0.275f;

                            poseStack.mulPose(Axis.ZP.rotationDegrees(zRot));
                            poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
                            poseStack.mulPose(Axis.XP.rotationDegrees(xRot));

                            Renders.cube(poseStack.last().pose(), Renders.getBuffer(Renders.Type.HIGHLIGHT), cube, 0x8BFFFFFF);
                        });

                        Renders.with(poseStack, () -> {
                            float cube = 0.4f;
                            poseStack.mulPose(Axis.ZP.rotationDegrees(zRot));
                            poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
                            poseStack.mulPose(Axis.XP.rotationDegrees(xRot));

                            Renders.cube(poseStack.last().pose(), Renders.getBuffer(Renders.Type.HIGHLIGHT), cube, 0x8BCCCCCC);
                        });

                        Renders.endBatch(Renders.Type.HIGHLIGHT);
                    });
                    entry.ingredients.forEach(one -> Renders.with(poseStack, () -> {

                        double x = center.x() + Mth.lerp(event.getPartialTick(), one.oldPosition.x(), one.position.x());
                        double y = center.y() + Mth.lerp(event.getPartialTick(), one.oldPosition.y(), one.position.y());
                        double z = center.z() + Mth.lerp(event.getPartialTick(), one.oldPosition.z(), one.position.z());

                        float zRot = Mth.rotLerp(event.getPartialTick(), one.oldRot.z(), one.rot.z());
                        float yRot = Mth.rotLerp(event.getPartialTick(), one.oldRot.y(), one.rot.y());
                        float xRot = Mth.rotLerp(event.getPartialTick(), one.oldRot.x(), one.rot.x());
                        BakedModel model = mc.getItemRenderer().getModel(one.stack, mc.level, null, 0);

                        float transY = model.getTransforms().getTransform(ItemDisplayContext.GROUND).translation.y();

                        poseStack.translate(x, y, z);

                        Renders.with(poseStack, () -> {
                            poseStack.scale(0.85f, 0.85f, 0.85f);
                            poseStack.mulPose(Axis.ZP.rotationDegrees(zRot));
                            poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
                            poseStack.mulPose(Axis.XP.rotationDegrees(xRot));

                            poseStack.translate(0, -transY, 0);

                            mc.getItemRenderer().render(one.stack, ItemDisplayContext.GROUND, false, poseStack, Renders.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, model);
                        });

                        poseStack.mulPose(Axis.ZP.rotationDegrees(zRot));
                        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
                        poseStack.mulPose(Axis.XP.rotationDegrees(xRot));
                        if (one.radius > 0) {
                            float triXRot = Util.getMillis() / 1000f * Mth.lerp(((one.color % 32f) / 32f), -180, 180);
                            float triZRot = (one.ticks % 360f) * 6f;

                            Renders.with(poseStack, () -> {
                                poseStack.mulPose(Axis.ZP.rotationDegrees(triZRot));
                                poseStack.mulPose(Axis.XP.rotationDegrees(triXRot));

                                Renders.hollowTriangle(poseStack.last().pose(), Renders.getBuffer(Renders.Type.LIGHTNING_NO_CULL), one.radius, 0.3f, one.color);
                            });

                            Renders.with(poseStack, () -> {
                                poseStack.mulPose(Axis.ZN.rotationDegrees(triZRot));
                                poseStack.mulPose(Axis.XP.rotationDegrees(triXRot + 90));

                                Renders.hollowTriangle(poseStack.last().pose(), Renders.getBuffer(Renders.Type.LIGHTNING_NO_CULL), one.radius, 0.3f, one.color);
                            });

                            Renders.endBatch(Renders.Type.LIGHTNING_NO_CULL);
                        }
                    }));
                }

                if (entry.fxSize > 0) {
                    Renders.with(poseStack, () -> {
                        float size = Mth.lerp(event.getPartialTick(), entry.oldFXSize, entry.fxSize);

                        RANDOM.setSeed(entry.seed);

                        poseStack.translate(center.x(), center.y(), center.z());
                        poseStack.scale(size, size, size);

                        for (int index = 0; index < COLORS.length; index++) {
                            int color = COLORS[index];
                            float plus = RANDOM.nextFloat() * 2;
                            float xRot = (index - (COLORS.length / 2f)) * 11.25f + RANDOM.nextFloat(-11.25f, 11.25f);
                            float yRot = RANDOM.nextFloat(360);
                            Renders.with(poseStack, () -> {
                                poseStack.scale(plus, plus, plus);
                                poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
                                poseStack.mulPose(Axis.XP.rotationDegrees(xRot + 90));
                                Renders.hollowTriangle(poseStack.last().pose(), Renders.getBuffer(Renders.Type.LIGHTNING_NO_CULL), 1, 0.2f, ((int) (entry.fxAlpha * 255) << 24) | color);
                            });
                        }

                        Renders.endBatch(Renders.Type.LIGHTNING_NO_CULL);
                    });
                }
            }));
        }
    }
}
