package com.mob_check;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.InfoBoxComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.util.List;

public class MobCheckOverlay extends Overlay
{
	private final Client client;
	private final MobCheckPlugin plugin;
	private final MobCheckConfig config;
	private final SpriteManager spriteManager;
	private final PanelComponent panelComponent = new PanelComponent();

	private static final Font OVERHEAD_FONT = new Font("Arial", Font.BOLD, 18);
	private static final Stroke RING_STROKE = new BasicStroke(3f);
	private static final Stroke SCREEN_FLASH_STROKE = new BasicStroke(16f);
	private static final Color PROGRESS_RING_BG_COLOR = new Color(0, 0, 0, 120);
	private static final Color PROTECTED_COLOR = new Color(0, 255, 60);
	private static final Color INFOBOX_BG_COLOR = new Color(0, 0, 0, 160);

	@Inject
	public MobCheckOverlay(Client client, MobCheckPlugin plugin, MobCheckConfig config, SpriteManager spriteManager)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.spriteManager = spriteManager;
		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showInfoBox() && !config.showOverhead() && !config.flashScreenOnWrongPrayer())
		{
			return null;
		}

		List<MobCheckPlugin.AttackState> attacks = plugin.getActiveAttacks();
		if (attacks.isEmpty())
		{
			return null;
		}

		MobCheckPlugin.AttackState priority = attacks.get(0);
		boolean isProtected = plugin.isPrayerProtected(priority.prayerStyle);
		boolean isUrgent = priority.ticks <= config.warningThreshold();

		// 1. Screen Danger Vignette Flash on Wrong Prayer (at 1 tick)
		if (config.flashScreenOnWrongPrayer() && isUrgent && !isProtected)
		{
			int width = client.getCanvasWidth();
			int height = client.getCanvasHeight();
			if (width > 0 && height > 0)
			{
				graphics.setColor(config.dangerFlashColor());
				graphics.setStroke(SCREEN_FLASH_STROKE);
				graphics.drawRect(0, 0, width, height);
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

		// 3. Render active attacks in the sidebar info panel
		if (config.showInfoBox())
		{
			panelComponent.getChildren().clear();

			boolean hasManticoreCombo = false;
			for (int i = 0; i < attacks.size(); i++)
			{
				if (attacks.get(i).isManticoreCombo)
				{
					hasManticoreCombo = true;
					break;
				}
			}

			if (hasManticoreCombo && config.showComboSequence())
			{
				panelComponent.getChildren().add(TitleComponent.builder()
					.text("Manticore Combo")
					.color(Color.YELLOW)
					.build());
			}

			int count = 0;
			for (MobCheckPlugin.AttackState attack : attacks)
			{
				if (count >= 4) // Show up to top 4 incoming attacks
				{
					break;
				}
				BufferedImage sprite = getPrayerSprite(attack.prayerStyle);
				if (sprite != null)
				{
					InfoBoxComponent infoBox = new InfoBoxComponent();
					infoBox.setImage(sprite);
					infoBox.setText(attack.ticks + "t");

					boolean itemProtected = plugin.isPrayerProtected(attack.prayerStyle);
					Color textColor;
					if (itemProtected)
					{
						textColor = Color.GREEN;
					}
					else if (attack.ticks <= config.warningThreshold())
					{
						textColor = Color.RED;
					}
					else
					{
						textColor = Color.WHITE;
					}

					infoBox.setColor(textColor);
					infoBox.setBackgroundColor(INFOBOX_BG_COLOR);
					panelComponent.getChildren().add(infoBox);
					count++;
				}
			}
			return panelComponent.render(graphics);
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
		net.runelite.api.Point point = player.getCanvasImageLocation(sprite, player.getLogicalHeight() + 100);
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

			// Draw tick countdown next to the icon
			Color textColor = isProtected ? PROTECTED_COLOR : (isUrgent ? Color.RED : Color.WHITE);
			graphics.setColor(textColor);
			graphics.setFont(OVERHEAD_FONT);
			graphics.drawString(attack.ticks + "t", point.getX() - 5, point.getY() + (sprite.getHeight() / 2) + 5);
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