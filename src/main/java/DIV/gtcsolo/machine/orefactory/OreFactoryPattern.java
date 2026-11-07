package DIV.gtcsolo.machine.orefactory;

import com.google.gson.JsonObject;
import DIV.gtcsolo.machine.mega.MegaPatternResource;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.common.data.GCYMBlocks;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.stream.IntStream;

import static com.gregtechceu.gtceu.api.pattern.Predicates.*;

/** Geometry is a generated resource, not a megabyte-sized Java initializer. */
public final class OreFactoryPattern {
    private static final String RESOURCE = "/data/gtcsolo/multiblocks/ore_processing_factory.json.gz";
    private static final MegaPatternResource LAYOUT = new MegaPatternResource(OreFactoryPattern.class, RESOURCE, "ore factory");
    private OreFactoryPattern() {}

    public static BlockPattern create(MultiblockMachineDefinition definition) {
        JsonObject layout = LAYOUT.readLayout();
        var builder = FactoryBlockPattern.start(RelativeDirection.RIGHT, RelativeDirection.UP, RelativeDirection.BACK);
        for (var aisle : layout.getAsJsonArray("aisles")) {
            var array = aisle.getAsJsonArray();
            String[] rows = new String[array.size()];
            for (int row=0; row<rows.length; row++) rows[row] = array.get(row).getAsString();
            builder.aisle(rows);
        }
        for (var entry : layout.getAsJsonObject("palette").entrySet()) {
            JsonObject material = entry.getValue().getAsJsonObject();
            ResourceLocation id = new ResourceLocation(material.get("block").getAsString());
            if (!ForgeRegistries.BLOCKS.containsKey(id)) throw new IllegalStateException("Ore factory requires " + id);
            Block block = ForgeRegistries.BLOCKS.getValue(id);
            BlockState state = block.defaultBlockState();
            for (var property : material.getAsJsonObject("state").entrySet())
                state = property(state, property.getKey(), property.getValue().getAsString());
            BlockState previewState = state;
            // Preview preserves stair/fence state; matching by block permits normal world rotations and updates.
            TraceabilityPredicate predicate = custom(world -> world.getBlockState().is(block),
                    () -> new BlockInfo[]{BlockInfo.fromBlockState(previewState)});
            if (material.has("optional") && material.get("optional").getAsBoolean())
                predicate = predicate.or(air()).addTooltips(Component.translatable("gtcsolo.ore_factory.optional"));
            builder.where(entry.getKey().charAt(0), predicate);
        }
        return builder.where('K', controller(blocks(definition.getBlock())))
                .where('7', previewPorts()).where('#', any()).where(' ', any()).build();
    }

    /** Shared with the sparse world preview; native preview selection still chooses the examples. */
    public static TraceabilityPredicate previewPorts() {
        int[] powerTiers = IntStream.rangeClosed(GTValues.UHV, GTValues.MAX).toArray();
        return blocks(GCYMBlocks.CASING_SECURE_MACERATION.get())
                .or(ability(PartAbility.INPUT_ENERGY, powerTiers).setMinGlobalLimited(1).setMaxGlobalLimited(2).setPreviewCount(1))
                .or(abilities(PartAbility.IMPORT_ITEMS).setPreviewCount(1))
                .or(abilities(PartAbility.EXPORT_ITEMS).setPreviewCount(1))
                .or(abilities(PartAbility.IMPORT_FLUIDS).setPreviewCount(4))
                .or(autoAbilities(true, false, true));
    }

    private static <T extends Comparable<T>> BlockState set(BlockState state, Property<T> property, String text) {
        T value = property.getValue(text).orElseThrow(() -> new IllegalStateException("Invalid ore factory state " + text));
        return state.setValue(property, value);
    }

    private static BlockState property(BlockState state, String name, String value) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty(name);
        if (property == null) throw new IllegalStateException("Unknown ore factory property " + name);
        return set(state, property, value);
    }
}
