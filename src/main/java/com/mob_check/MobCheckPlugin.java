package com.mob_check;

import com.google.inject.Provides;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@PluginDescriptor(name = "Mob Check", description = "Dynamic PvM Priority Prayer Helper", tags = { "pvm", "prayer",
		"dynamic", "projectiles", "sound", "combat", "helper" })
public class MobCheckPlugin extends Plugin {
	@Inject
	private Client client;

	@Inject
	private MobCheckConfig config;

	@Inject
	private MobCheckOverlay overlay;

	@Inject
	private OverlayManager overlayManager;

	public static class AttackState {
		public int ticks;
		public final String style;
		public final String npcName;

		public AttackState(int ticks, String style, String npcName) {
			this.ticks = ticks;
			this.style = style;
			this.npcName = npcName;
		}
	}

	private final Map<Integer, AttackState> npcMeleeAttacks = new HashMap<>();
	private String lastPriorityStyle = "";

	// Comprehensive projectile mappings
	private static final Map<Integer, String> PROJECTILE_STYLES = new HashMap<>();
	static {
		// Inferno
		PROJECTILE_STYLES.put(1374, "Pray Magic"); // Jal-Zek
		PROJECTILE_STYLES.put(1376, "Pray Range"); // Jal-Xil
		PROJECTILE_STYLES.put(448, "Pray Magic"); // Jad Mage
		PROJECTILE_STYLES.put(449, "Pray Range"); // Jad Range

		// Zulrah
		PROJECTILE_STYLES.put(1044, "Pray Magic"); // Mage phase standard / Snakeling Mage
		PROJECTILE_STYLES.put(1046, "Pray Range"); // Range phase standard / Snakeling Range

		// Vorkath
		PROJECTILE_STYLES.put(1481, "Pray Magic"); // Magic attack
		PROJECTILE_STYLES.put(1483, "Pray Range"); // Range attack
		PROJECTILE_STYLES.put(1482, "Pray Magic"); // Dragonfire (requires Mage or shield)

		// Cerberus
		PROJECTILE_STYLES.put(1243, "Pray Magic");
		PROJECTILE_STYLES.put(1244, "Pray Range");

		// Alchemical Hydra
		PROJECTILE_STYLES.put(1662, "Pray Magic");
		PROJECTILE_STYLES.put(1663, "Pray Range");

		// Hunllef (Gauntlet)
		PROJECTILE_STYLES.put(1707, "Pray Magic");
		PROJECTILE_STYLES.put(1708, "Pray Range");

		// Demonic Gorilla
		PROJECTILE_STYLES.put(1302, "Pray Magic");
		PROJECTILE_STYLES.put(1304, "Pray Range");

		// God Wars Dungeon
		PROJECTILE_STYLES.put(1220, "Pray Magic"); // Commander Zilyana
		PROJECTILE_STYLES.put(1217, "Pray Range"); // General Graardor
		PROJECTILE_STYLES.put(1211, "Pray Magic"); // K'ril Tsutsaroth

		// Fortis Colosseum
		PROJECTILE_STYLES.put(2685, "Pray Magic"); // Serpent Shaman / Magic attack
		PROJECTILE_STYLES.put(2686, "Pray Range"); // Javelinic Colossus / Range attack
		PROJECTILE_STYLES.put(2687, "Pray Magic"); // Manticore Magic projectile
		PROJECTILE_STYLES.put(2688, "Pray Range"); // Manticore Range projectile
		PROJECTILE_STYLES.put(2689, "Pray Magic"); // Sol Heredit Ranged/Magic special projectile

		// General / Standard
		PROJECTILE_STYLES.put(15, "Pray Range"); // Standard arrows / Range NPC projectiles
		PROJECTILE_STYLES.put(160, "Pray Magic"); // Standard spells / Magic NPC projectiles
	}

	// Comprehensive animation mappings (mainly for Melee/Instant attacks)
	private static final Map<Integer, Integer> MELEE_ANIMATIONS = new HashMap<>();
	static {
		MELEE_ANIMATIONS.put(2309, 4); // Abyssal demon
		MELEE_ANIMATIONS.put(1552, 4); // Bloodveld
		MELEE_ANIMATIONS.put(6964, 4); // Commander Zilyana Melee
		MELEE_ANIMATIONS.put(7060, 4); // General Graardor Melee
		MELEE_ANIMATIONS.put(6948, 4); // K'ril Tsutsaroth Melee
		MELEE_ANIMATIONS.put(4492, 4); // Cerberus Melee
		MELEE_ANIMATIONS.put(7226, 4); // Demonic Gorilla Melee

		// Fortis Colosseum Melee / Instant Animations
		MELEE_ANIMATIONS.put(10871, 4); // Jaguar Warrior Melee attack
		MELEE_ANIMATIONS.put(10872, 4); // Minotaur Melee attack
		MELEE_ANIMATIONS.put(10873, 4); // Fremennik Berserker Melee attack
		MELEE_ANIMATIONS.put(10874, 4); // Shockwave Colossus Melee attack
		MELEE_ANIMATIONS.put(10875, 4); // Sol Heredit Melee attack
	}

	@Provides
	MobCheckConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(MobCheckConfig.class);
	}

	@Override
	protected void startUp() {
		npcMeleeAttacks.clear();
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown() {
		npcMeleeAttacks.clear();
		overlayManager.remove(overlay);
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event) {
		if (!(event.getActor() instanceof NPC)) {
			return;
		}

		NPC npc = (NPC) event.getActor();
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null || npc.getInteracting() != localPlayer) {
			return;
		}

		int anim = npc.getAnimation();

		if (MELEE_ANIMATIONS.containsKey(anim)) {
			int warningTicks = MELEE_ANIMATIONS.get(anim);
			npcMeleeAttacks.put(npc.getIndex(),
					new AttackState(warningTicks, "Pray Melee", npc.getName()));
		}
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		// Countdown transient melee attacks
		npcMeleeAttacks.entrySet().removeIf(entry -> {
			entry.getValue().ticks--;
			return entry.getValue().ticks <= 0;
		});

		// Sound alert on priority change
		Optional<AttackState> priorityOpt = getPriorityAttack();
		if (priorityOpt.isPresent()) {
			AttackState priority = priorityOpt.get();
			if (config.playSoundAlert() && !priority.style.equals(lastPriorityStyle)) {
				client.playSoundEffect(config.soundEffectId());
			}
			lastPriorityStyle = priority.style;
		} else {
			lastPriorityStyle = "";
		}
	}

	public Optional<AttackState> getPriorityAttack() {
		List<AttackState> attacks = new ArrayList<>();
		Player localPlayer = client.getLocalPlayer();

		// Gather active projectiles targeting the player
		if (localPlayer != null) {
			for (Projectile projectile : client.getProjectiles()) {
				if (projectile.getTargetActor() == localPlayer) {
					String style = PROJECTILE_STYLES.get(projectile.getId());
					if (style == null && config.trackUnknownProjectiles()) {
						style = "Pray Magic";
					}

					if (style != null) {
						int ticksRemaining = (projectile.getRemainingCycles() + 29) / 30;
						if (ticksRemaining > 0) {
							attacks.add(new AttackState(ticksRemaining, style, "Incoming Projectile"));
						}
					}
				}
			}
		}

		// Gather active melee animation threats
		attacks.addAll(npcMeleeAttacks.values());

		// Return the threat with the lowest tick remaining (highest priority)
		return attacks.stream().min(Comparator.comparingInt(a -> a.ticks));
	}

}