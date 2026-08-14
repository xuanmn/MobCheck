package com.mob_check;

import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
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
		net.runelite.client.ui.overlay.OverlayManager overlayManager = mock(net.runelite.client.ui.overlay.OverlayManager.class);

		// Use reflection to inject private fields
		setPrivateField(plugin, "client", client);
		setPrivateField(plugin, "config", config);
		setPrivateField(plugin, "overlay", overlay);
		setPrivateField(plugin, "overlayManager", overlayManager);

		// Default: empty projectile deque so getPriorityAttack() doesn't NPE
		net.runelite.api.Deque<Projectile> emptyDeque = mockDeque(Collections.emptyList());
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
	private net.runelite.api.Deque<Projectile> mockDeque(List<Projectile> projectiles)
	{
		net.runelite.api.Deque<Projectile> deque = mock(net.runelite.api.Deque.class);
		doAnswer(invocation -> projectiles.iterator()).when(deque).iterator();
		return deque;
	}

	@Test
	public void testGetPriorityAttackEmpty()
	{
		net.runelite.api.Deque<Projectile> deque = mockDeque(Collections.emptyList());
		when(client.getProjectiles()).thenReturn(deque);
		Optional<MobCheckPlugin.AttackState> priority = plugin.getPriorityAttack();
		assertFalse(priority.isPresent());
	}

	@Test
	public void testGetPriorityAttackWithKnownProjectile()
	{
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);

		Projectile projectile = mock(Projectile.class);
		when(projectile.getTargetActor()).thenReturn(player);
		when(projectile.getId()).thenReturn(1374); // Jal-Zek (Magic)
		when(projectile.getRemainingCycles()).thenReturn(121); // (121 + 29) / 30 = 5 ticks

		List<Projectile> list = new ArrayList<>();
		list.add(projectile);
		net.runelite.api.Deque<Projectile> deque = mockDeque(list);
		when(client.getProjectiles()).thenReturn(deque);

		Optional<MobCheckPlugin.AttackState> priority = plugin.getPriorityAttack();
		assertTrue(priority.isPresent());
		assertEquals("Pray Magic", priority.get().style);
		assertEquals(5, priority.get().ticks);
	}

	@Test
	public void testGetPriorityAttackWithUnknownProjectile()
	{
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);

		Projectile projectile = mock(Projectile.class);
		when(projectile.getTargetActor()).thenReturn(player);
		when(projectile.getId()).thenReturn(9999); // Unknown projectile
		when(projectile.getRemainingCycles()).thenReturn(61); // 3 ticks

		List<Projectile> list = new ArrayList<>();
		list.add(projectile);
		net.runelite.api.Deque<Projectile> deque = mockDeque(list);
		when(client.getProjectiles()).thenReturn(deque);

		// With trackUnknownProjectiles false
		when(config.trackUnknownProjectiles()).thenReturn(false);
		Optional<MobCheckPlugin.AttackState> priority = plugin.getPriorityAttack();
		assertFalse(priority.isPresent());

		// With trackUnknownProjectiles true
		when(config.trackUnknownProjectiles()).thenReturn(true);
		priority = plugin.getPriorityAttack();
		assertTrue(priority.isPresent());
		assertEquals("Pray Magic", priority.get().style);
		assertEquals(3, priority.get().ticks);
	}

	@Test
	public void testGetPriorityAttackMultipleProjectiles()
	{
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);

		// Projectile 1: Magic, 5 ticks remaining
		Projectile proj1 = mock(Projectile.class);
		when(proj1.getTargetActor()).thenReturn(player);
		when(proj1.getId()).thenReturn(1374); // Jal-Zek (Magic)
		when(proj1.getRemainingCycles()).thenReturn(121); // 5 ticks

		// Projectile 2: Range, 2 ticks remaining
		Projectile proj2 = mock(Projectile.class);
		when(proj2.getTargetActor()).thenReturn(player);
		when(proj2.getId()).thenReturn(1376); // Jal-Xil (Range)
		when(proj2.getRemainingCycles()).thenReturn(31); // 2 ticks

		List<Projectile> list = new ArrayList<>();
		list.add(proj1);
		list.add(proj2);
		net.runelite.api.Deque<Projectile> deque = mockDeque(list);
		when(client.getProjectiles()).thenReturn(deque);

		Optional<MobCheckPlugin.AttackState> priority = plugin.getPriorityAttack();
		assertTrue(priority.isPresent());
		assertEquals("Pray Range", priority.get().style);
		assertEquals(2, priority.get().ticks);
	}

	@Test
	public void testMeleeAttackTriggerAndCountdown()
	{
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);

		net.runelite.api.Deque<Projectile> deque = mockDeque(Collections.emptyList());
		when(client.getProjectiles()).thenReturn(deque);

		NPC npc = mock(NPC.class);
		when(npc.getIndex()).thenReturn(123);
		when(npc.getName()).thenReturn("Bloodveld");
		when(npc.getAnimation()).thenReturn(1552); // Bloodveld melee animation, warningTicks = 4
		when(npc.getInteracting()).thenReturn(player);

		// Trigger animation change
		AnimationChanged animationChanged = new AnimationChanged();
		animationChanged.setActor(npc);
		plugin.onAnimationChanged(animationChanged);

		// Verify priority attack is melee with 4 ticks
		Optional<MobCheckPlugin.AttackState> priority = plugin.getPriorityAttack();
		assertTrue(priority.isPresent());
		assertEquals("Pray Melee", priority.get().style);
		assertEquals(4, priority.get().ticks);

		// Increment tick, countdown happens
		plugin.onGameTick(new GameTick());

		priority = plugin.getPriorityAttack();
		assertTrue(priority.isPresent());
		assertEquals(3, priority.get().ticks);

		// Countdown until it expires
		plugin.onGameTick(new GameTick()); // 2 ticks
		plugin.onGameTick(new GameTick()); // 1 tick
		plugin.onGameTick(new GameTick()); // 0 ticks (removed)

		priority = plugin.getPriorityAttack();
		assertFalse(priority.isPresent());
	}

	@Test
	public void testMeleeAttackIgnoredIfNotTargetingLocalPlayer()
	{
		Player localPlayer = mock(Player.class);
		Player otherPlayer = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(localPlayer);

		NPC npc = mock(NPC.class);
		when(npc.getIndex()).thenReturn(456);
		when(npc.getName()).thenReturn("Abyssal demon");
		when(npc.getAnimation()).thenReturn(2309);
		when(npc.getInteracting()).thenReturn(otherPlayer); // Targeting someone else

		AnimationChanged animationChanged = new AnimationChanged();
		animationChanged.setActor(npc);
		plugin.onAnimationChanged(animationChanged);

		Optional<MobCheckPlugin.AttackState> priority = plugin.getPriorityAttack();
		assertFalse(priority.isPresent());
	}

	@Test
	public void testGetPriorityAttackWithNullLocalPlayer()
	{
		when(client.getLocalPlayer()).thenReturn(null);

		Projectile projectile = mock(Projectile.class);
		when(projectile.getTargetActor()).thenReturn(null);
		when(projectile.getId()).thenReturn(1374);

		List<Projectile> list = new ArrayList<>();
		list.add(projectile);
		net.runelite.api.Deque<Projectile> deque = mockDeque(list);
		when(client.getProjectiles()).thenReturn(deque);

		Optional<MobCheckPlugin.AttackState> priority = plugin.getPriorityAttack();
		assertFalse(priority.isPresent());
	}

	@Test
	public void testSoundAlertOnPriorityChange()
	{
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);
		when(config.playSoundAlert()).thenReturn(true);
		when(config.soundEffectId()).thenReturn(2266);

		NPC npc = mock(NPC.class);
		when(npc.getIndex()).thenReturn(789);
		when(npc.getName()).thenReturn("Bloodveld");
		when(npc.getAnimation()).thenReturn(1552);
		when(npc.getInteracting()).thenReturn(player);

		AnimationChanged animationChanged = new AnimationChanged();
		animationChanged.setActor(npc);
		plugin.onAnimationChanged(animationChanged);

		// Execute game tick
		plugin.onGameTick(new GameTick());

		// Verify sound effect played
		verify(client, times(1)).playSoundEffect(2266);

		// Subsequent game tick without style change should not trigger sound again
		plugin.onGameTick(new GameTick());
		verify(client, times(1)).playSoundEffect(2266);
	}

	@Test
	public void testGetPriorityAttackWithColosseumProjectile()
	{
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);

		Projectile projectile = mock(Projectile.class);
		when(projectile.getTargetActor()).thenReturn(player);
		when(projectile.getId()).thenReturn(2685); // Serpent Shaman / Magic attack
		when(projectile.getRemainingCycles()).thenReturn(91); // (91 + 29) / 30 = 4 ticks

		List<Projectile> list = new ArrayList<>();
		list.add(projectile);
		net.runelite.api.Deque<Projectile> deque = mockDeque(list);
		when(client.getProjectiles()).thenReturn(deque);

		Optional<MobCheckPlugin.AttackState> priority = plugin.getPriorityAttack();
		assertTrue(priority.isPresent());
		assertEquals("Pray Magic", priority.get().style);
		assertEquals(4, priority.get().ticks);
	}

	@Test
	public void testGetPriorityAttackWithColosseumAnimation()
	{
		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);

		NPC npc = mock(NPC.class);
		when(npc.getIndex()).thenReturn(999);
		when(npc.getName()).thenReturn("Jaguar Warrior");
		when(npc.getAnimation()).thenReturn(10871); // Jaguar Warrior Melee
		when(npc.getInteracting()).thenReturn(player);

		AnimationChanged animationChanged = new AnimationChanged();
		animationChanged.setActor(npc);
		plugin.onAnimationChanged(animationChanged);

		Optional<MobCheckPlugin.AttackState> priority = plugin.getPriorityAttack();
		assertTrue(priority.isPresent());
		assertEquals("Pray Melee", priority.get().style);
		assertEquals(4, priority.get().ticks);
	}
}
