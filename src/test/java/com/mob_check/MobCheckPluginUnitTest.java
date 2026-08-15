package com.mob_check;

import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.Projectile;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameTick;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MobCheckPluginUnitTest
{
	private MobCheckPlugin plugin;
	private Client client;
	private MobCheckConfig config;

	@Before
	public void setUp() throws Exception
	{
		plugin = new MobCheckPlugin();
		client = mock(Client.class);
		config = mock(MobCheckConfig.class);
		MobCheckOverlay overlay = mock(MobCheckOverlay.class);
		MobCheckPrayerWidgetOverlay prayerWidgetOverlay = mock(MobCheckPrayerWidgetOverlay.class);
		MobCheckWorldOverlay worldOverlay = mock(MobCheckWorldOverlay.class);
		net.runelite.client.ui.overlay.OverlayManager overlayManager = mock(net.runelite.client.ui.overlay.OverlayManager.class);

		// Default config mocks
		when(config.soundEffectId()).thenReturn(2266);
		when(config.magicSoundId()).thenReturn(2266);
		when(config.rangeSoundId()).thenReturn(2267);
		when(config.meleeSoundId()).thenReturn(2268);
		when(config.wrongPrayerSoundId()).thenReturn(2277);
		when(config.playWrongPrayerAlert()).thenReturn(false);

		// Inject private fields
		setPrivateField(plugin, "client", client);
		setPrivateField(plugin, "config", config);
		setPrivateField(plugin, "overlay", overlay);
		setPrivateField(plugin, "prayerWidgetOverlay", prayerWidgetOverlay);
		setPrivateField(plugin, "worldOverlay", worldOverlay);
		setPrivateField(plugin, "overlayManager", overlayManager);

		// Default: empty projectile deque
		net.runelite.api.Deque<Projectile> emptyDeque = createMockDeque(Collections.emptyList());
		when(client.getProjectiles()).thenReturn(emptyDeque);

		plugin.startUp();
	}

	private void setPrivateField(Object obj, String fieldName, Object value) throws Exception
	{
		Field field = obj.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(obj, value);
	}

	@SuppressWarnings("unchecked")
	private net.runelite.api.Deque<Projectile> createMockDeque(List<Projectile> projectiles)
	{
		net.runelite.api.Deque<Projectile> deque = mock(net.runelite.api.Deque.class);
		doAnswer(invocation -> projectiles.iterator()).when(deque).iterator();
		return deque;
	}

	@Test
	public void testGetPriorityAttackEmpty()
	{
		net.runelite.api.Deque<Projectile> deque = createMockDeque(Collections.emptyList());
		when(client.getProjectiles()).thenReturn(deque);
		Optional<MobCheckPlugin.AttackState> priority = plugin.getPriorityAttack();
		assertFalse(priority.isPresent());
	}

	@Test
	public void testInfernoJalZekAndJalXilProjectiles()
	{
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);

		// Jal-Zek (Magic)
		Projectile proj1 = mock(Projectile.class);
		when(proj1.getTargetActor()).thenReturn(player);
		when(proj1.getId()).thenReturn(1374);
		when(proj1.getRemainingCycles()).thenReturn(121); // (121 + 29) / 30 = 5 ticks

		// Jal-Xil (Range)
		Projectile proj2 = mock(Projectile.class);
		when(proj2.getTargetActor()).thenReturn(player);
		when(proj2.getId()).thenReturn(1376);
		when(proj2.getRemainingCycles()).thenReturn(61); // 3 ticks

		net.runelite.api.Deque<Projectile> deque = createMockDeque(List.of(proj1, proj2));
		when(client.getProjectiles()).thenReturn(deque);

		List<MobCheckPlugin.AttackState> attacks = plugin.getActiveAttacks();
		assertEquals(2, attacks.size());
		// Lowest tick first
		assertEquals("Pray Range", attacks.get(0).style);
		assertEquals(MobCheckPlugin.PrayerStyle.RANGE, attacks.get(0).prayerStyle);
		assertEquals("Jal-Xil", attacks.get(0).npcName);
		assertEquals(3, attacks.get(0).ticks);

		assertEquals("Pray Magic", attacks.get(1).style);
		assertEquals(MobCheckPlugin.PrayerStyle.MAGIC, attacks.get(1).prayerStyle);
		assertEquals("Jal-Zek", attacks.get(1).npcName);
		assertEquals(5, attacks.get(1).ticks);
	}

	@Test
	public void testInfernoBlobProjectiles()
	{
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);

		// Jal-Ak Blob Magic projectile (1380)
		Projectile projMage = mock(Projectile.class);
		when(projMage.getTargetActor()).thenReturn(player);
		when(projMage.getId()).thenReturn(1380);
		when(projMage.getRemainingCycles()).thenReturn(91); // 4 ticks

		// Jal-Ak Blob Range projectile (1378)
		Projectile projRange = mock(Projectile.class);
		when(projRange.getTargetActor()).thenReturn(player);
		when(projRange.getId()).thenReturn(1378);
		when(projRange.getRemainingCycles()).thenReturn(31); // 2 ticks

		net.runelite.api.Deque<Projectile> deque = createMockDeque(List.of(projMage, projRange));
		when(client.getProjectiles()).thenReturn(deque);

		Optional<MobCheckPlugin.AttackState> priority = plugin.getPriorityAttack();
		assertTrue(priority.isPresent());
		assertEquals("Pray Range", priority.get().style);
		assertEquals("Jal-Ak (Range)", priority.get().npcName);
		assertEquals(2, priority.get().ticks);
	}

	@Test
	public void testInfernoMeleeAnimations()
	{
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);

		NPC meleer = mock(NPC.class);
		when(meleer.getIndex()).thenReturn(101);
		when(meleer.getName()).thenReturn("Jal-ImKot");
		when(meleer.getAnimation()).thenReturn(7597); // Jal-ImKot melee
		when(meleer.getInteracting()).thenReturn(player);

		AnimationChanged anim = new AnimationChanged();
		anim.setActor(meleer);
		plugin.onAnimationChanged(anim);

		Optional<MobCheckPlugin.AttackState> priority = plugin.getPriorityAttack();
		assertTrue(priority.isPresent());
		assertEquals("Pray Melee", priority.get().style);
		assertEquals(MobCheckPlugin.PrayerStyle.MELEE, priority.get().prayerStyle);
		assertEquals("Jal-ImKot", priority.get().npcName);
		assertEquals(4, priority.get().ticks);
	}

	@Test
	public void testFortisColosseumManticoreSequence()
	{
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);

		// Manticore fires Magic projectile (2687) landing in 1 tick
		Projectile manticoreMage = mock(Projectile.class);
		when(manticoreMage.getTargetActor()).thenReturn(player);
		when(manticoreMage.getId()).thenReturn(2687);
		when(manticoreMage.getRemainingCycles()).thenReturn(15); // 1 tick

		// Manticore fires Range projectile (2688) landing in 2 ticks
		Projectile manticoreRange = mock(Projectile.class);
		when(manticoreRange.getTargetActor()).thenReturn(player);
		when(manticoreRange.getId()).thenReturn(2688);
		when(manticoreRange.getRemainingCycles()).thenReturn(45); // 2 ticks

		net.runelite.api.Deque<Projectile> deque = createMockDeque(List.of(manticoreRange, manticoreMage));
		when(client.getProjectiles()).thenReturn(deque);

		List<MobCheckPlugin.AttackState> attacks = plugin.getActiveAttacks();
		assertEquals(2, attacks.size());
		// Sorted in order: Mage (1t) -> Range (2t)
		assertEquals("Pray Magic", attacks.get(0).style);
		assertEquals(1, attacks.get(0).ticks);
		assertTrue(attacks.get(0).isManticoreCombo);

		assertEquals("Pray Range", attacks.get(1).style);
		assertEquals(2, attacks.get(1).ticks);
		assertTrue(attacks.get(1).isManticoreCombo);
	}

	@Test
	public void testSoundAlertOnPriorityChangeDistinctStyles()
	{
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);
		when(config.playSoundAlert()).thenReturn(true);
		when(config.meleeSoundId()).thenReturn(2268);
		when(config.magicSoundId()).thenReturn(2266);

		NPC npc = mock(NPC.class);
		when(npc.getIndex()).thenReturn(789);
		when(npc.getName()).thenReturn("Jal-ImKot");
		when(npc.getAnimation()).thenReturn(7597);
		when(npc.getInteracting()).thenReturn(player);

		AnimationChanged animationChanged = new AnimationChanged();
		animationChanged.setActor(npc);
		plugin.onAnimationChanged(animationChanged);

		plugin.onGameTick(new GameTick());
		verify(client, times(1)).playSoundEffect(2268);

		// Subsequent tick with same style shouldn't repeat sound
		plugin.onGameTick(new GameTick());
		verify(client, times(1)).playSoundEffect(2268);
	}

	@Test
	public void testEmergencySoundAlertWhenUnprotected()
	{
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);
		when(config.playSoundAlert()).thenReturn(true);
		when(config.playWrongPrayerAlert()).thenReturn(true);
		when(config.magicSoundId()).thenReturn(2266);
		when(config.wrongPrayerSoundId()).thenReturn(2277);

		// Incoming magic projectile in 1 tick
		Projectile proj = mock(Projectile.class);
		when(proj.getTargetActor()).thenReturn(player);
		when(proj.getId()).thenReturn(1374); // Jal-Zek
		when(proj.getRemainingCycles()).thenReturn(15); // 1 tick

		net.runelite.api.Deque<Projectile> deque = createMockDeque(List.of(proj));
		when(client.getProjectiles()).thenReturn(deque);

		// Player is NOT praying magic
		when(client.isPrayerActive(Prayer.PROTECT_FROM_MAGIC)).thenReturn(false);

		plugin.onGameTick(new GameTick());

		// Style sound (2266) + emergency sound (2277)
		verify(client, times(1)).playSoundEffect(2266);
		verify(client, times(1)).playSoundEffect(2277);
	}

	@Test
	public void testGetPriorityAttackWithUnknownProjectile()
	{
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);

		Projectile projectile = mock(Projectile.class);
		when(projectile.getTargetActor()).thenReturn(player);
		when(projectile.getId()).thenReturn(99999);
		when(projectile.getRemainingCycles()).thenReturn(61);

		net.runelite.api.Deque<Projectile> deque = createMockDeque(List.of(projectile));
		when(client.getProjectiles()).thenReturn(deque);

		when(config.trackUnknownProjectiles()).thenReturn(false);
		assertFalse(plugin.getPriorityAttack().isPresent());

		when(config.trackUnknownProjectiles()).thenReturn(true);
		assertTrue(plugin.getPriorityAttack().isPresent());
		assertEquals("Pray Magic", plugin.getPriorityAttack().get().style);
	}
}

