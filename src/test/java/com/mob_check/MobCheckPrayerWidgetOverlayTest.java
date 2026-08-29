package com.mob_check;

import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import org.junit.Before;
import org.junit.Test;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Optional;

import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SuppressWarnings("deprecation")
public class MobCheckPrayerWidgetOverlayTest
{
	private MobCheckPrayerWidgetOverlay overlay;
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
	public void testRenderWidgetHighlightProtected()
	{
		MobCheckPlugin.AttackState attack = new MobCheckPlugin.AttackState(2, "Pray Magic", "Jal-Zek");
		when(plugin.getPriorityAttack()).thenReturn(Optional.of(attack));
		when(plugin.isPrayerProtected(MobCheckPlugin.PrayerStyle.MAGIC)).thenReturn(true);

		Widget widget = mock(Widget.class);
		when(widget.isHidden()).thenReturn(false);
		when(widget.getBounds()).thenReturn(new Rectangle(50, 100, 30, 30));
		when(client.getWidget(anyInt(), eq(MobCheckPlugin.PrayerStyle.MAGIC.getChildIndex()))).thenReturn(widget);

		overlay.render(graphics);

		// Verified border drawn around widget bounds
		verify(graphics, times(1)).drawRect(50, 100, 30, 30);
		// Verified tick text drawn (shadow + foreground)
		verify(graphics, times(2)).drawString(eq("2t"), anyInt(), anyInt());
	}

	@Test
	public void testRenderWidgetHighlightWrongPrayerAt1Tick()
	{
		MobCheckPlugin.AttackState attack = new MobCheckPlugin.AttackState(1, "Pray Range", "Jal-Xil");
		when(plugin.getPriorityAttack()).thenReturn(Optional.of(attack));
		when(plugin.isPrayerProtected(MobCheckPlugin.PrayerStyle.RANGE)).thenReturn(false);

		Widget widget = mock(Widget.class);
		when(widget.isHidden()).thenReturn(false);
		when(widget.getBounds()).thenReturn(new Rectangle(60, 120, 30, 30));
		when(client.getWidget(anyInt(), eq(MobCheckPlugin.PrayerStyle.RANGE.getChildIndex()))).thenReturn(widget);

		overlay.render(graphics);

		// Emergency red flash rectangle fill + outline drawn
		verify(graphics, times(1)).fillRect(60, 120, 30, 30);
		verify(graphics, times(1)).drawRect(59, 119, 32, 32);
	}

	@Test
	public void testRenderFallbackToParentChildren()
	{
		MobCheckPlugin.AttackState attack = new MobCheckPlugin.AttackState(3, "Pray Melee", "Jal-ImKot");
		when(plugin.getPriorityAttack()).thenReturn(Optional.of(attack));
		when(plugin.isPrayerProtected(MobCheckPlugin.PrayerStyle.MELEE)).thenReturn(false);

		// Direct widget lookup returns null
		when(client.getWidget(anyInt(), eq(MobCheckPlugin.PrayerStyle.MELEE.getChildIndex()))).thenReturn(null);

		// Parent has children array
		Widget parent = mock(Widget.class);
		when(parent.isHidden()).thenReturn(false);

		Widget child = mock(Widget.class);
		when(child.isHidden()).thenReturn(false);
		when(child.getBounds()).thenReturn(new Rectangle(70, 140, 30, 30));

		Widget[] children = new Widget[30];
		children[MobCheckPlugin.PrayerStyle.MELEE.getChildIndex()] = child;
		when(parent.getChildren()).thenReturn(children);

		when(client.getWidget(net.runelite.api.widgets.ComponentID.PRAYER_PARENT)).thenReturn(parent);

		overlay.render(graphics);

		verify(graphics, times(1)).drawRect(70, 140, 30, 30);
		verify(graphics, times(2)).drawString(eq("3t"), anyInt(), anyInt());
	}

	@Test
	public void testRenderHiddenWidgetReturnsNull()
	{
		MobCheckPlugin.AttackState attack = new MobCheckPlugin.AttackState(1, "Pray Magic", "Jal-Zek");
		when(plugin.getPriorityAttack()).thenReturn(Optional.of(attack));

		Widget widget = mock(Widget.class);
		when(widget.isHidden()).thenReturn(true);
		when(client.getWidget(anyInt(), eq(MobCheckPlugin.PrayerStyle.MAGIC.getChildIndex()))).thenReturn(widget);

		assertNull(overlay.render(graphics));
	}

	@Test
	public void testOnlyShowWidgetTicksWithoutBoxHighlight()
	{
		when(config.highlightPrayerWidget()).thenReturn(false);
		when(config.showWidgetTicks()).thenReturn(true);

		MobCheckPlugin.AttackState attack = new MobCheckPlugin.AttackState(2, "Pray Magic", "Jal-Zek");
		when(plugin.getPriorityAttack()).thenReturn(Optional.of(attack));
		when(plugin.isPrayerProtected(MobCheckPlugin.PrayerStyle.MAGIC)).thenReturn(true);

		Widget widget = mock(Widget.class);
		when(widget.isHidden()).thenReturn(false);
		when(widget.getBounds()).thenReturn(new Rectangle(50, 100, 30, 30));
		when(client.getWidget(anyInt(), eq(MobCheckPlugin.PrayerStyle.MAGIC.getChildIndex()))).thenReturn(widget);

		overlay.render(graphics);

		// Box is NOT drawn
		verify(graphics, never()).drawRect(anyInt(), anyInt(), anyInt(), anyInt());
		// Ticks are drawn
		verify(graphics, times(2)).drawString(eq("2t"), anyInt(), anyInt());
	}
}
