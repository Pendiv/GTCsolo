package DIV.gtcsolo.integration.jei;

import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.lowdragmc.lowdraglib.gui.editor.data.Resources;
import com.lowdragmc.lowdraglib.gui.editor.data.resource.Resource;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.DataInputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** RTUI appearance metadata decorates JEI without replacing GT's capability-aware layout. */
public final class JeiRecipeBackgrounds {
    public static final String DEFAULT_LAYOUT_FLAG = "gtcsolo:use_default_layout";
    public static final String TEXT_COLOR_TAG = "gtcsolo:jei_text_color";
    private static final Logger LOGGER = LogUtils.getLogger();
    private record Style(@Nullable IGuiTexture background, @Nullable Integer textColor) {}
    private static final Style EMPTY_STYLE = new Style(null, null);
    private static final Map<ResourceLocation, Style> STYLES = new ConcurrentHashMap<>();

    private JeiRecipeBackgrounds() {}

    /** Full custom RTUIs remain custom, even if the flag was copied into one by accident. */
    public static boolean usesDefaultLayout(CompoundTag document) {
        if (!document.getBoolean(DEFAULT_LAYOUT_FLAG) || !document.contains("root", Tag.TAG_COMPOUND)) return false;
        CompoundTag root = document.getCompound("root");
        return !root.contains("children")
                || root.get("children") instanceof ListTag children && children.isEmpty();
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static IGuiTexture decodeBackground(CompoundTag document) {
        if (!usesDefaultLayout(document)) return null;
        CompoundTag root = document.getCompound("root");
        if (!root.contains("backgroundTexture", Tag.TAG_COMPOUND)) return null;
        IGuiTexture texture = IGuiTexture.deserializeWrapper(root.getCompound("backgroundTexture"));
        Resources resources = Resources.fromNBT(document.getCompound("resources"));
        Resource<?> textures = resources.resources.get("ldlib.gui.editor.group.textures");
        if (textures != null) texture.setUIResource((Resource<IGuiTexture>) textures);
        return texture;
    }

    @Nullable
    public static IGuiTexture backgroundForType(GTRecipeType recipeType) {
        return styleForType(recipeType).background();
    }

    /** The authored color is valid for both full and background-only RTUIs. */
    @Nullable
    public static Integer decodeTextColor(CompoundTag document) {
        return document.contains(TEXT_COLOR_TAG, Tag.TAG_INT) ? document.getInt(TEXT_COLOR_TAG) : null;
    }

    @Nullable
    public static Integer textColorForType(GTRecipeType recipeType) {
        return styleForType(recipeType).textColor();
    }

    /** An RTUI choice overrides the instance setting; removing the tag restores that setting. */
    @Nullable
    public static Integer informationColorForType(GTRecipeType recipeType) {
        Integer authored = textColorForType(recipeType);
        return authored != null ? authored : JeiRecipeTextColors.colorForRecipeType(recipeType.registryName.toString());
    }

    private static Style styleForType(GTRecipeType recipeType) {
        return STYLES.computeIfAbsent(recipeType.registryName, JeiRecipeBackgrounds::load);
    }

    public static void invalidate(ResourceLocation recipeType) {
        STYLES.remove(recipeType);
    }

    private static Style load(ResourceLocation recipeType) {
        // Deliberately separate from GT's customUICache: ChemCap keeps that cache empty for LCR.
        ResourceLocation file = new ResourceLocation(recipeType.getNamespace(),
                "ui/recipe_type/" + recipeType.getPath() + ".rtui");
        try {
            var resource = Minecraft.getInstance().getResourceManager().getResource(file);
            if (resource.isEmpty()) return EMPTY_STYLE;
            try (var input = resource.get().open(); var data = new DataInputStream(input)) {
                CompoundTag document = NbtIo.read(data, NbtAccounter.UNLIMITED);
                if (document == null) return EMPTY_STYLE;
                Integer color = decodeTextColor(document);
                if (document.contains(TEXT_COLOR_TAG) && color == null) {
                    LOGGER.warn("Ignoring non-Int JEI text color in {}; keeping the instance color setting", file);
                }
                IGuiTexture background = null;
                try {
                    background = decodeBackground(document);
                } catch (RuntimeException exception) {
                    LOGGER.warn("Cannot read background-only texture from {}; keeping the native JEI background", file,
                            exception);
                }
                return new Style(background, color);
            }
        } catch (Exception exception) {
            LOGGER.warn("Cannot read RTUI appearance from {}; keeping the instance color and native JEI background",
                    file, exception);
            return EMPTY_STYLE;
        }
    }
}
