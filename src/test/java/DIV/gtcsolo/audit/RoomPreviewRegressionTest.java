package DIV.gtcsolo.audit;

import DIV.gtcsolo.api.tier.TieredBlockSet;
import DIV.gtcsolo.api.tier.TieredMultiblockRegistry;
import DIV.gtcsolo.machine.RoomStructures;
import DIV.gtcsolo.registry.ResourceRoomMachines;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import java.util.ArrayList;
import java.util.List;

public final class RoomPreviewRegressionTest {
    public static void run() {
        for (boolean construction : new boolean[]{false, true}) {
            var structure = construction ? RoomStructures.CONSTRUCTION_ROOM : RoomStructures.FAMING_ROOM;
            var ports = ResourceRoomMachines.previewPorts(construction);
            assert ports.size() == 6;
            assert !ports.containsKey(structure.core());
            for (var cell : ports.keySet()) assert structure.symbolAt(cell) == 'A' : cell;
        }
        var definition = MultiblockMachineDefinition.createDefinition(new ResourceLocation("gtcsolo", "room_preview_test"));
        var grades = new ArrayList<>(ResourceRoomMachines.TIERS.tiers());
        TieredMultiblockRegistry.registerPreviewTiers(definition, grades);
        grades.clear();
        List<Integer> pages = TieredMultiblockRegistry.getPreviewTiers(definition);
        assert pages.size() == 13;
        for (int index = 0; index < pages.size(); index++) assert pages.get(index) == index + GTValues.LV;
        var legacy = MultiblockMachineDefinition.createDefinition(new ResourceLocation("gtcsolo", "legacy_preview_test"));
        TieredMultiblockRegistry.register(legacy, TieredBlockSet.builder("legacy_test")
                .tier(GTValues.HV, () -> Blocks.STONE).tier(GTValues.LV, () -> Blocks.DIRT).build());
        assert TieredMultiblockRegistry.getPreviewTiers(legacy).equals(List.of(GTValues.LV, GTValues.HV));
        System.out.println("Room preview regressions passed: A-only hatches, 13 ordered grades, defensive snapshot, legacy labels.");
    }
}
