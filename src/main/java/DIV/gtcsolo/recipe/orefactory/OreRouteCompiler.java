package DIV.gtcsolo.recipe.orefactory;

import java.util.*;

/** Recipe-graph arithmetic without Minecraft state. No material-specific recipe table. */
public final class OreRouteCompiler {
    public static final int BATCH = 4096;
    public static final int DURATION = 320;
    public static final int TARGET_TIER = 9;
    public enum Route { WASHING, CHEMICAL_WASHING, SEPARATION }
    public enum Process { MACERATOR, WASHER, CHEMICAL_BATH, THERMAL_CENTRIFUGE, SIFTER }
    public enum Form { ORE, RAW, CRUSHED, PURIFIED, REFINED, DUST, PURE_DUST, IMPURE_DUST, OTHER }
    public record Item(String id, String material, Form form, int amount, int chance, int maxChance, int boost) {
        public boolean guaranteed() { return chance == maxChance; }
    }
    public record Node(String id, Process process, Set<String> inputs, int inputAmount,
                       List<Item> outputs, int selectionRank, String invalidReason) {}
    public record Step(String recipeId, long runs, int forwardedOutput) {}
    public record Plan(Route route, String root, List<Step> steps) {}
    public record Result(List<Plan> plans, Map<String,String> skipped) {}
    private record Transition(Node node, int output) {}

    private final Map<Process,Map<String,List<Node>>> index = new EnumMap<>(Process.class);
    private final List<Node> roots;

    public OreRouteCompiler(Collection<Node> nodes, Map<String,Item> knownItems) {
        var sorted = nodes.stream().sorted(Comparator.comparing(Node::id)).toList();
        var starts = new ArrayList<Node>();
        for (Node node : sorted) {
            for (String input : node.inputs())
                index.computeIfAbsent(node.process(), ignored -> new HashMap<>())
                        .computeIfAbsent(input, ignored -> new ArrayList<>()).add(node);
            if (node.process() == Process.MACERATOR && node.inputs().stream().map(knownItems::get)
                    .filter(Objects::nonNull).anyMatch(i -> i.form() == Form.ORE || i.form() == Form.RAW))
                starts.add(node);
        }
        roots = List.copyOf(starts);
    }

    public Result compile() {
        var plans = new ArrayList<Plan>();
        var skipped = new TreeMap<String,String>();
        for (Node root : roots) for (Route route : Route.values()) {
            try { plans.add(plan(root, route)); }
            catch (IllegalArgumentException | ArithmeticException rejected) { skipped.put(root.id()+"/"+route, rejected.getMessage()); }
        }
        return new Result(List.copyOf(plans), Collections.unmodifiableMap(skipped));
    }

    private Plan plan(Node root, Route route) {
        requireUsable(root);
        if (root.inputAmount() != 1) throw new IllegalArgumentException("Root recipe must consume one ore unit");
        int main = uniqueMain(root, null, Set.of(Form.CRUSHED));
        Item stream = root.outputs().get(main);
        var steps = new ArrayList<Step>();
        steps.add(new Step(root.id(), 1, main));
        long quantity = stream.amount();

        Process wash = route == Route.CHEMICAL_WASHING ? Process.CHEMICAL_BATH : Process.WASHER;
        Transition purified = find(wash, stream, Set.of(Form.PURIFIED, Form.DUST));
        long runs = runs(quantity, purified.node().inputAmount());
        stream = purified.node().outputs().get(purified.output());
        quantity = Math.multiplyExact(runs, stream.amount());
        if (stream.form() == Form.DUST) {
            if (route == Route.SEPARATION) throw new IllegalArgumentException("No purified ore available for sifting");
            steps.add(new Step(purified.node().id(), runs, -1));
            return new Plan(route, root.id(), List.copyOf(steps));
        }
        steps.add(new Step(purified.node().id(), runs, purified.output()));

        if (route == Route.SEPARATION) {
            Node sifter = find(Process.SIFTER, stream, null).node();
            steps.add(new Step(sifter.id(), runs(quantity,sifter.inputAmount()), -1));
            return new Plan(route, root.id(), List.copyOf(steps));
        }

        Transition refined = find(Process.THERMAL_CENTRIFUGE, stream, Set.of(Form.REFINED,Form.DUST));
        runs = runs(quantity,refined.node().inputAmount());
        stream = refined.node().outputs().get(refined.output());
        quantity = Math.multiplyExact(runs,stream.amount());
        if (stream.form() == Form.DUST) {
            steps.add(new Step(refined.node().id(),runs,-1));
            return new Plan(route,root.id(),List.copyOf(steps));
        }
        steps.add(new Step(refined.node().id(),runs,refined.output()));
        Transition dust = find(Process.MACERATOR,stream,Set.of(Form.DUST,Form.PURE_DUST,Form.IMPURE_DUST));
        runs = runs(quantity,dust.node().inputAmount());
        stream = dust.node().outputs().get(dust.output());
        quantity = Math.multiplyExact(runs,stream.amount());
        // A refined ore often macerates straight to clean dust: no invented extra wash.
        if (stream.form() == Form.DUST) {
            steps.add(new Step(dust.node().id(),runs,-1));
        } else {
            steps.add(new Step(dust.node().id(),runs,dust.output()));
            Transition clean = find(Process.WASHER,stream,Set.of(Form.DUST));
            steps.add(new Step(clean.node().id(),runs(quantity,clean.node().inputAmount()),-1));
        }
        return new Plan(route,root.id(),List.copyOf(steps));
    }

