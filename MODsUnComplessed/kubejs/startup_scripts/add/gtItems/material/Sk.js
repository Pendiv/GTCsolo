// priority: 10

GTCEuStartupEvents.registry('gtceu:material', event => {
//以下の形じゃないとクラッシュする。形はくずすな
  if (typeof global.conductorSuper !== 'function') {
    console.info('[austar] conductorSuper missing -> skip');
    return;
  }

  const v = global.v;
  const va = global.va;
//余計なことをするなクラッシュする
  global.conductorSuper(
    event, 
    'skystone',
    null,               
    0x404040,
    GTMaterialIconSet.METALLIC,
    [21600, 'highest', va['iv'], 4000],
    [v['lv'], 128, 0, true],
    //たぶんこれが一番多いと思います
    [
      GTMaterialFlags.GENERATE_PLATE,
      GTMaterialFlags.GENERATE_DENSE,
      GTMaterialFlags.GENERATE_ROD,
      GTMaterialFlags.GENERATE_LONG_ROD,
      GTMaterialFlags.GENERATE_BOLT_SCREW,
      GTMaterialFlags.GENERATE_RING,
      GTMaterialFlags.GENERATE_ROUND,
      GTMaterialFlags.GENERATE_GEAR,
      GTMaterialFlags.GENERATE_ROTOR,
      GTMaterialFlags.GENERATE_SMALL_GEAR,
      GTMaterialFlags.GENERATE_SPRING,
      GTMaterialFlags.GENERATE_FRAME,
      GTMaterialFlags.GENERATE_FINE_WIRE,
      GTMaterialFlags.DISABLE_DECOMPOSITION
    ]
  );
});
