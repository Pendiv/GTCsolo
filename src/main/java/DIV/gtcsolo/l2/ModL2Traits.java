package DIV.gtcsolo.l2;

import DIV.gtcsolo.Gtcsolo;
import DIV.gtcsolo.l2.trait.*;
import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import dev.xkmc.l2hostility.init.registrate.LHTraits;
import net.minecraft.ChatFormatting;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * gtcsolo 独自 L2Hostility MobTrait の登録 manager。
 *
 * <p>仕様: refs/claude/2026-05-17_L2_traits_spec.md (=29 trait 一覧、 確定版)
 * <p>本ファイルは 「簡単に作れる 10 個」 (= 基盤 2 + 単純系 8) を登録。 残り 19 は順次追加。
 *
 * <p>注: TraitConfig (cost/max_rank/min_level 等) は未指定 (= TraitConfig.DEFAULT)、
 * 配布対象 (entity_config) も未指定。 後日 datapack で調整。
 */
public class ModL2Traits {

    public static final DeferredRegister<MobTrait> TRAITS =
            DeferredRegister.create(LHTraits.TRAITS.key(), Gtcsolo.MODID);

    // === 基盤 2 (state 永続化テンプレ確定用) ===
    public static final RegistryObject<EndureTrait> ENDURE =
            TRAITS.register("endure", () -> new EndureTrait(ChatFormatting.GOLD));
    public static final RegistryObject<FirstMeetingTrait> FIRST_MEETING =
            TRAITS.register("first_meeting", () -> new FirstMeetingTrait(ChatFormatting.AQUA));

    // === 単純系 8 (state なし or 軽量) ===
    public static final RegistryObject<CutoffTrait> CUTOFF =
            TRAITS.register("cutoff", () -> new CutoffTrait(ChatFormatting.WHITE));
    public static final RegistryObject<WalkingAbyssTrait> WALKING_ABYSS =
            TRAITS.register("walking_abyss", () -> new WalkingAbyssTrait(ChatFormatting.DARK_GRAY));
    public static final RegistryObject<MagicalCreaturesTrait> MAGICAL_CREATURES =
            TRAITS.register("magical_creatures", () -> new MagicalCreaturesTrait(ChatFormatting.LIGHT_PURPLE));
    public static final RegistryObject<AngerTrait> ANGER =
            TRAITS.register("anger", () -> new AngerTrait(ChatFormatting.RED));
    public static final RegistryObject<DominationOverVictoryTrait> DOMINATION_OVER_VICTORY =
            TRAITS.register("domination_over_victory", () -> new DominationOverVictoryTrait(ChatFormatting.DARK_RED));
    public static final RegistryObject<WizardryTrait> WIZARDRY =
            TRAITS.register("wizardry", () -> new WizardryTrait(ChatFormatting.DARK_PURPLE));
    public static final RegistryObject<ExplosiveResonanceTrait> EXPLOSIVE_RESONANCE =
            TRAITS.register("explosive_resonance", () -> new ExplosiveResonanceTrait(ChatFormatting.DARK_RED));
    public static final RegistryObject<RebirthTrait> REBIRTH =
            TRAITS.register("rebirth", () -> new RebirthTrait(ChatFormatting.GREEN));

    // === 単純系 9 (mixin 不要、 batch 1) ===
    public static final RegistryObject<FactAdaptationTrait> FACT_ADAPTATION =
            TRAITS.register("fact_adaptation", () -> new FactAdaptationTrait(ChatFormatting.YELLOW));
    public static final RegistryObject<GroundBattleTrait> GROUND_BATTLE =
            TRAITS.register("ground_battle", () -> new GroundBattleTrait(ChatFormatting.DARK_GREEN));
    public static final RegistryObject<ParadiseLostTrait> PARADISE_LOST =
            TRAITS.register("paradise_lost", () -> new ParadiseLostTrait(ChatFormatting.DARK_AQUA));
    public static final RegistryObject<GroupPsychologyTrait> GROUP_PSYCHOLOGY =
            TRAITS.register("group_psychology", () -> new GroupPsychologyTrait(ChatFormatting.BLUE));
    public static final RegistryObject<InnocenceBattleTrait> INNOCENCE_BATTLE =
            TRAITS.register("innocence_battle", () -> new InnocenceBattleTrait(ChatFormatting.WHITE));
    public static final RegistryObject<LazinessTrait> LAZINESS =
            TRAITS.register("laziness", () -> new LazinessTrait(ChatFormatting.GRAY));
    public static final RegistryObject<SummoningRitualTrait> SUMMONING_RITUAL =
            TRAITS.register("summoning_ritual", () -> new SummoningRitualTrait(ChatFormatting.DARK_BLUE));
    public static final RegistryObject<IncompleteCombustionTrait> INCOMPLETE_COMBUSTION =
            TRAITS.register("incomplete_combustion", () -> new IncompleteCombustionTrait(ChatFormatting.GOLD));

