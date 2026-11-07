['item', 'block'].forEach(registry => {
    ServerEvents.tags(registry, event => {
        if (!Platform.isLoaded('create')) return;
        global.createRockProfiles.forEach(rock => event.add('forge:' + rock.id, 'create:' + rock.id));
    });
});

ServerEvents.recipes(event => {
    if (!Platform.isLoaded('create')) return;
    global.createRockProfiles.forEach(rock => {
        event.recipes.gtceu.macerator('gtcsolo:create_rocks/macerate_' + rock.id)
            .itemInputs('#forge:' + rock.id).itemOutputs('gtcsolo:' + rock.id + '_dust')
            .duration(150).EUt(2);
        const recipe = event.recipes.gtceu.centrifuge('gtcsolo:create_rocks/separate_' + rock.id)
            .itemInputs('8x gtcsolo:' + rock.id + '_dust')
            .duration(480).EUt(120);
        rock.components.forEach(part => recipe.itemOutputs(part[1] + 'x gtceu:' + part[0] + '_dust'));
        if (rock.bonus) recipe.chancedOutput('gtceu:tiny_' + rock.bonus[0] + '_dust', rock.bonus[1], 0);
    });
});
