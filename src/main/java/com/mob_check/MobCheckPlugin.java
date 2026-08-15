package com.mob_check;

import com.google.inject.Provides;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Projectile;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
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
	private OverlayManager overlayManager;

	public static class AttackState
	{
		public int ticks;
		public final String style;
		public final String npcName;

		public AttackState(int ticks, String style, String npcName)
		{
			this.ticks = ticks;
			this.style = style;
			this.npcName = npcName;
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
	}

	@Override
	protected void shutDown()
	{
		npcMeleeAttacks.clear();
		overlayManager.remove(overlay);
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
			npcMeleeAttacks.put(npc.getIndex(), new AttackState(warningTicks, "Pray Melee", name));
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

		// Sound alert on priority change
		Optional<AttackState> priorityOpt = getPriorityAttack();
		if (priorityOpt.isPresent())
		{
			AttackState priority = priorityOpt.get();
			if (config.playSoundAlert() && !priority.style.equals(lastPriorityStyle))
			{
				client.playSoundEffect(config.soundEffectId());
			}
			lastPriorityStyle = priority.style;
		}
		else
		{
			lastPriorityStyle = "";
		}
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
					String style = PROJECTILE_STYLES.get(id);
					if (style == null && config.trackUnknownProjectiles())
					{
						style = "Pray Magic";
					}

					if (style != null)
					{
						int ticksRemaining = (projectile.getRemainingCycles() + 29) / 30;
						if (ticksRemaining > 0)
						{
							String sourceName = null;
							Actor source = projectile.getSourceActor();
							if (source != null && source.getName() != null)
							{
								sourceName = source.getName();
							}
							if (sourceName == null)
							{
								sourceName = PROJECTILE_NPC_NAMES.getOrDefault(id, "Incoming Projectile");
							}
							attacks.add(new AttackState(ticksRemaining, style, sourceName));
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