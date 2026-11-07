package DIV.gtcsolo.recipe.orefactory;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.registry.ModRecipeTypes;
import com.google.gson.*;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.UnificationEntry;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.item.TagPrefixItem;
import com.gregtechceu.gtceu.api.recipe.*;
import com.gregtechceu.gtceu.api.recipe.chance.boost.ChanceBoostFunction;
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntCircuitIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.IntProviderIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import DIV.gtcsolo.recipe.orefactory.OreRouteCompiler.Process;

import static DIV.gtcsolo.recipe.orefactory.OreRouteCompiler.*;

/** Reads final GT recipe instances after KubeJS has applied its edits. */
public final class OreFactoryRecipes {
    private record Ref(String id, Material material, TagPrefix prefix, Set<String> tags) {}
    private record Input(int index, Ingredient ingredient, int amount) {}
    private OreFactoryRecipes() {}

    public static void inject(Map<ResourceLocation,Recipe<?>> runtime) {
        if (ModRecipeTypes.ORE_WASHING_PROCESS == null) return;
        Map<String,GTRecipe> sources = new TreeMap<>();
        for (Recipe<?> recipe : runtime.values())
            if (recipe instanceof GTRecipe gt && process(gt.recipeType)!=null) sources.put(gt.id.toString(),gt);
        Map<String,Ref> refs = new LinkedHashMap<>();
        Set<Material> materials = new LinkedHashSet<>();
        // Read declared vanilla/mod overrides directly. Calling the global
        // unification cache before tags bind can cache a false "not a material".
        Map<net.minecraft.world.item.Item,UnificationEntry> overrides=new IdentityHashMap<>();
        for(TagPrefix prefix:TagPrefix.values())for(var entry:prefix.getIgnored().entrySet())
            for(var supplier:entry.getValue())overrides.put(supplier.get().asItem(),new UnificationEntry(prefix,entry.getKey()));
        for (GTRecipe source : sources.values()) for (Content output : source.getOutputContents(ItemRecipeCapability.CAP)) {
            Ingredient ingredient=ItemRecipeCapability.CAP.of(output.content);
            if(ingredient instanceof IntProviderIngredient)continue;
            for(ItemStack stack:ingredient.getItems()) {
                Ref ref=ref(stack,overrides);
                refs.putIfAbsent(ref.id(),ref);
                if(ref.material()!=null) materials.add(ref.material());
            }
        }
        // Synthetic root tag markers avoid depending on live tag binding during reload.
        // They are used only for graph classification, never as actual recipe ingredients.
        var orePrefixes=new ArrayList<TagPrefix>(TagPrefix.ORES.keySet());
        orePrefixes.add(TagPrefix.rawOre);orePrefixes.add(TagPrefix.rawOreBlock);
        for(Material material:materials) for(TagPrefix prefix:orePrefixes) {
            if(!material.hasProperty(PropertyKey.ORE))continue;
            Set<String> tags=tags(prefix,material);
            String marker="#"+prefix.name+"/"+material.getResourceLocation();
            refs.putIfAbsent(marker,new Ref(marker,material,prefix,tags));
            var ignoredItems=prefix.getIgnored().get(material);
            if(ignoredItems!=null) for(var supplier:ignoredItems) {
                var item=supplier.get().asItem();String id=ForgeRegistries.ITEMS.getKey(item).toString();
                refs.put(id,new Ref(id,material,prefix,tags));
            }
        }
        Map<String,Set<String>> byTag=new HashMap<>();
        refs.values().forEach(ref->ref.tags().forEach(tag->byTag.computeIfAbsent(tag,k->new HashSet<>()).add(ref.id())));
        Map<String,Item> known=new HashMap<>();
        refs.values().forEach(ref->known.put(ref.id(),new Item(ref.id(),materialName(ref),form(ref.prefix()),1,1,1,0)));
        var nodes=new ArrayList<Node>();
        Map<String,String> rejected=new TreeMap<>();
        for(GTRecipe recipe:sources.values()) {
            try { nodes.add(node(recipe,refs,byTag)); }
            catch(RuntimeException failure) { rejected.put(recipe.id.toString(),failure.getMessage()); }
        }
        Result graph=new OreRouteCompiler(nodes,known).compile();
        List<GTRecipe> generated=new ArrayList<>();
        Map<String,String> skipped=new TreeMap<>(graph.skipped());
        for(Plan plan:graph.plans()) {
            try { generated.add(aggregate(plan,sources)); }
            catch(RuntimeException failure) { skipped.put(plan.root()+"/"+plan.route(),failure.getMessage()); }
        }
        // Replace our own generated IDs only. Explicit recipes in the new types are retained.
        runtime.keySet().removeIf(id->id.getNamespace().equals("gtcsolo")&&id.getPath().startsWith("ore_factory/"));
        for(GTRecipe recipe:generated) runtime.put(recipe.id,recipe);
        for(GTRecipeType type:List.of(ModRecipeTypes.ORE_WASHING_PROCESS,ModRecipeTypes.ORE_CHEMICAL_WASHING_PROCESS,ModRecipeTypes.ORE_SEPARATION_PROCESS)) {
            type.getLookup().removeAllRecipes();
            runtime.values().stream().filter(r->r.getType()==type).map(GTRecipe.class::cast).forEach(type.getLookup()::addRecipe);
        }
        Gtcsolo.LOGGER.info("[ore_factory] Generated {} routes from {} candidate recipes; skipped {} routes, rejected {} unsupported recipes",
                generated.size(),sources.size(),skipped.size(),rejected.size());
        try {
            JsonObject report=new JsonObject();report.addProperty("batch",BATCH);report.addProperty("duration",DURATION);
            report.addProperty("eut",GTValues.VA[GTValues.UHV]);report.addProperty("generated",generated.size());
            Gson gson=new GsonBuilder().setPrettyPrinting().create();
            report.add("plans",gson.toJsonTree(graph.plans()));report.add("skipped",gson.toJsonTree(skipped));
            report.add("rejectedSources",gson.toJsonTree(rejected));
            var output=FMLPaths.GAMEDIR.get().resolve("logs/gtcsolo-ore-factory.json");
            Files.createDirectories(output.getParent());Files.writeString(output,gson.toJson(report),StandardCharsets.UTF_8);
        } catch(Exception failure) { Gtcsolo.LOGGER.warn("Cannot write ore factory generation report",failure); }
    }

