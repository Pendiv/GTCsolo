package DIV.gtcsolo.integration.jei.multiblock;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.client.ClientProxy;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.Minecraft;
import DIV.gtcsolo.integration.mekanism.generator.CombustionSupportRecipe;
import mekanism.api.MekanismAPI;
import net.minecraftforge.fluids.FluidStack;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Collection;
import java.util.TreeMap;

/** Resource-backed entries allow KubeJS machines to use the same native introduction page. */
public final class MultiblockIntroductionResources {
    private static final Logger LOGGER = LogUtils.getLogger();

    private MultiblockIntroductionResources() {}

    public static List<MultiblockIntroduction> load(ResourceManager resources) {
        var introductions = new ArrayList<MultiblockIntroduction>();
        var entries = resources.listResources("multiblock_introductions", id -> id.getPath().endsWith(".json"));
        for (var entry : entries.entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().toString())).toList()) {
            try (var reader = entry.getValue().openAsReader()) {
                var root = JsonParser.parseReader(reader).getAsJsonObject();
                for (var raw : root.getAsJsonArray("entries")) {
                    var definition = raw.getAsJsonObject();
                    if (definition.has("block")) {
                        var block = BuiltInRegistries.BLOCK.get(new ResourceLocation(definition.get("block").getAsString()));
                        if (block == Blocks.AIR) continue;
                        var page = MultiblockIntroduction.singleBlock(block, definition.get("prefix").getAsString(),
                                definition.get("paragraphs").getAsInt());
                        if (definition.has("combustion_support") && definition.get("combustion_support").getAsBoolean())
                            page = withSupportMaterials(page);
                        introductions.add(page);
                        continue;
                    }
                    var machineId = new ResourceLocation(definition.get("machine").getAsString());
                    var machine = GTRegistries.MACHINES.get(machineId);
                    // A resource pack can also be present in profiles without its optional KubeJS machine.
                    if (machine == null) continue;
                    if (!(machine instanceof MultiblockMachineDefinition multiblock))
                        throw new IllegalArgumentException("Not a multiblock: " + machineId);
                    introductions.add(create(multiblock, definition));
                }
            } catch (IOException | RuntimeException error) {
                LOGGER.error("Could not load multiblock introductions from {}", entry.getKey(), error);
            }
        }
        return List.copyOf(introductions);
    }

    private static MultiblockIntroduction withSupportMaterials(MultiblockIntroduction page) {
        var level = Minecraft.getInstance().level;
        if (level == null) return page;
        var definitions = level.getRecipeManager().getAllRecipesFor(CombustionSupportRecipe.TYPE.get()).stream()
                .sorted(Comparator.comparing(CombustionSupportRecipe::getId)).toList();
        if (definitions.isEmpty()) return page.withParagraph("gtcsolo.gas_generator.introduction.support_none");
        for (var recipe : definitions) {
            boolean gas = recipe.kind() == CombustionSupportRecipe.Kind.GAS;
            var name = gas ? MekanismAPI.gasRegistry().getValue(recipe.material()).getTextComponent()
                    : new FluidStack(BuiltInRegistries.FLUID.get(recipe.material()), 1).getDisplayName();
            page = page.withParagraph("gtcsolo.gas_generator.introduction.support_entry", name,
                    net.minecraft.network.chat.Component.translatable("gtcsolo.gas_generator.introduction." + (gas ? "gas_slot" : "fluid_slot")),
                    String.format(java.util.Locale.ROOT, "%,d", recipe.euPerMb()));
        }
        return page;
    }

    static MultiblockIntroduction create(MultiblockMachineDefinition machine, JsonObject entry) {
        return create(machine, entry, entry.has("fluid_outputs")
                ? ClientProxy.CLIENT_FLUID_VEINS.values().stream().map(v -> v.getStoredFluid().get()).toList()
                : List.of());
    }

    static MultiblockIntroduction create(MultiblockMachineDefinition machine, JsonObject entry,
                                        Collection<Fluid> veinFluids) {
        String prefix = entry.get("prefix").getAsString();
        int paragraphs = entry.get("paragraphs").getAsInt();
        if (prefix.isBlank() || paragraphs < 1 || paragraphs > 128)
            throw new IllegalArgumentException("Invalid introduction text range for " + machine.getId());
        var introduction = MultiblockIntroduction.named(machine, prefix, paragraphs);
        if (!entry.has("fluid_outputs")) return introduction;
        var section = entry.getAsJsonObject("fluid_outputs");
        var groups = new ArrayList<MultiblockIntroduction.FluidGroup>();
        for (var raw : section.getAsJsonArray("groups")) {
            var group = raw.getAsJsonObject();
            var fluids = new TreeMap<String, FluidStack>();
            if (group.has("source")) {
                if (!group.get("source").getAsString().equals("bedrock_veins"))
                    throw new IllegalArgumentException("Unknown introduction fluid source: " + group.get("source"));
                for (var fluid : veinFluids) addFluid(fluids, fluid);
            }
            if (group.has("fluids")) for (var rawId : group.getAsJsonArray("fluids")) {
                var id = new ResourceLocation(rawId.getAsString());
                var fluid = BuiltInRegistries.FLUID.get(id);
                if (fluid == Fluids.EMPTY) LOGGER.warn("Missing introduction fluid {} for {}", id, machine.getId());
                else addFluid(fluids, fluid);
            }
            groups.add(new MultiblockIntroduction.FluidGroup(group.get("label").getAsString(),
                    List.copyOf(fluids.values())));
        }
        return introduction.withFluidSection(new MultiblockIntroduction.FluidSection(
                section.get("after_paragraph").getAsInt(), groups));
    }

    private static void addFluid(TreeMap<String, FluidStack> fluids, Fluid fluid) {
        if (fluid != Fluids.EMPTY) fluids.putIfAbsent(BuiltInRegistries.FLUID.getKey(fluid).toString(),
                new FluidStack(fluid, 1000));
    }
}
