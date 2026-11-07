// priority: 100
// Existing Mek TileEntityInductionCell/Provider only. No mixins, no ID -> capacity table.
(function () {
    var functional = global.mekGtModes.induction === 'gt';
    var Base = Java.loadClass('com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine');
    var Cell = Java.loadClass('mekanism.common.tile.multiblock.TileEntityInductionCell');
    var Provider = Java.loadClass('mekanism.common.tile.multiblock.TileEntityInductionProvider');
    var FL = Java.loadClass('mekanism.api.math.FloatingLong');
    var EU = Java.loadClass('com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability');
    var IO = Java.loadClass('com.gregtechceu.gtceu.api.capability.recipe.IO');
    var List = Java.loadClass('java.util.ArrayList');
    var EnergyContainer = Java.loadClass('com.gregtechceu.gtceu.api.capability.IEnergyContainer');
    var Decimal = Java.loadClass('java.math.BigDecimal');
    var Component = Java.loadClass('net.minecraft.network.chat.Component');
    var RD = Java.loadClass('com.gregtechceu.gtceu.api.pattern.util.RelativeDirection');
    var MF = Java.loadClass('com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction');
    // Work in bounded exact integer chunks at the JS/Java boundary. Capacity stays FloatingLong.
    var MAX_EU_OPERATION = 900719925474099;
    function joules(eu) { return FL.parseFloatingLong(String(eu)).multiply(10); }
    function euAmount(j) {
        var value = Math.floor(Math.min(MAX_EU_OPERATION, j.divide(10).doubleValue()));
        // Avoid rounding a large fractional value upward across a whole EU boundary.
        return value > 0 && joules(value).greaterThan(j) ? value - 1 : value;
    }
    function collect(state, type, key) {
        var tile = state.getTileEntity();
        if (!(tile instanceof type)) return false;
        state.getMatchContext().getOrCreate(key, function () { return new List(); }).add(tile);
        return true;
    }
    function partPredicate(type, key, preview) {
        return Predicates.custom(function (state) { return collect(state, type, key); },
            global.mekGtPreviewBlocks(preview).common.get(0).candidates);
    }
    function factory(holder) {
        var subscription = null;
        var inputs = [], outputs = [], cells = null, providers = null;
        function clear() { inputs = []; outputs = []; cells = null; providers = null; }
        function move(container, budget, charging) {
            var remaining = budget;
            for (var i = 0; cells && i < cells.size() && remaining > 0; i++) {
                // An empty input/full output cannot move energy; do not scan all 3840 cells.
                var hatch = charging ? container.getEnergyStored() : container.getEnergyCanBeInserted();
                if (hatch <= 0) break;
                var cell = cells.get(i);
                if (cell.isRemoved()) continue;
                var store = cell.getEnergyContainer();
                var room = charging ? store.getMaxEnergy().subtract(store.getEnergy()) : store.getEnergy();
                var amount = Math.floor(Math.min(remaining, euAmount(room), hatch));
                if (amount <= 0) continue;
                // changeEnergy returns the actual change; alter the cell by exactly that amount.
                var actual = Math.abs(container.changeEnergy(charging ? -amount : amount));
                if (actual <= 0) continue;
                var delta = joules(actual);
                store.setEnergy(charging ? store.getEnergy().add(delta) : store.getEnergy().subtract(delta));
                cell.setChanged();
                remaining -= actual;
            }
            return budget - remaining;
        }
        return new JavaAdapter(Base, {
            onLoad: function () {
                this.super$onLoad();
                var machine = this;
                if (functional && !this.getLevel().isClientSide) subscription = this.subscribeServerTick(function () {
                    if (!machine.isFormed() || !machine.isWorkingEnabled() || !providers) return;
                    var transfer = FL.ZERO.copy();
                    for (var p = 0; p < providers.size(); p++) {
                        var provider = providers.get(p);
                        if (!provider.isRemoved()) transfer = transfer.add(provider.tier.getOutput());
                    }
                    var budget = euAmount(transfer), remaining = budget;
                    for (var i = 0; i < inputs.length && remaining > 0; i++) remaining -= move(inputs[i], remaining, true);
                    remaining = budget;
                    for (var o = 0; o < outputs.length && remaining > 0; o++) remaining -= move(outputs[o], remaining, false);
                });
            },
            onUnload: function () {
                if (subscription) { subscription.unsubscribe(); subscription = null; }
                clear();
                this.super$onUnload();
            },
            onStructureFormed: function () {
                this.super$onStructureFormed();
                clear();
                if (!functional) return;
                cells = this.getMultiblockState().getMatchContext().get('mek_gt_cells');
                providers = this.getMultiblockState().getMatchContext().get('mek_gt_providers');
                this.getParts().forEach(function (part) {
                    part.getRecipeHandlers().forEach(function (handler) {
                        if (handler instanceof EnergyContainer && handler.getCapability().equals(EU.CAP)) {
                            if (handler.getHandlerIO().equals(IO.IN)) inputs.push(handler);
                            else if (handler.getHandlerIO().equals(IO.OUT)) outputs.push(handler);
                        }
                    });
                });
            },
            onStructureInvalid: function () { clear(); this.super$onStructureInvalid(); },
            addDisplayText: function (lines) {
                this.super$addDisplayText(lines);
                if (!functional) { lines.add(Component.literal('Display only')); return; }
                // Only query live server cell state; do not invent unsynchronized client values.
                if (!cells) return;
                var stored = Decimal.ZERO, capacity = Decimal.ZERO;
                for (var i = 0; i < cells.size(); i++) {
                    var store = cells.get(i).getEnergyContainer();
                    stored = stored.add(new Decimal(store.getEnergy().toString()));
                    capacity = capacity.add(new Decimal(store.getMaxEnergy().toString()));
                }
                lines.add(Component.literal(stored.divide(Decimal.TEN).toPlainString() + ' / ' + capacity.divide(Decimal.TEN).toPlainString() + ' EU'));
            }
        // Rhino-generated adapters do not preserve Java's varargs constructor flag.
        // WorkableElectricMultiblockMachine(holder, Object...) needs an explicit array here.
        }, holder, []);
    }
    GTCEuStartupEvents.registry('gtceu:machine', function (event) {
        var builder = event.create('mek_gt_induction_matrix', 'multiblock')
            .machine(factory).rotationState(RotationState.NON_Y_AXIS).recipeType('dummy')
            .recipeModifier(function () { return MF.NULL; })
            .appearanceBlock(function () { return Block.getBlock('mekanism:induction_casing'); })
            .pattern(function (definition) {
                var face = global.mekGtPreviewBlocks('mekanism:structural_glass').or(global.mekGtPreviewBlocks('mekanism:induction_casing'));
                if (functional) face = face.or(Predicates.abilities(PartAbility.INPUT_ENERGY).setMinGlobalLimited(1))
                    .or(Predicates.abilities(PartAbility.OUTPUT_ENERGY).setMinGlobalLimited(1));
                else face = face.or(global.mekGtPreviewBlocks('mekanism:induction_port').setMinGlobalLimited(2));
                var pattern = FactoryBlockPattern.start(RD.RIGHT, RD.UP, RD.BACK);
                for (var z = 0; z < 18; z++) {
                    var rows = [];
                    for (var y = 0; y < 18; y++) {
                        var row = '';
                        for (var x = 0; x < 18; x++) {
                            var edge = Number(x === 0 || x === 17) + Number(y === 0 || y === 17) + Number(z === 0 || z === 17);
                            row += x === 8 && y === 1 && z === 0 ? 'C' : edge >= 2 ? 'F' : edge ? 'W' : y === 16 ? 'P' : 'E';
                        }
                        rows.push(row);
                    }
                    pattern.aisle(rows);
                }
                return pattern.where('C', Predicates.controller(Predicates.blocks(definition.getBlock())))
                    .where('F', global.mekGtPreviewBlocks('mekanism:induction_casing')).where('W', face)
                    .where('E', partPredicate(Cell, 'mek_gt_cells', 'mekanism:ultimate_induction_cell'))
                    .where('P', partPredicate(Provider, 'mek_gt_providers', 'mekanism:ultimate_induction_provider')).build();
            });
        global.mekGtApplyModel(builder, 'mekanism:block/induction_casing', 'gtceu:block/multiblock/power_substation');
    });
})();