    private static Node node(GTRecipe recipe,Map<String,Ref> refs,Map<String,Set<String>> byTag) {
        if(!recipe.conditions.isEmpty()||!recipe.ingredientActions.isEmpty()||!recipe.data.isEmpty()) throw new IllegalArgumentException("Conditional/action/data recipe needs an explicit aggregation rule");
        if(recipe.recipeType.getChanceFunction()!=ChanceBoostFunction.OVERCLOCK&&recipe.recipeType.getChanceFunction()!=ChanceBoostFunction.NONE)
            throw new IllegalArgumentException("Custom chance function");
        if(recipe.outputChanceLogics.getOrDefault(ItemRecipeCapability.CAP,ChanceLogic.OR)!=ChanceLogic.OR)
            throw new IllegalArgumentException("Non-independent output chance logic");
        if(unsupported(recipe.inputs,Set.of(ItemRecipeCapability.CAP,FluidRecipeCapability.CAP))
                ||unsupported(recipe.outputs,Set.of(ItemRecipeCapability.CAP))
                ||unsupported(recipe.tickInputs,Set.of(EURecipeCapability.CAP))||unsupported(recipe.tickOutputs,Set.of()))
            throw new IllegalArgumentException("Unsupported processing capability");
        if(java.util.stream.Stream.of(recipe.inputs,recipe.outputs).flatMap(m->m.values().stream()).flatMap(List::stream)
                .anyMatch(c->c.slotName!=null))throw new IllegalArgumentException("Named-slot recipe needs an explicit aggregation rule");
        Input input=input(recipe);
        Set<String> ids=resolve(input.ingredient().toJson(),byTag);
        var outputs=new ArrayList<Item>();
        for(Content c:recipe.getOutputContents(ItemRecipeCapability.CAP)) {
            var ingredient=ItemRecipeCapability.CAP.of(c.content);int count=amount(ingredient);ItemStack[] stacks=ingredient.getItems();
            if(stacks.length!=1||stacks[0].hasTag()||c.chance<=0||c.tierChanceBoost<0)throw new IllegalArgumentException("Unsupported output ingredient/chance");
            Ref ref=refs.get(ForgeRegistries.ITEMS.getKey(stacks[0].getItem()).toString());
            outputs.add(new Item(ref.id(),materialName(ref),form(ref.prefix()),count,c.chance,c.maxChance,c.tierChanceBoost));
        }
        for(Content c:recipe.getInputContents(FluidRecipeCapability.CAP))
            if(c.chance!=c.maxChance||FluidRecipeCapability.CAP.of(c.content).getAmount()<=0)
                throw new IllegalArgumentException("Probabilistic/empty fluid input");
        int rank=0;
        if(process(recipe.recipeType)==Process.WASHER) {
            boolean fullWash=outputs.size()>1;
            String fluid=recipe.getInputContents(FluidRecipeCapability.CAP).stream()
                    .map(c->FluidRecipeCapability.CAP.of(c.content).toJson().toString()).reduce("",String::concat);
            rank=(fullWash?0:20)+(fluid.contains("distilled_water")?10:0);
        }
        return new Node(recipe.id.toString(),process(recipe.recipeType),ids,input.amount(),List.copyOf(outputs),rank,null);
    }

