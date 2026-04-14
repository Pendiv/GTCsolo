// conductorSuper.js (minimal)
// - KubeJS startup scripts 向け
// - 必要機能だけ: material作成 + flags + components + blastTemp + cableProperties

(function () {
  if (global.conductorSuper) return; // 既に定義済みなら何もしない

  /**
   * @param {Internal.MaterialRegistryEvent} event
   * @param {string} name
   * @param {string[]|null} components
   * @param {number} color
   * @param {*} iconSet
   * @param {any[]|null} blastParams
   * @param {any[]|null} cableParams
   * @param {any[]|null} flagsArr
   * @returns {*}
   */
  global.conductorSuper = function (
    event,
    name,
    components,
    color,
    iconSet,
    blastParams,
    cableParams,
    flagsArr
  ) {
    if (!name || typeof name !== "string") return null;

    var m = event.create(name)
      .ingot()
      .color(color)
      .iconSet(iconSet);

    // flags
    if (flagsArr && flagsArr.length) {
      m.flags.apply(m, flagsArr);
    }

    // components
    if (components && components.length) {
      // 環境差の吸収: components(array) が通らない場合に備え apply も試す
      try {
        m.components(components);
      } catch (e) {
        m.components.apply(m, components);
      }
    }

    // blast temp
    if (blastParams && blastParams.length) {
      m.blastTemp.apply(m, blastParams);
    }

    // cable properties
    if (cableParams && cableParams.length) {
      m.cableProperties.apply(m, cableParams);
    }

    return m;
  };
})();