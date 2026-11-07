(function () {
  StartupEvents.registry("fluid", event => {
    event.create("kubejs:unrefined_ma_fluid")
      .thickTexture(0xFFFFFF)
      .color(0xFFFFFF)
      .bucketColor(0xFFFFFF)
      .temperature(1300)
      .density(3000)
      .viscosity(6000)
      .luminosity(12)
      .noBucket()
      .noBlock();
  });
})();