    private static boolean unsupported(Map<RecipeCapability<?>,List<Content>> map,Set<RecipeCapability<?>> allowed) {
        return map.entrySet().stream().anyMatch(e->!e.getValue().isEmpty()&&!allowed.contains(e.getKey()));
    }

    private static GTRecipe aggregate(Plan plan,Map<String,GTRecipe> sources) {
        GTRecipe root=sources.get(plan.root());Input input=input(root);
        GTRecipeType type=switch(plan.route()) {
            case WASHING->ModRecipeTypes.ORE_WASHING_PROCESS;
            case CHEMICAL_WASHING->ModRecipeTypes.ORE_CHEMICAL_WASHING_PROCESS;
            case SEPARATION->ModRecipeTypes.ORE_SEPARATION_PROCESS;
        };
        ResourceLocation id=new ResourceLocation("gtcsolo","ore_factory/"+plan.route().name().toLowerCase(Locale.ROOT)+"/"+root.id.getNamespace()+"/"+root.id.getPath());
        GTRecipe result=type.recipeBuilder(id).duration(DURATION).EUt(GTValues.VA[GTValues.UHV]).buildRawRecipe();
        result.setId(id); // buildRawRecipe otherwise prepends the recipe-type path.
        result.inputs.put(ItemRecipeCapability.CAP,new ArrayList<>(List.of(scale(root.getInputContents(ItemRecipeCapability.CAP).get(input.index()),ItemRecipeCapability.CAP,scaled(input.amount(),1),input.amount()))));
        var outputs=new ArrayList<Content>();var fluids=new ArrayList<Content>();
        for(Step step:plan.steps()) {
            GTRecipe source=sources.get(step.recipeId());
            for(Content c:source.getInputContents(FluidRecipeCapability.CAP)) {
                int amount=FluidRecipeCapability.CAP.of(c.content).getAmount();
                fluids.add(scale(c,FluidRecipeCapability.CAP,scaled(amount,step.runs()),amount));
            }
            var list=source.getOutputContents(ItemRecipeCapability.CAP);
            for(int i=0;i<list.size();i++) {
                if(i==step.forwardedOutput())continue;
                Content original=list.get(i);int amount=amount(ItemRecipeCapability.CAP.of(original.content));
                Content c=scale(original,ItemRecipeCapability.CAP,scaled(amount,step.runs()),amount);
                int boost=source.recipeType.getChanceFunction()==ChanceBoostFunction.NONE?0:original.tierChanceBoost;
                int sourceTier=RecipeHelper.getRecipeEUtTier(source);
                c.chance=rebaseChance(original.chance,original.maxChance,boost,sourceTier);
                c.tierChanceBoost=boost;
                outputs.add(c);
            }
        }
        if(outputs.size()>24||fluids.size()>4)throw new IllegalArgumentException("Aggregated I/O exceeds declared slots");
        result.outputs.put(ItemRecipeCapability.CAP,outputs);
        if(!fluids.isEmpty())result.inputs.put(FluidRecipeCapability.CAP,fluids);
        result.data.putInt("ore_factory_batch",BATCH);
        result.data.putString("ore_factory_sources",String.join(" -> ",plan.steps().stream().map(Step::recipeId).toList()));
        result.data.putString("ore_factory_route",plan.route().name());
        return result;
    }

    private static Content scale(Content original,RecipeCapability<?> cap,int value,int amount) {
        Content result=original.copy(cap);
        result.content=cap.copyContent(original.content,ContentModifier.multiplier((double)value/amount));
        result.tierChanceBoost=original.tierChanceBoost;
        result.uiName=null;
        return result;
    }

