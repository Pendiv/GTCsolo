(function () {
    const components = [
        ['beam_pipe', 'Beam Pipe', true],
        ['quadrupole_magnet', 'Quadrupole Magnet', true],
        ['rf_cavity', 'Radio-Frequency Cavity', true],
        ['detector_module', 'Detector Module', false],
        ['controller', 'Particle Accelerator Control Panel', false]
    ];

    StartupEvents.registry('block', event => {
        components.forEach(([id, name, through]) => {
            const texture = 'gtcsolo:block/particle_accelerator/' + id;
            const block = event.create('gtcsolo:particle_accelerator_' + id, 'cardinal')
                .displayName(name)
                .hardness(5).resistance(6).soundType('metal')
                .requiresTool().noValidSpawns(true)
                .tagBlock('minecraft:mineable/pickaxe')
                .tagBlock('gtceu:mineable/wrench')
                .textureAll(texture + '_side')
                .texture('front', texture + '_end');
            if (through) block.model(texture);
        });
    });

    StartupEvents.modifyCreativeTab('gtcsolo:gtcsolo_tab', event => {
        event.add(components.map(([id]) => 'gtcsolo:particle_accelerator_' + id));
    });
})();