    private Transition find(Process process, Item input, Set<Form> targets) {
        var matches = new ArrayList<Transition>();
        for (Node node : index.getOrDefault(process,Map.of()).getOrDefault(input.id(),List.of())) {
            if (node.invalidReason()!=null) continue;
            try { matches.add(new Transition(node,targets==null?-1:uniqueMain(node,input.material(),targets))); }
            catch (IllegalArgumentException ignored) { /* Not an edge for this material/form. */ }
        }
        if (matches.isEmpty()) throw new IllegalArgumentException("Missing "+process+" edge for "+input.id());
        matches.sort(Comparator.comparingInt((Transition t)->t.node().selectionRank()).thenComparing(t->t.node().id()));
        if (matches.size()>1 && matches.get(0).node().selectionRank()==matches.get(1).node().selectionRank())
            throw new IllegalArgumentException("Ambiguous "+process+" edge: "+matches.get(0).node().id()+", "+matches.get(1).node().id());
        return matches.get(0);
    }

    private static int uniqueMain(Node node,String material,Set<Form> forms) {
        // A guaranteed dust byproduct is not the main stream if the requested
        // purified/refined intermediate is also available.
        for(Form preferred:List.of(Form.CRUSHED,Form.PURIFIED,Form.REFINED)) {
            if(forms.contains(preferred)&&node.outputs().stream().anyMatch(o->o.guaranteed()&&o.form()==preferred
                    &&(material==null||material.equals(o.material())))) { forms=Set.of(preferred);break; }
        }
        int result=-1;
        for(int i=0;i<node.outputs().size();i++) {
            Item output=node.outputs().get(i);
            if(output.guaranteed() && forms.contains(output.form()) && (material==null||material.equals(output.material()))) {
                if(result>=0)throw new IllegalArgumentException("Ambiguous main output: "+node.id());
                result=i;
            }
        }
        if(result<0)throw new IllegalArgumentException("Missing guaranteed main output: "+node.id());
        for(int i=0;i<node.outputs().size();i++)if(i!=result&&Set.of(Form.CRUSHED,Form.PURIFIED,Form.REFINED,Form.PURE_DUST,Form.IMPURE_DUST).contains(node.outputs().get(i).form()))
            throw new IllegalArgumentException("Additional intermediate branch requires a separate routing rule: "+node.id());
        return result;
    }

    private static void requireUsable(Node node) {
        if(node.invalidReason()!=null)throw new IllegalArgumentException(node.invalidReason());
        if(node.inputAmount()<=0)throw new IllegalArgumentException("Invalid input amount");
    }

    public static long runs(long amount,int perRecipe) {
        if(perRecipe<=0||amount<=0||amount%perRecipe!=0)throw new IllegalArgumentException("Fractional or empty intermediate batch");
        return amount/perRecipe;
    }

    public static int scaled(int amount,long runs) {
        long result=Math.multiplyExact(Math.multiplyExact((long)amount,runs),BATCH);
        if(result<0||result>Integer.MAX_VALUE)throw new IllegalArgumentException("Aggregated amount exceeds GT int range");
        return (int)result;
    }

    /** Rebase GT's linear voltage bonus without converting probabilistic outputs into average yields. */
    public static int rebaseChance(int chance,int maxChance,int boost,int sourceTier) {
        if(maxChance<=0||chance<0||chance>maxChance||boost<0||sourceTier>TARGET_TIER)
            throw new IllegalArgumentException("Unsupported chance or source voltage");
        int stages=Math.max(0,TARGET_TIER-sourceTier-(sourceTier==0?1:0));
        return (int)Math.min(maxChance,(long)chance+(long)boost*stages);
    }
}
