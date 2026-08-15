package com.mob_check;

import com.google.inject.Provides;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.Projectile;
import net.runelite.api.SpriteID;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@PluginDescriptor(
	name = "Mob Check",
	description = "Dynamic PvM Priority Prayer Helper specialized for Inferno and Fortis Colosseum",
	tags = {"pvm", "prayer", "dynamic", "projectiles", "inferno", "colosseum", "combat", "helper"}
)
public class MobCheckPlugin extends Plugin
{
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
		public final String style;
		public final PrayerStyle prayerStyle;
		public final String npcName;
		public final NPC sourceNpc;
		public final Projectile projectile;
		public final boolean isManticoreCombo;

		public AttackState(int ticks, String style, String npcName)
		{
			this(ticks, ticks, PrayerStyle.fromDisplayName(style), npcName, null, null, false);
		}

		public AttackState(int ticks, int initialTicks, PrayerStyle prayerStyle, String npcName, NPC sourceNpc, Projectile projectile, boolean isManticoreCombo)
		{
			this.ticks = ticks;
			this.initialTicks = Math.max(initialTicks, ticks);
			this.prayerStyle = prayerStyle != null ? prayerStyle : PrayerStyle.MAGIC;
			this.style = this.prayerStyle.getDisplayName();
			this.npcName = npcName;
			this.sourceNpc = sourceNpc;
			this.projectile = projectile;
			this.isManticoreCombo = isManticoreCombo;
		}
	}

	private final Map<Integer, AttackState> npcMeleeAttacks = new HashMap<>();
	private String lastPriorityStyle = "";

	// Comprehensive projectile mappings
	private static final Map<Integer, String> PROJECTILE_STYLES = new HashMap<>();
	private static final Map<Integer, String> PROJECTILE_NPC_NAMES = new HashMap<>();
	static
	{
		// === INFERNO ===
		// Jal-Zek (Mage)
		PROJECTILE_STYLES.put(1374, "Pray Magic");
		PROJECTILE_NPC_NAMES.put(1374, "Jal-Zek");
		PROJECTILE_STYLES.put(1381, "Pray Magic");
		PROJECTILE_NPC_NAMES.put(1381, "Jal-AkRek-Mej");

		// Jal-Xil (Ranger)
		PROJECTILE_STYLES.put(1376, "Pray Range");
		PROJECTILE_NPC_NAMES.put(1376, "Jal-Xil");
		PROJECTILE_STYLES.put(1379, "Pray Range");
		PROJECTILE_NPC_NAMES.put(1379, "Jal-AkRek-Xil");

		// Jal-Ak (Blob)
		PROJECTILE_STYLES.put(1380, "Pray Magic");
		PROJECTILE_NPC_NAMES.put(1380, "Jal-Ak (Magic)");
		PROJECTILE_STYLES.put(1378, "Pray Range");
		PROJECTILE_NPC_NAMES.put(1378, "Jal-Ak (Range)");

		// Jal-MejRah (Bat)
		PROJECTILE_STYLES.put(1382, "Pray Range");
		PROJECTILE_NPC_NAMES.put(1382, "Jal-MejRah");

		// JalTok-Jad
		PROJECTILE_STYLES.put(448, "Pray Magic");
		PROJECTILE_NPC_NAMES.put(448, "JalTok-Jad (Magic)");
		PROJECTILE_STYLES.put(449, "Pray Range");
		PROJECTILE_NPC_NAMES.put(449, "JalTok-Jad (Range)");

		// === FORTIS COLOSSEUM ===
		// Serpent Shaman
		PROJECTILE_STYLES.put(2685, "Pray Magic");
		PROJECTILE_NPC_NAMES.put(2685, "Serpent Shaman");

		// Javelinic Colossus
		PROJECTILE_STYLES.put(2686, "Pray Range");
		PROJECTILE_NPC_NAMES.put(2686, "Javelinic Colossus");

		// Manticore
		PROJECTILE_STYLES.put(2687, "Pray Magic");
		PROJECTILE_NPC_NAMES.put(2687, "Manticore (Magic)");
		PROJECTILE_STYLES.put(2688, "Pray Range");
		PROJECTILE_NPC_NAMES.put(2688, "Manticore (Range)");

		// Sol Heredit
		PROJECTILE_STYLES.put(2689, "Pray Magic");
		PROJECTILE_NPC_NAMES.put(2689, "Sol Heredit");

		// Fremennik Warband
		PROJECTILE_STYLES.put(15, "Pray Range"); // Standard arrows / Fremennik Archer
		PROJECTILE_NPC_NAMES.put(15, "Archer");
		PROJECTILE_STYLES.put(160, "Pray Magic"); // Standard spells / Fremennik Seer
		PROJECTILE_NPC_NAMES.put(160, "Seer");

		// === ADDITIONAL BOSSES ===
		// Zulrah
		PROJECTILE_STYLES.put(1044, "Pray Magic");
		PROJECTILE_NPC_NAMES.put(1044, "Zulrah");
		PROJECTILE_STYLES.put(1046, "Pray Range");
		PROJECTILE_NPC_NAMES.put(1046, "Zulrah");

		// Vorkath
		PROJECTILE_STYLES.put(1481, "Pray Magic");
		PROJECTILE_NPC_NAMES.put(1481, "Vorkath");
		PROJECTILE_STYLES.put(1483, "Pray Range");
		PROJECTILE_NPC_NAMES.put(1483, "Vorkath");

		// Cerberus
		PROJECTILE_STYLES.put(1243, "Pray Magic");
		PROJECTILE_NPC_NAMES.put(1243, "Cerberus");
		PROJECTILE_STYLES.put(1244, "Pray Range");
		PROJECTILE_NPC_NAMES.put(1244, "Cerberus");

		// Alchemical Hydra
		PROJECTILE_STYLES.put(1662, "Pray Magic");
		PROJECTILE_NPC_NAMES.put(1662, "Alchemical Hydra");
		PROJECTILE_STYLES.put(1663, "Pray Range");
		PROJECTILE_NPC_NAMES.put(1663, "Alchemical Hydra");

		// Hunllef (Gauntlet)
		PROJECTILE_STYLES.put(1707, "Pray Magic");
		PROJECTILE_NPC_NAMES.put(1707, "Hunllef");
		PROJECTILE_STYLES.put(1708, "Pray Range");
		PROJECTILE_NPC_NAMES.put(1708, "Hunllef");

		// Demonic Gorilla
		PROJECTILE_STYLES.put(1302, "Pray Magic");
		PROJECTILE_NPC_NAMES.put(1302, "Demonic Gorilla");
		PROJECTILE_STYLES.put(1304, "Pray Range");
		PROJECTILE_NPC_NAMES.put(1304, "Demonic Gorilla");

		// God Wars Dungeon
		PROJECTILE_STYLES.put(1220, "Pray Magic");
		PROJECTILE_NPC_NAMES.put(1220, "Commander Zilyana");
		PROJECTILE_STYLES.put(1211, "Pray Magic");
		PROJECTILE_NPC_NAMES.put(1211, "K'ril Tsutsaroth");
	}

	// Comprehensive animation mappings (for Melee/Instant attacks)
	private static final Map<Integer, Integer> MELEE_ANIMATIONS = new HashMap<>();
	static
	{
		// === INFERNO ANIMATIONS ===
		MELEE_ANIMATIONS.put(7597, 4); // Jal-ImKot (Meleer)
		MELEE_ANIMATIONS.put(7612, 4); // Jal-Zek (Mage Melee punch)
		MELEE_ANIMATIONS.put(7604, 4); // Jal-Xil (Ranger Melee punch)
		MELEE_ANIMATIONS.put(7582, 4); // Jal-Ak (Blob Melee attack)
		MELEE_ANIMATIONS.put(7590, 4); // JalTok-Jad Melee attack
		MELEE_ANIMATIONS.put(7578, 4); // Jal-MejRah (Bat attack)

		// === FORTIS COLOSSEUM ANIMATIONS ===
		MELEE_ANIMATIONS.put(10871, 4); // Jaguar Warrior Melee attack
		MELEE_ANIMATIONS.put(10872, 4); // Minotaur Melee attack
		MELEE_ANIMATIONS.put(10873, 4); // Fremennik Berserker Melee attack
		MELEE_ANIMATIONS.put(10874, 4); // Shockwave Colossus Melee attack
		MELEE_ANIMATIONS.put(10875, 4); // Sol Heredit Melee attack
		MELEE_ANIMATIONS.put(10876, 4); // Sol Heredit Melee sweep
		MELEE_ANIMATIONS.put(10877, 4); // Sol Heredit Melee slam

		// === ADDITIONAL MELEE ANIMATIONS ===
		MELEE_ANIMATIONS.put(2309, 4); // Abyssal demon
		MELEE_ANIMATIONS.put(1552, 4); // Bloodveld
		MELEE_ANIMATIONS.put(6964, 4); // Commander Zilyana Melee
		MELEE_ANIMATIONS.put(7060, 4); // General Graardor Melee
		MELEE_ANIMATIONS.put(6948, 4); // K'ril Tsutsaroth Melee
		MELEE_ANIMATIONS.put(4492, 4); // Cerberus Melee
		MELEE_ANIMATIONS.put(7226, 4); // Demonic Gorilla Melee
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
		overlayManager.add(overlay);
		overlayManager.add(prayerWidgetOverlay);
		overlayManager.add(worldOverlay);
	}

	@Override
	protected void shutDown()
	{
		npcMeleeAttacks.clear();
		overlayManager.remove(overlay);
		overlayManager.remove(prayerWidgetOverlay);
		overlayManager.remove(worldOverlay);
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
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		// Countdown active melee attacks
		npcMeleeAttacks.entrySet().removeIf(entry -> {
			entry.getValue().ticks--;
			return entry.getValue().ticks <= 0;
		});

		// Sound alert on priority change or emergency wrong-prayer warning
		Optional<AttackState> priorityOpt = getPriorityAttack();
		if (priorityOpt.isPresent())
		{
			AttackState priority = priorityOpt.get();
			boolean styleChanged = !priority.style.equals(lastPriorityStyle);
			if (styleChanged)
			{
				playAlertSound(priority.prayerStyle);
			}
			lastPriorityStyle = priority.style;

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
				soundId = config.soundEffectId();
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

	public List<AttackState> getActiveAttacks()
	{
		List<AttackState> attacks = new ArrayList<>();
		Player localPlayer = client.getLocalPlayer();

		// Gather active projectiles targeting the player
		if (localPlayer != null && client.getProjectiles() != null)
		{
			for (Projectile projectile : client.getProjectiles())
			{
				if (projectile.getTargetActor() == localPlayer)
				{
					int id = projectile.getId();
					String styleName = PROJECTILE_STYLES.get(id);
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
								sourceName = PROJECTILE_NPC_NAMES.getOrDefault(id, "Incoming Projectile");
							}

							boolean isManticore = (id == 2687 || id == 2688);
							attacks.add(new AttackState(
								ticksRemaining,
								ticksRemaining,
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
		}

		// Gather active melee animation threats
		attacks.addAll(npcMeleeAttacks.values());

		// Sort threats by ticks ascending (lowest ticks = nearest impact = highest priority)
		attacks.sort(Comparator.comparingInt(a -> a.ticks));
		return Collections.unmodifiableList(attacks);
	}

	public Optional<AttackState> getPriorityAttack()
	{
		List<AttackState> attacks = getActiveAttacks();
		return attacks.isEmpty() ? Optional.empty() : Optional.of(attacks.get(0));
	}
}