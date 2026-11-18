(function () {
    // BEGIN GENERATED SHAPE
    const aisles = [
        ["                                      ", "                                      ", "                                      ", "                                      ", "                             O        ", "                                      ", "                                      ", "                                      ", "                                      ", "                                      ", "                      I I   I I       ", "                                      ", "                                      ", "                                      "],
        ["                                      ", "                                      ", "                                      ", "                                      ", "                             Y        ", "                                      ", "                                      ", "                     D        D       ", "                                      ", "                                      ", "                      U U   U U       ", "                                      ", "                                      ", "                                      "],
        ["                                      ", "                                      ", "    SSS                               ", "    JKW                               ", "                     D       UD       ", "                     D  SSSSS D       ", "                     D DGGGGGDD       ", "                    DDBDGGGGGDDDD     ", "                     D DGGGGGDD       ", "                     D  SSSSS D       ", "                     DU U   U U       ", "                                      ", "                                      ", "                                      "],
        ["                      DD     DD       ", "  DDDDDD              DD     DD       ", "  DDSSSD              DD     DD       ", "    SSS              D        D  UUUU ", "                    DDBBBBBBBUDDDU  U ", " EDE                DDBBBBBBBBBDDU  U ", "                    DDBBBBBBBBBDDU  U ", " D                  DDBBBBBBBBBDDT    ", "                    DDBBBBBBBBBDDT    ", "                    DDBBBBBBBBBDD     ", "                    DDUBUBBBUBUDD     ", "   A                 D        D       ", "                                      ", "                                      "],
        ["  DD    DD   DD  DD   DD     DD       ", "  DDDDDDDD   DD  DD   DD     DD       ", "  DDDDDD             DFVVVVVVFD       ", "                    DDBBBBBBBBDDD   U ", "                    DDMMMMMMMMMDD   U ", " DDD                DD         DD   U ", " D                  DD         DDT  U ", "SDSSH  QQ  QQ  QQ HDBD         DBTDDDS", " D                  DD         DDT    ", " D                  DD         DD     ", "                    DDBBBBBBBBBDD     ", "   U                DDBBBBBBBBDDD     ", "                     D        D       ", "                                      "],
        ["  DD    DD   DD  DD   DD     DD   DDD ", "FFDDFFFVVVVVVVVVVVDF  DD     DD   DDD ", "        V    V   V   DDD     DD   DDD ", "        V    V   V  DDBBBBBBBBBDD DDD ", " D      V    V   VVVDDMMMMMMMMMDD DDD ", "SDDDH  QQ  QQ  QQ VDB  MMMHMMM  BDDDDS", "SDSSH  QQ  QQ  QQ VDB           BDDDDS", "SDSSH  QC  QC  QC VNB           BDXXXS", "SDSSH  QQ  QQ  QQ DDB           BDDDDS", "SDSSH  QQ  QQ  QQ DDB           BDDDDS", " DDDDD              DD         DD     ", "  MMMM              DDBBBBBBBBBDD     ", "  MMMM               D        D       ", "                                      "],
        ["  DD    DD   DD  DD   DD     DD   DDD ", "  DD    DD   DD  DD   DD     DD   DDD ", "  DD    DD   DD  DD  DDD     DD   DDD ", "  DD    DD   DD  DD DDBBBBBBBBBDD DDD ", " DDD    VD   VD  VD DDMMMMMMMMMDD DDD ", "SDSSH  QQ  QQ  QQ DDB  MMHHHMM  BDDDDS", "SSSS              DNB           BDXXXX", "SSSSPPPPPPPPPPPPPPDNB           BDXXXX", "SSSS              DNB           BDXXXX", "SDSSH  QQ  QQ  QQ DDB           BDDDDS", " DDDDD              DD         DD     ", "  MMMM              DDBBBBBBBBBDD     ", "  MMMM               D   HHH  D       ", "                         HHH          "],
        ["  DD    DD   DD  DD   DD     DD   DDD ", "  DD    DD   DD  DD  DDD     DD   DDD ", "  DD    DD   DD  DD DDBBBBBBBBDDD DDD ", " DDD    DD   DD  DD DDBBBBBBBBBDD DDD ", "SDSSH  QVD QQVDQQVDDBDMMMMMMMMMDBDDDDS", "SDSSH  QC  QC  QC DNB  MMHHHMM  BDXXXS", "SSSSPPPPPPPPPPPPPPDNB     H     BDXXXX", "SSSSPPPPPPPPPPPPPPPPP     N     PDXXXX", "SSSSPPPPPPPPPPPPPPDNB     H     BDXXXX", "SDUSH  QC  QC  QC DNB     H     BDXXXS", "SDUDDD QQ  QQ  QQ DDBD    H    DBDDDDS", " DUMMM              DDBBBBHBBBBDD     ", "  MMMM              DDBBBHHHBBDDD     ", "                     D   HHH  D       "],
        ["  DD    DD   DD  DD   DD     DD   DDD ", "  DD    DD   DD  DD   DD     DD   DDD ", "  DD    DD   DD  DD  DDD     DD   DDD ", "  DD    DD   DD  DD DDBBBBBBBBBDD DDD ", " DDD    VD   VD  VD DDMMMMMMMMMDD DDD ", "SDSSH  QQ  QQ  QQ DDB  MMHHHMM  BDDDDS", "SSSS              DNB           BDXXXX", "SSSSPPPPPPPPPPPPPPDNB           BDXXXX", "SSSS              DNB           BDXXXX", "SDSSH  QQ  QQ  QQ DDB           BDDDDS", " D                  DD         DD     ", "                    DDBBBBBBBBBDD     ", "                     D   HHH  D       ", "                         HHH          "],
        ["  DD    DD   DD  DD   DD     DD   DDD ", "FFDDFFFVVVVVVVVVVVDF  DD     DD   DDD ", "        V    V   V   DDD     DD   DDD ", "        V    V   V  DDBBBBBBBBBDD DDD ", " D      V    V   VVVDDMMMMMMMMMDD DDD ", "SDSSH  QQ  QQ  QQ VDB  MMMHMMM  BDDDDS", "SDSSH  QQ  QQ  QQ VDB           BDDDDS", "SDSSH  QC  QC  QC VNB           BDXXXS", "SDSSH  QQ  QQ  QQ DDB           BDDDDS", "SDSSH  QQ  QQ  QQ DDB           BDDDDS", " D                  DD         DD     ", "                    DDBBBBBBBBBDD     ", "                     D        D       ", "                                      "],
        ["  DD    DD   DD  DD   DD     DD       ", "  DD    DD   DD  DD   DD     DD       ", "                     DFVVVVVVFD       ", "                    DDBBBBBBBBDDD     ", "                    DDMMMMMMMMMDD     ", " D                  DD         DD     ", " D                  DD         DD     ", "SDSSH  QQ  QQ  QQ HDBD         DBDDDDS", " D                  DD         DD     ", " D                  DD         DD     ", "                    DDBBBBBBBBBDD     ", "                    DDBBBBBBBBDDD     ", "                     D        D       ", "                                      "],
        ["                      DD     DD       ", "                      DD     DD       ", "                      DD     DD       ", "                     D        D       ", "                    DDBBBBBBBBDDD     ", "                    DDBBBBBBBBBDD     ", "                    DDBBBBBBBBBDD     ", " D                  DDBBBBBBBBBDD     ", "                    DDBBBBBBBBBDD     ", "                    DDBBBBBBBBBDD     ", "                    DDUBUBBBUBUDD     ", "                     D        D       ", "                                      ", "                                      "],
        ["                                      ", "                                      ", "                                      ", "                                      ", "                     D        D       ", "                     D        D       ", "                     D        D       ", "                    DDBBBBBBBBDDD     ", "                     D        D       ", "                     D        D       ", "                     DU U   U U       ", "                                      ", "                                      ", "                                      "],
        ["                                      ", "                                      ", "                                      ", "                                      ", "                                      ", "                                      ", "                                      ", "                     D        D       ", "                                      ", "                                      ", "                      U U   U U       ", "                                      ", "                                      ", "                                      "],
        ["                                      ", "                                      ", "                                      ", "                                      ", "                                      ", "                                      ", "                                      ", "                                      ", "                                      ", "                                      ", "                      I I   I I       ", "                                      ", "                                      ", "                                      "]
    ];
    const blocks = {"D": "gtcsolo:ma_shielding_casing", "F": "gtceu:steel_frame", "S": "gtceu:solid_machine_casing", "P": "gtcsolo:particle_accelerator_beam_pipe", "Q": "gtcsolo:particle_accelerator_quadrupole_magnet", "C": "gtcsolo:particle_accelerator_rf_cavity", "N": "gtcsolo:particle_accelerator_detector_module", "T": "gtcsolo:particle_accelerator_detector_module", "U": "gtcsolo:stainless_steel_pipe_casing", "B": "gtcsolo:delinoy_pressure_casing", "M": "gtcsolo:sus316l_machine_casing", "H": "gtcsolo:delinoy_gearbox", "G": "gtceu:laminated_glass", "X": "gtceu:raw_graphite_block", "V": "gtcsolo:entronium_frame"};
    // END GENERATED SHAPE

    const casing = 'gtcsolo:ma_shielding_casing';

    GTCEuStartupEvents.registry('gtceu:recipe_type', event => {
        event.create('ma_irradiation')
            .category('multiblock')
            .setMaxIOSize(2, 1, 8, 1)
            .setEUIO('in')
            .setProgressBar(GuiTextures.PROGRESS_BAR_FUSION, FillDirection.LEFT_TO_RIGHT);
    });

    GTCEuStartupEvents.registry('gtceu:machine', event => {
        const Direction = Java.loadClass('com.gregtechceu.gtceu.api.pattern.util.RelativeDirection');
        event.create('ma_irradiator', 'multiblock')
            .rotationState(RotationState.NON_Y_AXIS)
            .allowExtendedFacing(false)
            .allowFlip(false)
            .recipeType('ma_irradiation')
            .recipeModifiers(true, [GTRecipeModifiers.DEFAULT_ENVIRONMENT_REQUIREMENT, GTRecipeModifiers.OC_NON_PERFECT])
            .appearanceBlock(() => Block.getBlock(casing))
            .pattern(definition => {
                const energy = Predicates.abilities(PartAbility.INPUT_ENERGY)
                    .setMinGlobalLimited(1).setPreviewCount(2);
                const maintenance = Predicates['autoAbilities(boolean,boolean,boolean)'](true, false, false);
                const elements = Predicates.abilities(PartAbility.IMPORT_FLUIDS)
                    .setMinGlobalLimited(1).setPreviewCount(8);
                let pattern = FactoryBlockPattern.start(Direction.RIGHT, Direction.UP, Direction.BACK);
                aisles.forEach(rows => { pattern = pattern.aisle(rows); });
                pattern = pattern
                    .where(' ', Predicates.any())
                    .where('K', Predicates.controller(Predicates.blocks(definition.getBlock())))
                    .where('E', Predicates.blocks(casing).or(energy))
                    .where('A', Predicates.abilities(PartAbility.IMPORT_FLUIDS))
                    .where('I', Predicates.blocks(blocks.U).or(elements))
                    .where('J', Predicates.abilities(PartAbility.IMPORT_ITEMS))
                    .where('W', Predicates.blocks(blocks.S).or(maintenance))
                    .where('Y', Predicates.blocks(blocks.U)
                        .or(Predicates.abilities(PartAbility.EXPORT_ITEMS).setPreviewCount(1)))
                    .where('O', Predicates.abilities(PartAbility.EXPORT_FLUIDS));
                Object.keys(blocks).forEach(symbol => {
                    pattern = pattern.where(symbol, Predicates.blocks(blocks[symbol]));
                });
                return pattern.build();
            })
            .workableCasingRenderer('gtcsolo:block/ma_irradiator/shielding_casing',
                'gtcsolo:block/multiblock/ma_irradiator', false);
    });
})();