    // === state 持ち 5 (event handler + mixin 一部) ===
    public static final RegistryObject<ArroganceTrait> ARROGANCE =
            TRAITS.register("arrogance", () -> new ArroganceTrait(ChatFormatting.LIGHT_PURPLE));
    public static final RegistryObject<LunaticCurseTrait> LUNATIC_CURSE =
            TRAITS.register("lunatic_curse", () -> new LunaticCurseTrait(ChatFormatting.DARK_PURPLE));
    public static final RegistryObject<GamblerTrait> GAMBLER =
            TRAITS.register("gambler", () -> new GamblerTrait(ChatFormatting.GOLD));
    public static final RegistryObject<EqualTrait> EQUAL =
            TRAITS.register("equal", () -> new EqualTrait(ChatFormatting.AQUA));
    public static final RegistryObject<RejectionUnknownTrait> REJECTION_UNKNOWN =
            TRAITS.register("rejection_unknown", () -> new RejectionUnknownTrait(ChatFormatting.WHITE));

    // === 複合系 5 (mixin / event handler フル活用) ===
    public static final RegistryObject<SoulDestructionTrait> SOUL_DESTRUCTION =
            TRAITS.register("soul_destruction", () -> new SoulDestructionTrait(ChatFormatting.DARK_RED));
    public static final RegistryObject<CooperativenessTrait> COOPERATIVENESS =
            TRAITS.register("cooperativeness", () -> new CooperativenessTrait(ChatFormatting.GREEN));
    public static final RegistryObject<PandemicTrait> PANDEMIC =
            TRAITS.register("pandemic", () -> new PandemicTrait(ChatFormatting.DARK_GREEN));
    public static final RegistryObject<BushidoSpiritTrait> BUSHIDO_SPIRIT =
            TRAITS.register("bushido_spirit", () -> new BushidoSpiritTrait(ChatFormatting.RED));
    public static final RegistryObject<ResentmentTrait> RESENTMENT =
            TRAITS.register("resentment", () -> new ResentmentTrait(ChatFormatting.DARK_GRAY));
    public static final RegistryObject<DragonicHeartTrait> DRAGONIC_HEART =
            TRAITS.register("dragonic_heart", () -> new DragonicHeartTrait(ChatFormatting.LIGHT_PURPLE));

    // === Phase 2: 単純系 5 個 ===
    public static final RegistryObject<IronLegsTrait> IRON_LEGS =
            TRAITS.register("iron_legs", () -> new IronLegsTrait(ChatFormatting.GRAY));
    public static final RegistryObject<AttentionSeekerTrait> ATTENTION_SEEKER =
            TRAITS.register("attention_seeker", () -> new AttentionSeekerTrait(ChatFormatting.YELLOW));
    public static final RegistryObject<PureHeartTrait> PURE_HEART =
            TRAITS.register("pure_heart", () -> new PureHeartTrait(ChatFormatting.WHITE));
    public static final RegistryObject<PreparedTrait> PREPARED =
            TRAITS.register("prepared", () -> new PreparedTrait(ChatFormatting.AQUA));
    public static final RegistryObject<KinCallTrait> KIN_CALL =
            TRAITS.register("kin_call", () -> new KinCallTrait(ChatFormatting.DARK_GREEN));

    // === Phase 3: 中規模 8 個 (state あり、 vanilla 介入なし) ===
    public static final RegistryObject<EctothermTrait> ECTOTHERM =
            TRAITS.register("ectotherm", () -> new EctothermTrait(ChatFormatting.BLUE));
    public static final RegistryObject<HighAltitudeTrait> HIGH_ALTITUDE =
            TRAITS.register("high_altitude", () -> new HighAltitudeTrait(ChatFormatting.RED));
    public static final RegistryObject<DefianceTrait> DEFIANCE =
            TRAITS.register("defiance", () -> new DefianceTrait(ChatFormatting.DARK_RED));
    public static final RegistryObject<TossUpTrait> TOSS_UP =
            TRAITS.register("toss_up", () -> new TossUpTrait(ChatFormatting.GOLD));
    public static final RegistryObject<JustParryTrait> JUST_PARRY =
            TRAITS.register("just_parry", () -> new JustParryTrait(ChatFormatting.AQUA));
    public static final RegistryObject<CarriedAwayTrait> CARRIED_AWAY =
            TRAITS.register("carried_away", () -> new CarriedAwayTrait(ChatFormatting.GREEN));
    public static final RegistryObject<NocturnalTrait> NOCTURNAL =
            TRAITS.register("nocturnal", () -> new NocturnalTrait(ChatFormatting.DARK_BLUE));
    public static final RegistryObject<DamageAuraTrait> DAMAGE_AURA =
            TRAITS.register("damage_aura", () -> new DamageAuraTrait(ChatFormatting.DARK_PURPLE));

