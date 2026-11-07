(function () {
  StartupEvents.registry("fluid", event => {
    event.create("kubejs:overheat_magma")
      .thickTexture(0xC73838)
      .color(0xC73838)
      .bucketColor(0xC73838)
      .temperature(3300)
      .density(3000)
      .viscosity(6000)
      .luminosity(12);
  });
})();
