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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

public class MobCheckPrayerWidgetOverlayTest
{
	private MobCheckPrayerWidgetOverlay overlay;
	private Client client;
	private MobCheckPlugin plugin;
	private MobCheckConfig config;
	private Graphics2D graphics;
	private Font originalFont;
	private Stroke originalStroke;

	@Before
	public void setUp()
	{
		client = mock(Client.class);
		plugin = mock(MobCheckPlugin.class);
		config = mock(MobCheckConfig.class);
		graphics = mock(Graphics2D.class);

		originalFont = new Font("Dialog", Font.PLAIN, 12);
		originalStroke = new BasicStroke(1f);
		when(graphics.getFont()).thenReturn(originalFont);
		when(graphics.getStroke()).thenReturn(originalStroke);

		FontMetrics fontMetrics = mock(FontMetrics.class);
		when(graphics.getFontMetrics()).thenReturn(fontMetrics);
		when(graphics.getFontMetrics(any())).thenReturn(fontMetrics);
		when(fontMetrics.stringWidth(any())).thenReturn(15);
		when(fontMetrics.getHeight()).thenReturn(10);
		when(fontMetrics.getAscent()).thenReturn(8);

		when(config.highlightPrayerWidget()).thenReturn(true);
		when(config.showWidgetTicks()).thenReturn(true);
		when(config.flashWrongPrayerWidget()).thenReturn(true);
		when(config.warningThreshold()).thenReturn(1);

		overlay = new MobCheckPrayerWidgetOverlay(client, plugin, config);
	}

	@Test
	public void testPrayerStyleUsesGamevalComponentIds()
	{
		assertEquals(InterfaceID.Prayerbook.PRAYER17, MobCheckPlugin.PrayerStyle.MAGIC.getComponentId());
		assertEquals(InterfaceID.Prayerbook.PRAYER18, MobCheckPlugin.PrayerStyle.RANGE.getComponentId());
		assertEquals(InterfaceID.Prayerbook.PRAYER19, MobCheckPlugin.PrayerStyle.MELEE.getComponentId());
	}

	@Test
	public void testRenderDisabledReturnsNull()
	{
		when(config.highlightPrayerWidget()).thenReturn(false);
		when(config.showWidgetTicks()).thenReturn(false);

		assertNull(overlay.render(graphics));
		verify(plugin, never()).getPriorityAttack();
	}

	@Test
	public void testRenderNoPriorityAttackReturnsNull()
	{
		when(plugin.getPriorityAttack()).thenReturn(Optional.empty());

		assertNull(overlay.render(graphics));
	}

	@Test
	public void testRenderWidgetHighlightProtectedAndRestoresGraphicsState()
	{
		MobCheckPlugin.AttackState attack = new MobCheckPlugin.AttackState(2, MobCheckPlugin.PrayerStyle.MAGIC, "Jal-Zek");
		when(plugin.getPriorityAttack()).thenReturn(Optional.of(attack));
		when(plugin.isPrayerProtected(MobCheckPlugin.PrayerStyle.MAGIC)).thenReturn(true);

		Widget widget = mock(Widget.class);
		when(widget.isHidden()).thenReturn(false);
		when(widget.getBounds()).thenReturn(new Rectangle(50, 100, 30, 30));
		when(client.getWidget(InterfaceID.Prayerbook.PRAYER17)).thenReturn(widget);

		overlay.render(graphics);

		// Verified border drawn around widget bounds
		verify(graphics, times(1)).drawRect(50, 100, 30, 30);
		// Verified tick text drawn (shadow + foreground)
		verify(graphics, times(2)).drawString(eq("2t"), anyInt(), anyInt());
		// Verified Graphics2D stroke and font restored
		verify(graphics).setStroke(originalStroke);
		verify(graphics).setFont(originalFont);
	}

	@Test
	public void testRenderWidgetHighlightWrongPrayerAt1Tick()
	{
		MobCheckPlugin.AttackState attack = new MobCheckPlugin.AttackState(1, MobCheckPlugin.PrayerStyle.RANGE, "Jal-Xil");
		when(plugin.getPriorityAttack()).thenReturn(Optional.of(attack));
		when(plugin.isPrayerProtected(MobCheckPlugin.PrayerStyle.RANGE)).thenReturn(false);

		Widget widget = mock(Widget.class);
		when(widget.isHidden()).thenReturn(false);
		when(widget.getBounds()).thenReturn(new Rectangle(60, 120, 30, 30));
		when(client.getWidget(InterfaceID.Prayerbook.PRAYER18)).thenReturn(widget);

		overlay.render(graphics);

		// Emergency red flash rectangle fill + outline drawn
		verify(graphics, times(1)).fillRect(60, 120, 30, 30);
		verify(graphics, times(1)).drawRect(59, 119, 32, 32);
	}

	@Test
	public void testRenderHiddenWidgetReturnsNull()
	{
		MobCheckPlugin.AttackState attack = new MobCheckPlugin.AttackState(1, MobCheckPlugin.PrayerStyle.MAGIC, "Jal-Zek");
		when(plugin.getPriorityAttack()).thenReturn(Optional.of(attack));

		Widget widget = mock(Widget.class);
		when(widget.isHidden()).thenReturn(true);
		when(client.getWidget(InterfaceID.Prayerbook.PRAYER17)).thenReturn(widget);

		assertNull(overlay.render(graphics));
	}

	@Test
	public void testOnlyShowWidgetTicksWithoutBoxHighlight()
	{
		when(config.highlightPrayerWidget()).thenReturn(false);
		when(config.showWidgetTicks()).thenReturn(true);

		MobCheckPlugin.AttackState attack = new MobCheckPlugin.AttackState(2, MobCheckPlugin.PrayerStyle.MAGIC, "Jal-Zek");
		when(plugin.getPriorityAttack()).thenReturn(Optional.of(attack));
		when(plugin.isPrayerProtected(MobCheckPlugin.PrayerStyle.MAGIC)).thenReturn(true);

		Widget widget = mock(Widget.class);
		when(widget.isHidden()).thenReturn(false);
		when(widget.getBounds()).thenReturn(new Rectangle(50, 100, 30, 30));
		when(client.getWidget(InterfaceID.Prayerbook.PRAYER17)).thenReturn(widget);

		overlay.render(graphics);

		// Box is NOT drawn
		verify(graphics, never()).drawRect(anyInt(), anyInt(), anyInt(), anyInt());
		// Ticks are drawn
		verify(graphics, times(2)).drawString(eq("2t"), anyInt(), anyInt());
	}
}
