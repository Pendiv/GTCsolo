// Authored precision_blast_furnance.txt, normalized from its east-facing controller.
(function () {
    GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
        event.create('precision_blast_furnace')
            .category('multiblock').setMaxIOSize(9, 0, 3, 1).setEUIO('in')
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, FillDirection.LEFT_TO_RIGHT);
    });

    GTCEuStartupEvents.registry('gtceu:machine', event => {
        const CoilMachine = Java.loadClass('com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine');
        const GTUtil = Java.loadClass('com.gregtechceu.gtceu.utils.GTUtil');
        const RecipeHelper = Java.loadClass('com.gregtechceu.gtceu.api.recipe.RecipeHelper');
        const ModifierFunction = Java.loadClass('com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction');
        const ContentModifier = Java.loadClass('com.gregtechceu.gtceu.api.recipe.content.ContentModifier');
        const FluidCapability = Java.loadClass('com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability');
        const Direction = Java.loadClass('com.gregtechceu.gtceu.api.pattern.util.RelativeDirection');

        GTRecipeTypes.get('precision_blast_furnace').addDataInfo(data =>
            Component.translatable('gtceu.recipe.temperature', data.getInt('ebf_temp')).getString());

        function precisionOverclock(machine, recipe) {
            if (!machine.isFormed()) return ModifierFunction.NULL;
            const voltage = Number(machine.getOverclockVoltage());
            const eut = Number(RecipeHelper.getInputEUt(recipe));
            if (eut > voltage) return ModifierFunction.NULL;
            // Keep native furnace temperature eligibility; output allowance depends on the coil itself.
            const coilTemperature = Number(machine.getCoilType().getCoilTemperature());
            const temperature = coilTemperature + 100 * Math.max(0, machine.getTier() - 2);
            const requiredTemperature = recipe.data.getInt('ebf_temp');
            if (requiredTemperature > temperature) return ModifierFunction.NULL;

            let volume = 0;
            recipe.getOutputContents(FluidCapability.CAP).forEach(content => {
                volume += Number(content.content.getAmount());
            });
            if (!Number.isFinite(volume) || volume < 0 || volume > 9007199254740991) return ModifierFunction.NULL;
            const allowance = 144 * (1 + Math.floor(Math.max(0, coilTemperature - 1800) / 900));
            const bands = Math.ceil(Math.max(0, volume - allowance) / 144);
            const baseTicks = Number(recipe.duration);
            if (baseTicks < 1) return ModifierFunction.NULL;
            const ticks = Math.ceil(baseTicks * (10 + bands) / 10);
            if (!Number.isFinite(ticks) || ticks > 2147483647) return ModifierFunction.NULL;
            const recipeTier = RecipeHelper.getRecipeEUtTier(recipe);
            const maxOCs = GTUtil.getOCTierByVoltage(voltage) - recipeTier - (recipeTier === 0 ? 1 : 0);
            const perfectOCs = Math.floor(Math.max(0, temperature - requiredTemperature) / 1800);
            let duration = ticks;
            let power = eut;
            let applied = 0;
            let nextPower = 0;
            let nextDuration = 0;
            while (power > 0 && applied < maxOCs) {
                nextPower = power * 4;
                nextDuration = duration / (applied < perfectOCs ? 4 : 2.25);
                if (nextPower > voltage || nextDuration < 1) break;
                power = nextPower;
                duration = nextDuration;
                applied++;
            }
            // Exact integer duration; native content copying preserves quantities and catalyst chances.
            const modifier = ModifierFunction.builder()
                .durationModifier(ContentModifier.addition(Math.floor(duration) - baseTicks));
            if (applied > 0) modifier.eutMultiplier(Math.pow(4, applied)).addOCs(applied);
            return modifier.build();
        }

        event.create('precision_blast_furnace', 'multiblock')
            .rotationState(RotationState.NON_Y_AXIS).allowExtendedFacing(false)
            .machine(holder => new CoilMachine(holder))
            .recipeTypes([GTRecipeTypes.get('precision_blast_furnace'), GTRecipeTypes.get('alloy_blast_smelter')])
            .recipeModifiers(true, [GTRecipeModifiers.DEFAULT_ENVIRONMENT_REQUIREMENT, precisionOverclock])
            .appearanceBlock(() => Block.getBlock('gtceu:robust_machine_casing'))
            .pattern(definition => FactoryBlockPattern.start(Direction.RIGHT, Direction.UP, Direction.BACK)
                // AUTHORED_AISLES_BEGIN
                .aisle('##ASA##', '##BBB##', '##CCC##', '##BBB##', '##CCC##', '##BBB##', '##AAA##')
                .aisle('#AAAAA#', '#BD#DB#', '#CD#DC#', '#BD#DB#', '#CD#DC#', '#BD#DB#', '#AAAAA#')
                .aisle('AAAAAAA', 'BD###DB', 'CD###DC', 'BD###DB', 'CD###DC', 'BD###DB', 'AAAAAAA')
                .aisle('AAAAAAA', 'B#####B', 'C#####C', 'B#####B', 'C#####C', 'B#####B', 'AAAFAAA')
                .aisle('AAAAAAA', 'BD###DB', 'CD###DC', 'BD###DB', 'CD###DC', 'BD###DB', 'AAAAAAA')
                .aisle('#AAAAA#', '#BD#DB#', '#CD#DC#', '#BD#DB#', '#CD#DC#', '#BD#DB#', '#AAAAA#')
                .aisle('##AAA##', '##BBB##', '##CCC##', '##BBB##', '##CCC##', '##BBB##', '##AAA##')
                // AUTHORED_AISLES_END
                .where('S', Predicates.controller(Predicates.blocks(definition.getBlock())))
                .where('A', Predicates.blocks('gtceu:robust_machine_casing').setMinGlobalLimited(44)
                    .or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1).setMaxGlobalLimited(2).setPreviewCount(1))
                    .or(Predicates.abilities(PartAbility.IMPORT_ITEMS).setMinGlobalLimited(1).setPreviewCount(1))
                    .or(Predicates.abilities(PartAbility.IMPORT_FLUIDS).setPreviewCount(1))
                    .or(Predicates.abilities(PartAbility.EXPORT_FLUIDS).setMinGlobalLimited(1).setPreviewCount(1))
                    .or(Predicates['autoAbilities(boolean,boolean,boolean)'](true, false, false)))
                .where('B', Predicates.heatingCoils())
                .where('C', Predicates.blocks('gtceu:tungstensteel_firebox_casing'))
                .where('D', Predicates.blocks('gtceu:tungstensteel_pipe_casing'))
                .where('F', Predicates.abilities(PartAbility.MUFFLER).setExactLimit(1))
                .where('#', Predicates.any()).build())
            .workableCasingRenderer('gtceu:block/casings/solid/machine_casing_robust_tungstensteel',
                'gtceu:block/multiblock/electric_blast_furnace', false)
            .tooltips([Component.translatable('gtceu.machine.available_recipe_map_2.tooltip',
                Component.translatable('gtceu.precision_blast_furnace'), Component.translatable('gtceu.alloy_blast_smelter')),
                Component.translatable('gtcsolo.jei.introduction.details_hint')]);
    });
})();
