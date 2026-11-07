// Additional solid elements, alloys and functional metals. One catalog drives registration and production.
// Existing custom singularities and Avaritia's iron/lead/tin/uranium/steel/invar/electrum are reused.
(function () {
    if (typeof AvaritiaEvents === 'undefined') return;

    const elementDusts = [
        'cadmium', 'caesium', 'calcium', 'carbon', 'cerium', 'iodine', 'lanthanum',
        'lithium', 'lutetium', 'magnesium', 'selenium', 'sodium', 'tellurium'
    ];
    const metalIngots = [
        'americium', 'annealed_copper', 'battery_alloy', 'beryllium', 'bismuth_bronze',
        'black_bronze', 'black_steel', 'blue_steel', 'brass', 'cobalt_brass',
        'cupronickel', 'damascus_steel', 'enriched_naquadah',
        'enriched_naquadah_trinium_europium_duranide', 'gallium_arsenide',
        'hastelloy_c_276', 'hastelloy_x', 'hsla_steel', 'hsse', 'hssg', 'hsss',
        'incoloy_ma_956', 'indium_gallium_phosphide',
        'indium_tin_barium_titanium_cuprate', 'iridium', 'kanthal', 'magnalium',
        'magnesium_diboride', 'manganese', 'manganese_phosphide', 'maraging_steel_300',
        'mercury_barium_calcium_cuprate', 'molybdenum_disilicide', 'naquadah_alloy',
        'naquadria', 'nichrome', 'nickel_zinc_ferrite', 'niobium_nitride',
        'niobium_titanium', 'osmiridium', 'plutonium_241', 'potin', 'red_steel',
        'rhodium_plated_palladium', 'rose_gold', 'rtm_alloy', 'ruridit',
        'ruthenium_trinium_americium_neutronate', 'samarium_iron_arsenic_oxide',
        'silicon', 'soldering_alloy', 'stainless_steel', 'stellite_100',
        'sterling_silver', 'tantalum_carbide', 'thorium', 'tin_alloy', 'titanium',
        'titanium_carbide', 'titanium_tungsten_carbide', 'tungsten',
        'tungsten_carbide', 'tungsten_steel', 'ultimet', 'uranium_235',
        'uranium_rhodium_dinaquadide', 'uranium_triplatinum', 'vanadium',
        'vanadium_gallium', 'vanadium_steel', 'watertight_steel', 'wrought_iron',
        'yttrium', 'yttrium_barium_cuprate', 'zeron_100', 'zinc'
    ];
    const entries = elementDusts.map(name => ({name: name, item: 'gtceu:' + name + '_dust'}))
        .concat(metalIngots.map(name => ({name: name, item: 'gtceu:' + name + '_ingot'})));
    const count = 1000;
    const duration = 240;
    const eut = 131072; // Existing singularity compressor baseline: ZPM.

    function materialFor(entry) {
        const material = GTMaterials.get('gtceu:' + entry.name);
        if (material === null || !Item.exists(entry.item)) {
            console.warn('[Element/alloy singularities] Missing material or item: ' + entry.name + ' / ' + entry.item);
            return null;
        }
        return material;
    }

    function brighten(rgb) {
        return (Math.min(255, Math.round(((rgb >> 16) & 255) * 1.4)) << 16)
            | (Math.min(255, Math.round(((rgb >> 8) & 255) * 1.4)) << 8)
            | Math.min(255, Math.round((rgb & 255) * 1.4));
    }

    AvaritiaEvents.singularity(event => {
        let registered = 0;
        entries.forEach(entry => {
            const material = materialFor(entry);
            if (material === null) return;
            const color = material.getMaterialRGB() & 0xFFFFFF;
            event.register('gtcsolo:singularity_' + entry.name, singularity => {
                singularity.setDisplayName(material.getUnlocalizedName());
                singularity.setColors(brighten(color), color);
                singularity.setIngredient(Ingredient.of(entry.item));
                singularity.setCount(count);
                singularity.setTimeCost(duration);
                singularity.setRecipeEnabled(false);
            });
            registered++;
        });

        // Aggregation target. Its ingredients and production recipe are intentionally left for pack authoring.
        // A nonempty display ingredient is required by the native Shift-preview renderer.
        event.register('gtcsolo:singularity_gregtech', singularity => {
            singularity.setDisplayName('singularity.gtcsolo.gregtech');
            singularity.setColors(0xCC3333, 0xC6C6C6);
            singularity.setIngredient(Ingredient.of('minecraft:barrier'));
            singularity.setRecipeEnabled(false);
        });
        console.info('[Element/alloy singularities] Registered ' + registered + ' material singularities and the GregTech aggregation target.');
    });

    ServerEvents.recipes(event => {
        entries.forEach(entry => {
            if (materialFor(entry) === null) return;
            const singularityId = 'gtcsolo:singularity_' + entry.name;
            event.recipes.gtcsolo.singularity_compresser('gtcsolo:autogen_singularity_gtcsolo_singularity_' + entry.name)
                .itemInputs(count + 'x ' + entry.item)
                .itemOutputs(Item.of('avaritia:singularity', {Id: singularityId}))
                .duration(duration).EUt(eut);
        });
    });
})();
