package DIV.gtcsolo.integration.jei.multiblock;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.integration.jei.GtcSoloJeiPlugin;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/** Commands enter a temporary JEI page-selection session; ordinary reading has no editing controls. */
@Mod.EventBusSubscriber(modid = Gtcsolo.MODID, value = Dist.CLIENT)
public final class DocumentationEditing {
    public enum Kind { GLOSSARY, INTRODUCTION }
    private static Kind pending, selected;
    private static boolean editorOpen;
    private DocumentationEditing() {}

    @SubscribeEvent public static void commands(RegisterClientCommandsEvent event) {
        var edit = Commands.literal("edit");
        edit.then(Commands.literal("用語集").executes(context -> request(Kind.GLOSSARY)));
        edit.then(Commands.literal("glossary").executes(context -> request(Kind.GLOSSARY)));
        edit.then(Commands.literal("マルチブロック紹介").executes(context -> request(Kind.INTRODUCTION)));
        edit.then(Commands.literal("multiblock").executes(context -> request(Kind.INTRODUCTION)));
        event.getDispatcher().register(Commands.literal("gtcsolo").then(edit));
    }

    private static int request(Kind kind) {
        // ChatScreen closes after command execution. Open JEI on the following client tick.
        pending = kind;
        return 1;
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (pending == null && selected == null) return;
        var client = Minecraft.getInstance();
        if (client.player == null) { pending = null; editorOpen = false; stop(); return; }
        var runtime = GtcSoloJeiPlugin.getRuntimeOrNull();
        if (pending != null) {
            var kind = pending; pending = null;
            if (runtime == null) {
                client.player.displayClientMessage(Component.translatable("gtcsolo.documentation.jei_unavailable"), false);
                return;
            }
            DocumentationEdits.reload(); selected = kind;
            runtime.getRecipesGui().showTypes(List.of(kind == Kind.GLOSSARY ? GlossaryCategory.RECIPE_TYPE
                    : MultiblockIntroductionCategory.RECIPE_TYPE));
        } else if (selected != null && !editorOpen && (runtime == null || client.screen != runtime.getRecipesGui())) stop();
    }

    public static boolean selecting(Kind kind) { return selected == kind; }
    public static void stop() { selected = null; }
    public static void editorClosed() { editorOpen = false; }

    public static void edit(DocumentationPage page, Runnable afterSave) {
        var client = Minecraft.getInstance();
        String language = DocumentationEdits.language();
        var store = DocumentationEdits.store(language);
        store.reload();
        editorOpen = true;
        client.setScreen(new DocumentationEditorScreen(client.screen, page, store, language, afterSave));
    }

    public static WidgetGroup toolbar(int width, int y, Runnable edit) {
        var toolbar = new WidgetGroup(0, y, width, 20);
        toolbar.setClientSideWidget();
        var open = new ButtonWidget(0, 2, 76, 16, click -> edit.run())
                .setButtonTexture(GuiTextures.BUTTON, new TextTexture("gtcsolo.documentation.edit", 0xff202020));
        var close = new ButtonWidget(width - 92, 2, 92, 16, click -> stop())
                .setButtonTexture(GuiTextures.BUTTON, new TextTexture("gtcsolo.documentation.finish", 0xff202020));
        open.setClientSideWidget(); close.setClientSideWidget();
        toolbar.addWidget(open); toolbar.addWidget(close);
        return toolbar;
    }
}
