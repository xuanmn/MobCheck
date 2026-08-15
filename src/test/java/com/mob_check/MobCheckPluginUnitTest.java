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
	public void testThreeWaySimultaneousThreatsSortedCorrectly()
	{
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);

		// 1. Magic projectile in 4 ticks
		Projectile mageProj = mock(Projectile.class);
		when(mageProj.getTargetActor()).thenReturn(player);
		when(mageProj.getId()).thenReturn(1374); // Jal-Zek
		when(mageProj.getRemainingCycles()).thenReturn(91); // 4 ticks

		// 2. Range projectile in 2 ticks
		Projectile rangeProj = mock(Projectile.class);
		when(rangeProj.getTargetActor()).thenReturn(player);
		when(rangeProj.getId()).thenReturn(1376); // Jal-Xil
		when(rangeProj.getRemainingCycles()).thenReturn(35); // 2 ticks

		net.runelite.api.Deque<Projectile> deque = createMockDeque(List.of(mageProj, rangeProj));
		when(client.getProjectiles()).thenReturn(deque);

		// 3. Melee attack in 1 tick
		NPC meleer = mock(NPC.class);
		when(meleer.getIndex()).thenReturn(55);
		when(meleer.getName()).thenReturn("Jal-ImKot");
		when(meleer.getAnimation()).thenReturn(7597);
		when(meleer.getInteracting()).thenReturn(player);

		AnimationChanged anim = new AnimationChanged();
		anim.setActor(meleer);
		plugin.onAnimationChanged(anim);

		// Advance 3 game ticks so melee is down to 1 tick
		plugin.onGameTick(new GameTick());
		plugin.onGameTick(new GameTick());
		plugin.onGameTick(new GameTick());

		List<MobCheckPlugin.AttackState> attacks = plugin.getActiveAttacks();
		assertEquals(3, attacks.size());

		// Priority 1: Melee (1t)
		assertEquals(MobCheckPlugin.PrayerStyle.MELEE, attacks.get(0).prayerStyle);
		assertEquals(1, attacks.get(0).ticks);

		// Priority 2: Range (2t)
		assertEquals(MobCheckPlugin.PrayerStyle.RANGE, attacks.get(1).prayerStyle);
		assertEquals(2, attacks.get(1).ticks);

		// Priority 3: Magic (4t)
		assertEquals(MobCheckPlugin.PrayerStyle.MAGIC, attacks.get(2).prayerStyle);
		assertEquals(4, attacks.get(2).ticks);
	}

	@Test
	public void testJalTokJadAttacks()
	{
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);

		// Jad Magic (448)
		Projectile jadMage = mock(Projectile.class);
		when(jadMage.getTargetActor()).thenReturn(player);
		when(jadMage.getId()).thenReturn(448);
		when(jadMage.getRemainingCycles()).thenReturn(70); // 3 ticks

		net.runelite.api.Deque<Projectile> deque = createMockDeque(List.of(jadMage));
		when(client.getProjectiles()).thenReturn(deque);

		Optional<MobCheckPlugin.AttackState> priority = plugin.getPriorityAttack();
		assertTrue(priority.isPresent());
		assertEquals("Pray Magic", priority.get().style);
		assertEquals(MobCheckPlugin.PrayerStyle.MAGIC, priority.get().prayerStyle);
		assertEquals("JalTok-Jad (Magic)", priority.get().npcName);

		// Jad Melee Animation (7590)
		NPC jad = mock(NPC.class);
		when(jad.getIndex()).thenReturn(900);
		when(jad.getName()).thenReturn("JalTok-Jad");
		when(jad.getAnimation()).thenReturn(7590);
		when(jad.getInteracting()).thenReturn(player);

		AnimationChanged anim = new AnimationChanged();
		anim.setActor(jad);
		plugin.onAnimationChanged(anim);

		List<MobCheckPlugin.AttackState> attacks = plugin.getActiveAttacks();
		assertTrue(attacks.stream().anyMatch(a -> a.prayerStyle == MobCheckPlugin.PrayerStyle.MELEE && a.npcName.equals("JalTok-Jad")));
	}

	@Test
	public void testColosseumEnemiesCoverage()
	{
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);

		// Serpent Shaman (2685)
		Projectile shamanProj = mock(Projectile.class);
		when(shamanProj.getTargetActor()).thenReturn(player);
		when(shamanProj.getId()).thenReturn(2685);
		when(shamanProj.getRemainingCycles()).thenReturn(45);

		// Javelinic Colossus (2686)
		Projectile javelinProj = mock(Projectile.class);
		when(javelinProj.getTargetActor()).thenReturn(player);
		when(javelinProj.getId()).thenReturn(2686);
		when(javelinProj.getRemainingCycles()).thenReturn(65);

		// Sol Heredit Magic (2689)
		Projectile solProj = mock(Projectile.class);
		when(solProj.getTargetActor()).thenReturn(player);
		when(solProj.getId()).thenReturn(2689);
		when(solProj.getRemainingCycles()).thenReturn(85);

		net.runelite.api.Deque<Projectile> deque = createMockDeque(List.of(shamanProj, javelinProj, solProj));
		when(client.getProjectiles()).thenReturn(deque);

		List<MobCheckPlugin.AttackState> attacks = plugin.getActiveAttacks();
		assertEquals(3, attacks.size());
		assertEquals("Serpent Shaman", attacks.get(0).npcName);
		assertEquals(MobCheckPlugin.PrayerStyle.MAGIC, attacks.get(0).prayerStyle);

		assertEquals("Javelinic Colossus", attacks.get(1).npcName);
		assertEquals(MobCheckPlugin.PrayerStyle.RANGE, attacks.get(1).prayerStyle);

		assertEquals("Sol Heredit", attacks.get(2).npcName);
		assertEquals(MobCheckPlugin.PrayerStyle.MAGIC, attacks.get(2).prayerStyle);
	}

	@Test
	public void testBossCoverageZulrahVorkathCerberusHydraHunllef()
	{
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);

		// Zulrah Range (1046)
		Projectile zulrah = mock(Projectile.class);
		when(zulrah.getTargetActor()).thenReturn(player);
		when(zulrah.getId()).thenReturn(1046);
		when(zulrah.getRemainingCycles()).thenReturn(30);

		// Vorkath Magic (1481)
		Projectile vorkath = mock(Projectile.class);
		when(vorkath.getTargetActor()).thenReturn(player);
		when(vorkath.getId()).thenReturn(1481);
		when(vorkath.getRemainingCycles()).thenReturn(40);

		// Cerberus Range (1244)
		Projectile cerberus = mock(Projectile.class);
		when(cerberus.getTargetActor()).thenReturn(player);
		when(cerberus.getId()).thenReturn(1244);
		when(cerberus.getRemainingCycles()).thenReturn(50);

		// Hydra Magic (1662)
		Projectile hydra = mock(Projectile.class);
		when(hydra.getTargetActor()).thenReturn(player);
		when(hydra.getId()).thenReturn(1662);
		when(hydra.getRemainingCycles()).thenReturn(60);

		// Hunllef Range (1708)
		Projectile hunllef = mock(Projectile.class);
		when(hunllef.getTargetActor()).thenReturn(player);
		when(hunllef.getId()).thenReturn(1708);
		when(hunllef.getRemainingCycles()).thenReturn(70);

		// Demonic Gorilla Magic (1302)
		Projectile gorilla = mock(Projectile.class);
		when(gorilla.getTargetActor()).thenReturn(player);
		when(gorilla.getId()).thenReturn(1302);
		when(gorilla.getRemainingCycles()).thenReturn(80);

		net.runelite.api.Deque<Projectile> deque = createMockDeque(List.of(zulrah, vorkath, cerberus, hydra, hunllef, gorilla));
		when(client.getProjectiles()).thenReturn(deque);

		List<MobCheckPlugin.AttackState> attacks = plugin.getActiveAttacks();
		assertEquals(6, attacks.size());
		assertEquals("Zulrah", attacks.get(0).npcName);
		assertEquals("Vorkath", attacks.get(1).npcName);
		assertEquals("Cerberus", attacks.get(2).npcName);
		assertEquals("Alchemical Hydra", attacks.get(3).npcName);
		assertEquals("Hunllef", attacks.get(4).npcName);
		assertEquals("Demonic Gorilla", attacks.get(5).npcName);
	}

	@Test
	public void testIgnoreProjectilesAndAnimationsNotTargetingPlayer()
	{
		Player player = mock(Player.class);
		Player otherPlayer = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);

		// Projectile targeting another player
		Projectile proj = mock(Projectile.class);
		when(proj.getTargetActor()).thenReturn(otherPlayer);
		when(proj.getId()).thenReturn(1374);
		when(proj.getRemainingCycles()).thenReturn(45);

		net.runelite.api.Deque<Projectile> deque = createMockDeque(List.of(proj));
		when(client.getProjectiles()).thenReturn(deque);

		// NPC meleeing another player
		NPC npc = mock(NPC.class);
		when(npc.getIndex()).thenReturn(99);
		when(npc.getAnimation()).thenReturn(7597);
		when(npc.getInteracting()).thenReturn(otherPlayer);

		AnimationChanged anim = new AnimationChanged();
		anim.setActor(npc);
		plugin.onAnimationChanged(anim);

		// Neither should be counted
		assertTrue(plugin.getActiveAttacks().isEmpty());
	}

	@Test
	public void testIsPrayerProtectedHelper()
	{
		when(client.isPrayerActive(Prayer.PROTECT_FROM_MAGIC)).thenReturn(true);
		when(client.isPrayerActive(Prayer.PROTECT_FROM_MISSILES)).thenReturn(false);
		when(client.isPrayerActive(Prayer.PROTECT_FROM_MELEE)).thenReturn(false);

		assertTrue(plugin.isPrayerProtected(MobCheckPlugin.PrayerStyle.MAGIC));
		assertFalse(plugin.isPrayerProtected(MobCheckPlugin.PrayerStyle.RANGE));
		assertFalse(plugin.isPrayerProtected(MobCheckPlugin.PrayerStyle.MELEE));
	}

	@Test
	public void testShutDownCleansUp()
	{
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);

		NPC npc = mock(NPC.class);
		when(npc.getIndex()).thenReturn(12);
		when(npc.getName()).thenReturn("Jal-ImKot");
		when(npc.getAnimation()).thenReturn(7597);
		when(npc.getInteracting()).thenReturn(player);

		AnimationChanged anim = new AnimationChanged();
		anim.setActor(npc);
		plugin.onAnimationChanged(anim);

		assertFalse(plugin.getActiveAttacks().isEmpty());

		plugin.shutDown();
		assertTrue(plugin.getActiveAttacks().isEmpty());
	}
}

