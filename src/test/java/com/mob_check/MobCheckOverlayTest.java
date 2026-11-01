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

import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

public class MobCheckOverlayTest
{
	private MobCheckOverlay overlay;
	private Client client;
	private MobCheckPlugin plugin;
	private MobCheckConfig config;
	private SpriteManager spriteManager;
	private TooltipManager tooltipManager;
	private Graphics2D graphics;

	@Before
	public void setUp()
	{
		client = mock(Client.class);
		plugin = mock(MobCheckPlugin.class);
		config = mock(MobCheckConfig.class);
		spriteManager = mock(SpriteManager.class);
		tooltipManager = mock(TooltipManager.class);
		graphics = mock(Graphics2D.class);

		FontMetrics fontMetrics = mock(FontMetrics.class);
		when(graphics.getFontMetrics()).thenReturn(fontMetrics);
		when(graphics.getFontMetrics(any())).thenReturn(fontMetrics);
		when(fontMetrics.stringWidth(any())).thenReturn(20);
		when(fontMetrics.getHeight()).thenReturn(12);
		when(fontMetrics.getAscent()).thenReturn(10);

		when(config.showInfoBox()).thenReturn(true);
		when(config.warningThreshold()).thenReturn(1);

		overlay = new MobCheckOverlay(client, plugin, config, spriteManager, tooltipManager);
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
	public void testHoveringInfoBoxSubmitsTooltipToTooltipManager()
	{
		MobCheckPlugin.AttackState state = new MobCheckPlugin.AttackState(1, MobCheckPlugin.PrayerStyle.MAGIC, "Jal-Zek");
		when(plugin.getActiveAttacks()).thenReturn(List.of(state));
		when(plugin.isPrayerProtected(MobCheckPlugin.PrayerStyle.MAGIC)).thenReturn(true);

		BufferedImage sprite = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
		when(spriteManager.getSprite(anyInt(), eq(0))).thenReturn(sprite);

		// Simulate OverlayRenderer translating TOP_LEFT overlay bounds to (10, 20)
		overlay.getBounds().setLocation(10, 20);
		// PanelComponent default border is 4px; first 32x32 InfoBox spans rel (4..36, 4..36) -> canvas (14..46, 24..56)
		when(client.getMouseCanvasPosition()).thenReturn(new Point(25, 35));

		assertNotNull(overlay.render(graphics));
		verify(tooltipManager, times(1)).add(new Tooltip("Jal-Zek - Pray Magic"));
	}

	@Test
	public void testUnhoveredInfoBoxDoesNotSubmitTooltip()
	{
		MobCheckPlugin.AttackState state = new MobCheckPlugin.AttackState(1, MobCheckPlugin.PrayerStyle.MAGIC, "Jal-Zek");
		when(plugin.getActiveAttacks()).thenReturn(List.of(state));

		BufferedImage sprite = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
		when(spriteManager.getSprite(anyInt(), eq(0))).thenReturn(sprite);

		overlay.getBounds().setLocation(10, 20);
		when(client.getMouseCanvasPosition()).thenReturn(new Point(500, 500));

		assertNotNull(overlay.render(graphics));
		verify(tooltipManager, never()).add(any());
	}

	@Test
	public void testHoveringInfoBoxWithNullNpcNameFallsBackToStyleDisplayName()
	{
		MobCheckPlugin.AttackState state = new MobCheckPlugin.AttackState(2, MobCheckPlugin.PrayerStyle.RANGE, null);
		when(plugin.getActiveAttacks()).thenReturn(List.of(state));

		BufferedImage sprite = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
		when(spriteManager.getSprite(anyInt(), eq(0))).thenReturn(sprite);

		overlay.getBounds().setLocation(0, 0);
		when(client.getMouseCanvasPosition()).thenReturn(new Point(15, 15));

		assertNotNull(overlay.render(graphics));
		verify(tooltipManager, times(1)).add(new Tooltip("Pray Range"));
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
	public void testManticoreComboHeaderInInfoBoxStillSubmitsHoveredBoxTooltip()
	{
		MobCheckPlugin.AttackState manticoreAttack = new MobCheckPlugin.AttackState(
			1,
			1,
			MobCheckPlugin.PrayerStyle.MAGIC,
			"Manticore (Magic)",
			null,
			true);
		when(plugin.getActiveAttacks()).thenReturn(List.of(manticoreAttack));
		when(config.showComboSequence()).thenReturn(true);

		BufferedImage sprite = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
		when(spriteManager.getSprite(anyInt(), eq(0))).thenReturn(sprite);

		// Render once to discover actual layout bounds of first InfoBox below TitleComponent
		assertNotNull(overlay.render(graphics));
		// TitleComponent occupies top ~16px; InfoBox sits below it around y=20..52, x=4..36
		when(client.getMouseCanvasPosition()).thenReturn(new Point(20, 30));

		assertNotNull(overlay.render(graphics));
		verify(tooltipManager, times(1)).add(new Tooltip("Manticore (Magic) - Pray Magic"));
	}
}
