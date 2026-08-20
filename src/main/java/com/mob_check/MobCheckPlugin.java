package com.mob_check;

import com.google.inject.Provides;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.Projectile;
import net.runelite.api.SpriteID;
import net.runelite.api.Actor;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
		public static final int HYDRA_MAGIC = 1662;
		public static final int HYDRA_RANGE = 1663;
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

		private AnimationID() {}
	}

	// ==========================================
	// Colosseum Region IDs (for gating generic projectile IDs)
	// ==========================================
	private static final Set<Integer> COLOSSEUM_REGION_IDS = new HashSet<>();
	static
	{
		// Fortis Colosseum region IDs
		COLOSSEUM_REGION_IDS.add(7216);
		COLOSSEUM_REGION_IDS.add(7472);
	}

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

	public enum PrayerStyle
	{
		MAGIC("Pray Magic", Prayer.PROTECT_FROM_MAGIC, SpriteID.PRAYER_PROTECT_FROM_MAGIC, 25, new Color(0, 200, 255)),
		RANGE("Pray Range", Prayer.PROTECT_FROM_MISSILES, SpriteID.PRAYER_PROTECT_FROM_MISSILES, 26, new Color(0, 255, 100)),
		MELEE("Pray Melee", Prayer.PROTECT_FROM_MELEE, SpriteID.PRAYER_PROTECT_FROM_MELEE, 27, new Color(255, 110, 0));

		private final String displayName;
		private final Prayer prayer;
		private final int spriteId;
		private final int childIndex;
		private final Color color;

		PrayerStyle(String displayName, Prayer prayer, int spriteId, int childIndex, Color color)
		{
			this.displayName = displayName;
			this.prayer = prayer;
			this.spriteId = spriteId;
			this.childIndex = childIndex;
			this.color = color;
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

		public AttackState(int ticks, String styleName, String npcName)
		{
			this(ticks, ticks, PrayerStyle.fromDisplayName(styleName), npcName, null, null, false);
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

	private final Map<Integer, AttackState> npcMeleeAttacks = new HashMap<>();
	private String lastPriorityStyle = "";

	// #1: Cached attack list — rebuilt once per game tick
	private List<AttackState> cachedAttacks = Collections.emptyList();

	// #2: Tracks the initial tick count for each projectile (keyed by identity hash)
	private final Map<Integer, Integer> projectileInitialTicks = new HashMap<>();

	// Comprehensive projectile mappings
	private static final Map<Integer, String> PROJECTILE_STYLES = new HashMap<>();
	private static final Map<Integer, String> PROJECTILE_NPC_NAMES = new HashMap<>();
	static
	{
		// === INFERNO ===
		// Jal-Zek (Mage)
		PROJECTILE_STYLES.put(ProjectileID.JAL_ZEK_MAGIC, "Pray Magic");
		PROJECTILE_NPC_NAMES.put(ProjectileID.JAL_ZEK_MAGIC, "Jal-Zek");
		PROJECTILE_STYLES.put(ProjectileID.JAL_AKREK_MEJ_MAGIC, "Pray Magic");
		PROJECTILE_NPC_NAMES.put(ProjectileID.JAL_AKREK_MEJ_MAGIC, "Jal-AkRek-Mej");

		// Jal-Xil (Ranger)
		PROJECTILE_STYLES.put(ProjectileID.JAL_XIL_RANGE, "Pray Range");
		PROJECTILE_NPC_NAMES.put(ProjectileID.JAL_XIL_RANGE, "Jal-Xil");
		PROJECTILE_STYLES.put(ProjectileID.JAL_AKREK_XIL_RANGE, "Pray Range");
		PROJECTILE_NPC_NAMES.put(ProjectileID.JAL_AKREK_XIL_RANGE, "Jal-AkRek-Xil");

		// Jal-Ak (Blob)
		PROJECTILE_STYLES.put(ProjectileID.JAL_AK_MAGIC, "Pray Magic");
		PROJECTILE_NPC_NAMES.put(ProjectileID.JAL_AK_MAGIC, "Jal-Ak (Magic)");
		PROJECTILE_STYLES.put(ProjectileID.JAL_AK_RANGE, "Pray Range");
		PROJECTILE_NPC_NAMES.put(ProjectileID.JAL_AK_RANGE, "Jal-Ak (Range)");

		// Jal-MejRah (Bat)
		PROJECTILE_STYLES.put(ProjectileID.JAL_MEJRAH_RANGE, "Pray Range");
		PROJECTILE_NPC_NAMES.put(ProjectileID.JAL_MEJRAH_RANGE, "Jal-MejRah");

		// JalTok-Jad
		PROJECTILE_STYLES.put(ProjectileID.JALTOK_JAD_MAGIC, "Pray Magic");
		PROJECTILE_NPC_NAMES.put(ProjectileID.JALTOK_JAD_MAGIC, "JalTok-Jad (Magic)");
		PROJECTILE_STYLES.put(ProjectileID.JALTOK_JAD_RANGE, "Pray Range");
		PROJECTILE_NPC_NAMES.put(ProjectileID.JALTOK_JAD_RANGE, "JalTok-Jad (Range)");

		// === FORTIS COLOSSEUM ===
		// Serpent Shaman
		PROJECTILE_STYLES.put(ProjectileID.SERPENT_SHAMAN_MAGIC, "Pray Magic");
		PROJECTILE_NPC_NAMES.put(ProjectileID.SERPENT_SHAMAN_MAGIC, "Serpent Shaman");

		// Javelinic Colossus
		PROJECTILE_STYLES.put(ProjectileID.JAVELINIC_COLOSSUS_RANGE, "Pray Range");
		PROJECTILE_NPC_NAMES.put(ProjectileID.JAVELINIC_COLOSSUS_RANGE, "Javelinic Colossus");

		// Manticore
		PROJECTILE_STYLES.put(ProjectileID.MANTICORE_MAGIC, "Pray Magic");
		PROJECTILE_NPC_NAMES.put(ProjectileID.MANTICORE_MAGIC, "Manticore (Magic)");
		PROJECTILE_STYLES.put(ProjectileID.MANTICORE_RANGE, "Pray Range");
		PROJECTILE_NPC_NAMES.put(ProjectileID.MANTICORE_RANGE, "Manticore (Range)");

		// Sol Heredit
		PROJECTILE_STYLES.put(ProjectileID.SOL_HEREDIT_MAGIC, "Pray Magic");
		PROJECTILE_NPC_NAMES.put(ProjectileID.SOL_HEREDIT_MAGIC, "Sol Heredit");

		// Note: Fremennik Archer (15) and Seer (160) are generic engine IDs.
		// They are only matched inside Colosseum regions — see isInColosseumRegion().

		// === ADDITIONAL BOSSES ===
		// Zulrah
		PROJECTILE_STYLES.put(ProjectileID.ZULRAH_MAGIC, "Pray Magic");
		PROJECTILE_NPC_NAMES.put(ProjectileID.ZULRAH_MAGIC, "Zulrah");
		PROJECTILE_STYLES.put(ProjectileID.ZULRAH_RANGE, "Pray Range");
		PROJECTILE_NPC_NAMES.put(ProjectileID.ZULRAH_RANGE, "Zulrah");

		// Vorkath
		PROJECTILE_STYLES.put(ProjectileID.VORKATH_MAGIC, "Pray Magic");
		PROJECTILE_NPC_NAMES.put(ProjectileID.VORKATH_MAGIC, "Vorkath");
		PROJECTILE_STYLES.put(ProjectileID.VORKATH_RANGE, "Pray Range");
		PROJECTILE_NPC_NAMES.put(ProjectileID.VORKATH_RANGE, "Vorkath");

		// Cerberus
		PROJECTILE_STYLES.put(ProjectileID.CERBERUS_MAGIC, "Pray Magic");
		PROJECTILE_NPC_NAMES.put(ProjectileID.CERBERUS_MAGIC, "Cerberus");
		PROJECTILE_STYLES.put(ProjectileID.CERBERUS_RANGE, "Pray Range");
		PROJECTILE_NPC_NAMES.put(ProjectileID.CERBERUS_RANGE, "Cerberus");

		// Alchemical Hydra
		PROJECTILE_STYLES.put(ProjectileID.HYDRA_MAGIC, "Pray Magic");
		PROJECTILE_NPC_NAMES.put(ProjectileID.HYDRA_MAGIC, "Alchemical Hydra");
		PROJECTILE_STYLES.put(ProjectileID.HYDRA_RANGE, "Pray Range");
		PROJECTILE_NPC_NAMES.put(ProjectileID.HYDRA_RANGE, "Alchemical Hydra");

		// Hunllef (Gauntlet)
		PROJECTILE_STYLES.put(ProjectileID.HUNLLEF_MAGIC, "Pray Magic");
		PROJECTILE_NPC_NAMES.put(ProjectileID.HUNLLEF_MAGIC, "Hunllef");
		PROJECTILE_STYLES.put(ProjectileID.HUNLLEF_RANGE, "Pray Range");
		PROJECTILE_NPC_NAMES.put(ProjectileID.HUNLLEF_RANGE, "Hunllef");

		// Demonic Gorilla
		PROJECTILE_STYLES.put(ProjectileID.DEMONIC_GORILLA_MAGIC, "Pray Magic");
		PROJECTILE_NPC_NAMES.put(ProjectileID.DEMONIC_GORILLA_MAGIC, "Demonic Gorilla");
		PROJECTILE_STYLES.put(ProjectileID.DEMONIC_GORILLA_RANGE, "Pray Range");
		PROJECTILE_NPC_NAMES.put(ProjectileID.DEMONIC_GORILLA_RANGE, "Demonic Gorilla");

		// God Wars Dungeon
		PROJECTILE_STYLES.put(ProjectileID.ZILYANA_MAGIC, "Pray Magic");
		PROJECTILE_NPC_NAMES.put(ProjectileID.ZILYANA_MAGIC, "Commander Zilyana");
		PROJECTILE_STYLES.put(ProjectileID.KRIL_MAGIC, "Pray Magic");
		PROJECTILE_NPC_NAMES.put(ProjectileID.KRIL_MAGIC, "K'ril Tsutsaroth");

		// Phantom Muspah
		PROJECTILE_STYLES.put(ProjectileID.MUSPAH_MAGIC, "Pray Magic");
		PROJECTILE_NPC_NAMES.put(ProjectileID.MUSPAH_MAGIC, "Phantom Muspah (Magic)");
		PROJECTILE_STYLES.put(ProjectileID.MUSPAH_RANGE, "Pray Range");
		PROJECTILE_NPC_NAMES.put(ProjectileID.MUSPAH_RANGE, "Phantom Muspah (Range)");

		// Tormented Demons
		PROJECTILE_STYLES.put(ProjectileID.TORMENTED_DEMON_MAGIC, "Pray Magic");
		PROJECTILE_NPC_NAMES.put(ProjectileID.TORMENTED_DEMON_MAGIC, "Tormented Demon (Magic)");
		PROJECTILE_STYLES.put(ProjectileID.TORMENTED_DEMON_RANGE, "Pray Range");
		PROJECTILE_NPC_NAMES.put(ProjectileID.TORMENTED_DEMON_RANGE, "Tormented Demon (Range)");
	}

	// Region-gated projectile mappings (only active in specific regions)
	private static final Map<Integer, String> COLOSSEUM_ONLY_PROJECTILE_STYLES = new HashMap<>();
	private static final Map<Integer, String> COLOSSEUM_ONLY_PROJECTILE_NPC_NAMES = new HashMap<>();
	static
	{
		COLOSSEUM_ONLY_PROJECTILE_STYLES.put(ProjectileID.FREMENNIK_ARCHER_RANGE, "Pray Range");
		COLOSSEUM_ONLY_PROJECTILE_NPC_NAMES.put(ProjectileID.FREMENNIK_ARCHER_RANGE, "Archer");
		COLOSSEUM_ONLY_PROJECTILE_STYLES.put(ProjectileID.FREMENNIK_SEER_MAGIC, "Pray Magic");
		COLOSSEUM_ONLY_PROJECTILE_NPC_NAMES.put(ProjectileID.FREMENNIK_SEER_MAGIC, "Seer");
	}

	// Comprehensive animation mappings (for Melee/Instant attacks)
	private static final Map<Integer, Integer> MELEE_ANIMATIONS = new HashMap<>();
	static
	{
		// === INFERNO ANIMATIONS ===
		MELEE_ANIMATIONS.put(AnimationID.JAL_IMKOT_MELEE, 4);
		MELEE_ANIMATIONS.put(AnimationID.JAL_ZEK_MELEE, 4);
		MELEE_ANIMATIONS.put(AnimationID.JAL_XIL_MELEE, 4);
		MELEE_ANIMATIONS.put(AnimationID.JAL_AK_MELEE, 4);
		MELEE_ANIMATIONS.put(AnimationID.JALTOK_JAD_MELEE, 4);
		MELEE_ANIMATIONS.put(AnimationID.JAL_MEJRAH_MELEE, 4);

		// === FORTIS COLOSSEUM ANIMATIONS ===
		MELEE_ANIMATIONS.put(AnimationID.JAGUAR_WARRIOR_MELEE, 4);
		MELEE_ANIMATIONS.put(AnimationID.MINOTAUR_MELEE, 4);
		MELEE_ANIMATIONS.put(AnimationID.FREMENNIK_BERSERKER_MELEE, 4);
		MELEE_ANIMATIONS.put(AnimationID.SHOCKWAVE_COLOSSUS_MELEE, 4);
		MELEE_ANIMATIONS.put(AnimationID.SOL_HEREDIT_MELEE_ATTACK, 4);
		MELEE_ANIMATIONS.put(AnimationID.SOL_HEREDIT_MELEE_SWEEP, 4);
		MELEE_ANIMATIONS.put(AnimationID.SOL_HEREDIT_MELEE_SLAM, 4);

		// === ADDITIONAL MELEE ANIMATIONS ===
		MELEE_ANIMATIONS.put(AnimationID.ABYSSAL_DEMON_MELEE, 4);
		MELEE_ANIMATIONS.put(AnimationID.BLOODVELD_MELEE, 4);
		MELEE_ANIMATIONS.put(AnimationID.ZILYANA_MELEE, 4);
		MELEE_ANIMATIONS.put(AnimationID.GRAARDOR_MELEE, 4);
		MELEE_ANIMATIONS.put(AnimationID.KRIL_MELEE, 4);
		MELEE_ANIMATIONS.put(AnimationID.CERBERUS_MELEE, 4);
		MELEE_ANIMATIONS.put(AnimationID.DEMONIC_GORILLA_MELEE, 4);
		MELEE_ANIMATIONS.put(AnimationID.MUSPAH_MELEE_ATTACK, 4);
		MELEE_ANIMATIONS.put(AnimationID.MUSPAH_MELEE_SWIPE, 4);
		MELEE_ANIMATIONS.put(AnimationID.TORMENTED_DEMON_MELEE, 4);
	}

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
		cachedAttacks = Collections.emptyList();
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
		cachedAttacks = Collections.emptyList();
		overlayManager.remove(overlay);
		overlayManager.remove(prayerWidgetOverlay);
		overlayManager.remove(worldOverlay);
		log.debug("Mob Check plugin stopped");
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
	public void onGameTick(GameTick event)
	{
		// #3: Countdown active melee attacks — remove when ticks go below 0
		// so the warning stays visible at 0t (the impact tick)
		npcMeleeAttacks.entrySet().removeIf(entry -> {
			entry.getValue().ticks--;
			return entry.getValue().ticks < 0;
		});

		// #1: Rebuild and cache the attack list once per tick
		cachedAttacks = buildActiveAttacks();

		// Sound alert on priority change or emergency wrong-prayer warning
		Optional<AttackState> priorityOpt = getPriorityAttack();
		if (priorityOpt.isPresent())
		{
			AttackState priority = priorityOpt.get();
			boolean styleChanged = !priority.getStyleDisplayName().equals(lastPriorityStyle);
			if (styleChanged)
			{
				playAlertSound(priority.prayerStyle);
			}
			lastPriorityStyle = priority.getStyleDisplayName();

			// Emergency audio cue if unprotected and 1 tick away
			if (config.playWrongPrayerAlert() && priority.ticks <= 1 && !isPrayerProtected(priority.prayerStyle))
			{
				client.playSoundEffect(config.wrongPrayerSoundId());
			}
		}
		else
		{
			lastPriorityStyle = "";
		}
	}

	public void playAlertSound(PrayerStyle style)
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

	/**
	 * Checks whether the player is currently in a Colosseum region.
	 * Used to gate generic projectile IDs (15, 160) that would false-positive elsewhere.
	 */
	private boolean isInColosseumRegion()
	{
		if (client.getMapRegions() == null)
		{
			return false;
		}
		for (int region : client.getMapRegions())
		{
			if (COLOSSEUM_REGION_IDS.contains(region))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Resolves the prayer style for a projectile ID, considering region-gated IDs.
	 */
	private String resolveProjectileStyle(int projectileId)
	{
		String style = PROJECTILE_STYLES.get(projectileId);
		if (style != null)
		{
			return style;
		}

		// Check region-gated projectiles (Fremennik Warband arrows/spells)
		if (isInColosseumRegion())
		{
			return COLOSSEUM_ONLY_PROJECTILE_STYLES.get(projectileId);
		}

		return null;
	}

	/**
	 * Resolves the NPC name for a projectile ID, considering region-gated IDs.
	 */
	private String resolveProjectileNpcName(int projectileId)
	{
		String name = PROJECTILE_NPC_NAMES.get(projectileId);
		if (name != null)
		{
			return name;
		}

		if (isInColosseumRegion())
		{
			return COLOSSEUM_ONLY_PROJECTILE_NPC_NAMES.get(projectileId);
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

		// #5: Track seen projectiles for deduplication within this tick
		Set<Projectile> seenProjectiles = new HashSet<>();

		// #2: Track which projectile identity hashes are still alive to prune stale entries
		Set<Integer> aliveProjectileHashes = new HashSet<>();

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
				String styleName = resolveProjectileStyle(id);
				if (styleName == null && config.trackUnknownProjectiles())
				{
					styleName = "Pray Magic";
				}

				if (styleName != null)
				{
					int ticksRemaining = (projectile.getRemainingCycles() + 29) / 30;
					if (ticksRemaining > 0)
					{
						PrayerStyle prayerStyle = PrayerStyle.fromDisplayName(styleName);
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
							sourceName = resolveProjectileNpcName(id);
							if (sourceName == null)
							{
								sourceName = "Incoming Projectile";
							}
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

		// Gather active melee animation threats
		attacks.addAll(npcMeleeAttacks.values());

		// Sort threats by ticks ascending (lowest ticks = nearest impact = highest priority)
		attacks.sort(Comparator.comparingInt(a -> a.ticks));
		return Collections.unmodifiableList(attacks);
	}
}