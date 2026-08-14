package com.mob_check;

import net.runelite.api.Client;
import net.runelite.api.SpriteID;
import net.runelite.api.Player;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.InfoBoxComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.Font;

public class MobCheckOverlay extends Overlay
{
	private final Client client;
	private final MobCheckPlugin plugin;
	private final MobCheckConfig config;
	private final SpriteManager spriteManager;
	private final PanelComponent panelComponent = new PanelComponent();
	private static final Font OVERHEAD_FONT = new Font("Arial", Font.BOLD, 18);

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
		if (!config.showInfoBox() && !config.showOverhead())
		{
			return null;
		}

		panelComponent.getChildren().clear();

		plugin.getPriorityAttack().ifPresent(priority -> {
			BufferedImage sprite = getPrayerSprite(priority.style);

			if (sprite != null)
			{
				// 1. Add to the side panel if enabled
				if (config.showInfoBox())
				{
					InfoBoxComponent infoBox = new InfoBoxComponent();
					infoBox.setImage(sprite);
					infoBox.setText(priority.ticks + "t");
					infoBox.setColor(priority.ticks <= config.warningThreshold() ? Color.RED : Color.WHITE);
					infoBox.setBackgroundColor(new Color(0, 0, 0, 150));
					panelComponent.getChildren().add(infoBox);
				}

				// 2. Render above player's head if enabled
				if (config.showOverhead())
				{
					renderAbovePlayer(graphics, sprite, priority.ticks);
				}
			}
		});

		return config.showInfoBox() ? panelComponent.render(graphics) : null;
	}

	private void renderAbovePlayer(Graphics2D graphics, BufferedImage sprite, int ticks)
	{
		Player player = client.getLocalPlayer();
		if (player == null) return;

		// Offset slightly to the side of the actual overheads
		net.runelite.api.Point point = player.getCanvasImageLocation(sprite, player.getLogicalHeight() + 100);
		if (point != null)
		{
			graphics.drawImage(sprite, point.getX() - 40, point.getY(), null);

			// Draw tick countdown next to the icon
			graphics.setColor(ticks <= config.warningThreshold() ? Color.RED : Color.WHITE);
			graphics.setFont(OVERHEAD_FONT);
			graphics.drawString(ticks + "t", point.getX() - 5, point.getY() + (sprite.getHeight() / 2) + 5);
		}
	}

	private static final int SPRITE_PRAYER_PROTECT_FROM_MAGIC = SpriteID.PRAYER_PROTECT_FROM_MAGIC;
	private static final int SPRITE_PRAYER_PROTECT_FROM_MISSILES = SpriteID.PRAYER_PROTECT_FROM_MISSILES;
	private static final int SPRITE_PRAYER_PROTECT_FROM_MELEE = SpriteID.PRAYER_PROTECT_FROM_MELEE;

	private BufferedImage getPrayerSprite(String style)
	{
		int spriteId;
		switch (style)
		{
			case "Pray Magic": spriteId = SPRITE_PRAYER_PROTECT_FROM_MAGIC; break;
			case "Pray Range": spriteId = SPRITE_PRAYER_PROTECT_FROM_MISSILES; break;
			case "Pray Melee": spriteId = SPRITE_PRAYER_PROTECT_FROM_MELEE; break;
			default: return null;
		}
		return spriteManager.getSprite(spriteId, 0);
	}
}