package DIV.gtcsolo.integration.jei.multiblock;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;

import java.util.List;
import java.util.Objects;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraftforge.fluids.FluidStack;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/** One introduction, backed by a registered machine and editable language keys. */
public record MultiblockIntroduction(List<Variant> variants, String pageId, List<PageLink> relatedPages,
                                    FluidSection fluidSection, SingleBlock singleBlock) {
    public MultiblockIntroduction {
        variants = List.copyOf(variants);
        if (variants.isEmpty() == (singleBlock == null)) throw new IllegalArgumentException("An introduction needs one subject kind");
        if (variants.stream().map(v -> v.definition().getId()).distinct().count() != variants.size())
            throw new IllegalArgumentException("Duplicate machine in introduction group");
        Objects.requireNonNull(pageId);
        relatedPages = List.copyOf(relatedPages);
        Objects.requireNonNull(fluidSection);
        if (variants.stream().anyMatch(v -> fluidSection.afterParagraph() > v.paragraphs().size()))
            throw new IllegalArgumentException("Fluid section is outside the introduction text");
    }

    public MultiblockIntroduction(List<Variant> variants, String pageId, List<PageLink> relatedPages, FluidSection section) {
        this(variants, pageId, relatedPages, section, null);
    }

    public MultiblockIntroduction(List<Variant> variants, String pageId, List<PageLink> relatedPages) {
        this(variants, pageId, relatedPages, FluidSection.EMPTY);
    }

    public MultiblockIntroduction(MultiblockMachineDefinition definition, List<String> descriptionKeys) {
        this(definition, descriptionKeys, "", List.of());
    }

    public MultiblockIntroduction(MultiblockMachineDefinition definition, List<String> descriptionKeys,
                                  String pageId, List<PageLink> relatedPages) {
        this(List.of(new Variant(definition, descriptionKeys.stream().map(Paragraph::new).toList())), pageId, relatedPages);
    }

    public MultiblockMachineDefinition definition() { return variants.get(0).definition(); }
    public List<String> descriptionKeys() { return paragraphs(0).stream().map(Paragraph::key).toList(); }
    public boolean isGrouped() { return variants.size() > 1; }
    public boolean isSingleBlock() { return singleBlock != null; }
    public List<Paragraph> paragraphs(int index) { return isSingleBlock() ? singleBlock.paragraphs() : variants.get(index).paragraphs(); }
    public ItemStack subject(int index) { return isSingleBlock() ? singleBlock.stack().copy() : variants.get(index).definition().asStack(); }
    public List<ItemStack> subjects() { return isSingleBlock() ? List.of(subject(0)) : variants.stream().map(v -> v.definition().asStack()).toList(); }

    public static MultiblockIntroduction singleBlock(Block block, String prefix, int count) {
        if (count < 1 || count > 128) throw new IllegalArgumentException("Invalid introduction text range");
        var text = Stream.concat(Stream.of(new Paragraph(block.getDescriptionId())),
                IntStream.rangeClosed(1, count).mapToObj(i -> new Paragraph(prefix + i))).toList();
        var id = BuiltInRegistries.BLOCK.getKey(block);
        return new MultiblockIntroduction(List.of(), id.toString(), List.of(), FluidSection.EMPTY,
                new SingleBlock(id, new ItemStack(block), text));
    }

    public int indexOfController(Item item) {
        if (isSingleBlock()) return singleBlock.stack().getItem() == item ? 0 : -1;
        for (int index = 0; index < variants.size(); index++)
            if (variants.get(index).definition().asStack().getItem() == item) return index;
        return -1;
    }

    public static MultiblockIntroduction grouped(List<MultiblockIntroduction> introductions) {
        if (introductions.stream().anyMatch(p -> p.isSingleBlock() || !p.pageId().isEmpty() || !p.relatedPages().isEmpty()
                || !p.fluidSection().groups().isEmpty()))
            throw new IllegalArgumentException("Linked or illustrated introductions cannot be merged implicitly");
        return new MultiblockIntroduction(introductions.stream().flatMap(p -> p.variants().stream()).toList(), "", List.of());
    }

    public MultiblockIntroduction withParagraph(String key, Object... arguments) {
        var paragraph = new Paragraph(key, List.of(arguments));
        if (isSingleBlock()) return new MultiblockIntroduction(variants, pageId, relatedPages, fluidSection,
                new SingleBlock(singleBlock.id(), singleBlock.stack(),
                        Stream.concat(singleBlock.paragraphs().stream(), Stream.of(paragraph)).toList()));
        return new MultiblockIntroduction(variants.stream().map(v -> new Variant(v.definition(),
                Stream.concat(v.paragraphs().stream(), Stream.of(paragraph)).toList())).toList(), pageId, relatedPages, fluidSection);
    }

    public static MultiblockIntroduction numbered(MultiblockMachineDefinition definition, String prefix, int lines) {
        if (lines < 1) throw new IllegalArgumentException("An introduction needs at least one line");
        return new MultiblockIntroduction(definition,
                IntStream.range(0, lines).mapToObj(i -> prefix + i).toList());
    }

    /** Use the machine's native localized name and shared paragraphs numbered from one. */
    public static MultiblockIntroduction named(MultiblockMachineDefinition definition, String prefix, int paragraphs) {
        if (paragraphs < 1) throw new IllegalArgumentException("An introduction needs at least one paragraph");
        return new MultiblockIntroduction(definition, Stream.concat(Stream.of(definition.getDescriptionId()),
                IntStream.rangeClosed(1, paragraphs).mapToObj(i -> prefix + i)).toList());
    }

    public MultiblockIntroduction withNavigation(String id, PageLink... links) {
        if (id.isBlank()) throw new IllegalArgumentException("A linked page needs an id");
        return new MultiblockIntroduction(variants, id, List.of(links), fluidSection, singleBlock);
    }

    public MultiblockIntroduction withFluidSection(FluidSection section) {
        return new MultiblockIntroduction(variants, pageId, relatedPages, section, singleBlock);
    }

    /** Amounts are display-only; production rates belong in the introduction text. */
    public record FluidGroup(String labelKey, List<FluidStack> fluids) {
        public FluidGroup {
            Objects.requireNonNull(labelKey);
            fluids = fluids.stream().filter(f -> !f.isEmpty()).map(FluidStack::copy).toList();
        }
    }

    public record FluidSection(int afterParagraph, List<FluidGroup> groups) {
        public static final FluidSection EMPTY = new FluidSection(0, List.of());
        public FluidSection {
            if (afterParagraph < 0) throw new IllegalArgumentException("Negative paragraph offset");
            groups = List.copyOf(groups);
        }
    }

    public record Variant(MultiblockMachineDefinition definition, List<Paragraph> paragraphs) {
        public Variant { Objects.requireNonNull(definition); paragraphs = List.copyOf(paragraphs); }
    }

    public record SingleBlock(ResourceLocation id, ItemStack stack, List<Paragraph> paragraphs) {
        public SingleBlock {
            Objects.requireNonNull(id);
            if (stack.isEmpty()) throw new IllegalArgumentException("Missing introduction block item");
            stack = stack.copy();
            paragraphs = List.copyOf(paragraphs);
        }
    }

    public record Paragraph(String key, List<Object> arguments) {
        public Paragraph { Objects.requireNonNull(key); arguments = List.copyOf(arguments); }
        public Paragraph(String key) { this(key, List.of()); }
        public Component component() { return Component.translatable(key, arguments.toArray()); }
    }

    public record PageLink(String targetId, String labelKey) {
        public PageLink {
            if (targetId.isBlank() || labelKey.isBlank()) throw new IllegalArgumentException("Incomplete page link");
        }
    }
}
