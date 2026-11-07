ServerEvents.recipes(event => {
    if (!Platform.isLoaded('fantasy_weapons')) return;
    event.remove({ mod: 'fantasy_weapons' })
    // Reject Cucumber tag recipes before KJS resolves outputs on worker threads.
    event.remove({ output: '@fantasy_weapons', not: { or: [
        { type: 'cucumber:shaped_tag' },
        { type: 'cucumber:shapeless_tag' }
    ] } })
})
