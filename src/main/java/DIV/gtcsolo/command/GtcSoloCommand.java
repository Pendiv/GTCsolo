package DIV.gtcsolo.command;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.common.AbsoluteKillHandler;
import DIV.gtcsolo.dump.RecipeDumpModels;
import DIV.gtcsolo.dump.RecipeDumpService;
import DIV.gtcsolo.dump.extend.ExtendedDumpService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;

public class GtcSoloCommand {
    private final RecipeDumpService recipeDumpService;
    private final ExtendedDumpService extendedDumpService;

    public GtcSoloCommand(RecipeDumpService recipeDumpService) {
        this.recipeDumpService = recipeDumpService;
        this.extendedDumpService = new ExtendedDumpService();
    }

    public void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("gtcsolo")
                        .requires(source -> source.hasPermission(2))

                        .then(
                                Commands.literal("dump_recipe")
                                        .then(
                                                Commands.argument("recipe_type", ResourceLocationArgument.id())
                                                        .suggests((context, builder) ->
                                                                SharedSuggestionProvider.suggestResource(
                                                                        BuiltInRegistries.RECIPE_TYPE.keySet(),
                                                                        builder
                                                                )
                                                        )
                                                        .executes(this::dumpRecipe)
                                        )
                        )

                        .then(
                                Commands.literal("absolute_kill")
                                        .executes(this::absoluteKillSelf)
                        )

                        .then(
                                Commands.literal("dump_extend")
                                        .then(
                                                Commands.literal("ore_vein")
                                                        .executes(this::dumpOreVeinFromJei)
                                        )
                        )
        );
    }

    private int dumpRecipe(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ResourceLocation recipeTypeId = ResourceLocationArgument.getId(context, "recipe_type");

        try {
            RecipeDumpModels.DumpExecutionResult result =
                    recipeDumpService.dump(source.getServer(), recipeTypeId);

            source.sendSuccess(
                    () -> Component.literal(
                            "Dumped " + result.recipeCount() + " recipes for " + result.recipeTypeId()
                                    + " -> " + result.outputPath().toAbsolutePath()
                    ),
                    true
            );
            return result.recipeCount();
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        } catch (Exception e) {
            Gtcsolo.LOGGER.error("Failed to dump recipe type {}", recipeTypeId, e);
            source.sendFailure(Component.literal("Failed to dump recipe type: " + recipeTypeId));
            return 0;
        }
    }

    private int absoluteKillSelf(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (source.getEntity() instanceof ServerPlayer player) {
            AbsoluteKillHandler.absoluteKill(player, player.damageSources().magic());
            source.sendSuccess(() -> Component.literal("Absolute kill applied to self."), true);
            return 1;
        }
        source.sendFailure(Component.literal("Must be run by a player."));
        return 0;
    }

    private int dumpOreVeinFromJei(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        try {
            ExtendedDumpService.ExtendedDumpResult result =
                    extendedDumpService.dumpOreVeinsFromJei(source.getServer());

            source.sendSuccess(
                    () -> Component.literal(
                            "Extended dump finished: " + result.target()
                                    + " -> " + result.outputPath().toAbsolutePath()
                                    + " (" + result.count() + " entries)"
                    ),
                    true
            );
            return result.count();
        } catch (IllegalStateException e) {
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        } catch (Exception e) {
            Gtcsolo.LOGGER.error("Failed to dump ore vein data from JEI", e);
            source.sendFailure(Component.literal("Failed to dump ore vein data from JEI"));
            return 0;
        }
    }
}