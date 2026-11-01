package com.mob_check;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.util.Optional;

import javax.inject.Inject;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class MobCheckHudOverlay extends Overlay
{
	private final Client client;
	private final MobCheckPlugin plugin;
	private final MobCheckConfig config;
	private final SpriteManager spriteManager;

	private static final Font OVERHEAD_FONT = new Font("Arial", Font.BOLD, 18);
	private static final Stroke RING_STROKE = new BasicStroke(3f);
	private static final float SCREEN_FLASH_WIDTH = 16f;
	private static final int SCREEN_FLASH_INSET = (int) (SCREEN_FLASH_WIDTH / 2);
	private static final Stroke SCREEN_FLASH_STROKE = new BasicStroke(SCREEN_FLASH_WIDTH);
	private static final Color PROGRESS_RING_BG_COLOR = new Color(0, 0, 0, 120);
	private static final Color PROTECTED_COLOR = new Color(0, 255, 60);

	@Inject
	public MobCheckHudOverlay(Client client, MobCheckPlugin plugin, MobCheckConfig config, SpriteManager spriteManager)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.spriteManager = spriteManager;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(Overlay.PRIORITY_HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showOverhead() && !config.flashScreenOnWrongPrayer())
		{
			return null;
		}

		Optional<MobCheckPlugin.AttackState> priorityOpt = plugin.getPriorityAttack();
		if (!priorityOpt.isPresent())
		{
			return null;
		}

		MobCheckPlugin.AttackState priority = priorityOpt.get();
		boolean isProtected = plugin.isPrayerProtected(priority.prayerStyle);
		boolean isUrgent = priority.ticks <= config.warningThreshold();
		Stroke originalStroke = graphics.getStroke();
		Font originalFont = graphics.getFont();

		try
		{
			// 1. Screen Danger Vignette Flash on Wrong Prayer
			if (config.flashScreenOnWrongPrayer() && isUrgent && !isProtected)
			{
				int width = client.getCanvasWidth();
				int height = client.getCanvasHeight();
				if (width > 0 && height > 0)
				{
					graphics.setColor(config.dangerFlashColor());
					graphics.setStroke(SCREEN_FLASH_STROKE);
					graphics.drawRect(SCREEN_FLASH_INSET, SCREEN_FLASH_INSET, width - (SCREEN_FLASH_INSET * 2), height - (SCREEN_FLASH_INSET * 2));
				}
			}

			// 2. Render immediate priority attack above player's head
			if (config.showOverhead())
			{
				BufferedImage sprite = getPrayerSprite(priority.prayerStyle);
				if (sprite != null)
				{
					renderAbovePlayer(graphics, sprite, priority, isProtected, isUrgent);
				}
			}
		}
		finally
		{
			graphics.setStroke(originalStroke);
			graphics.setFont(originalFont);
		}

		return null;
	}

	private void renderAbovePlayer(Graphics2D graphics, BufferedImage sprite, MobCheckPlugin.AttackState attack, boolean isProtected, boolean isUrgent)
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return;
		}

		// Offset slightly to the side of the actual overheads
		Point point = player.getCanvasImageLocation(sprite, player.getLogicalHeight() + 100);
		if (point != null)
		{
			int spriteX = point.getX() - 40;
			int spriteY = point.getY();

			// Draw Tick Progress Arc around icon if enabled
			if (config.showTickProgressRing() && attack.initialTicks > 0)
			{
				int ringSize = Math.max(sprite.getWidth(), sprite.getHeight()) + 8;
				int ringX = spriteX + (sprite.getWidth() - ringSize) / 2;
				int ringY = spriteY + (sprite.getHeight() - ringSize) / 2;

				// Background ring
				graphics.setColor(PROGRESS_RING_BG_COLOR);
				graphics.setStroke(RING_STROKE);
				graphics.drawOval(ringX, ringY, ringSize, ringSize);

				// Active progress arc (progress increases as ticks decrease)
				double progressRatio = (double) attack.ticks / attack.initialTicks;
				int arcAngle = (int) (360.0 * Math.max(0.0, Math.min(1.0, progressRatio)));

				Color ringColor = isProtected ? PROTECTED_COLOR : (isUrgent ? Color.RED : attack.prayerStyle.getColor());
				graphics.setColor(ringColor);
				graphics.drawArc(ringX, ringY, ringSize, ringSize, 90, -arcAngle);
			}

			// Draw prayer sprite
			graphics.drawImage(sprite, spriteX, spriteY, null);

			// Draw tick countdown next to the icon with drop shadow for readability
			String tickText = MobCheckPlugin.formatTicks(attack.ticks);
			int textX = point.getX() - 5;
			int textY = point.getY() + (sprite.getHeight() / 2) + 5;

			graphics.setFont(OVERHEAD_FONT);
			graphics.setColor(Color.BLACK);
			graphics.drawString(tickText, textX + 1, textY + 1);

			Color textColor = isProtected ? PROTECTED_COLOR : (isUrgent ? Color.RED : Color.WHITE);
			graphics.setColor(textColor);
			graphics.drawString(tickText, textX, textY);
		}
	}

	private BufferedImage getPrayerSprite(MobCheckPlugin.PrayerStyle style)
	{
		if (style == null)
		{
			return null;
		}
		return spriteManager.getSprite(style.getSpriteId(), 0);
	}
}
