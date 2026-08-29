package com.mob_check;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.client.game.SpriteManager;
import org.junit.Before;
import org.junit.Test;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class MobCheckOverlayTest
{
	private MobCheckOverlay overlay;
	private Client client;
	private MobCheckPlugin plugin;
	private MobCheckConfig config;
	private SpriteManager spriteManager;
	private Graphics2D graphics;

	@Before
	public void setUp()
	{
		client = mock(Client.class);
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
		when(config.showOverhead()).thenReturn(true);
		when(config.showTickProgressRing()).thenReturn(true);
		when(config.flashScreenOnWrongPrayer()).thenReturn(false);
		when(config.warningThreshold()).thenReturn(1);
		when(config.dangerFlashColor()).thenReturn(new Color(255, 0, 0, 70));

		overlay = new MobCheckOverlay(client, plugin, config, spriteManager);
	}

	@Test
	public void testRenderDisabledTogglesReturnNull()
	{
		when(config.showInfoBox()).thenReturn(false);
		when(config.showOverhead()).thenReturn(false);
		when(config.flashScreenOnWrongPrayer()).thenReturn(false);

		assertNull(overlay.render(graphics));
		verify(plugin, never()).getActiveAttacks();
	}

	@Test
	public void testRenderNoPriorityAttackReturnsNull()
	{
		when(plugin.getActiveAttacks()).thenReturn(Collections.emptyList());

		assertNull(overlay.render(graphics));
	}

	@Test
	public void testRenderPriorityAttackWithInfoBoxAndOverhead()
	{
		MobCheckPlugin.AttackState state = new MobCheckPlugin.AttackState(1, MobCheckPlugin.PrayerStyle.MAGIC, "Jal-Zek");
		when(plugin.getActiveAttacks()).thenReturn(List.of(state));
		when(plugin.isPrayerProtected(MobCheckPlugin.PrayerStyle.MAGIC)).thenReturn(true);

		BufferedImage sprite = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
		when(spriteManager.getSprite(anyInt(), eq(0))).thenReturn(sprite);

		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);
		when(player.getLogicalHeight()).thenReturn(200);
		when(player.getCanvasImageLocation(any(BufferedImage.class), anyInt())).thenReturn(new Point(100, 100));

		assertNotNull(overlay.render(graphics));

		// Verify overhead rendering drew the sprite and text string
		verify(graphics, times(1)).drawImage(eq(sprite), eq(60), eq(100), any());
		verify(graphics, times(1)).drawString(eq("1t"), eq(95), eq(121));
	}

	@Test
	public void testRenderDangerFlashWhenUnprotected()
	{
		MobCheckPlugin.AttackState state = new MobCheckPlugin.AttackState(1, MobCheckPlugin.PrayerStyle.MAGIC, "Jal-Zek");
		when(plugin.getActiveAttacks()).thenReturn(List.of(state));
		when(plugin.isPrayerProtected(MobCheckPlugin.PrayerStyle.MAGIC)).thenReturn(false);
		when(config.flashScreenOnWrongPrayer()).thenReturn(true);
		when(client.getCanvasWidth()).thenReturn(800);
		when(client.getCanvasHeight()).thenReturn(600);

		overlay.render(graphics);

		// Verify screen danger flash border was drawn
		verify(graphics, times(1)).drawRect(0, 0, 800, 600);
	}

	@Test
	public void testRenderProgressRingWhenEnabled()
	{
		MobCheckPlugin.AttackState state = new MobCheckPlugin.AttackState(2, MobCheckPlugin.PrayerStyle.RANGE, "Jal-Xil");
		when(plugin.getActiveAttacks()).thenReturn(List.of(state));
		when(plugin.isPrayerProtected(MobCheckPlugin.PrayerStyle.RANGE)).thenReturn(true);
		when(config.showTickProgressRing()).thenReturn(true);

		BufferedImage sprite = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
		when(spriteManager.getSprite(anyInt(), eq(0))).thenReturn(sprite);

		Player player = mock(Player.class);
		when(client.getLocalPlayer()).thenReturn(player);
		when(player.getLogicalHeight()).thenReturn(200);
		when(player.getCanvasImageLocation(any(BufferedImage.class), anyInt())).thenReturn(new Point(100, 100));

		overlay.render(graphics);

		// Verify circular background ring + progress arc drawn
		verify(graphics, times(1)).drawOval(anyInt(), anyInt(), anyInt(), anyInt());
		verify(graphics, times(1)).drawArc(anyInt(), anyInt(), anyInt(), anyInt(), eq(90), anyInt());
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

		when(config.showOverhead()).thenReturn(false);
		when(config.showInfoBox()).thenReturn(true);

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
			null,
			true
		);
		when(plugin.getActiveAttacks()).thenReturn(List.of(manticoreAttack));
		when(config.showComboSequence()).thenReturn(true);
		when(config.showInfoBox()).thenReturn(true);
		when(config.showOverhead()).thenReturn(false);

		BufferedImage sprite = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
		when(spriteManager.getSprite(anyInt(), eq(0))).thenReturn(sprite);

		assertNotNull(overlay.render(graphics));
	}
}

