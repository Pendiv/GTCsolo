// priority: -1000
// Derive alternatives from the native GT/KubeJS recipe collection after pack recipes are added.
ServerEvents.recipes(event => {
    const GTRecipeJS = Java.loadClass('com.gregtechceu.gtceu.integration.kjs.recipe.GTRecipeSchema$GTRecipeJS');
    const RecipeJS = Java.loadClass('dev.latvian.mods.kubejs.recipe.RecipeJS');
    const ResourceLocation = Java.loadClass('net.minecraft.resources.ResourceLocation');
    const JsonParser = Java.loadClass('com.google.gson.JsonParser');
    const variants = [
        {name: 'hnbr', fluid: 'gtcsolo:hydrogenated_nitrile_butadiene_rubber', divisor: 4},
        {name: 'ffkm', fluid: 'gtcsolo:perfluoroelastomer', divisor: 16}
    ];
    if (variants.some(variant => !Fluid.exists(variant.fluid))) {
        console.info('[Rubber alternatives] Restart Minecraft to register the advanced rubber fluids.');
        return;
    }
    // Regenerate only our own alternatives, including removal of older casting variants.
    event.remove({id: /^gtcsolo:rubber_alternatives\//});
    // PBI assembly recipes can also use the two advanced elastomers.
    const rubberFluids = {
        'gtceu:rubber': 1,
        'gtceu:silicone_rubber': 2,
        'gtceu:styrene_butadiene_rubber': 3,
        'gtceu:polybenzimidazole': 4
    };
    const rubberTags = {
        'forge:rubber': 1,
        'forge:silicone_rubber': 2,
        'forge:styrene_butadiene_rubber': 3,
        'forge:polybenzimidazole': 4
    };
    // Efficiency applies to assembling components, never to casting/converting polymer feedstock.
    const assemblyTypes = {
        'gtceu:assembler': true,
        'gtceu:assembly_line': true,
        'gtceu:circuit_assembler': true,
        'gtceu:circuit_assembly_line': true
    };
    const rubberMaterials = ['rubber', 'silicone_rubber', 'styrene_butadiene_rubber', 'polybenzimidazole',
        'hydrogenated_nitrile_butadiene_rubber', 'perfluoroelastomer'];
    const plans = {};
    let matched = 0;
    let rejected = 0;

    // Canonicalize only for duplicate detection; recipe arrays retain their original order.
    function signature(value) {
        if (value == null || typeof value !== 'object') return JSON.stringify(value);
        if (Array.isArray(value)) return '[' + value.map(signature).join(',') + ']';
        return '{' + Object.keys(value).sort().map(key => JSON.stringify(key) + ':' + signature(value[key])).join(',') + '}';
    }

    function consumedRubber(entry) {
        if (!entry || entry.chance === 0 || !entry.content) return false;
        const content = entry.content;
        if (!Number.isInteger(content.amount) || content.amount <= 0) return false;
        if (!Array.isArray(content.value) || content.value.length === 0) return false;
        // A mixed rubber/non-rubber choice is not a rubber-only input and stays unchanged.
        return content.value.every(value =>
            (value.fluid && rubberFluids[value.fluid]) || (value.tag && rubberTags[value.tag]));
    }

    function producesRubberMaterial(value) {
        if (!value || typeof value !== 'object') return false;
        if (Array.isArray(value)) return value.some(producesRubberMaterial);
        return Object.keys(value).some(key => {
            const child = value[key];
            // Rhino 18 can retain a block-scoped const between callback invocations.
            const path = typeof child === 'string' ? child.split(':').pop() : '';
            if ((key === 'item' || key === 'fluid' || key === 'tag') && typeof child === 'string') {
                if (rubberMaterials.some(name => path === name || path.indexOf(name + '_') === 0 ||
                    path.endsWith('/' + name) || path.endsWith('_' + name) || path.indexOf('_' + name + '_') !== -1)) return true;
            }
            return child && typeof child === 'object' && producesRubberMaterial(child);
        });
    }

    // Use KubeJS's synchronous native collection, including recipes added by other scripts.
    // Fluid-stack input filters depend on bound tags; inspect native ingredient IDs instead,
    // so first world load and /reload behave identically before tag binding finishes.
    event.forEachRecipe({}, recipe => {
        if (!(recipe instanceof GTRecipeJS)) return;
        const source = String(recipe.id);
        if (source.indexOf('rubber_alternatives/') !== -1) return;
        let data;
        try {
            // Flush native builder fields before reading new pack recipes from this event.
            recipe.serialize();
            data = JSON.parse(String(recipe.json));
        } catch (error) {
            rejected++;
            console.warn('[Rubber alternatives] Cannot inspect ' + source + ': ' + error);
            return;
        }
        if (!assemblyTypes[data.type]) return;
        if (producesRubberMaterial(data.outputs) || producesRubberMaterial(data.tickOutputs)) return;
        const positions = [];
        ['inputs', 'tickInputs'].forEach(section => {
            const entries = data[section] && data[section].fluid;
            if (!Array.isArray(entries)) return;
            entries.forEach((entry, index) => {
                if (consumedRubber(entry)) positions.push({section: section, index: index});
            });
        });
        if (positions.length === 0) return;
        matched++;
        variants.forEach(variant => {
            const copy = JSON.parse(JSON.stringify(data));
            const normalized = JSON.parse(JSON.stringify(data));
            let amount = 0;
            positions.forEach(position => {
                const original = data[position.section].fluid[position.index].content;
                const replacement = copy[position.section].fluid[position.index].content;
                replacement.amount = Math.max(1, Math.ceil(original.amount / variant.divisor));
                replacement.value = [{fluid: variant.fluid}];
                amount += replacement.amount;
                const keyContent = normalized[position.section].fluid[position.index].content;
                // Multiple/start-and-tick inputs can have different allocation constraints.
                // Coalesce different quantities only for a single rubber slot.
                keyContent.amount = positions.length === 1 ? 0 : replacement.amount;
                keyContent.value = [{fluid: variant.fluid}];
            });
            // Identity belongs to the derived recipe, not to the copied payload.
            delete copy.id;
            delete normalized.id;
            const key = signature(normalized);
            const previous = plans[key];
            // Native rubber options can describe the same operation with different quantities.
            // Keep the cheapest derived option; never discard differences in other inputs,
            // outputs, research, conditions, time, power, probabilities or NBT.
            if (!previous || amount < previous.amount || (amount === previous.amount && source < previous.source)) {
                plans[key] = {source: source, amount: amount, variant: variant.name, data: copy};
            }
        });
    });

    // Add only after enumeration; a derived recipe must never become another source.
    const keys = Object.keys(plans).sort();
    keys.forEach(key => {
        const plan = plans[key];
        const id = 'gtcsolo:rubber_alternatives/' + plan.variant + '/' + plan.source.replace(':', '/');
        // This is an already-finalized native GT recipe, not a new GT data builder.
        // A plain RecipeJS passes it to its native serializer without rerunning GT's
        // builder injection (which requires the private GTRecipeJS idWithoutType).
        const derived = new RecipeJS();
        derived.type = event.getRecipeFunction(plan.data.type);
        derived.id = new ResourceLocation(id);
        derived.json = JsonParser.parseString(JSON.stringify(plan.data)).getAsJsonObject();
        derived.newRecipe = true;
        event.addRecipe(derived, true);
    });
    console.info('[Rubber alternatives] Matched ' + matched + ' native GT recipes; added ' + keys.length +
        ' alternatives (HNBR /4, FFKM /16); unreadable recipes: ' + rejected + '.');
});
