package com.mob_check;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.OverlayPosition;

public class MobCheckOverlayTest
{
	private MobCheckOverlay overlay;
	private MobCheckPlugin plugin;
	private MobCheckConfig config;
	private SpriteManager spriteManager;
	private Graphics2D graphics;

	@Before
	public void setUp()
	{
		plugin = mock(MobCheckPlugin.class);
		config = mock(MobCheckConfig.class);
		spriteManager = mock(SpriteManager.class);
		graphics = mock(Graphics2D.class);

		FontMetrics fontMetrics = mock(FontMetrics.class);
		when(graphics.getFontMetrics()).thenReturn(fontMetrics);
		when(graphics.getFontMetrics(any())).thenReturn(fontMetrics);
		when(fontMetrics.stringWidth(any())).thenReturn(20);
		when(fontMetrics.getHeight()).thenReturn(12);
		when(fontMetrics.getAscent()).thenReturn(10);

		when(config.showInfoBox()).thenReturn(true);
		when(config.warningThreshold()).thenReturn(1);

		overlay = new MobCheckOverlay(plugin, config, spriteManager);
	}

	@Test
	public void testUsesTopLeftPositionForDraggableInfoBoxPanel()
	{
		assertEquals(OverlayPosition.TOP_LEFT, overlay.getPosition());
	}

	@Test
	public void testRenderDisabledReturnsNull()
	{
		when(config.showInfoBox()).thenReturn(false);

		assertNull(overlay.render(graphics));
		verify(plugin, never()).getActiveAttacks();
	}

	@Test
	public void testRenderNoActiveAttacksReturnsNull()
	{
		when(plugin.getActiveAttacks()).thenReturn(Collections.emptyList());

		assertNull(overlay.render(graphics));
	}

	@Test
	public void testRenderInfoBoxPopulatesTooltipWithNpcNameAndStyle()
	{
		MobCheckPlugin.AttackState state = new MobCheckPlugin.AttackState(1, MobCheckPlugin.PrayerStyle.MAGIC, "Jal-Zek");
		when(plugin.getActiveAttacks()).thenReturn(List.of(state));
		when(plugin.isPrayerProtected(MobCheckPlugin.PrayerStyle.MAGIC)).thenReturn(true);

		BufferedImage sprite = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
		when(spriteManager.getSprite(anyInt(), eq(0))).thenReturn(sprite);

		assertNotNull(overlay.render(graphics));
		assertEquals("Jal-Zek - Pray Magic", overlay.getTooltipForIndex(0));
	}

	@Test
	public void testInfoBoxCappedAtFourAttacks()
	{
		MobCheckPlugin.AttackState a1 = new MobCheckPlugin.AttackState(1, MobCheckPlugin.PrayerStyle.MAGIC, "Mob 1");
		MobCheckPlugin.AttackState a2 = new MobCheckPlugin.AttackState(2, MobCheckPlugin.PrayerStyle.RANGE, "Mob 2");
		MobCheckPlugin.AttackState a3 = new MobCheckPlugin.AttackState(3, MobCheckPlugin.PrayerStyle.MELEE, "Mob 3");
		MobCheckPlugin.AttackState a4 = new MobCheckPlugin.AttackState(4, MobCheckPlugin.PrayerStyle.MAGIC, "Mob 4");
		MobCheckPlugin.AttackState a5 = new MobCheckPlugin.AttackState(5, MobCheckPlugin.PrayerStyle.RANGE, "Mob 5");
		MobCheckPlugin.AttackState a6 = new MobCheckPlugin.AttackState(6, MobCheckPlugin.PrayerStyle.MELEE, "Mob 6");

		when(plugin.getActiveAttacks()).thenReturn(List.of(a1, a2, a3, a4, a5, a6));

		BufferedImage sprite = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
		when(spriteManager.getSprite(anyInt(), eq(0))).thenReturn(sprite);

		assertNotNull(overlay.render(graphics));

		// Sprite manager called at most 4 times for top 4 info boxes
		verify(spriteManager, times(4)).getSprite(anyInt(), eq(0));
	}

	@Test
	public void testManticoreComboHeaderInInfoBox()
	{
		MobCheckPlugin.AttackState manticoreAttack = new MobCheckPlugin.AttackState(
			1,
			1,
			MobCheckPlugin.PrayerStyle.MAGIC,
			"Manticore",
			null,
			true);
		when(plugin.getActiveAttacks()).thenReturn(List.of(manticoreAttack));
		when(config.showComboSequence()).thenReturn(true);

		BufferedImage sprite = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
		when(spriteManager.getSprite(anyInt(), eq(0))).thenReturn(sprite);

		assertNotNull(overlay.render(graphics));
	}
}