    private static Input input(GTRecipe recipe) {
        Input selected=null;var entries=recipe.getInputContents(ItemRecipeCapability.CAP);
        for(int i=0;i<entries.size();i++) {
            Content c=entries.get(i);Ingredient ingredient=ItemRecipeCapability.CAP.of(c.content);
            Ingredient inner=ingredient instanceof SizedIngredient sized?sized.getInner():ingredient;
            if(c.chance==0&&inner instanceof IntCircuitIngredient)continue;
            if(c.chance!=c.maxChance||selected!=null)throw new IllegalArgumentException("Expected one deterministic ore input plus internal circuit");
            selected=new Input(i,ingredient,amount(ingredient));
        }
        if(selected==null)throw new IllegalArgumentException("No ore input");
        return selected;
    }

    private static int amount(Ingredient ingredient) {
        if(ingredient instanceof IntProviderIngredient)throw new IllegalArgumentException("Random ingredient quantity");
        int n=ingredient instanceof SizedIngredient sized?sized.getAmount():1;
        if(n<=0)throw new IllegalArgumentException("Empty ingredient");
        return n;
    }

    /** Match tag expressions structurally: reload need not have rebound vanilla item holders yet. */
    static Set<String> resolve(JsonElement json,Map<String,Set<String>> tags) {
        if(json.isJsonArray()) {Set<String> union=new HashSet<>();for(var child:json.getAsJsonArray())union.addAll(resolve(child,tags));return union;}
        JsonObject obj=json.getAsJsonObject();
        if(obj.has("nbt"))throw new IllegalArgumentException("NBT-specific ingredient");
        if(obj.has("item"))return Set.of(obj.get("item").getAsString());
        if(obj.has("tag"))return Set.copyOf(tags.getOrDefault(obj.get("tag").getAsString(),Set.of()));
        String type=obj.has("type")?obj.get("type").getAsString():"";
        if(type.equals("gtceu:sized"))return resolve(obj.get("ingredient"),tags);
        if(type.equals("forge:intersection")) {
            Set<String> intersection=null;
            for(var child:obj.getAsJsonArray("children")) {
                Set<String> values=resolve(child,tags);
                if(intersection==null)intersection=new HashSet<>(values);else intersection.retainAll(values);
            }
            return intersection==null?Set.of():intersection;
        }
        if(type.equals("forge:compound"))return resolve(obj.get("children"),tags);
        throw new IllegalArgumentException("Unsupported ingredient expression: "+type);
    }

    private static Ref ref(ItemStack stack,Map<net.minecraft.world.item.Item,UnificationEntry> overrides) {
        String id=ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
        UnificationEntry entry=stack.getItem() instanceof TagPrefixItem item?new UnificationEntry(item.tagPrefix,item.material):overrides.get(stack.getItem());
        if(entry==null||entry.material==null)return new Ref(id,null,null,Set.of());
        return new Ref(id,entry.material,entry.tagPrefix,tags(entry.tagPrefix,entry.material));
    }

    private static Set<String> tags(TagPrefix prefix,Material material) {
        Set<String> set=new HashSet<>();
        Arrays.stream(prefix.getItemTags(material)).forEach(t->set.add(t.location().toString()));
        Arrays.stream(prefix.getItemParentTags()).forEach(t->set.add(t.location().toString()));
        return Set.copyOf(set);
    }
    private static String materialName(Ref ref) {return ref.material()==null?"":ref.material().getResourceLocation().toString();}
    private static Form form(TagPrefix prefix) {
        if(prefix==TagPrefix.rawOre||prefix==TagPrefix.rawOreBlock)return Form.RAW;
        if(prefix!=null&&TagPrefix.ORES.containsKey(prefix))return Form.ORE;
        if(prefix==TagPrefix.crushed)return Form.CRUSHED;
        if(prefix==TagPrefix.crushedPurified)return Form.PURIFIED;
        if(prefix==TagPrefix.crushedRefined)return Form.REFINED;
        if(prefix==TagPrefix.dust)return Form.DUST;
        if(prefix==TagPrefix.dustPure)return Form.PURE_DUST;
        if(prefix==TagPrefix.dustImpure)return Form.IMPURE_DUST;
        return Form.OTHER;
    }
    private static Process process(GTRecipeType type) {
        if(type==GTRecipeTypes.MACERATOR_RECIPES)return Process.MACERATOR;
        if(type==GTRecipeTypes.ORE_WASHER_RECIPES)return Process.WASHER;
        if(type==GTRecipeTypes.CHEMICAL_BATH_RECIPES)return Process.CHEMICAL_BATH;
        if(type==GTRecipeTypes.THERMAL_CENTRIFUGE_RECIPES)return Process.THERMAL_CENTRIFUGE;
        if(type==GTRecipeTypes.SIFTER_RECIPES)return Process.SIFTER;
        return null;
    }
}
