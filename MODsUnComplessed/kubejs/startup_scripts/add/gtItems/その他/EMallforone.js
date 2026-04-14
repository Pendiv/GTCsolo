// priority: 10

(function() {
// ==========================================================
// 【究極一括追加リスト】
// 基本形式: ['内部ID', '元素記号', 色コード]
// --- 追加オプション（順番は自由） ---
// 1, 'Tier', '256A' : ケーブル/ワイヤーを追加（例: 1, 'UIV', '256A'）
//                     アンペア省略時は 32A
// 2, ['成分...']     : 合金として登録（例: 2, ['3x skystone', '1x titanium']）
// 3                  : 鉱石を追加
// 4                  : プラズマを追加
// 5, 'B|D|S|M'       : IconSet指定（Bright / Dull / Shiny / Metallic）
// '+' , 陽子, 中性子 : 陽子数と中性子数をカスタマイズ
// ==========================================================
// ['stellarium', 'St', 0x9, 1, 'UXV','256A'],

var MASTER_MATERIALS = [
  // 4は未知のバグが起きるためしようしないことを推奨
['refined_glowstone', 'OsGl', 0xE6C76A, 1,'MV','4A','4'], 
['prosperity', 'Ps', 0x5FB8D6], 
['refined_obsidian', 'OsMgFeSi2O4', 0xC58BFF, 1,'EV','16A'], 
['better_gold', '⇑Au', 0xFFC300],
 ['pluslitherite', '⇑NrLi3(C2H3ClW)', 0x9FA3A7, 3], 
['singularity', '◇', 0x2E0854,],
  ['skystone_titanium', 'StTi', 0xC8A2FF, 2, ['3x skystone', '1x titanium'], '+', 100, 150],
  ['singularity_tritanium', '◇Tr', 0xB35A6B],
  ['flaurite','Fa',0xB35A6B,3],
  ['exporonium', 'ExPr3', 0xADFF2F, 3, '+', 169, 119],
  ['endnium', 'θEn', 0x050007,3,1,'HV'],
 ['hafhnium', 'Hf', 0x9FA3A7, 3], 
  ['enigma', 'Eg', 0xA3182F, 3], 
  ['hafhnium_diboride', 'HfB2', 0x5E6064, 2, ['1x hafhnium', '2x boron']], 
  ['hafhnium_carbide', 'HfC', 0xB8A07E, 2, ['1x hafhnium', '1x carbon']],
    ['stellarium', 'St', 0x9FA3A7, 3], 
    ['stellarium_enigma', 'St3Eg2', 0x9FA3A7, 3,2,['3x stellarium','2x enigma']], 
  ['zirconium_diboride', 'ZrB2', 0xD1D1D1, 2, ['1x zirconium', '2x boron']],
 ['kover', 'FeNiCo', 0x33E0E0,2, ['1x iron', '1x nickel', '1x cobalt']],
  ['ultra_alloy', 'Ux', 0xFF00FF,2, ['1x gold', '1x silver', '1x copper', '1x tin']],
  ['singularity_bismath', '◇Bi', 0x7FC8CF],
  ['fractal', '≡F', 0x123456,3],
   ['time_mory', 'T=F', 0x32FF21],
   ['fractaline', '-F', 0x123456,1,'UXV','1111111A'],
  ['singularity_iron', '◇Fe', 0xE0E0E0],
  ['singularity_naquadah', '◇Nq', 0x4A4A4A],
  ['singularity_silver', '◇Ag', 0xE6E6E6],
  ['singularity_tungsten', '◇W', 0x3A3A38],
  ['singularity_osmium', '◇Os', 0x5A8FD6],
  ['singularity_samarium', '◇Sm', 0xB7C98A],
  ['singularity_gold', '◇Au', 0xF9C834],
  ['singularity_diamond', '◇H', 0x33E0E0],
['jupitate','Ju',0x50C878,3,1,'UEV','1024A'],
['hyperx_neutronium','Hy≡χNu',0x8B8589,1,'UXV','524888A',3],
['stagnanted_neutronium','Nu??',0xDFBFDF ,3],
['valinium','Vl',0x8F8F8F ,3,],
  ['originalium', 'Or', 0x505050,3,1,'UV','128A'],
  ['bedrockium','◇Br',0x000000,3],

  ['mithril', 'ΦMit', 0xBFC9D9],
  ['nocturnium', 'ΦNoc', 0x1C2238],
  ['etherium', 'ΦEth', 0xD7E6F5],
  ['nebulite', 'ΦNeb', 0x7A6FA8],
  ['orichalcum', 'ΦOri', 0xC88A3D],
  ['axiom_steel', 'FeΦAxi', 0x6E7684],
  ['mialineum', 'ΦMia', 0xE7F6FF],
  ['nethera_mialineum', 'Nr+ΦMia', 0xB85A73],
  ['auroralium', 'ΦAur', 0x69D4C3],
  ['velzenium', 'ΦVel', 0x6A1B3A],
  ['adamantite', 'ΦAdm', 0x7E8A96],
  ['viliria_steel', 'FeΦVil', 0x4A4A4F],
  ['dilithium', 'ΦDil', 0x74A7FF],
  ['vesker', 'ΦVes', 0x8A8F99],
  ['refined_netherite', 'Nr+', 0x746772],
  ['harmonium', 'ΦHrm', 0xF1D36B],
  ['urumetal', 'ΦUru', 0xD7B45A],
  ['he_netherite', 'Φ', 0x4D434A],

  ['he_friultail','AuAlBiHfRu<HE>',0x8FC9C7],
['he_bedrockium','◇BdNr+WNu<HE>',0xAFAFAF ,1,'UHV','32768A',3,2,['3x bedrockium','3x netherite','3x tungsten','1x neutronium']],












['monel','CuNi2',0xFFE4E1,1,'MV',2,['1x copper','2x nickel']]
];

var COMMON_FLAGS = [
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
  GTMaterialFlags.DISABLE_DECOMPOSITION
];

var WIRE_FLAGS_EXTRA = [GTMaterialFlags.GENERATE_FINE_WIRE];

var VALID_TIERS = {
  ulv: true, lv: true, mv: true, hv: true, ev: true, iv: true,
  luv: true, zpm: true, uv: true, uhv: true, uev: true, uiv: true,
  uxv: true, opv: true, max: true
};

function hasTier(v, va, tier) {
  return !!(v && va &&
    Object.prototype.hasOwnProperty.call(v, tier) &&
    Object.prototype.hasOwnProperty.call(va, tier));
}

function normalizeComponent(s) {
  s = String(s).trim();
  var m = s.match(/^(\d+)\s*x\s+(.+)$/i);
  if (m) {
    var amt = m[1];
    var matId = m[2].trim();
    if (!matId.includes(':')) matId = 'gtceu:' + matId;
    return amt + 'x ' + matId;
  }
  if (!s.includes(':')) s = 'gtceu:' + s;
  return s;
}

function parseAmperage(val) {
  if (val === null || val === undefined) return 32;

  if (typeof val === 'number' && !isNaN(val)) {
    return val > 0 ? Math.floor(val) : 32;
  }

  var s = String(val).trim().toUpperCase();
  var m = s.match(/^(\d+)\s*A?$/);
  if (m) {
    var n = parseInt(m[1], 10);
    return n > 0 ? n : 32;
  }

  return 32;
}

function isOptionMarker(val) {
  return val === 1 || val === 2 || val === 3 || val === 4 || val === 5 || val === '+';
}

function resolveIconSet(code) {
  var c = (code === null || code === undefined) ? 'M' : String(code).trim().toUpperCase();

  if (c === 'B') return GTMaterialIconSet.BRIGHT;
  if (c === 'D') return GTMaterialIconSet.DULL;
  if (c === 'S') return GTMaterialIconSet.SHINY;
  return GTMaterialIconSet.METALLIC;
}

function parseRow(row) {
  var parsed = {
    id: row[0],
    symbol: row[1],
    color: row[2],

    isWire: false,
    tier: null,
    amperage: 32,

    isAlloy: false,
    components: null,

    isOre: false,
    hasPlasma: false,

    iconCode: 'M',
    iconSet: GTMaterialIconSet.METALLIC,

    p: 110,
    n: 160
  };

  for (var i = 3; i < row.length; i++) {
    var val = row[i];

    if (val === 1) {
      parsed.isWire = true;
      parsed.tier = String(row[i + 1]).replace(/^\s+|\s+$/g, '').toLowerCase();
      i++;

      // 次の値がオプション記号でなければアンペア値として読む
      if (i + 1 < row.length && !isOptionMarker(row[i + 1])) {
        parsed.amperage = parseAmperage(row[i + 1]);
        i++;
      } else {
        parsed.amperage = 32;
      }
    }
    else if (val === 2) {
      parsed.isAlloy = true;
      parsed.components = row[i + 1];
      i++;
    }
    else if (val === 3) {
      parsed.isOre = true;
    }
    else if (val === 4) {
      parsed.hasPlasma = true;
    }
    else if (val === 5) {
      parsed.iconCode = (row[i + 1] === null || row[i + 1] === undefined) ? 'M' : String(row[i + 1]).trim().toUpperCase();
      parsed.iconSet = resolveIconSet(parsed.iconCode);
      i++;
    }
    else if (val === '+') {
      parsed.p = row[i + 1];
      parsed.n = row[i + 2];
      i += 2;
    }
  }

  return parsed;
}

// Element登録
GTCEuStartupEvents.registry('gtceu:element', function (event) {
  for (var i = 0; i < MASTER_MATERIALS.length; i++) {
    var parsed = parseRow(MASTER_MATERIALS[i]);

    if (!parsed.isAlloy) {
      event.create('e_' + parsed.id)
        .protons(parsed.p)
        .neutrons(parsed.n)
        .symbol(parsed.symbol);
    }
  }
});

// Material登録
GTCEuStartupEvents.registry('gtceu:material', function (event) {
  var v = global.v;
  var va = global.va;
  var conductorSuper = global.conductorSuper;

  for (var i = 0; i < MASTER_MATERIALS.length; i++) {
    var parsed = parseRow(MASTER_MATERIALS[i]);
    var matBuilder = null;
    var formattedComponents = null;

    if (parsed.isAlloy && parsed.components) {
      formattedComponents = parsed.components.map(normalizeComponent);
    }

    if (
      parsed.isWire &&
      VALID_TIERS[parsed.tier] &&
      typeof conductorSuper === 'function' &&
      hasTier(v, va, parsed.tier)
    ) {
      var wireFlags = COMMON_FLAGS.concat(WIRE_FLAGS_EXTRA);

      matBuilder = conductorSuper(
        event,
        parsed.id,
        formattedComponents,
        parsed.color,
        parsed.iconSet,
        [21600, 'highest', va[parsed.tier], 4000],
        [v[parsed.tier], parsed.amperage, 0, true],
        wireFlags
      );
    }
    else {
      matBuilder = event.create(parsed.id)
        .color(parsed.color)
        .iconSet(parsed.iconSet)
        .ingot();

      if (parsed.hasPlasma) {
        matBuilder.plasma();
      }

      matBuilder.flags.apply(matBuilder, COMMON_FLAGS);
    }

    if (matBuilder) {
      matBuilder.dust().fluid();

      if (parsed.isOre) {
        matBuilder.ore(1, 1);
      }

      if (parsed.isAlloy) {
        if (!parsed.isWire && formattedComponents) {
          matBuilder.components(formattedComponents);
        }
      } else {
        matBuilder.element('e_' + parsed.id);
      }

      // ワイヤー材でも 4 があれば plasma を追加
      if (parsed.isWire && parsed.hasPlasma) {
        matBuilder.plasma();
      }
    }
  }
});
})();
