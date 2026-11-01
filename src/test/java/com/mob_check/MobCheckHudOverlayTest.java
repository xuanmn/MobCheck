package com.mob_check;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.OverlayPosition;

public class MobCheckHudOverlayTest
{
	private MobCheckHudOverlay hudOverlay;
	private Client client;
	private MobCheckPlugin plugin;
	private MobCheckConfig config;
	private SpriteManager spriteManager;
	private Graphics2D graphics;
	private Font originalFont;
	private Stroke originalStroke;

	@Before
	public void setUp()
	{
		client = mock(Client.class);
		plugin = mock(MobCheckPlugin.class);
		config = mock(MobCheckConfig.class);
		spriteManager = mock(SpriteManager.class);
		graphics = mock(Graphics2D.class);

		originalFont = new Font("Dialog", Font.PLAIN, 12);
		originalStroke = new BasicStroke(1f);
		when(graphics.getFont()).thenReturn(originalFont);
		when(graphics.getStroke()).thenReturn(originalStroke);

		when(config.showOverhead()).thenReturn(true);
		when(config.showTickProgressRing()).thenReturn(true);
		when(config.flashScreenOnWrongPrayer()).thenReturn(false);
		when(config.warningThreshold()).thenReturn(1);
		when(config.dangerFlashColor()).thenReturn(new Color(255, 0, 0, 70));

		hudOverlay = new MobCheckHudOverlay(client, plugin, config, spriteManager);
	}

	@Test
	public void testUsesDynamicOverlayPositionToPreventSnapCornerTranslation()
	{
		assertEquals(OverlayPosition.DYNAMIC, hudOverlay.getPosition());
	}

	@Test
	public void testRenderDisabledTogglesReturnNull()
	{
		when(config.showOverhead()).thenReturn(false);
		when(config.flashScreenOnWrongPrayer()).thenReturn(false);

		assertNull(hudOverlay.render(graphics));
		verify(plugin, never()).getPriorityAttack();
	}

	@Test
	public void testRenderNoPriorityAttackReturnsNull()
	{
		when(plugin.getPriorityAttack()).thenReturn(Optional.empty());

		assertNull(hudOverlay.render(graphics));
	}

	@Test
	public void testRenderOverheadAndRestoresGraphicsState()
	{
		MobCheckPlugin.AttackState state = new MobCheckPlugin.AttackState(1, MobCheckPlugin.PrayerStyle.MAGIC, "Jal-Zek");
		when(plugin.getPriorityAttack()).thenReturn(Optional.of(state));
		when(plugin.isPrayerProtected(MobCheckPlugin.PrayerStyle.MAGIC)).thenReturn(true);

		BufferedImage sprite = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
		when(spriteManager.getSprite(anyInt(), eq(0))).thenReturn(sprite);

		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);
		when(player.getLogicalHeight()).thenReturn(200);
		when(player.getCanvasImageLocation(any(BufferedImage.class), anyInt())).thenReturn(new Point(100, 100));

		assertNull(hudOverlay.render(graphics));

		verify(graphics, times(1)).drawImage(eq(sprite), eq(60), eq(100), any());
		verify(graphics, times(1)).drawString(eq("1t"), eq(96), eq(122));
		verify(graphics, times(1)).drawString(eq("1t"), eq(95), eq(121));
		verify(graphics).setStroke(originalStroke);
		verify(graphics).setFont(originalFont);
	}

	@Test
	public void testRenderDangerFlashWhenUnprotectedRestoresStroke()
	{
		MobCheckPlugin.AttackState state = new MobCheckPlugin.AttackState(1, MobCheckPlugin.PrayerStyle.MAGIC, "Jal-Zek");
		when(plugin.getPriorityAttack()).thenReturn(Optional.of(state));
		when(plugin.isPrayerProtected(MobCheckPlugin.PrayerStyle.MAGIC)).thenReturn(false);
		when(config.flashScreenOnWrongPrayer()).thenReturn(true);
		when(config.showOverhead()).thenReturn(false);
		when(client.getCanvasWidth()).thenReturn(800);
		when(client.getCanvasHeight()).thenReturn(600);

		hudOverlay.render(graphics);

		verify(graphics, times(1)).drawRect(8, 8, 784, 584);
		verify(graphics).setStroke(originalStroke);
	}

	@Test
	public void testRenderProgressRingWhenEnabled()
	{
		MobCheckPlugin.AttackState state = new MobCheckPlugin.AttackState(2, MobCheckPlugin.PrayerStyle.RANGE, "Jal-Xil");
		when(plugin.getPriorityAttack()).thenReturn(Optional.of(state));
		when(plugin.isPrayerProtected(MobCheckPlugin.PrayerStyle.RANGE)).thenReturn(true);
		when(config.showTickProgressRing()).thenReturn(true);

		BufferedImage sprite = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
		when(spriteManager.getSprite(anyInt(), eq(0))).thenReturn(sprite);

		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);
		when(player.getLogicalHeight()).thenReturn(200);
		when(player.getCanvasImageLocation(any(BufferedImage.class), anyInt())).thenReturn(new Point(100, 100));

		hudOverlay.render(graphics);

		verify(graphics, times(1)).drawOval(anyInt(), anyInt(), anyInt(), anyInt());
		verify(graphics, times(1)).drawArc(anyInt(), anyInt(), anyInt(), anyInt(), eq(90), anyInt());
	}
}
