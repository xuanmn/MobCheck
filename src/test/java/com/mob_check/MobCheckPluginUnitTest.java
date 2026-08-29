package com.mob_check;

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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("deprecation")
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

	/**
	 * Helper: triggers onGameTick to rebuild the cached attack list.
	 * Must be called after setting up projectiles/animations before reading getActiveAttacks().
	 */
	private void tickAndRefresh()
	{
		plugin.onGameTick(new GameTick());
	}

	@Test
	public void testGetPriorityAttackEmpty()
	{
		net.runelite.api.Deque<Projectile> deque = createMockDeque(Collections.emptyList());
		when(client.getProjectiles()).thenReturn(deque);
		tickAndRefresh();
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
		when(proj1.getId()).thenReturn(MobCheckPlugin.ProjectileID.JAL_ZEK_MAGIC);
		when(proj1.getRemainingCycles()).thenReturn(121); // (121 + 29) / 30 = 5 ticks

		// Jal-Xil (Range)
		Projectile proj2 = mock(Projectile.class);
		when(proj2.getTargetActor()).thenReturn(player);
		when(proj2.getId()).thenReturn(MobCheckPlugin.ProjectileID.JAL_XIL_RANGE);
		when(proj2.getRemainingCycles()).thenReturn(61); // 3 ticks

		net.runelite.api.Deque<Projectile> deque = createMockDeque(List.of(proj1, proj2));
		when(client.getProjectiles()).thenReturn(deque);

		tickAndRefresh();
		List<MobCheckPlugin.AttackState> attacks = plugin.getActiveAttacks();
		assertEquals(2, attacks.size());
		// Lowest tick first
		assertEquals(MobCheckPlugin.PrayerStyle.RANGE, attacks.get(0).prayerStyle);
		assertEquals("Pray Range", attacks.get(0).getStyleDisplayName());
		assertEquals("Jal-Xil", attacks.get(0).npcName);
		assertEquals(3, attacks.get(0).ticks);

		assertEquals(MobCheckPlugin.PrayerStyle.MAGIC, attacks.get(1).prayerStyle);
		assertEquals("Pray Magic", attacks.get(1).getStyleDisplayName());
		assertEquals("Jal-Zek", attacks.get(1).npcName);
		assertEquals(5, attacks.get(1).ticks);
	}

	@Test
	public void testInfernoBlobProjectiles()
	{
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);

		// Jal-Ak Blob Magic projectile
		Projectile projMage = mock(Projectile.class);
		when(projMage.getTargetActor()).thenReturn(player);
		when(projMage.getId()).thenReturn(MobCheckPlugin.ProjectileID.JAL_AK_MAGIC);
		when(projMage.getRemainingCycles()).thenReturn(91); // 4 ticks

		// Jal-Ak Blob Range projectile
		Projectile projRange = mock(Projectile.class);
		when(projRange.getTargetActor()).thenReturn(player);
		when(projRange.getId()).thenReturn(MobCheckPlugin.ProjectileID.JAL_AK_RANGE);
		when(projRange.getRemainingCycles()).thenReturn(31); // 2 ticks

		net.runelite.api.Deque<Projectile> deque = createMockDeque(List.of(projMage, projRange));
		when(client.getProjectiles()).thenReturn(deque);

		tickAndRefresh();
		Optional<MobCheckPlugin.AttackState> priority = plugin.getPriorityAttack();
		assertTrue(priority.isPresent());
		assertEquals("Pray Range", priority.get().getStyleDisplayName());
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
		when(meleer.getAnimation()).thenReturn(MobCheckPlugin.AnimationID.JAL_IMKOT_MELEE);
		when(meleer.getInteracting()).thenReturn(player);

		AnimationChanged anim = new AnimationChanged();
		anim.setActor(meleer);
		plugin.onAnimationChanged(anim);

		tickAndRefresh();
		Optional<MobCheckPlugin.AttackState> priority = plugin.getPriorityAttack();
		assertTrue(priority.isPresent());
		assertEquals("Pray Melee", priority.get().getStyleDisplayName());
		assertEquals(MobCheckPlugin.PrayerStyle.MELEE, priority.get().prayerStyle);
		assertEquals("Jal-ImKot", priority.get().npcName);
		// #3: After one tick decrement, melee attack registered at 4 should be at 3
		assertEquals(3, priority.get().ticks);
	}

	@Test
	public void testMeleeAttackVisibleAtZeroTicks()
	{
		// #3: Verify the off-by-one fix — melee attacks should remain visible at 0t
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);

		NPC meleer = mock(NPC.class);
		when(meleer.getIndex()).thenReturn(101);
		when(meleer.getName()).thenReturn("Jal-ImKot");
		when(meleer.getAnimation()).thenReturn(MobCheckPlugin.AnimationID.JAL_IMKOT_MELEE);
		when(meleer.getInteracting()).thenReturn(player);

		AnimationChanged anim = new AnimationChanged();
		anim.setActor(meleer);
		plugin.onAnimationChanged(anim);

		// Tick down 4 times: 4 -> 3 -> 2 -> 1 -> 0
		tickAndRefresh();
		tickAndRefresh();
		tickAndRefresh();
		tickAndRefresh();

		// At 0t the attack should still be visible (the impact tick)
		List<MobCheckPlugin.AttackState> attacks = plugin.getActiveAttacks();
		assertTrue("Melee attack should still be visible at 0t", attacks.stream()
			.anyMatch(a -> a.prayerStyle == MobCheckPlugin.PrayerStyle.MELEE && a.ticks == 0));

		// One more tick and it should be gone
		tickAndRefresh();
		attacks = plugin.getActiveAttacks();
		assertFalse("Melee attack should be removed after 0t", attacks.stream()
			.anyMatch(a -> a.prayerStyle == MobCheckPlugin.PrayerStyle.MELEE && a.npcName.equals("Jal-ImKot")));
	}

	@Test
	public void testFortisColosseumManticoreSequence()
	{
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);

		// Manticore fires Magic projectile landing in 1 tick
		Projectile manticoreMage = mock(Projectile.class);
		when(manticoreMage.getTargetActor()).thenReturn(player);
		when(manticoreMage.getId()).thenReturn(MobCheckPlugin.ProjectileID.MANTICORE_MAGIC);
		when(manticoreMage.getRemainingCycles()).thenReturn(15); // 1 tick

		// Manticore fires Range projectile landing in 2 ticks
		Projectile manticoreRange = mock(Projectile.class);
		when(manticoreRange.getTargetActor()).thenReturn(player);
		when(manticoreRange.getId()).thenReturn(MobCheckPlugin.ProjectileID.MANTICORE_RANGE);
		when(manticoreRange.getRemainingCycles()).thenReturn(45); // 2 ticks

		net.runelite.api.Deque<Projectile> deque = createMockDeque(List.of(manticoreRange, manticoreMage));
		when(client.getProjectiles()).thenReturn(deque);

		tickAndRefresh();
		List<MobCheckPlugin.AttackState> attacks = plugin.getActiveAttacks();
		assertEquals(2, attacks.size());
		// Sorted in order: Mage (1t) -> Range (2t)
		assertEquals("Pray Magic", attacks.get(0).getStyleDisplayName());
		assertEquals(1, attacks.get(0).ticks);
		assertTrue(attacks.get(0).isManticoreCombo);

		assertEquals("Pray Range", attacks.get(1).getStyleDisplayName());
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
		when(npc.getAnimation()).thenReturn(MobCheckPlugin.AnimationID.JAL_IMKOT_MELEE);
		when(npc.getInteracting()).thenReturn(player);

		AnimationChanged animationChanged = new AnimationChanged();
		animationChanged.setActor(npc);
		plugin.onAnimationChanged(animationChanged);

		tickAndRefresh();
		verify(client, times(1)).playSoundEffect(2268);

		// Subsequent tick with same style shouldn't repeat sound
		tickAndRefresh();
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
		when(proj.getId()).thenReturn(MobCheckPlugin.ProjectileID.JAL_ZEK_MAGIC);
		when(proj.getRemainingCycles()).thenReturn(15); // 1 tick

		net.runelite.api.Deque<Projectile> deque = createMockDeque(List.of(proj));
		when(client.getProjectiles()).thenReturn(deque);

		// Player is NOT praying magic
		when(client.isPrayerActive(Prayer.PROTECT_FROM_MAGIC)).thenReturn(false);

		tickAndRefresh();

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
		when(mageProj.getId()).thenReturn(MobCheckPlugin.ProjectileID.JAL_ZEK_MAGIC);
		when(mageProj.getRemainingCycles()).thenReturn(91); // 4 ticks

		// 2. Range projectile in 2 ticks
		Projectile rangeProj = mock(Projectile.class);
		when(rangeProj.getTargetActor()).thenReturn(player);
		when(rangeProj.getId()).thenReturn(MobCheckPlugin.ProjectileID.JAL_XIL_RANGE);
		when(rangeProj.getRemainingCycles()).thenReturn(35); // 2 ticks

		net.runelite.api.Deque<Projectile> deque = createMockDeque(List.of(mageProj, rangeProj));
		when(client.getProjectiles()).thenReturn(deque);

		// 3. Melee attack in 1 tick (will be 4 initially, tick down 3 times)
		NPC meleer = mock(NPC.class);
		when(meleer.getIndex()).thenReturn(55);
		when(meleer.getName()).thenReturn("Jal-ImKot");
		when(meleer.getAnimation()).thenReturn(MobCheckPlugin.AnimationID.JAL_IMKOT_MELEE);
		when(meleer.getInteracting()).thenReturn(player);

		AnimationChanged anim = new AnimationChanged();
		anim.setActor(meleer);
		plugin.onAnimationChanged(anim);

		// Advance 3 game ticks so melee is down to 1 tick
		tickAndRefresh();
		tickAndRefresh();
		tickAndRefresh();

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

		// Jad Magic
		Projectile jadMage = mock(Projectile.class);
		when(jadMage.getTargetActor()).thenReturn(player);
		when(jadMage.getId()).thenReturn(MobCheckPlugin.ProjectileID.JALTOK_JAD_MAGIC);
		when(jadMage.getRemainingCycles()).thenReturn(70); // 3 ticks

		net.runelite.api.Deque<Projectile> deque = createMockDeque(List.of(jadMage));
		when(client.getProjectiles()).thenReturn(deque);

		tickAndRefresh();
		Optional<MobCheckPlugin.AttackState> priority = plugin.getPriorityAttack();
		assertTrue(priority.isPresent());
		assertEquals("Pray Magic", priority.get().getStyleDisplayName());
		assertEquals(MobCheckPlugin.PrayerStyle.MAGIC, priority.get().prayerStyle);
		assertEquals("JalTok-Jad (Magic)", priority.get().npcName);

		// Jad Melee Animation
		NPC jad = mock(NPC.class);
		when(jad.getIndex()).thenReturn(900);
		when(jad.getName()).thenReturn("JalTok-Jad");
		when(jad.getAnimation()).thenReturn(MobCheckPlugin.AnimationID.JALTOK_JAD_MELEE);
		when(jad.getInteracting()).thenReturn(player);

		AnimationChanged anim = new AnimationChanged();
		anim.setActor(jad);
		plugin.onAnimationChanged(anim);

		tickAndRefresh();
		List<MobCheckPlugin.AttackState> attacks = plugin.getActiveAttacks();
		assertTrue(attacks.stream().anyMatch(a -> a.prayerStyle == MobCheckPlugin.PrayerStyle.MELEE && a.npcName.equals("JalTok-Jad")));
	}

	@Test
	public void testColosseumEnemiesCoverage()
	{
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);

		// Serpent Shaman
		Projectile shamanProj = mock(Projectile.class);
		when(shamanProj.getTargetActor()).thenReturn(player);
		when(shamanProj.getId()).thenReturn(MobCheckPlugin.ProjectileID.SERPENT_SHAMAN_MAGIC);
		when(shamanProj.getRemainingCycles()).thenReturn(45);

		// Javelinic Colossus
		Projectile javelinProj = mock(Projectile.class);
		when(javelinProj.getTargetActor()).thenReturn(player);
		when(javelinProj.getId()).thenReturn(MobCheckPlugin.ProjectileID.JAVELINIC_COLOSSUS_RANGE);
		when(javelinProj.getRemainingCycles()).thenReturn(65);

		// Sol Heredit Magic
		Projectile solProj = mock(Projectile.class);
		when(solProj.getTargetActor()).thenReturn(player);
		when(solProj.getId()).thenReturn(MobCheckPlugin.ProjectileID.SOL_HEREDIT_MAGIC);
		when(solProj.getRemainingCycles()).thenReturn(85);

		net.runelite.api.Deque<Projectile> deque = createMockDeque(List.of(shamanProj, javelinProj, solProj));
		when(client.getProjectiles()).thenReturn(deque);

		tickAndRefresh();
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

		// Zulrah Range
		Projectile zulrah = mock(Projectile.class);
		when(zulrah.getTargetActor()).thenReturn(player);
		when(zulrah.getId()).thenReturn(MobCheckPlugin.ProjectileID.ZULRAH_RANGE);
		when(zulrah.getRemainingCycles()).thenReturn(30);

		// Vorkath Magic
		Projectile vorkath = mock(Projectile.class);
		when(vorkath.getTargetActor()).thenReturn(player);
		when(vorkath.getId()).thenReturn(MobCheckPlugin.ProjectileID.VORKATH_MAGIC);
		when(vorkath.getRemainingCycles()).thenReturn(40);

		// Cerberus Range
		Projectile cerberus = mock(Projectile.class);
		when(cerberus.getTargetActor()).thenReturn(player);
		when(cerberus.getId()).thenReturn(MobCheckPlugin.ProjectileID.CERBERUS_RANGE);
		when(cerberus.getRemainingCycles()).thenReturn(50);

		// Hydra Magic
		Projectile hydra = mock(Projectile.class);
		when(hydra.getTargetActor()).thenReturn(player);
		when(hydra.getId()).thenReturn(MobCheckPlugin.ProjectileID.HYDRA_MAGIC);
		when(hydra.getRemainingCycles()).thenReturn(60);

		// Hunllef Range
		Projectile hunllef = mock(Projectile.class);
		when(hunllef.getTargetActor()).thenReturn(player);
		when(hunllef.getId()).thenReturn(MobCheckPlugin.ProjectileID.HUNLLEF_RANGE);
		when(hunllef.getRemainingCycles()).thenReturn(70);

		// Demonic Gorilla Magic
		Projectile gorilla = mock(Projectile.class);
		when(gorilla.getTargetActor()).thenReturn(player);
		when(gorilla.getId()).thenReturn(MobCheckPlugin.ProjectileID.DEMONIC_GORILLA_MAGIC);
		when(gorilla.getRemainingCycles()).thenReturn(80);

		net.runelite.api.Deque<Projectile> deque = createMockDeque(List.of(zulrah, vorkath, cerberus, hydra, hunllef, gorilla));
		when(client.getProjectiles()).thenReturn(deque);

		tickAndRefresh();
		List<MobCheckPlugin.AttackState> attacks = plugin.getActiveAttacks();
		assertEquals(6, attacks.size());
		assertEquals("Zulrah", attacks.get(0).npcName);
		assertEquals("Vorkath", attacks.get(1).npcName);
		assertEquals("Cerberus", attacks.get(2).npcName);
		assertEquals("Alchemical Hydra", attacks.get(3).npcName);
		assertEquals(MobCheckPlugin.PrayerStyle.MAGIC, attacks.get(3).prayerStyle);
		assertEquals("Hunllef", attacks.get(4).npcName);
		assertEquals("Demonic Gorilla", attacks.get(5).npcName);
	}

	@Test
	public void testPhantomMuspahAttacksAndMeleeAnimation()
	{
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);

		// Phantom Muspah Magic Projectile
		Projectile muspahMage = mock(Projectile.class);
		when(muspahMage.getTargetActor()).thenReturn(player);
		when(muspahMage.getId()).thenReturn(MobCheckPlugin.ProjectileID.MUSPAH_MAGIC);
		when(muspahMage.getRemainingCycles()).thenReturn(60); // 2 ticks

		// Phantom Muspah Range Projectile
		Projectile muspahRange = mock(Projectile.class);
		when(muspahRange.getTargetActor()).thenReturn(player);
		when(muspahRange.getId()).thenReturn(MobCheckPlugin.ProjectileID.MUSPAH_RANGE);
		when(muspahRange.getRemainingCycles()).thenReturn(90); // 3 ticks

		net.runelite.api.Deque<Projectile> deque = createMockDeque(List.of(muspahMage, muspahRange));
		when(client.getProjectiles()).thenReturn(deque);

		tickAndRefresh();
		List<MobCheckPlugin.AttackState> attacks = plugin.getActiveAttacks();
		assertEquals(2, attacks.size());
		assertEquals("Phantom Muspah (Magic)", attacks.get(0).npcName);
		assertEquals(MobCheckPlugin.PrayerStyle.MAGIC, attacks.get(0).prayerStyle);
		assertEquals(2, attacks.get(0).ticks);

		assertEquals("Phantom Muspah (Range)", attacks.get(1).npcName);
		assertEquals(MobCheckPlugin.PrayerStyle.RANGE, attacks.get(1).prayerStyle);
		assertEquals(3, attacks.get(1).ticks);

		// Muspah Melee Animation
		NPC muspah = mock(NPC.class);
		when(muspah.getIndex()).thenReturn(701);
		when(muspah.getName()).thenReturn("Phantom Muspah");
		when(muspah.getAnimation()).thenReturn(MobCheckPlugin.AnimationID.MUSPAH_MELEE_SWIPE);
		when(muspah.getInteracting()).thenReturn(player);

		AnimationChanged anim = new AnimationChanged();
		anim.setActor(muspah);
		plugin.onAnimationChanged(anim);

		tickAndRefresh();
		List<MobCheckPlugin.AttackState> allAttacks = plugin.getActiveAttacks();
		assertTrue(allAttacks.stream().anyMatch(a -> a.prayerStyle == MobCheckPlugin.PrayerStyle.MELEE && a.npcName.equals("Phantom Muspah")));
	}

	@Test
	public void testTormentedDemonsAttacksAndMeleeAnimation()
	{
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);

		// Tormented Demon Magic Projectile
		Projectile tdMage = mock(Projectile.class);
		when(tdMage.getTargetActor()).thenReturn(player);
		when(tdMage.getId()).thenReturn(MobCheckPlugin.ProjectileID.TORMENTED_DEMON_MAGIC);
		when(tdMage.getRemainingCycles()).thenReturn(30); // 1 tick

		// Tormented Demon Range Projectile
		Projectile tdRange = mock(Projectile.class);
		when(tdRange.getTargetActor()).thenReturn(player);
		when(tdRange.getId()).thenReturn(MobCheckPlugin.ProjectileID.TORMENTED_DEMON_RANGE);
		when(tdRange.getRemainingCycles()).thenReturn(75); // 3 ticks

		net.runelite.api.Deque<Projectile> deque = createMockDeque(List.of(tdMage, tdRange));
		when(client.getProjectiles()).thenReturn(deque);

		tickAndRefresh();
		List<MobCheckPlugin.AttackState> attacks = plugin.getActiveAttacks();
		assertEquals(2, attacks.size());
		assertEquals("Tormented Demon (Magic)", attacks.get(0).npcName);
		assertEquals(MobCheckPlugin.PrayerStyle.MAGIC, attacks.get(0).prayerStyle);
		assertEquals(1, attacks.get(0).ticks);

		assertEquals("Tormented Demon (Range)", attacks.get(1).npcName);
		assertEquals(MobCheckPlugin.PrayerStyle.RANGE, attacks.get(1).prayerStyle);
		assertEquals(3, attacks.get(1).ticks);

		// Tormented Demon Melee Animation
		NPC td = mock(NPC.class);
		when(td.getIndex()).thenReturn(801);
		when(td.getName()).thenReturn("Tormented Demon");
		when(td.getAnimation()).thenReturn(MobCheckPlugin.AnimationID.TORMENTED_DEMON_MELEE);
		when(td.getInteracting()).thenReturn(player);

		AnimationChanged anim = new AnimationChanged();
		anim.setActor(td);
		plugin.onAnimationChanged(anim);

		tickAndRefresh();
		List<MobCheckPlugin.AttackState> allAttacks = plugin.getActiveAttacks();
		assertTrue(allAttacks.stream().anyMatch(a -> a.prayerStyle == MobCheckPlugin.PrayerStyle.MELEE && a.npcName.equals("Tormented Demon")));
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
		when(proj.getId()).thenReturn(MobCheckPlugin.ProjectileID.JAL_ZEK_MAGIC);
		when(proj.getRemainingCycles()).thenReturn(45);

		net.runelite.api.Deque<Projectile> deque = createMockDeque(List.of(proj));
		when(client.getProjectiles()).thenReturn(deque);

		// NPC meleeing another player
		NPC npc = mock(NPC.class);
		when(npc.getIndex()).thenReturn(99);
		when(npc.getAnimation()).thenReturn(MobCheckPlugin.AnimationID.JAL_IMKOT_MELEE);
		when(npc.getInteracting()).thenReturn(otherPlayer);

		AnimationChanged anim = new AnimationChanged();
		anim.setActor(npc);
		plugin.onAnimationChanged(anim);

		tickAndRefresh();
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
		when(npc.getAnimation()).thenReturn(MobCheckPlugin.AnimationID.JAL_IMKOT_MELEE);
		when(npc.getInteracting()).thenReturn(player);

		AnimationChanged anim = new AnimationChanged();
		anim.setActor(npc);
		plugin.onAnimationChanged(anim);

		tickAndRefresh();
		assertFalse(plugin.getActiveAttacks().isEmpty());

		plugin.shutDown();
		assertTrue(plugin.getActiveAttacks().isEmpty());
	}

	@Test
	public void testGenericProjectileIdsIgnoredOutsideColosseum()
	{
		// #4: Projectile IDs 15 and 160 should NOT trigger outside Colosseum regions
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);
		when(client.getMapRegions()).thenReturn(new int[]{12850}); // Random non-Colosseum region

		Projectile arrowProj = mock(Projectile.class);
		when(arrowProj.getTargetActor()).thenReturn(player);
		when(arrowProj.getId()).thenReturn(MobCheckPlugin.ProjectileID.FREMENNIK_ARCHER_RANGE);
		when(arrowProj.getRemainingCycles()).thenReturn(45);

		net.runelite.api.Deque<Projectile> deque = createMockDeque(List.of(arrowProj));
		when(client.getProjectiles()).thenReturn(deque);

		tickAndRefresh();
		assertTrue("Generic arrow projectile should be ignored outside Colosseum",
			plugin.getActiveAttacks().isEmpty());
	}

	@Test
	public void testGenericProjectileIdsActiveInsideColosseum()
	{
		// #4: Projectile IDs 15 and 160 should trigger inside Colosseum regions
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);
		when(client.getMapRegions()).thenReturn(new int[]{7216}); // Colosseum region

		Projectile arrowProj = mock(Projectile.class);
		when(arrowProj.getTargetActor()).thenReturn(player);
		when(arrowProj.getId()).thenReturn(MobCheckPlugin.ProjectileID.FREMENNIK_ARCHER_RANGE);
		when(arrowProj.getRemainingCycles()).thenReturn(45);

		Projectile spellProj = mock(Projectile.class);
		when(spellProj.getTargetActor()).thenReturn(player);
		when(spellProj.getId()).thenReturn(MobCheckPlugin.ProjectileID.FREMENNIK_SEER_MAGIC);
		when(spellProj.getRemainingCycles()).thenReturn(60);

		net.runelite.api.Deque<Projectile> deque = createMockDeque(List.of(arrowProj, spellProj));
		when(client.getProjectiles()).thenReturn(deque);

		tickAndRefresh();
		List<MobCheckPlugin.AttackState> attacks = plugin.getActiveAttacks();
		assertEquals(2, attacks.size());
		assertEquals("Archer", attacks.get(0).npcName);
		assertEquals(MobCheckPlugin.PrayerStyle.RANGE, attacks.get(0).prayerStyle);
		assertEquals("Seer", attacks.get(1).npcName);
		assertEquals(MobCheckPlugin.PrayerStyle.MAGIC, attacks.get(1).prayerStyle);
	}

	@Test
	public void testProjectileInitialTicksPreserved()
	{
		// #2: Verify that initialTicks is captured on first sight and preserved
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);

		Projectile proj = mock(Projectile.class);
		when(proj.getTargetActor()).thenReturn(player);
		when(proj.getId()).thenReturn(MobCheckPlugin.ProjectileID.JAL_ZEK_MAGIC);
		when(proj.getRemainingCycles()).thenReturn(150); // 5 ticks initially

		net.runelite.api.Deque<Projectile> deque = createMockDeque(List.of(proj));
		when(client.getProjectiles()).thenReturn(deque);

		tickAndRefresh();
		List<MobCheckPlugin.AttackState> attacks = plugin.getActiveAttacks();
		assertEquals(1, attacks.size());
		assertEquals(5, attacks.get(0).initialTicks);
		assertEquals(5, attacks.get(0).ticks);

		// Simulate projectile decaying to 3 ticks — same object, less cycles
		when(proj.getRemainingCycles()).thenReturn(90); // 3 ticks now

		tickAndRefresh();
		attacks = plugin.getActiveAttacks();
		assertEquals(1, attacks.size());
		// initialTicks should still be 5 (preserved from first sight)
		assertEquals(5, attacks.get(0).initialTicks);
		assertEquals(3, attacks.get(0).ticks);
	}

	@Test
	public void testDagannothRexMeleeAttack()
	{
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);

		NPC rex = mock(NPC.class);
		when(rex.getName()).thenReturn("Dagannoth Rex");
		when(rex.getInteracting()).thenReturn(player);
		when(rex.getIndex()).thenReturn(101);
		when(rex.getAnimation()).thenReturn(MobCheckPlugin.AnimationID.DAGANNOTH_REX_MELEE);

		AnimationChanged event = new AnimationChanged();
		event.setActor(rex);
		plugin.onAnimationChanged(event);

		tickAndRefresh();
		List<MobCheckPlugin.AttackState> attacks = plugin.getActiveAttacks();
		assertEquals(1, attacks.size());
		assertEquals(MobCheckPlugin.PrayerStyle.MELEE, attacks.get(0).prayerStyle);
		assertEquals("Pray Melee", attacks.get(0).getStyleDisplayName());
		assertEquals("Dagannoth Rex", attacks.get(0).npcName);
		assertEquals(3, attacks.get(0).ticks); // 4 initialized, decremented by 1 on game tick
	}

	@Test
	public void testDagannothPrimeMagicProjectile()
	{
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);
		when(client.getMapRegions()).thenReturn(new int[]{11589}); // DK Lair region

		NPC primeNpc = mock(NPC.class);
		when(primeNpc.getName()).thenReturn("Dagannoth Prime");

		Projectile proj = mock(Projectile.class);
		when(proj.getTargetActor()).thenReturn(player);
		when(proj.getSourceActor()).thenReturn(primeNpc);
		when(proj.getId()).thenReturn(MobCheckPlugin.ProjectileID.DAGANNOTH_PRIME_MAGIC);
		when(proj.getRemainingCycles()).thenReturn(90); // 3 ticks

		net.runelite.api.Deque<Projectile> deque = createMockDeque(List.of(proj));
		when(client.getProjectiles()).thenReturn(deque);

		tickAndRefresh();
		List<MobCheckPlugin.AttackState> attacks = plugin.getActiveAttacks();
		assertEquals(1, attacks.size());
		assertEquals(MobCheckPlugin.PrayerStyle.MAGIC, attacks.get(0).prayerStyle);
		assertEquals("Pray Magic", attacks.get(0).getStyleDisplayName());
		assertEquals("Dagannoth Prime", attacks.get(0).npcName);
		assertEquals(3, attacks.get(0).ticks);
	}

	@Test
	public void testDagannothPrimeIgnoredOutsideLair()
	{
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);
		when(client.getMapRegions()).thenReturn(new int[]{12850}); // Non-DK region

		Projectile proj = mock(Projectile.class);
		when(proj.getTargetActor()).thenReturn(player);
		when(proj.getId()).thenReturn(MobCheckPlugin.ProjectileID.DAGANNOTH_PRIME_MAGIC);
		when(proj.getRemainingCycles()).thenReturn(90);

		net.runelite.api.Deque<Projectile> deque = createMockDeque(List.of(proj));
		when(client.getProjectiles()).thenReturn(deque);

		tickAndRefresh();
		List<MobCheckPlugin.AttackState> attacks = plugin.getActiveAttacks();
		assertEquals(0, attacks.size());
	}

	@Test
	public void testDagannothSupremeRangedProjectiles()
	{
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);

		NPC supremeNpc = mock(NPC.class);
		when(supremeNpc.getName()).thenReturn("Dagannoth Supreme");

		// Test primary spine projectile (475)
		Projectile proj1 = mock(Projectile.class);
		when(proj1.getTargetActor()).thenReturn(player);
		when(proj1.getSourceActor()).thenReturn(supremeNpc);
		when(proj1.getId()).thenReturn(MobCheckPlugin.ProjectileID.DAGANNOTH_SUPREME_RANGE);
		when(proj1.getRemainingCycles()).thenReturn(60); // 2 ticks

		// Test secondary spine projectile (476)
		Projectile proj2 = mock(Projectile.class);
		when(proj2.getTargetActor()).thenReturn(player);
		when(proj2.getSourceActor()).thenReturn(supremeNpc);
		when(proj2.getId()).thenReturn(MobCheckPlugin.ProjectileID.DAGANNOTH_SUPREME_RANGE_2);
		when(proj2.getRemainingCycles()).thenReturn(120); // 4 ticks

		net.runelite.api.Deque<Projectile> deque = createMockDeque(List.of(proj1, proj2));
		when(client.getProjectiles()).thenReturn(deque);

		tickAndRefresh();
		List<MobCheckPlugin.AttackState> attacks = plugin.getActiveAttacks();
		assertEquals(2, attacks.size());
		assertEquals(MobCheckPlugin.PrayerStyle.RANGE, attacks.get(0).prayerStyle);
		assertEquals("Pray Range", attacks.get(0).getStyleDisplayName());
		assertEquals("Dagannoth Supreme", attacks.get(0).npcName);
		assertEquals(2, attacks.get(0).ticks);

		assertEquals(MobCheckPlugin.PrayerStyle.RANGE, attacks.get(1).prayerStyle);
		assertEquals("Pray Range", attacks.get(1).getStyleDisplayName());
		assertEquals("Dagannoth Supreme", attacks.get(1).npcName);
		assertEquals(4, attacks.get(1).ticks);
	}

	@Test
	public void testDagannothKingsMultiAttackPriority()
	{
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);
		when(client.getMapRegions()).thenReturn(new int[]{11589}); // DK Lair region

		// Rex melee attack registered via animation (4 initial ticks)
		NPC rex = mock(NPC.class);
		when(rex.getName()).thenReturn("Dagannoth Rex");
		when(rex.getInteracting()).thenReturn(player);
		when(rex.getIndex()).thenReturn(201);
		when(rex.getAnimation()).thenReturn(MobCheckPlugin.AnimationID.DAGANNOTH_REX_MELEE);

		AnimationChanged event = new AnimationChanged();
		event.setActor(rex);
		plugin.onAnimationChanged(event);

		// Prime magic projectile (3 ticks)
		Projectile primeProj = mock(Projectile.class);
		when(primeProj.getTargetActor()).thenReturn(player);
		when(primeProj.getId()).thenReturn(MobCheckPlugin.ProjectileID.DAGANNOTH_PRIME_MAGIC);
		when(primeProj.getRemainingCycles()).thenReturn(90);

		// Supreme ranged projectile (2 ticks)
		Projectile supremeProj = mock(Projectile.class);
		when(supremeProj.getTargetActor()).thenReturn(player);
		when(supremeProj.getId()).thenReturn(MobCheckPlugin.ProjectileID.DAGANNOTH_SUPREME_RANGE);
		when(supremeProj.getRemainingCycles()).thenReturn(60);

		net.runelite.api.Deque<Projectile> deque = createMockDeque(List.of(primeProj, supremeProj));
		when(client.getProjectiles()).thenReturn(deque);

		tickAndRefresh();
		List<MobCheckPlugin.AttackState> attacks = plugin.getActiveAttacks();
		assertEquals(3, attacks.size());

		// Priority 1: Supreme (2t Range)
		assertEquals(MobCheckPlugin.PrayerStyle.RANGE, attacks.get(0).prayerStyle);
		assertEquals("Dagannoth Supreme", attacks.get(0).npcName);
		assertEquals(2, attacks.get(0).ticks);

		// Priority 2: Prime (3t Magic)
		assertEquals(MobCheckPlugin.PrayerStyle.MAGIC, attacks.get(1).prayerStyle);
		assertEquals("Dagannoth Prime", attacks.get(1).npcName);
		assertEquals(3, attacks.get(1).ticks);

		// Priority 3: Rex (3t Melee after tick decrement from 4)
		assertEquals(MobCheckPlugin.PrayerStyle.MELEE, attacks.get(2).prayerStyle);
		assertEquals("Dagannoth Rex", attacks.get(2).npcName);
		assertEquals(3, attacks.get(2).ticks);

		// Verify getPriorityAttack() returns Supreme
		Optional<MobCheckPlugin.AttackState> priority = plugin.getPriorityAttack();
		assertTrue(priority.isPresent());
		assertEquals(MobCheckPlugin.PrayerStyle.RANGE, priority.get().prayerStyle);
	}

	@Test
	public void testNpcDespawnedPurgesMeleeAttack()
	{
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);

		NPC npc = mock(NPC.class);
		when(npc.getName()).thenReturn("Jal-ImKot");
		when(npc.getInteracting()).thenReturn(player);
		when(npc.getIndex()).thenReturn(55);
		when(npc.getAnimation()).thenReturn(MobCheckPlugin.AnimationID.JAL_IMKOT_MELEE);

		AnimationChanged animEvent = new AnimationChanged();
		animEvent.setActor(npc);
		plugin.onAnimationChanged(animEvent);

		tickAndRefresh();
		assertEquals(1, plugin.getActiveAttacks().size());

		// Trigger NPC despawn
		net.runelite.api.events.NpcDespawned despawnEvent = new net.runelite.api.events.NpcDespawned(npc);
		plugin.onNpcDespawned(despawnEvent);

		tickAndRefresh();
		assertEquals(0, plugin.getActiveAttacks().size());
	}

	@Test
	public void testDagannothRexMeleeVariant2()
	{
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);

		NPC rex = mock(NPC.class);
		when(rex.getName()).thenReturn("Dagannoth Rex");
		when(rex.getInteracting()).thenReturn(player);
		when(rex.getIndex()).thenReturn(102);
		when(rex.getAnimation()).thenReturn(MobCheckPlugin.AnimationID.DAGANNOTH_REX_MELEE_2);

		AnimationChanged event = new AnimationChanged();
		event.setActor(rex);
		plugin.onAnimationChanged(event);

		tickAndRefresh();
		List<MobCheckPlugin.AttackState> attacks = plugin.getActiveAttacks();
		assertEquals(1, attacks.size());
		assertEquals(MobCheckPlugin.PrayerStyle.MELEE, attacks.get(0).prayerStyle);
		assertEquals("Dagannoth Rex", attacks.get(0).npcName);
		assertEquals(3, attacks.get(0).ticks);
	}

	@Test
	public void testBossMeleeAttackTickDurations()
	{
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);

		// Zilyana 2-tick melee
		NPC zilyana = mock(NPC.class);
		when(zilyana.getName()).thenReturn("Commander Zilyana");
		when(zilyana.getInteracting()).thenReturn(player);
		when(zilyana.getIndex()).thenReturn(301);
		when(zilyana.getAnimation()).thenReturn(MobCheckPlugin.AnimationID.ZILYANA_MELEE);

		AnimationChanged eventZil = new AnimationChanged();
		eventZil.setActor(zilyana);
		plugin.onAnimationChanged(eventZil);

		// Graardor 6-tick melee
		NPC graardor = mock(NPC.class);
		when(graardor.getName()).thenReturn("General Graardor");
		when(graardor.getInteracting()).thenReturn(player);
		when(graardor.getIndex()).thenReturn(302);
		when(graardor.getAnimation()).thenReturn(MobCheckPlugin.AnimationID.GRAARDOR_MELEE);

		AnimationChanged eventGra = new AnimationChanged();
		eventGra.setActor(graardor);
		plugin.onAnimationChanged(eventGra);

		// Gorilla 5-tick melee
		NPC gorilla = mock(NPC.class);
		when(gorilla.getName()).thenReturn("Demonic Gorilla");
		when(gorilla.getInteracting()).thenReturn(player);
		when(gorilla.getIndex()).thenReturn(303);
		when(gorilla.getAnimation()).thenReturn(MobCheckPlugin.AnimationID.DEMONIC_GORILLA_MELEE);

		AnimationChanged eventGor = new AnimationChanged();
		eventGor.setActor(gorilla);
		plugin.onAnimationChanged(eventGor);

		tickAndRefresh();
		List<MobCheckPlugin.AttackState> attacks = plugin.getActiveAttacks();
		assertEquals(3, attacks.size());

		// Zilyana: 2 initial ticks - 1 tick = 1 remaining
		assertEquals("Commander Zilyana", attacks.get(0).npcName);
		assertEquals(1, attacks.get(0).ticks);

		// Gorilla: 5 initial ticks - 1 tick = 4 remaining
		assertEquals("Demonic Gorilla", attacks.get(1).npcName);
		assertEquals(4, attacks.get(1).ticks);

		// Graardor: 6 initial ticks - 1 tick = 5 remaining
		assertEquals("General Graardor", attacks.get(2).npcName);
		assertEquals(5, attacks.get(2).ticks);
	}
}
