package DIV.gtcsolo.integration.jei;

import DIV.gtcsolo.Gtcsolo;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.common.Internal;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = Gtcsolo.MODID, value = Dist.CLIENT)
public final class JeiOpenCommand {
    private static Runnable pending;

    private JeiOpenCommand() {}

    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) {
        if (!ModList.get().isLoaded("jei")) return;
        event.getDispatcher().register(Commands.literal("jeiopen")
                .executes(context -> Open.defaultCategory())
                .then(Commands.literal("type").then(Commands.argument("type", ResourceLocationArgument.id())
                        .executes(context -> Open.type(ResourceLocationArgument.getId(context, "type")))))
                .then(Commands.literal("id").then(Commands.argument("recipe", ResourceLocationArgument.id())
                        .executes(context -> Open.recipe(ResourceLocationArgument.getId(context, "recipe"))))));
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || pending == null) return;
        var open = pending;
        pending = null;
        if (Minecraft.getInstance().player != null) open.run();
    }

    private static final class Open {
        private static final SimpleCommandExceptionType JEI_UNAVAILABLE = new SimpleCommandExceptionType(
                Component.translatable("gtcsolo.documentation.jei_unavailable"));
        private static final SimpleCommandExceptionType NO_RECIPES = new SimpleCommandExceptionType(
                Component.translatable("gtcsolo.jeiopen.no_recipes"));
        private static final DynamicCommandExceptionType UNKNOWN_TYPE = new DynamicCommandExceptionType(
                id -> Component.translatable("gtcsolo.jeiopen.unknown_type", id));
        private static final DynamicCommandExceptionType UNKNOWN_RECIPE = new DynamicCommandExceptionType(
                id -> Component.translatable("gtcsolo.jeiopen.unknown_recipe", id));

        private static int defaultCategory() throws CommandSyntaxException {
            var runtime = requireRuntime();
            var manager = runtime.getRecipeManager();
            int availableHeight = recipeAreaHeight();
            IRecipeCategory<?> smallest = null;
            long smallestCount = Long.MAX_VALUE;
            var categories = manager.createRecipeCategoryLookup().get().iterator();
            while (categories.hasNext()) {
                var category = categories.next();
                // JEI 15.20 adds four pixels above and below each recipe and four between layouts.
                int borderedHeight = category.getHeight() + 8;
                int slots = Math.max(1, 1 + (availableHeight - borderedHeight) / (borderedHeight + 4));
                long count = manager.createRecipeLookup(category.getRecipeType()).get()
                        .limit(Math.min(slots, smallestCount)).count();
                if (count > 0 && count < smallestCount) {
                    smallest = category;
                    smallestCount = count;
                    if (count == 1) break;
                }
            }
            if (smallest == null) throw NO_RECIPES.create();
            return scheduleCategory(runtime, smallest);
        }

        private static int recipeAreaHeight() {
            var client = Minecraft.getInstance();
            var config = Internal.getJeiClientConfigs().getClientConfig();
            int screenInset = config.isCenterSearchBarEnabled() ? 76 : 58;
            int guiHeight = Math.min(config.getMaxRecipeGuiHeight(), client.getWindow().getGuiScaledHeight() - screenInset);
            // JEI's title, two navigation rows, and border leave this area for the recipe layouts.
            int headerAndPadding = client.font.lineHeight + 31;
            return guiHeight - headerAndPadding;
        }

        private static int type(ResourceLocation id) throws CommandSyntaxException {
            var runtime = requireRuntime();
            var category = runtime.getRecipeManager().createRecipeCategoryLookup().get()
                    .filter(candidate -> candidate.getRecipeType().getUid().equals(id))
                    .findFirst().orElseThrow(() -> UNKNOWN_TYPE.create(id));
            return scheduleCategory(runtime, category);
        }

        private static int recipe(ResourceLocation id) throws CommandSyntaxException {
            var runtime = requireRuntime();
            var show = runtime.getRecipeManager().createRecipeCategoryLookup().get()
                    .flatMap(category -> findRecipe(runtime, category, id).stream())
                    .findFirst().orElseThrow(() -> UNKNOWN_RECIPE.create(id));
            return schedule(runtime, show);
        }

        private static <T> Optional<Runnable> findRecipe(IJeiRuntime runtime, IRecipeCategory<T> category,
                                                          ResourceLocation id) {
            return runtime.getRecipeManager().createRecipeLookup(category.getRecipeType()).get()
                    .filter(recipe -> id.equals(category.getRegistryName(recipe)))
                    .findFirst().map(recipe -> () -> runtime.getRecipesGui().showRecipes(category, List.of(recipe), List.of()));
        }

        private static IJeiRuntime requireRuntime() throws CommandSyntaxException {
            var runtime = GtcSoloJeiPlugin.getRuntimeOrNull();
            if (runtime == null) throw JEI_UNAVAILABLE.create();
            return runtime;
        }

        private static int scheduleCategory(IJeiRuntime runtime, IRecipeCategory<?> category) {
            return schedule(runtime, () -> runtime.getRecipesGui().showTypes(List.of(category.getRecipeType())));
        }

        private static int schedule(IJeiRuntime runtime, Runnable show) {
            // ChatScreen closes after executing the command.
            pending = () -> {
                if (GtcSoloJeiPlugin.getRuntimeOrNull() == runtime) show.run();
            };
            return 1;
        }
    }
}
