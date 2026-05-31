package DIV.gtcsolo.command;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.common.AbsoluteKillHandler;
import DIV.gtcsolo.dump.RecipeDumpModels;
import DIV.gtcsolo.dump.RecipeDumpService;
import DIV.gtcsolo.dump.extend.ExtendedDumpService;
import DIV.gtcsolo.progression.ProgressionCapability;
import DIV.gtcsolo.progression.ProgressionManager;
import DIV.gtcsolo.progression.ProgressionNode;
import DIV.gtcsolo.progression.ProgressionNodes;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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

                        .then(
                                Commands.literal("progression")
                                        .then(Commands.literal("info").executes(this::progressionInfo))
                                        .then(Commands.literal("nodes").executes(this::progressionNodes))
                                        .then(Commands.literal("addxp")
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                        .executes(this::progressionAddXp)))
                                        .then(Commands.literal("buy")
                                                .then(Commands.argument("node", ResourceLocationArgument.id())
                                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                                                ProgressionNodes.INSTANCE.map().keySet(), builder))
                                                        .executes(this::progressionBuy)))
                                        .then(Commands.literal("respec").executes(this::progressionRespec))
                        )
        );
    }

    private int progressionInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Must be run by a player."));
            return 0;
        }
        player.getCapability(ProgressionCapability.PLAYER).ifPresent(d ->
                source.sendSuccess(() -> Component.literal(String.format(
                        "Progression: level %d (lifetimeXp %d) | skill %d/%d | stat %d/%d | nodes %d",
                        d.getPermanentLevel(), d.getLifetimeXp(),
                        d.getSkillPoints(), d.getSkillEarned(),
                        d.getStatPoints(), d.getStatEarned(),
                        d.getNodeLevels().size())), false));
        return 1;
    }

    private int progressionAddXp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Must be run by a player."));
            return 0;
        }
        int amount = IntegerArgumentType.getInteger(context, "amount");
        player.getCapability(ProgressionCapability.PLAYER).ifPresent(d -> {
            int gained = d.addXp(amount);
            source.sendSuccess(() -> Component.literal(String.format(
                    "Added %d xp -> level %d (+%d levels), skill %d / stat %d",
                    amount, d.getPermanentLevel(), gained, d.getSkillPoints(), d.getStatPoints())), false);
        });
        return 1;
    }

    private int progressionRespec(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Must be run by a player."));
            return 0;
        }
        ProgressionManager.respec(player);
        player.getCapability(ProgressionCapability.PLAYER).ifPresent(d ->
                source.sendSuccess(() -> Component.literal(String.format(
                        "Respec done: skill %d / stat %d refunded, nodes cleared.",
                        d.getSkillPoints(), d.getStatPoints())), false));
        return 1;
    }

    private int progressionBuy(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Must be run by a player."));
            return 0;
        }
        ResourceLocation nodeId = ResourceLocationArgument.getId(context, "node");
        ProgressionManager.Result result = ProgressionManager.tryPurchase(player, nodeId);
        if (result != ProgressionManager.Result.SUCCESS) {
            source.sendFailure(Component.literal("Buy failed (" + nodeId + "): " + result));
            return 0;
        }
        player.getCapability(ProgressionCapability.PLAYER).ifPresent(d ->
                source.sendSuccess(() -> Component.literal(String.format(
                        "Bought %s -> level %d | skill %d / stat %d left",
                        nodeId, d.getNodeLevel(nodeId), d.getSkillPoints(), d.getStatPoints())), false));
        return 1;
    }

    private int progressionNodes(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        var nodes = ProgressionNodes.INSTANCE.all();
        if (nodes.isEmpty()) {
            source.sendFailure(Component.literal("No progression nodes loaded."));
            return 0;
        }
        ServerPlayer player = source.getEntity() instanceof ServerPlayer p ? p : null;
        source.sendSuccess(() -> Component.literal("Loaded " + nodes.size() + " node(s):"), false);
        for (ProgressionNode node : nodes) {
            int level = player == null ? 0
                    : player.getCapability(ProgressionCapability.PLAYER).map(d -> d.getNodeLevel(node.id())).orElse(0);
            source.sendSuccess(() -> Component.literal(String.format(
                    "  %s [%s] lv %d/%d  next-cost %d",
                    node.id(), node.tree(), level, node.maxLevel(),
                    level < node.maxLevel() ? node.costForLevel(level + 1) : 0)), false);
        }
        return 1;
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