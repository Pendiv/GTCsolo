package DIV.gtcsolo.machine.mega;

import DIV.gtcsolo.machine.IsmbfRules;
import DIV.gtcsolo.registry.ModRecipeTypes;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.*;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.common.data.GCYMRecipeTypes;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.utils.GTUtil;
import com.lowdragmc.lowdraglib.gui.texture.*;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import java.util.List;

public class MegaAlloyBlastFurnaceMachine extends CoilWorkableElectricMultiblockMachine {
    private static final ManagedFieldHolder FIELDS = new ManagedFieldHolder(
            MegaAlloyBlastFurnaceMachine.class, MANAGED_FIELD_HOLDER);
    private static final String USED_HEAT = "gtcsolo_mega_blast_used_heat";
    @Persisted @DescSynced private boolean maximumFire;
    @Persisted @DescSynced private int targetTemperature = 20000;
    @Persisted @DescSynced private double heat;
    @DescSynced private long lastHeatingEU;
    private TickableSubscription thermalSubscription;
    private long lastProductiveTick = Long.MIN_VALUE;

    public MegaAlloyBlastFurnaceMachine(IMachineBlockEntity holder) { super(holder); }
    @Override public ManagedFieldHolder getFieldHolder() { return FIELDS; }
    @Override protected RecipeLogic createRecipeLogic(Object... args) { return new ThermalLogic(this); }
    @Override public boolean keepSubscribing() { return true; }

    public int normalTemperature() { return MegaBlastRules.normalTemperature(getCoilType().getCoilTemperature()); }
    public int effectiveTemperature() {
        return maximumFire ? Math.max(normalTemperature(), (int) MegaBlastRules.sanitize(heat)) : normalTemperature();
    }
    private boolean jobPresent() { return recipeLogic.getLastRecipe() != null && recipeLogic.getMaxProgress() > 0; }

    @Override public void setActiveRecipeType(int type) {
        super.setActiveRecipeType(maximumFire ? Math.floorMod(type, getRecipeTypes().length) : 0);
    }

    public boolean canProcess(GTRecipe recipe) {
        if (!isRecipeLogicAvailable()) return false;
        boolean alloy = recipe.recipeType == GCYMRecipeTypes.ALLOY_BLAST_RECIPES;
        boolean extreme = recipe.recipeType == ModRecipeTypes.EXTREME_TEMPERATURE_BLAST_FURNACE;
        if (!alloy && (!maximumFire || (!extreme && recipe.recipeType != GTRecipeTypes.BLAST_RECIPES))) return false;
        int required = recipe.data.getInt("ebf_temp");
        if (required <= 0 || required > effectiveTemperature()) return false;
        // Extreme recipes retain their material's minimum physical-coil requirement.
        if (extreme && getCoilType().getCoilTemperature() < recipe.data.getInt(IsmbfRules.START_TEMPERATURE)) return false;
        return effectiveTemperature() >= recipe.data.getInt(USED_HEAT);
    }

    @Override public GTRecipe fullModifyRecipe(GTRecipe recipe) {
        if (!canProcess(recipe)) return null;
        GTRecipe detached = recipe.copy(ContentModifier.multiplier(1), false);
        detached.data = recipe.data.copy();
        return super.fullModifyRecipe(detached);
    }

    public static ModifierFunction batch(MetaMachine machine, GTRecipe recipe) {
        if (!(machine instanceof MegaAlloyBlastFurnaceMachine furnace) || !furnace.canProcess(recipe)
                || MegaMixerRecipeModifiers.safeMultiplier(recipe) < MegaBlastRules.BATCH) return ModifierFunction.NULL;
        return r -> MegaMixerRecipeModifiers.scaleBatch(r, MegaBlastRules.BATCH);
    }

    public static ModifierFunction temperatureOC(MetaMachine machine, GTRecipe recipe) {
        if (!(machine instanceof MegaAlloyBlastFurnaceMachine furnace) || !furnace.canProcess(recipe)) return ModifierFunction.NULL;
        long eut = RecipeHelper.getInputEUt(recipe), voltage = furnace.getOverclockVoltage();
        if (eut <= 0 || eut > voltage) return ModifierFunction.NULL;
        int tier = GTUtil.getTierByVoltage(eut);
        int ocs = Math.max(0, GTUtil.getOCTierByVoltage(voltage) - tier - (tier == GTValues.ULV ? 1 : 0));
        int limit = MegaMixerRecipeModifiers.availableParallels(furnace, recipe, MegaMixerRecipeModifiers.safeMultiplier(recipe));
        if (limit < 1) return ModifierFunction.NULL;
        int temperature = furnace.effectiveTemperature(), required = recipe.data.getInt("ebf_temp");
        var oc = OverclockingLogic.heatingCoilOC(new OverclockingLogic.OCParams(eut, recipe.duration, ocs, limit), voltage, required, temperature);
        double discount = OverclockingLogic.getCoilEUtDiscount(required, temperature);
        return source -> {
            GTRecipe result = oc.toModifier().apply(source.copy(ContentModifier.multiplier(1), false));
            result.data = source.data.copy();
            result.data.putInt(USED_HEAT, temperature);
            // At extreme temperatures the native exponential discount can underflow. Never make processing free.
            long power = Math.max(1L, (long) Math.ceil(eut * oc.eutMultiplier() * discount));
            EURecipeCapability.putEUContent(result.tickInputs, power);
            return result;
        };
    }