    // === Phase 4: L2DT 介入 3 個 ===
    public static final RegistryObject<DivineMightTrait> DIVINE_MIGHT =
            TRAITS.register("divine_might", () -> new DivineMightTrait(ChatFormatting.GOLD));
    public static final RegistryObject<AllOrNothingTrait> ALL_OR_NOTHING =
            TRAITS.register("all_or_nothing", () -> new AllOrNothingTrait(ChatFormatting.DARK_RED));
    public static final RegistryObject<LastStandTrait> LAST_STAND =
            TRAITS.register("last_stand", () -> new LastStandTrait(ChatFormatting.LIGHT_PURPLE));

    // === Phase 5: クリーパー専用 (vanilla 介入なし) 6 個 + 汎用 1 個 ===
    public static final RegistryObject<ChainDetonationTrait> CHAIN_DETONATION =
            TRAITS.register("chain_detonation", () -> new ChainDetonationTrait(ChatFormatting.RED));
    public static final RegistryObject<ContrarianTrait> CONTRARIAN =
            TRAITS.register("contrarian", () -> new ContrarianTrait(ChatFormatting.DARK_GRAY));
    public static final RegistryObject<ProximityFuseTrait> PROXIMITY_FUSE =
            TRAITS.register("proximity_fuse", () -> new ProximityFuseTrait(ChatFormatting.YELLOW));
    public static final RegistryObject<PeerPressureTrait> PEER_PRESSURE =
            TRAITS.register("peer_pressure", () -> new PeerPressureTrait(ChatFormatting.DARK_GREEN));
    public static final RegistryObject<ExplosiveHeresyTrait> EXPLOSIVE_HERESY =
            TRAITS.register("explosive_heresy", () -> new ExplosiveHeresyTrait(ChatFormatting.DARK_RED));
    public static final RegistryObject<SummonKinTrait> SUMMON_KIN =
            TRAITS.register("summon_kin", () -> new SummonKinTrait(ChatFormatting.GREEN));
    public static final RegistryObject<BomberDispatchTrait> BOMBER_DISPATCH =
            TRAITS.register("bomber_dispatch", () -> new BomberDispatchTrait(ChatFormatting.RED));

    // === Phase 8: 復活系 2 個 ===
    public static final RegistryObject<SecondChanceTrait> SECOND_CHANCE =
            TRAITS.register("second_chance", () -> new SecondChanceTrait(ChatFormatting.GREEN));
    public static final RegistryObject<EndlessTaleTrait> ENDLESS_TALE =
            TRAITS.register("endless_tale", () -> new EndlessTaleTrait(ChatFormatting.AQUA));

    // === Phase 6: クリーパー swell / 爆発 override 5 個 ===
    public static final RegistryObject<HairTriggerTrait> HAIR_TRIGGER =
            TRAITS.register("hair_trigger", () -> new HairTriggerTrait(ChatFormatting.YELLOW));
    public static final RegistryObject<LovesickTrait> LOVESICK =
            TRAITS.register("lovesick", () -> new LovesickTrait(ChatFormatting.LIGHT_PURPLE));
    public static final RegistryObject<FullTankTrait> FULL_TANK =
            TRAITS.register("full_tank", () -> new FullTankTrait(ChatFormatting.GOLD));
    public static final RegistryObject<VolatileMixTrait> VOLATILE_MIX =
            TRAITS.register("volatile_mix", () -> new VolatileMixTrait(ChatFormatting.DARK_GREEN));
    public static final RegistryObject<BurningPassionTrait> BURNING_PASSION =
            TRAITS.register("burning_passion", () -> new BurningPassionTrait(ChatFormatting.RED));

    // === Phase 7: スケルトン射撃 3 個 ===
    public static final RegistryObject<HomingShotTrait> HOMING_SHOT =
            TRAITS.register("homing_shot", () -> new HomingShotTrait(ChatFormatting.AQUA));
    public static final RegistryObject<RapidFireTrait> RAPID_FIRE =
            TRAITS.register("rapid_fire", () -> new RapidFireTrait(ChatFormatting.YELLOW));
    public static final RegistryObject<BurstFireTrait> BURST_FIRE =
            TRAITS.register("burst_fire", () -> new BurstFireTrait(ChatFormatting.GOLD));

    public static void register(IEventBus bus) {
        TRAITS.register(bus);
    }
}
