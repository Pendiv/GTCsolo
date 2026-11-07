// priority: 900

(function () {
  if (global.gtcsoloSuperconductors) return;

  const sizes = ['single', 'double', 'quadruple', 'octal', 'hex'];
  const tiers = [
    ['lv',  'gtcsolo', 'tariton'],
    ['mv',  'gtcsolo', 'energetic_alloy'],
    ['hv',  'gtcsolo', 'energized_steel'],
    ['ev',  'gtcsolo', 'refined_obsidian'],
    ['iv',  'gtcsolo', 'oblivion'],
    ['luv', 'gtcsolo', 'hssx'],
    ['zpm', 'gtcsolo', 'pure_naquadah'],
    ['uv',  'gtcsolo', 'originalium'],
    ['uhv', 'gtceu',   'ruthenium_trinium_americium_neutronate'],
    ['uev', 'gtcsolo', 'jupitate'],
    ['uiv', 'gtcsolo', 'aurum_stellis_gold'],
    ['uxv', 'gtcsolo', 'hyperx_neutronium'],
    ['opv', 'gtcsolo', 'fractaline'],
    ['max', 'gtcsolo', 'timeline']
  ];

  global.gtcsoloSuperconductors = tiers.map(entry => {
    const tier = entry[0];
    const namespace = entry[1];
    const material = entry[2];
    const wires = {};
    sizes.forEach(size => {
      wires[size] = `${namespace}:${material}_${size}_wire`;
    });
    return { tier: tier, material: `${namespace}:${material}`, wires: wires };
  });
})();