    @Override public void onLoad() {
        super.onLoad();
        heat = MegaBlastRules.sanitize(heat);
        targetTemperature = Math.max(1, targetTemperature);
        if (getLevel() instanceof ServerLevel) thermalSubscription = subscribeServerTick(thermalSubscription, this::thermalTick);
    }
    @Override public void onUnload() {
        if (thermalSubscription != null) { thermalSubscription.unsubscribe(); thermalSubscription = null; }
        super.onUnload();
    }
    @Override public boolean onWorking() {
        if (!super.onWorking()) return false;
        lastProductiveTick = getLevel().getGameTime();
        return true;
    }
    private void thermalTick() {
        double before = heat;
        lastHeatingEU = 0;
        long now = getLevel().getGameTime();
        // Count actual successful processing, not WAITING or the presence of a cached recipe.
        if (lastProductiveTick != now && lastProductiveTick != now - 1) heat = MegaBlastRules.cool(heat);
        if (maximumFire && isRecipeLogicAvailable() && isWorkingEnabled() && energyContainer != null) {
            heat = Math.max(normalTemperature(), heat);
            int target = Math.max(normalTemperature(), targetTemperature);
            long wanted = MegaBlastRules.heatingEU(heat, target, getCoilType().getEnergyDiscount());
            long paid = energyContainer.removeEnergy(Math.min(wanted, energyContainer.getEnergyStored()));
            heat = MegaBlastRules.heatAfterPayment(heat, target, getCoilType().getEnergyDiscount(), paid);
            lastHeatingEU = Math.max(0, paid);
        }
        if (heat != before) markDirty();
    }
    private void setMaximumFire(boolean value) {
        if (getLevel().isClientSide || jobPresent()) return;
        maximumFire = value;
        if (!value) setActiveRecipeType(0);
        recipeLogic.markLastRecipeDirty();
        markDirty();
    }
    private void setTarget(String value) {
        if (getLevel().isClientSide || jobPresent()) return;
        try { targetTemperature = Math.max(1, Integer.parseInt(value)); markDirty(); }
        catch (NumberFormatException ignored) { /* incomplete or forged input */ }
    }
    @Override public void addDisplayText(List<Component> text) {
        super.addDisplayText(text);
        text.add(Component.translatable("gtcsolo.mega_blast.heat", effectiveTemperature(), normalTemperature()));
        text.add(Component.translatable("gtcsolo.mega_blast.heating", lastHeatingEU));
        text.add(Component.translatable("gtcsolo.mega_blast.cost", (long) Math.ceil(MegaBlastRules.euPerK(getCoilType().getEnergyDiscount()))));
        if (jobPresent()) text.add(Component.translatable("gtcsolo.mega_blast.settings_locked"));
    }
    @Override public Widget createUIWidget() {
        var group = new WidgetGroup(0, 0, 190, 125);
        group.setBackground(GuiTextures.BACKGROUND_INVERSE);
        group.addWidget(new DraggableScrollableWidgetGroup(4, 4, 182, 72).setBackground(getScreenTexture())
                .addWidget(new ComponentPanelWidget(4, 3, this::addDisplayText)
                        .textSupplier(getLevel().isClientSide ? null : this::addDisplayText)
                        .setMaxWidthLimit(170).clickHandler(this::handleDisplayClick)));
        group.addWidget(new SwitchWidget(4, 78, 182, 18, (click, value) -> setMaximumFire(value))
                .setTexture(new GuiTextureGroup(ResourceBorderTexture.BUTTON_COMMON, new TextTexture("gtcsolo.mega_blast.normal")),
                        new GuiTextureGroup(ResourceBorderTexture.BUTTON_COMMON, new TextTexture("gtcsolo.mega_blast.maximum")))
                .setSupplier(() -> maximumFire));
        group.addWidget(new LabelWidget(5, 105, "gtcsolo.mega_blast.target"));
        group.addWidget(new TextFieldWidget(77, 102, 108, 16, () -> Integer.toString(targetTemperature), this::setTarget)
                .setNumbersOnly(1, Integer.MAX_VALUE).setMaxStringLength(10).setBackground(GuiTextures.DISPLAY));
        return group;
    }
    private static final class ThermalLogic extends RecipeLogic {
        private final MegaAlloyBlastFurnaceMachine furnace;
        ThermalLogic(MegaAlloyBlastFurnaceMachine machine) { super(machine); furnace = machine; }
        @Override public void handleRecipeWorking() {
            if (lastRecipe != null && !furnace.canProcess(lastRecipe)) {
                if (isWorking()) lastRecipe.postWorking(furnace);
                setWaiting(Component.translatable("gtcsolo.mega_blast.wait_heat"));
                return;
            }
            super.handleRecipeWorking();
        }
    }
}
