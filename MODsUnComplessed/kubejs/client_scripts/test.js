ItemEvents.tooltip(event => {
  event.addAdvanced('minecraft:diamond', (item, advanced, text) => {
    console.info('[TOOLTIP_TEST] class=' + text.getClass().getName())
    const methods = text.getClass().getMethods()
    for (let i = 0; i < methods.length; i++) {
      console.info('[TOOLTIP_TEST] ' + methods[i].getName())
    }
  })
})