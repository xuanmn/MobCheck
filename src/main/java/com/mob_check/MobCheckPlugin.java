package com.mob_check;

import java.awt.Color;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Provides;

import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.Projectile;
import net.runelite.api.SpriteID;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Mob Check",
	description = "Dynamic PvM Priority Prayer Helper specialized for Inferno and Fortis Colosseum",
	tags = {"pvm", "prayer", "dynamic", "projectiles", "inferno", "colosseum", "combat", "helper"}
)
public class MobCheckPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(MobCheckPlugin.class);

	// ==========================================
	// Projectile ID Constants
	// ==========================================
	public static final class ProjectileID
	{
		// Inferno
		public static final int JAL_ZEK_MAGIC = 1374;
		public static final int JAL_AKREK_MEJ_MAGIC = 1381;
		public static final int JAL_XIL_RANGE = 1376;
		public static final int JAL_AKREK_XIL_RANGE = 1379;
		public static final int JAL_AK_MAGIC = 1380;
		public static final int JAL_AK_RANGE = 1378;
		public static final int JAL_MEJRAH_RANGE = 1382;
		public static final int JALTOK_JAD_MAGIC = 448;
		public static final int JALTOK_JAD_RANGE = 449;

		// Fortis Colosseum
		public static final int SERPENT_SHAMAN_MAGIC = 2685;
		public static final int JAVELINIC_COLOSSUS_RANGE = 2686;
		public static final int MANTICORE_MAGIC = 2687;
		public static final int MANTICORE_RANGE = 2688;
		public static final int SOL_HEREDIT_MAGIC = 2689;
		public static final int FREMENNIK_ARCHER_RANGE = 15;
		public static final int FREMENNIK_SEER_MAGIC = 160;

		// Additional Bosses
		public static final int ZULRAH_MAGIC = 1044;
		public static final int ZULRAH_RANGE = 1046;
		public static final int VORKATH_MAGIC = 1481;
		public static final int VORKATH_RANGE = 1483;
		public static final int CERBERUS_MAGIC = 1243;
		public static final int CERBERUS_RANGE = 1244;
		public static final int HYDRA_MAGIC = 1663;
		public static final int HYDRA_RANGE = 1662;
		public static final int HUNLLEF_MAGIC = 1707;
		public static final int HUNLLEF_RANGE = 1708;
		public static final int DEMONIC_GORILLA_MAGIC = 1302;
		public static final int DEMONIC_GORILLA_RANGE = 1304;
		public static final int ZILYANA_MAGIC = 1220;
		public static final int KRIL_MAGIC = 1211;
		public static final int MUSPAH_MAGIC = 2329;
		public static final int MUSPAH_RANGE = 2330;
		public static final int TORMENTED_DEMON_MAGIC = 1885;
		public static final int TORMENTED_DEMON_RANGE = 1884;

		// Dagannoth Kings
		public static final int DAGANNOTH_PRIME_MAGIC = 162;
		public static final int DAGANNOTH_SUPREME_RANGE = 475;
		public static final int DAGANNOTH_SUPREME_RANGE_2 = 476;

		private ProjectileID() {}
	}

	// ==========================================
	// Animation ID Constants
	// ==========================================
	public static final class AnimationID
	{
		// Inferno
		public static final int JAL_IMKOT_MELEE = 7597;
		public static final int JAL_ZEK_MELEE = 7612;
		public static final int JAL_XIL_MELEE = 7604;
		public static final int JAL_AK_MELEE = 7582;
		public static final int JALTOK_JAD_MELEE = 7590;
		public static final int JAL_MEJRAH_MELEE = 7578;

		// Fortis Colosseum
		public static final int JAGUAR_WARRIOR_MELEE = 10871;
		public static final int MINOTAUR_MELEE = 10872;
		public static final int FREMENNIK_BERSERKER_MELEE = 10873;
		public static final int SHOCKWAVE_COLOSSUS_MELEE = 10874;
		public static final int SOL_HEREDIT_MELEE_ATTACK = 10875;
		public static final int SOL_HEREDIT_MELEE_SWEEP = 10876;
		public static final int SOL_HEREDIT_MELEE_SLAM = 10877;

		// Additional Bosses & Slayer
		public static final int ABYSSAL_DEMON_MELEE = 2309;
		public static final int BLOODVELD_MELEE = 1552;
		public static final int ZILYANA_MELEE = 6964;
		public static final int GRAARDOR_MELEE = 7060;
		public static final int KRIL_MELEE = 6948;
		public static final int CERBERUS_MELEE = 4492;
		public static final int DEMONIC_GORILLA_MELEE = 7226;
		public static final int MUSPAH_MELEE_ATTACK = 9920;
		public static final int MUSPAH_MELEE_SWIPE = 9922;
		public static final int TORMENTED_DEMON_MELEE = 10922;
		public static final int DAGANNOTH_REX_MELEE = 2853;
		public static final int DAGANNOTH_REX_MELEE_2 = 2851;

		private AnimationID() {}
	}

	// ==========================================
	// Region IDs (for gating generic projectile IDs)
	// ==========================================
	// Fortis Colosseum region IDs
	private static final Set<Integer> COLOSSEUM_REGION_IDS = Set.of(7216, 7472);

	// Waterbirth Island Dungeon / Dagannoth Kings Lair
	private static final Set<Integer> DAGANNOTH_KINGS_REGION_IDS = Set.of(11588, 11589);

	@Inject
	private Client client;

	@Inject
	private MobCheckConfig config;

	@Inject
	private MobCheckOverlay overlay;

	@Inject
	private MobCheckPrayerWidgetOverlay prayerWidgetOverlay;

	@Inject
	private MobCheckWorldOverlay worldOverlay;

	@Inject
	private OverlayManager overlayManager;

	@SuppressWarnings("deprecation")
	public enum PrayerStyle
	{
		MAGIC("Pray Magic", Prayer.PROTECT_FROM_MAGIC, SpriteID.PRAYER_PROTECT_FROM_MAGIC, 25, new Color(0, 200, 255), new Color(0, 200, 255, 35), new Color(0, 200, 255, 25)),
		RANGE("Pray Range", Prayer.PROTECT_FROM_MISSILES, SpriteID.PRAYER_PROTECT_FROM_MISSILES, 26, new Color(0, 255, 100), new Color(0, 255, 100, 35), new Color(0, 255, 100, 25)),
		MELEE("Pray Melee", Prayer.PROTECT_FROM_MELEE, SpriteID.PRAYER_PROTECT_FROM_MELEE, 27, new Color(255, 110, 0), new Color(255, 110, 0, 35), new Color(255, 110, 0, 25));

		private final String displayName;
		private final Prayer prayer;
		private final int spriteId;
		private final int childIndex;
		private final Color color;
		private final Color tileFillColor;
		private final Color hullFillColor;

		PrayerStyle(String displayName, Prayer prayer, int spriteId, int childIndex, Color color, Color tileFillColor, Color hullFillColor)
		{
			this.displayName = displayName;
			this.prayer = prayer;
			this.spriteId = spriteId;
			this.childIndex = childIndex;
			this.color = color;
			this.tileFillColor = tileFillColor;
			this.hullFillColor = hullFillColor;
		}

		public String getDisplayName()
		{
			return displayName;
		}

		public Prayer getPrayer()
		{
			return prayer;
		}

		public int getSpriteId()
		{
			return spriteId;
		}

		public int getChildIndex()
		{
			return childIndex;
		}

		public Color getColor()
		{
			return color;
		}

		public Color getTileFillColor()
		{
			return tileFillColor;
		}

		public Color getHullFillColor()
		{
			return hullFillColor;
		}

		public static PrayerStyle fromDisplayName(String name)
		{
			for (PrayerStyle style : values())
			{
				if (style.displayName.equalsIgnoreCase(name) || style.name().equalsIgnoreCase(name))
				{
					return style;
				}
			}
			return MAGIC;
		}
	}

	public static class AttackState
	{
		public int ticks;
		public final int initialTicks;
		public final PrayerStyle prayerStyle;
		public final String npcName;
		public final NPC sourceNpc;
		public final Projectile projectile;
		public final boolean isManticoreCombo;

		public AttackState(int ticks, PrayerStyle prayerStyle, String npcName)
		{
			this(ticks, ticks, prayerStyle, npcName, null, null, false);
		}

		public AttackState(int ticks, int initialTicks, PrayerStyle prayerStyle, String npcName, NPC sourceNpc, Projectile projectile, boolean isManticoreCombo)
		{
			this.ticks = ticks;
			this.initialTicks = Math.max(initialTicks, ticks);
			this.prayerStyle = prayerStyle != null ? prayerStyle : PrayerStyle.MAGIC;
			this.npcName = npcName;
			this.sourceNpc = sourceNpc;
			this.projectile = projectile;
			this.isManticoreCombo = isManticoreCombo;
		}

		/**
		 * Returns the display name for this attack's prayer style.
		 */
		public String getStyleDisplayName()
		{
			return prayerStyle.getDisplayName();
		}
	}

	public static final class ProjectileInfo
	{
		private final PrayerStyle prayerStyle;
		private final String name;

		public ProjectileInfo(PrayerStyle prayerStyle, String name)
		{
			this.prayerStyle = prayerStyle;
			this.name = name;
		}

		public PrayerStyle getPrayerStyle()
		{
			return prayerStyle;
		}

		public String getName()
		{
			return name;
		}
	}

	private static final String[] TICK_STRINGS = new String[31];
	static
	{
		for (int i = 0; i < TICK_STRINGS.length; i++)
		{
			TICK_STRINGS[i] = i + "t";
		}
	}

	public static String formatTicks(int ticks)
	{
		if (ticks >= 0 && ticks < TICK_STRINGS.length)
		{
			return TICK_STRINGS[ticks];
		}
		return ticks + "t";
	}

	private static final Comparator<AttackState> ATTACK_COMPARATOR = Comparator.comparingInt(a -> a.ticks);
	private final Map<Integer, AttackState> npcMeleeAttacks = new HashMap<>();
	private PrayerStyle lastPriorityPrayer = null;

	// #1: Cached attack list — rebuilt once per game tick
	private volatile List<AttackState> cachedAttacks = Collections.emptyList();

	// #2: Tracks the initial tick count for each projectile (keyed by identity hash)
	private final Map<Integer, Integer> projectileInitialTicks = new HashMap<>();

	// Reusable set buffers for per-tick projectile processing to avoid heap allocations
	private final Set<Projectile> seenProjectiles = new HashSet<>();
	private final Set<Integer> aliveProjectileHashes = new HashSet<>();

	// Comprehensive projectile mappings: projectile ID -> ProjectileInfo(PrayerStyle, NPC name)
	private static final Map<Integer, ProjectileInfo> PROJECTILE_INFO = Map.ofEntries(
		// === INFERNO ===
		projEntry(ProjectileID.JAL_ZEK_MAGIC, PrayerStyle.MAGIC, "Jal-Zek"),
		projEntry(ProjectileID.JAL_AKREK_MEJ_MAGIC, PrayerStyle.MAGIC, "Jal-AkRek-Mej"),
		projEntry(ProjectileID.JAL_XIL_RANGE, PrayerStyle.RANGE, "Jal-Xil"),
		projEntry(ProjectileID.JAL_AKREK_XIL_RANGE, PrayerStyle.RANGE, "Jal-AkRek-Xil"),
		projEntry(ProjectileID.JAL_AK_MAGIC, PrayerStyle.MAGIC, "Jal-Ak (Magic)"),
		projEntry(ProjectileID.JAL_AK_RANGE, PrayerStyle.RANGE, "Jal-Ak (Range)"),
		projEntry(ProjectileID.JAL_MEJRAH_RANGE, PrayerStyle.RANGE, "Jal-MejRah"),
		projEntry(ProjectileID.JALTOK_JAD_MAGIC, PrayerStyle.MAGIC, "JalTok-Jad (Magic)"),
		projEntry(ProjectileID.JALTOK_JAD_RANGE, PrayerStyle.RANGE, "JalTok-Jad (Range)"),
		// === FORTIS COLOSSEUM ===
		projEntry(ProjectileID.SERPENT_SHAMAN_MAGIC, PrayerStyle.MAGIC, "Serpent Shaman"),
		projEntry(ProjectileID.JAVELINIC_COLOSSUS_RANGE, PrayerStyle.RANGE, "Javelinic Colossus"),
		projEntry(ProjectileID.MANTICORE_MAGIC, PrayerStyle.MAGIC, "Manticore (Magic)"),
		projEntry(ProjectileID.MANTICORE_RANGE, PrayerStyle.RANGE, "Manticore (Range)"),
		projEntry(ProjectileID.SOL_HEREDIT_MAGIC, PrayerStyle.MAGIC, "Sol Heredit"),
		// Note: Fremennik Archer (15) and Seer (160) are generic engine IDs.
		// They are only matched inside Colosseum regions — see COLOSSEUM_ONLY_PROJECTILE_INFO.
		// === ADDITIONAL BOSSES ===
		projEntry(ProjectileID.ZULRAH_MAGIC, PrayerStyle.MAGIC, "Zulrah"),
		projEntry(ProjectileID.ZULRAH_RANGE, PrayerStyle.RANGE, "Zulrah"),
		projEntry(ProjectileID.VORKATH_MAGIC, PrayerStyle.MAGIC, "Vorkath"),
		projEntry(ProjectileID.VORKATH_RANGE, PrayerStyle.RANGE, "Vorkath"),
		projEntry(ProjectileID.CERBERUS_MAGIC, PrayerStyle.MAGIC, "Cerberus"),
		projEntry(ProjectileID.CERBERUS_RANGE, PrayerStyle.RANGE, "Cerberus"),
		projEntry(ProjectileID.HYDRA_MAGIC, PrayerStyle.MAGIC, "Alchemical Hydra"),
		projEntry(ProjectileID.HYDRA_RANGE, PrayerStyle.RANGE, "Alchemical Hydra"),
		projEntry(ProjectileID.HUNLLEF_MAGIC, PrayerStyle.MAGIC, "Hunllef"),
		projEntry(ProjectileID.HUNLLEF_RANGE, PrayerStyle.RANGE, "Hunllef"),
		projEntry(ProjectileID.DEMONIC_GORILLA_MAGIC, PrayerStyle.MAGIC, "Demonic Gorilla"),
		projEntry(ProjectileID.DEMONIC_GORILLA_RANGE, PrayerStyle.RANGE, "Demonic Gorilla"),
		projEntry(ProjectileID.ZILYANA_MAGIC, PrayerStyle.MAGIC, "Commander Zilyana"),
		projEntry(ProjectileID.KRIL_MAGIC, PrayerStyle.MAGIC, "K'ril Tsutsaroth"),
		projEntry(ProjectileID.MUSPAH_MAGIC, PrayerStyle.MAGIC, "Phantom Muspah (Magic)"),
		projEntry(ProjectileID.MUSPAH_RANGE, PrayerStyle.RANGE, "Phantom Muspah (Range)"),
		projEntry(ProjectileID.TORMENTED_DEMON_MAGIC, PrayerStyle.MAGIC, "Tormented Demon (Magic)"),
		projEntry(ProjectileID.TORMENTED_DEMON_RANGE, PrayerStyle.RANGE, "Tormented Demon (Range)"),
		projEntry(ProjectileID.DAGANNOTH_SUPREME_RANGE, PrayerStyle.RANGE, "Dagannoth Supreme"),
		projEntry(ProjectileID.DAGANNOTH_SUPREME_RANGE_2, PrayerStyle.RANGE, "Dagannoth Supreme")
	);

	private static Map.Entry<Integer, ProjectileInfo> projEntry(int id, PrayerStyle style, String name)
	{
		return Map.entry(id, new ProjectileInfo(style, name));
	}

	// Region-gated projectile mappings (only active in specific regions)
	private static final Map<Integer, ProjectileInfo> COLOSSEUM_ONLY_PROJECTILE_INFO = Map.of(
		ProjectileID.FREMENNIK_ARCHER_RANGE, new ProjectileInfo(PrayerStyle.RANGE, "Archer"),
		ProjectileID.FREMENNIK_SEER_MAGIC, new ProjectileInfo(PrayerStyle.MAGIC, "Seer")
	);

	private static final Map<Integer, ProjectileInfo> DAGANNOTH_KINGS_ONLY_PROJECTILE_INFO = Map.of(
		ProjectileID.DAGANNOTH_PRIME_MAGIC, new ProjectileInfo(PrayerStyle.MAGIC, "Dagannoth Prime")
	);

	// Comprehensive animation mappings (for Melee/Instant attacks)
	private static final Map<Integer, Integer> MELEE_ANIMATIONS = Map.ofEntries(
		// === INFERNO ANIMATIONS ===
		Map.entry(AnimationID.JAL_IMKOT_MELEE, 4),
		Map.entry(AnimationID.JAL_ZEK_MELEE, 4),
		Map.entry(AnimationID.JAL_XIL_MELEE, 4),
		Map.entry(AnimationID.JAL_AK_MELEE, 4),
		Map.entry(AnimationID.JALTOK_JAD_MELEE, 4),
		Map.entry(AnimationID.JAL_MEJRAH_MELEE, 4),
		// === FORTIS COLOSSEUM ANIMATIONS ===
		Map.entry(AnimationID.JAGUAR_WARRIOR_MELEE, 4),
		Map.entry(AnimationID.MINOTAUR_MELEE, 4),
		Map.entry(AnimationID.FREMENNIK_BERSERKER_MELEE, 4),
		Map.entry(AnimationID.SHOCKWAVE_COLOSSUS_MELEE, 4),
		Map.entry(AnimationID.SOL_HEREDIT_MELEE_ATTACK, 4),
		Map.entry(AnimationID.SOL_HEREDIT_MELEE_SWEEP, 4),
		Map.entry(AnimationID.SOL_HEREDIT_MELEE_SLAM, 4),
		// === ADDITIONAL MELEE ANIMATIONS ===
		Map.entry(AnimationID.ABYSSAL_DEMON_MELEE, 4),
		Map.entry(AnimationID.BLOODVELD_MELEE, 4),
		Map.entry(AnimationID.ZILYANA_MELEE, 2),
		Map.entry(AnimationID.GRAARDOR_MELEE, 6),
		Map.entry(AnimationID.KRIL_MELEE, 6),
		Map.entry(AnimationID.CERBERUS_MELEE, 6),
		Map.entry(AnimationID.DEMONIC_GORILLA_MELEE, 5),
		Map.entry(AnimationID.MUSPAH_MELEE_ATTACK, 6),
		Map.entry(AnimationID.MUSPAH_MELEE_SWIPE, 6),
		Map.entry(AnimationID.TORMENTED_DEMON_MELEE, 4),
		Map.entry(AnimationID.DAGANNOTH_REX_MELEE, 4),
		Map.entry(AnimationID.DAGANNOTH_REX_MELEE_2, 4)
	);

	@Provides
	MobCheckConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MobCheckConfig.class);
	}

	@Override
	protected void startUp()
	{
		npcMeleeAttacks.clear();
		projectileInitialTicks.clear();
		seenProjectiles.clear();
		aliveProjectileHashes.clear();
		cachedAttacks = Collections.emptyList();
		lastPriorityPrayer = null;
		overlayManager.add(overlay);
		overlayManager.add(prayerWidgetOverlay);
		overlayManager.add(worldOverlay);
		log.debug("Mob Check plugin started");
	}

	@Override
	protected void shutDown()
	{
		npcMeleeAttacks.clear();
		projectileInitialTicks.clear();
		seenProjectiles.clear();
		aliveProjectileHashes.clear();
		cachedAttacks = Collections.emptyList();
		lastPriorityPrayer = null;
		overlayManager.remove(overlay);
		overlayManager.remove(prayerWidgetOverlay);
		overlayManager.remove(worldOverlay);
		log.debug("Mob Check plugin stopped");
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		NPC npc = event.getNpc();
		if (npc != null)
		{
			npcMeleeAttacks.remove(npc.getIndex());
		}
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (!(event.getActor() instanceof NPC))
		{
			return;
		}

		NPC npc = (NPC) event.getActor();
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null || npc.getInteracting() != localPlayer)
		{
			return;
		}

		int anim = npc.getAnimation();

		if (MELEE_ANIMATIONS.containsKey(anim))
		{
			int warningTicks = MELEE_ANIMATIONS.get(anim);
			String name = npc.getName() != null ? npc.getName() : "Enemy";
			npcMeleeAttacks.put(npc.getIndex(), new AttackState(
				warningTicks,
				warningTicks,
				PrayerStyle.MELEE,
				name,
				npc,
				null,
				false
			));
			log.debug("Melee attack registered: {} (anim={}, ticks={})", name, anim, warningTicks);
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOGGING_IN || state == GameState.HOPPING || state == GameState.LOGIN_SCREEN)
		{
			npcMeleeAttacks.clear();
			projectileInitialTicks.clear();
			seenProjectiles.clear();
			aliveProjectileHashes.clear();
			cachedAttacks = Collections.emptyList();
			lastPriorityPrayer = null;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		// #3: Countdown active melee attacks — remove if NPC died or when ticks go below 0
		// so the warning stays visible at 0t (the impact tick)
		npcMeleeAttacks.entrySet().removeIf(entry -> {
			AttackState attack = entry.getValue();
			if (attack.sourceNpc != null && attack.sourceNpc.isDead())
			{
				return true;
			}
			attack.ticks--;
			return attack.ticks < 0;
		});

		// #1: Rebuild and cache the attack list once per tick
		cachedAttacks = buildActiveAttacks();

		// Sound alert on priority change or emergency wrong-prayer warning
		Optional<AttackState> priorityOpt = getPriorityAttack();
		if (priorityOpt.isPresent())
		{
			AttackState priority = priorityOpt.get();
			boolean styleChanged = (priority.prayerStyle != lastPriorityPrayer);
			if (styleChanged)
			{
				playAlertSound(priority.prayerStyle);
			}
			lastPriorityPrayer = priority.prayerStyle;

			// Emergency audio cue if unprotected and 1 tick away
			if (config.playWrongPrayerAlert() && priority.ticks <= 1 && !isPrayerProtected(priority.prayerStyle))
			{
				client.playSoundEffect(config.wrongPrayerSoundId());
			}
		}
		else
		{
			lastPriorityPrayer = null;
		}
	}

	private void playAlertSound(PrayerStyle style)
	{
		if (!config.playSoundAlert() || style == null)
		{
			return;
		}

		int soundId;
		switch (style)
		{
			case MAGIC:
				soundId = config.magicSoundId();
				break;
			case RANGE:
				soundId = config.rangeSoundId();
				break;
			case MELEE:
				soundId = config.meleeSoundId();
				break;
			default:
				// Unreachable — PrayerStyle only has MAGIC, RANGE, MELEE
				soundId = config.magicSoundId();
				break;
		}

		client.playSoundEffect(soundId);
	}

	@SuppressWarnings("deprecation")
	public boolean isPrayerProtected(PrayerStyle style)
	{
		if (client == null || style == null)
		{
			return false;
		}
		return client.isPrayerActive(style.getPrayer());
	}

	/**
	 * Returns the cached attack list from the most recent game tick.
	 * This list is rebuilt once per tick in onGameTick() to ensure consistent
	 * state across all overlays rendering on the same frame.
	 */
	public List<AttackState> getActiveAttacks()
	{
		return cachedAttacks;
	}

	public Optional<AttackState> getPriorityAttack()
	{
		List<AttackState> attacks = getActiveAttacks();
		return attacks.isEmpty() ? Optional.empty() : Optional.of(attacks.get(0));
	}

	@SuppressWarnings("deprecation")
	private int[] getPlayerMapRegions()
	{
		if (client == null)
		{
			return null;
		}
		int[] regions = null;
		try
		{
			if (client.getTopLevelWorldView() != null)
			{
				regions = client.getTopLevelWorldView().getMapRegions();
			}
		}
		catch (NoSuchMethodError | Exception ignored)
		{
		}
		if (regions == null)
		{
			regions = client.getMapRegions();
		}
		return regions;
	}

	private static boolean matchesAnyRegion(int[] playerRegions, Set<Integer> targetRegions)
	{
		if (playerRegions == null || targetRegions == null || targetRegions.isEmpty())
		{
			return false;
		}
		for (int region : playerRegions)
		{
			if (targetRegions.contains(region))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks whether the player is currently in a Colosseum region.
	 * Used to gate generic projectile IDs (15, 160) that would false-positive elsewhere.
	 */
	public boolean isInColosseumRegion()
	{
		return matchesAnyRegion(getPlayerMapRegions(), COLOSSEUM_REGION_IDS);
	}

	/**
	 * Checks whether the player is currently in the Dagannoth Kings Lair region.
	 * Used to gate generic Water Wave projectile ID (162).
	 */
	public boolean isInDagannothKingsRegion()
	{
		return matchesAnyRegion(getPlayerMapRegions(), DAGANNOTH_KINGS_REGION_IDS);
	}

	/**
	 * Resolves both the prayer style and NPC name for a projectile ID in a single lookup,
	 * considering region-gated IDs. Returns null if the projectile is not recognized.
	 * Region booleans are passed in to avoid redundant region checks per projectile.
	 */
	private ProjectileInfo resolveProjectile(int projectileId, boolean inColosseum, boolean inDagKings)
	{
		ProjectileInfo info = PROJECTILE_INFO.get(projectileId);
		if (info != null)
		{
			return info;
		}

		// Check region-gated projectiles (Fremennik Warband arrows/spells)
		if (inColosseum)
		{
			ProjectileInfo colosseumInfo = COLOSSEUM_ONLY_PROJECTILE_INFO.get(projectileId);
			if (colosseumInfo != null)
			{
				return colosseumInfo;
			}
		}

		// Check region-gated Dagannoth Kings projectiles (Water Wave 162)
		if (inDagKings)
		{
			ProjectileInfo dkInfo = DAGANNOTH_KINGS_ONLY_PROJECTILE_INFO.get(projectileId);
			if (dkInfo != null)
			{
				return dkInfo;
			}
		}

		return null;
	}

	/**
	 * Builds the full active attack list from live projectiles and melee attack tracking.
	 * Called once per game tick. Handles projectile deduplication and initialTicks caching.
	 */
	private List<AttackState> buildActiveAttacks()
	{
		List<AttackState> attacks = new ArrayList<>();
		Player localPlayer = client.getLocalPlayer();

		// #5: Track seen projectiles for deduplication within this tick (reusing buffer)
		seenProjectiles.clear();

		// #2: Track which projectile identity hashes are still alive to prune stale entries (reusing buffer)
		aliveProjectileHashes.clear();

		// Cache region checks once per tick to avoid redundant calls per projectile
		int[] mapRegions = getPlayerMapRegions();
		boolean inColosseum = matchesAnyRegion(mapRegions, COLOSSEUM_REGION_IDS);
		boolean inDagKings = matchesAnyRegion(mapRegions, DAGANNOTH_KINGS_REGION_IDS);

		// Gather active projectiles targeting the player
		if (localPlayer != null && client.getProjectiles() != null)
		{
			for (Projectile projectile : client.getProjectiles())
			{
				if (projectile.getTargetActor() != localPlayer)
				{
					continue;
				}

				// #5: Skip duplicate projectile references
				if (!seenProjectiles.add(projectile))
				{
					continue;
				}

				int id = projectile.getId();
				ProjectileInfo projInfo = resolveProjectile(id, inColosseum, inDagKings);
				PrayerStyle prayerStyle = projInfo != null ? projInfo.getPrayerStyle() : null;
				String resolvedName = projInfo != null ? projInfo.getName() : null;
				if (prayerStyle == null && config.trackUnknownProjectiles())
				{
					prayerStyle = PrayerStyle.MAGIC;
				}

				if (prayerStyle != null)
				{
					int ticksRemaining = (projectile.getRemainingCycles() + 29) / 30;
					if (ticksRemaining > 0)
					{
						String sourceName = null;
						NPC sourceNpc = null;
						Actor source = projectile.getSourceActor();
						if (source instanceof NPC)
						{
							sourceNpc = (NPC) source;
							sourceName = sourceNpc.getName();
						}
						else if (source != null && source.getName() != null)
						{
							sourceName = source.getName();
						}

						if (sourceName == null)
						{
							sourceName = resolvedName != null ? resolvedName : "Incoming Projectile";
						}

						// #2: Cache initialTicks on first sight of this projectile
						int projHash = System.identityHashCode(projectile);
						aliveProjectileHashes.add(projHash);
						int initialTicks = projectileInitialTicks.computeIfAbsent(projHash, k -> ticksRemaining);

						boolean isManticore = (id == ProjectileID.MANTICORE_MAGIC || id == ProjectileID.MANTICORE_RANGE);
						attacks.add(new AttackState(
							ticksRemaining,
							initialTicks,
							prayerStyle,
							sourceName,
							sourceNpc,
							projectile,
							isManticore
						));
					}
				}
			}
		}

		// #2: Prune stale entries from the initialTicks cache
		projectileInitialTicks.keySet().retainAll(aliveProjectileHashes);

		// Gather active melee animation threats (skip dead NPCs)
		for (AttackState meleeAttack : npcMeleeAttacks.values())
		{
			if (meleeAttack.sourceNpc == null || !meleeAttack.sourceNpc.isDead())
			{
				attacks.add(meleeAttack);
			}
		}

		// Sort threats by ticks ascending (lowest ticks = nearest impact = highest priority)
		attacks.sort(ATTACK_COMPARATOR);
		return Collections.unmodifiableList(attacks);
	}
}
