package com.mob_check;

import net.runelite.api.Client;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.Optional;

public class MobCheckPrayerWidgetOverlay extends Overlay
{
	private final Client client;
	private final MobCheckPlugin plugin;
	private final MobCheckConfig config;

	private static final Font WIDGET_FONT = new Font("Arial", Font.BOLD, 13);
	private static final Stroke THICK_STROKE = new BasicStroke(3f);
	private static final Stroke REGULAR_STROKE = new BasicStroke(2f);

	@Inject
	public MobCheckPrayerWidgetOverlay(Client client, MobCheckPlugin plugin, MobCheckConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(OverlayPriority.HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.highlightPrayerWidget() && !config.showWidgetTicks())
		{
			return null;
		}

		Optional<MobCheckPlugin.AttackState> priorityOpt = plugin.getPriorityAttack();
		if (!priorityOpt.isPresent())
		{
			return null;
		}

		MobCheckPlugin.AttackState priority = priorityOpt.get();
		MobCheckPlugin.PrayerStyle style = priority.prayerStyle;
		if (style == null)
		{
			return null;
		}

		Widget prayerWidget = client.getWidget(InterfaceID.PRAYER, style.getChildIndex());
		if (prayerWidget == null || prayerWidget.isHidden())
		{
			Widget parent = client.getWidget(ComponentID.PRAYER_PARENT);
			if (parent != null && parent.getChildren() != null && style.getChildIndex() < parent.getChildren().length)
			{
				prayerWidget = parent.getChildren()[style.getChildIndex()];
			}
		}

		if (prayerWidget == null || prayerWidget.isHidden())
		{
			return null;
		}

		Rectangle bounds = prayerWidget.getBounds();
		if (bounds == null || bounds.isEmpty())
		{
			return null;
		}

		boolean isProtected = plugin.isPrayerProtected(style);
		boolean isUrgent = priority.ticks <= config.warningThreshold();

		// 1. Highlight box outline on prayer button
		if (config.highlightPrayerWidget())
		{
			if (isProtected)
			{
				// Green border when correctly active
				graphics.setColor(new Color(0, 255, 60, 220));
				graphics.setStroke(REGULAR_STROKE);
				graphics.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
			}
			else if (isUrgent && config.flashWrongPrayerWidget())
			{
				// Emergency red pulsing outline and semi-transparent flash
				graphics.setColor(new Color(255, 30, 30, 240));
				graphics.setStroke(THICK_STROKE);
				graphics.drawRect(bounds.x - 1, bounds.y - 1, bounds.width + 2, bounds.height + 2);
				graphics.setColor(new Color(255, 0, 0, 50));
				graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
			}
			else
			{
				// Normal indicator outline matching prayer style color
				graphics.setColor(style.getColor());
				graphics.setStroke(REGULAR_STROKE);
				graphics.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
			}
		}

		// 2. Render tick countdown on the prayer button
		if (config.showWidgetTicks())
		{
			String tickText = priority.ticks + "t";
			graphics.setFont(WIDGET_FONT);
			FontMetrics fm = graphics.getFontMetrics(WIDGET_FONT);
			int textWidth = fm.stringWidth(tickText);

			int textX = bounds.x + (bounds.width - textWidth) / 2;
			int textY = bounds.y + bounds.height - 4;

			// Draw shadow for readability
			graphics.setColor(Color.BLACK);
			graphics.drawString(tickText, textX + 1, textY + 1);

			// Draw text
			Color textColor = (!isProtected && isUrgent) ? Color.RED : Color.WHITE;
			graphics.setColor(textColor);
			graphics.drawString(tickText, textX, textY);
		}

		return null;
	}
}
