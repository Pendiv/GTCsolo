ServerEvents.tags('item', event => {
  const entries = global.gtcsoloSuperconductors || [];

  entries.forEach(entry => {
    Object.keys(entry.wires).forEach(size => {
      const wire = entry.wires[size];
      event.add('gtcsolo:superconducting_wires', wire);
      event.add(`gtcsolo:superconducting_wires/${entry.tier}`, wire);
      event.add(`gtcsolo:superconducting_wires/${entry.tier}/${size}`, wire);
    });
  });
});
