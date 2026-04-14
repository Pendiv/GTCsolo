// priority: 10

GTCEuStartupEvents.registry('gtceu:material', event => {
//以下の形じゃないとクラッシュする。ほとんど例外なし。余計なことをしてクラッシュ地獄の沼に逆戻りしないよう、将来への自分の戒め
  if (typeof global.conductorSuper !== 'function') {
    console.info('[austar] conductorSuper missing -> skip');
    return;
  }

  const v = global.v;
  const va = global.va;
//余計なことをするなクラッシュする
  global.conductorSuper(
    event, 
    'aurum_stellis_gold',
    null,               
    0xFFD700,
    GTMaterialIconSet.METALLIC,
    [21600, 'highest', va['uiv'], 4000],
    [v['uiv'], 128, 0, true],
    [
      GTMaterialFlags.GENERATE_PLATE,
      GTMaterialFlags.GENERATE_DENSE,
      GTMaterialFlags.GENERATE_ROD,
      GTMaterialFlags.GENERATE_LONG_ROD,
      GTMaterialFlags.GENERATE_BOLT_SCREW,
      GTMaterialFlags.GENERATE_RING,
      GTMaterialFlags.GENERATE_ROUND,
      GTMaterialFlags.GENERATE_GEAR,
      GTMaterialFlags.GENERATE_SMALL_GEAR,
      GTMaterialFlags.GENERATE_SPRING,
      GTMaterialFlags.GENERATE_FRAME,
      GTMaterialFlags.GENERATE_FINE_WIRE,
      GTMaterialFlags.DISABLE_DECOMPOSITION
    ]
  );
});
