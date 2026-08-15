package com.mob_check;

import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import org.junit.Before;
import org.junit.Test;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class MobCheckWorldOverlayTest
{
	private MobCheckWorldOverlay worldOverlay;
	private Client client;
	private MobCheckPlugin plugin;
	private MobCheckConfig config;
	private Graphics2D graphics;

	@Before
	public void setUp()
	{
		client = mock(Client.class);
		plugin = mock(MobCheckPlugin.class);
		config = mock(MobCheckConfig.class);
		graphics = mock(Graphics2D.class);

		when(config.highlightThreatNpc()).thenReturn(true);
		when(config.highlightNpcTrueTile()).thenReturn(true);
		when(config.warningThreshold()).thenReturn(1);

		worldOverlay = new MobCheckWorldOverlay(client, plugin, config);
	}

	@Test
	public void testRenderDisabledReturnsNull()
	{
		when(config.highlightThreatNpc()).thenReturn(false);
		when(config.highlightNpcTrueTile()).thenReturn(false);

		assertNull(worldOverlay.render(graphics));
		verify(plugin, never()).getActiveAttacks();
	}

	@Test
	public void testRenderNoAttacksReturnsNull()
	{
		when(plugin.getActiveAttacks()).thenReturn(Collections.emptyList());

		assertNull(worldOverlay.render(graphics));
	}

	@Test
	public void testRenderAttackingNpcHullAndTickText()
	{
		NPC npc = mock(NPC.class);
		when(npc.getIndex()).thenReturn(42);
		when(npc.isDead()).thenReturn(false);
		when(npc.getLogicalHeight()).thenReturn(150);
		when(npc.getLocalLocation()).thenReturn(new LocalPoint(1000, 2000));

		Shape hull = new Rectangle2D.Double(10, 10, 50, 50);
		when(npc.getConvexHull()).thenReturn(hull);
		when(npc.getCanvasTextLocation(any(), anyString(), anyInt())).thenReturn(new Point(100, 80));

		MobCheckPlugin.AttackState attack = new MobCheckPlugin.AttackState(
			3,
			3,
			MobCheckPlugin.PrayerStyle.MAGIC,
			"Jal-Zek",
			npc,
			null,
			false
		);
		when(plugin.getActiveAttacks()).thenReturn(List.of(attack));

		worldOverlay.render(graphics);

		// Verified hull drawn and filled
		verify(graphics, times(1)).draw(hull);
		verify(graphics, times(1)).fill(hull);
		// Verified tick text drawn above NPC
		verify(graphics, times(1)).drawString(eq("3t"), eq(100), eq(80));
	}

	@Test
	public void testRenderDeadNpcSkipped()
	{
		NPC deadNpc = mock(NPC.class);
		when(deadNpc.getIndex()).thenReturn(99);
		when(deadNpc.isDead()).thenReturn(true);

		MobCheckPlugin.AttackState attack = new MobCheckPlugin.AttackState(
			1,
			1,
			MobCheckPlugin.PrayerStyle.RANGE,
			"Jal-Xil",
			deadNpc,
			null,
			false
		);
		when(plugin.getActiveAttacks()).thenReturn(List.of(attack));

		worldOverlay.render(graphics);

		verify(deadNpc, never()).getConvexHull();
	}

	@Test
	public void testRenderDeduplicatesMultipleAttacksFromSameNpc()
	{
		NPC npc = mock(NPC.class);
		when(npc.getIndex()).thenReturn(50);
		when(npc.isDead()).thenReturn(false);
		when(npc.getLogicalHeight()).thenReturn(100);
		when(npc.getLocalLocation()).thenReturn(new LocalPoint(500, 500));

		Shape hull = new Rectangle2D.Double(0, 0, 20, 20);
		when(npc.getConvexHull()).thenReturn(hull);

		// Two simultaneous attacks from same NPC (e.g. Manticore rapid sequence)
		MobCheckPlugin.AttackState attack1 = new MobCheckPlugin.AttackState(1, 1, MobCheckPlugin.PrayerStyle.MAGIC, "Manticore", npc, null, true);
		MobCheckPlugin.AttackState attack2 = new MobCheckPlugin.AttackState(2, 2, MobCheckPlugin.PrayerStyle.RANGE, "Manticore", npc, null, true);
		when(plugin.getActiveAttacks()).thenReturn(List.of(attack1, attack2));

		worldOverlay.render(graphics);

		// Hull only drawn once despite 2 attacks from the same NPC index
		verify(graphics, times(1)).draw(hull);
	}
}
